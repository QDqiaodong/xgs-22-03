package com.example.lightmanager.service.impl;

import com.example.lightmanager.entity.GarageZone;
import com.example.lightmanager.exception.BusinessException;
import com.example.lightmanager.exception.ZoneHierarchyConflictException;
import com.example.lightmanager.mapper.GarageZoneMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 分区编辑 / 换父级的事务执行器。
 *
 * 在单个事务、单连接内完成：
 * 1) 锁定被编辑分区与（若更换父级）新父分区，串行化并发的层级调整，
 *    从根上杜绝两个管理员同时移动而相互成环；
 * 2) 基于锁后读到的最新全量层级做环检测（自挂 / 挂到任意后代）；
 * 3) 版本号 CAS 更新当前节点；
 * 4) 换父级时在同一事务内整体平移所有后代的 level。
 *
 * CAS 失败或校验失败一律抛出标记异常并回滚，绝不留下“父级已改、层级未刷”的半棵子树；
 * 最新数据的重读由外层在事务结束、锁与连接释放后进行（避免同事务占用第二条连接）。
 */
@Component
public class ZoneUpdateExecutor {

    /** CAS 未命中：客户端版本已过期，事务回滚，外层重读最新树并转 409 */
    public static class ZoneVersionConflict extends RuntimeException {
        private final Long zoneId;
        public ZoneVersionConflict(Long zoneId) {
            super("ZONE_VERSION_CONFLICT");
            this.zoneId = zoneId;
        }
        public Long getZoneId() { return zoneId; }
    }

    public static class Result {
        /** 移动节点新的层级（用于外层提示），非移动时为空 */
        private final boolean parentChanged;
        private final Integer newLevel;
        /** 被移动的整棵子树节点 id（含自身），用于整体平移层级 */
        private final List<Long> shiftedIds;

        public Result(boolean parentChanged, Integer newLevel, List<Long> shiftedIds) {
            this.parentChanged = parentChanged;
            this.newLevel = newLevel;
            this.shiftedIds = shiftedIds;
        }

        public boolean isParentChanged() { return parentChanged; }
        public Integer getNewLevel() { return newLevel; }
        public List<Long> getShiftedIds() { return shiftedIds; }
    }

    private final GarageZoneMapper garageZoneMapper;

    public ZoneUpdateExecutor(GarageZoneMapper garageZoneMapper) {
        this.garageZoneMapper = garageZoneMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Result execute(GarageZone request) {
        // 1) 锁定全部分区行，把并发的层级调整串行化：
        //    仅锁“当前节点+新父节点”防不住跨子树的并发互挂（深层移动仍可能相互成环），
        //    分区表规模小，整体加写锁即可从根上杜绝；普通树查询不加锁，不受阻塞。
        Long zoneId = request.getId();
        Long newParentId = normalizeParentId(request.getParentId());
        List<GarageZone> all = garageZoneMapper.selectAllZonesForUpdate();
        Map<Long, List<GarageZone>> childrenMap = new HashMap<>();
        for (GarageZone z : all) {
            if (z.getParentId() != null && z.getParentId() != 0L) {
                childrenMap.computeIfAbsent(z.getParentId(), k -> new ArrayList<>()).add(z);
            }
        }

        GarageZone current = all.stream()
                .filter(z -> z.getId().equals(zoneId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("分区不存在或已被删除"));
        GarageZone targetParent = null;
        if (newParentId != null) {
            targetParent = all.stream()
                    .filter(z -> z.getId().equals(newParentId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("目标上级分区不存在或已被删除"));
        }

        // 2) 环检测基于这份锁后最新层级（含禁用节点）
        Long oldParentId = normalizeParentId(current.getParentId());
        boolean parentChanged = newParentId == null ? oldParentId != null : !newParentId.equals(oldParentId);

        // 3) 环检测：自挂、挂到自身任意后代（等价于一切成环调整），并给出具体冲突分区
        if (parentChanged) {
            if (newParentId != null && newParentId.equals(zoneId)) {
                throw new ZoneHierarchyConflictException(
                        "不能把分区「" + current.getZoneName() + "」的上级设置为它自身，否则会形成循环层级");
            }
            List<GarageZone> descendants = collectSubtree(zoneId, childrenMap);
            for (GarageZone descendant : descendants) {
                if (descendant.getId().equals(newParentId)) {
                    throw new ZoneHierarchyConflictException(
                            "不能把分区「" + current.getZoneName() + "」挂到它的下级分区「"
                                    + descendant.getZoneName()
                                    + "」下，否则会形成循环层级；请选择本分区及其后代之外的分区作为上级");
                }
            }
        }

        // 4) 计算新层级：根为 1，非根为父层级 + 1
        int newLevel = targetParent != null ? targetParent.getLevel() + 1 : 1;

        // 5) 组装 CAS 更新（version 必须与读取时一致），只更新客户端提交的非空字段
        GarageZone update = new GarageZone();
        update.setId(zoneId);
        update.setVersion(request.getVersion());
        update.setZoneCode(request.getZoneCode());
        update.setZoneName(request.getZoneName());
        update.setSortOrder(request.getSortOrder());
        update.setStatus(request.getStatus());
        if (parentChanged) {
            update.setParentId(newParentId == null ? 0L : newParentId);
            update.setLevel(newLevel);
        }
        int affected = garageZoneMapper.casUpdateZone(update);
        if (affected == 0) {
            // 版本过期：另一位管理员已先确认写入，放弃本次更新并回滚
            throw new ZoneVersionConflict(zoneId);
        }

        // 6) 换父级：同一事务内整棵子树平移层级，全部成功或随主更新一起回滚。
        //    只改分区 level，不触碰任何灯组的 zone_id——灯组始终归属各自原分区。
        List<Long> shiftedIds = List.of();
        if (parentChanged) {
            int delta = newLevel - current.getLevel();
            List<GarageZone> descendants = collectSubtree(zoneId, childrenMap);
            if (delta != 0 && !descendants.isEmpty()) {
                shiftedIds = descendants.stream().map(GarageZone::getId).toList();
                garageZoneMapper.shiftSubtreeLevels(shiftedIds, delta);
            }
        }
        return new Result(parentChanged, newLevel, shiftedIds);
    }

    /**
     * 从 rootId 出发收集全部后代（不含自身）。childrenMap 来自最新全量数据；
     * visited 防御历史脏数据中已存在的环，保证不会死循环。
     */
    private List<GarageZone> collectSubtree(Long rootId, Map<Long, List<GarageZone>> childrenMap) {
        List<GarageZone> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        visited.add(rootId);
        List<Long> queue = new ArrayList<>();
        queue.add(rootId);
        int head = 0;
        while (head < queue.size()) {
            Long cur = queue.get(head++);
            for (GarageZone child : childrenMap.getOrDefault(cur, List.of())) {
                if (visited.add(child.getId())) {
                    result.add(child);
                    queue.add(child.getId());
                }
            }
        }
        return result;
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId == 0L ? null : parentId;
    }
}
