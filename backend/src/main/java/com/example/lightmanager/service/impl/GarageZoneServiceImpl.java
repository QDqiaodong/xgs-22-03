package com.example.lightmanager.service.impl;

import com.example.lightmanager.dto.ZoneDeleteBlockDTO;
import com.example.lightmanager.entity.GarageZone;
import com.example.lightmanager.exception.BusinessException;
import com.example.lightmanager.exception.ConflictException;
import com.example.lightmanager.exception.ZoneDeleteBlockException;
import com.example.lightmanager.mapper.GarageZoneMapper;
import com.example.lightmanager.service.GarageZoneService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GarageZoneServiceImpl implements GarageZoneService {

    /** 删除阻断明细最多回传条数（灯组/流水），其余以数量概括 */
    private static final int BLOCK_DETAIL_LIMIT = 50;

    private final GarageZoneMapper garageZoneMapper;

    public GarageZoneServiceImpl(GarageZoneMapper garageZoneMapper) {
        this.garageZoneMapper = garageZoneMapper;
    }

    // ==================== 读取：任意深度按排序完整展开 ====================

    @Override
    public List<GarageZone> getZoneTree() {
        List<GarageZone> allZones = garageZoneMapper.selectAllZones();
        fillLightCounts(allZones);
        return buildTree(allZones);
    }

    /**
     * 按 parentId 分组后自顶向下递归装配任意深度：
     * 每一级都按 sortOrder、id 排序；父分区缺失的孤儿节点、意外成环的节点不会丢，
     * 统一兜底为根节点，保证页面不丢节点、不反复加载。
     */
    private List<GarageZone> buildTree(List<GarageZone> zones) {
        Map<Long, GarageZone> byId = new LinkedHashMap<>();
        for (GarageZone z : zones) {
            z.setChildren(null);
            byId.put(z.getId(), z);
        }

        Map<Long, List<GarageZone>> childrenMap = new HashMap<>();
        List<GarageZone> roots = new ArrayList<>();
        for (GarageZone z : zones) {
            Long pid = z.getParentId();
            if (pid != null && pid != 0L && byId.containsKey(pid)) {
                childrenMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(z);
            } else {
                // parentId=0(顶级) 或父分区不存在(孤儿) 都作为根，避免节点丢失
                roots.add(z);
            }
        }
        childrenMap.values().forEach(this::sortZones);
        sortZones(roots);

        // 全局已挂载集合：正常树与脏数据环都不会重复/丢失节点，也不会死循环
        Set<Long> attached = new HashSet<>();
        List<GarageZone> result = new ArrayList<>();
        for (GarageZone root : roots) {
            if (attached.add(root.getId())) {
                attachChildren(root, childrenMap, attached);
                result.add(root);
            }
        }
        // 脏数据兜底：历史成环节点未从任何根到达，把其中未挂载者作为根继续展开
        for (GarageZone z : zones) {
            if (attached.add(z.getId())) {
                attachChildren(z, childrenMap, attached);
                result.add(z);
            }
        }
        return result;
    }

    /** 递归挂载子节点；attached 全局去重，脏数据环不会死循环或重复展示节点 */
    private void attachChildren(GarageZone parent, Map<Long, List<GarageZone>> childrenMap,
                                Set<Long> attached) {
        List<GarageZone> visible = new ArrayList<>();
        for (GarageZone child : childrenMap.getOrDefault(parent.getId(), new ArrayList<>())) {
            if (attached.add(child.getId())) {
                visible.add(child);
            }
        }
        parent.setChildren(visible.isEmpty() ? null : visible);
        for (GarageZone child : visible) {
            attachChildren(child, childrenMap, attached);
        }
    }

    private void sortZones(List<GarageZone> zones) {
        zones.sort((a, b) -> {
            int sa = a.getSortOrder() == null ? 0 : a.getSortOrder();
            int sb = b.getSortOrder() == null ? 0 : b.getSortOrder();
            if (sa != sb) {
                return Integer.compare(sa, sb);
            }
            return Long.compare(a.getId(), b.getId());
        });
    }

    @Override
    public GarageZone getZoneById(Long id) {
        return garageZoneMapper.selectById(id);
    }

    // ==================== 新增 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GarageZone createZone(GarageZone zone) {
        requireText(zone.getZoneCode(), "分区编码");
        requireText(zone.getZoneName(), "分区名称");
        // 层级变更串行化：取全表最新数据并加锁
        List<GarageZone> all = garageZoneMapper.selectAllZonesForUpdate();
        Map<Long, GarageZone> byId = indexById(all);

        long parentId = normalizeParentId(zone.getParentId());
        if (parentId != 0L && !byId.containsKey(parentId)) {
            throw new BusinessException("所选上级分区不存在或已被删除，请刷新后重试");
        }
        for (GarageZone z : all) {
            if (z.getZoneCode().equals(zone.getZoneCode().trim())) {
                throw new BusinessException("分区编码已存在：" + zone.getZoneCode().trim());
            }
        }

        zone.setZoneCode(zone.getZoneCode().trim());
        zone.setZoneName(zone.getZoneName().trim());
        zone.setParentId(parentId);
        zone.setLevel(parentId == 0L ? 1 : byId.get(parentId).getLevel() + 1);
        if (zone.getSortOrder() == null) {
            zone.setSortOrder(0);
        }
        if (zone.getStatus() == null) {
            zone.setStatus(1);
        }
        zone.setVersion(0);
        garageZoneMapper.insert(zone);
        return zone;
    }

    // ==================== 编辑 / 换父级（乐观锁 + 最新层级环校验 + 子树原子移动） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GarageZone updateZone(GarageZone request) {
        if (request.getId() == null) {
            throw new BusinessException("分区ID不能为空");
        }
        requireText(request.getZoneCode(), "分区编码");
        requireText(request.getZoneName(), "分区名称");
        if (request.getVersion() == null) {
            throw new BusinessException("缺少分区版本号，请刷新分区树后重试");
        }

        // 全表行锁串行化层级变更：以下环校验、版本判定与写入在同一事务内基于最新已提交关系完成
        List<GarageZone> all = garageZoneMapper.selectAllZonesForUpdate();
        Map<Long, GarageZone> byId = indexById(all);

        GarageZone current = byId.get(request.getId());
        if (current == null) {
            throw new BusinessException("分区不存在或已被删除，请刷新后重试");
        }

        // 乐观锁：持过期树的后确认者不得覆盖较早结果
        if (!request.getVersion().equals(current.getVersion())) {
            throw new ConflictException(
                    "分区「" + current.getZoneName() + "」刚被其他管理员调整过，您看到的是过期层级；"
                            + "已为您刷新为最新父子关系，请确认后再决定是否重试",
                    current);
        }

        String code = request.getZoneCode().trim();
        String name = request.getZoneName().trim();
        for (GarageZone z : all) {
            if (!z.getId().equals(request.getId()) && z.getZoneCode().equals(code)) {
                throw new BusinessException("分区编码已存在：" + code);
            }
        }

        long newParentId = normalizeParentId(request.getParentId());
        long oldParentId = current.getParentId() == null ? 0L : current.getParentId();

        // 先按当前（已锁定的最新）层级固定自身全部后代：
        // 既用于"挂到后代会成环"校验，也作为换父级后原子重算子树层级的快照；
        // CAS 更新会改写被移动节点的 parentId，之后再回溯就找不到原来的后代了。
        List<GarageZone> descendants = collectSubtree(request.getId(), byId);

        if (newParentId != 0L) {
            GarageZone newParent = byId.get(newParentId);
            if (newParent == null) {
                throw new BusinessException("所选上级分区不存在或已被删除，请刷新后重试");
            }
            // 自挂
            if (newParentId == request.getId()) {
                throw new BusinessException("不能把分区「" + name + "」挂到自身下面，上级分区不能选择自己");
            }
            // 挂到任意后代（会形成环）—— 基于刚锁定的最新层级判定，冲突分区具体点名
            for (GarageZone d : descendants) {
                if (d.getId().equals(newParentId)) {
                    throw new BusinessException("不能把分区「" + name + "」挂到它自己的下级分区「"
                            + newParent.getZoneName() + "」下面，该调整会形成循环层级");
                }
            }
        }

        int newLevel = newParentId == 0L ? 1 : byId.get(newParentId).getLevel() + 1;
        Integer sortOrder = request.getSortOrder() == null ? 0 : request.getSortOrder();
        Integer status = request.getStatus() == null ? current.getStatus() : request.getStatus();

        GarageZone update = new GarageZone();
        update.setId(current.getId());
        update.setZoneCode(code);
        update.setZoneName(name);
        update.setParentId(newParentId);
        update.setLevel(newLevel);
        update.setSortOrder(sortOrder);
        update.setStatus(status);
        update.setVersion(request.getVersion());

        // CAS 双保险：版本号已变则 0 行更新
        int affected = garageZoneMapper.casUpdateZone(update);
        if (affected == 0) {
            GarageZone latest = garageZoneMapper.selectById(request.getId());
            throw new ConflictException(
                    "分区「" + (latest != null ? latest.getZoneName() : name)
                            + "」刚被其他管理员调整过，您看到的是过期层级；已刷新为最新父子关系，请确认后再重试",
                    latest);
        }

        // 换父级：仅重算子树层级并推进版本，绝不触碰 light_group.zone_id，
        // 子树整体随目录移动，灯组仍归属各自原分区，不发生隐式划转。
        if (newParentId != oldParentId) {
            // 锁定快照里各节点仍是旧 level，用 override 记录本事务内已算出的新层级；
            // collectSubtree 按层序（父先于子）输出，顺序处理即可，无需再排序
            Map<Long, Integer> newLevels = new HashMap<>();
            newLevels.put(current.getId(), newLevel);
            List<Long> bumpedIds = new ArrayList<>();
            for (GarageZone node : descendants) {
                Long pid = node.getParentId() == null ? 0L : node.getParentId();
                Integer parentLevel = newLevels.get(pid);
                if (parentLevel == null) {
                    GarageZone parent = byId.get(pid);
                    parentLevel = parent == null ? 0 : parent.getLevel();
                }
                int nodeLevel = parentLevel + 1;
                garageZoneMapper.updateLevel(node.getId(), nodeLevel);
                newLevels.put(node.getId(), nodeLevel);
                bumpedIds.add(node.getId());
            }
            // 子树全部节点版本 +1：并发编辑这些节点的顺便推进这些节点的版本号，
            // 使持有旧树并发编辑子树节点的管理员同样收到过期提示
            if (!bumpedIds.isEmpty()) {
                garageZoneMapper.bumpVersionByIds(bumpedIds);
            }
        }

        return garageZoneMapper.selectById(request.getId());
    }

    // ==================== 删除：仅空叶子，逐条列出阻断原因 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteZone(Long id) {
        List<GarageZone> all = garageZoneMapper.selectAllZonesForUpdate();
        Map<Long, GarageZone> byId = indexById(all);
        GarageZone target = byId.get(id);
        if (target == null) {
            throw new BusinessException("分区不存在或已被删除");
        }

        List<ZoneDeleteBlockDTO> blocks = new ArrayList<>();

        // 1. 仍有子分区（含禁用）直接阻断：只有叶子可删
        List<ZoneDeleteBlockDTO> childBlocks = garageZoneMapper.selectChildZoneBlocks(id);
        blocks.addAll(childBlocks);

        // 2. 本分区或任意后代下仍存在灯组
        List<GarageZone> subtree = collectSubtree(id, byId);
        List<Long> subtreeIds = new ArrayList<>();
        subtreeIds.add(id);
        subtree.forEach(z -> subtreeIds.add(z.getId()));
        List<ZoneDeleteBlockDTO> lightBlocks = garageZoneMapper.selectLightGroupBlocks(subtreeIds, BLOCK_DETAIL_LIMIT);
        blocks.addAll(lightBlocks);

        // 3. 本分区或任意后代仍被未核销划转流水引用
        List<ZoneDeleteBlockDTO> ledgerBlocks = garageZoneMapper.selectOpenLedgerBlocks(subtreeIds, BLOCK_DETAIL_LIMIT);
        blocks.addAll(ledgerBlocks);

        if (!blocks.isEmpty()) {
            List<String> parts = new ArrayList<>();
            if (!childBlocks.isEmpty()) {
                parts.add("仍存在子分区");
            }
            if (!lightBlocks.isEmpty()) {
                parts.add("本分区或后代下存在灯组引用" + (lightBlocks.size() >= BLOCK_DETAIL_LIMIT
                        ? "（仅列出前" + BLOCK_DETAIL_LIMIT + "条）" : ""));
            }
            if (!ledgerBlocks.isEmpty()) {
                parts.add("存在未核销划转流水" + (ledgerBlocks.size() >= BLOCK_DETAIL_LIMIT
                        ? "（仅列出前" + BLOCK_DETAIL_LIMIT + "条）" : ""));
            }
            String summary = String.join("、", parts);
            throw new ZoneDeleteBlockException(
                    "分区「" + target.getZoneName() + "」不是空叶子分区，删除被阻断：" + summary, blocks);
        }

        garageZoneMapper.deleteById(id);
    }

    // ==================== 列表 ====================

    @Override
    public List<GarageZone> getAllZones() {
        List<GarageZone> zones = garageZoneMapper.selectAllZones();
        fillLightCounts(zones);
        return zones;
    }

    // ==================== 公共辅助 ====================

    /** 分区列表共同显示已归档：每个分区灯组总数与已归档数 */
    private void fillLightCounts(List<GarageZone> zones) {
        Map<Long, int[]> counts = new HashMap<>();
        for (Map<String, Object> row : garageZoneMapper.selectZoneLightCounts()) {
            Object zoneIdObj = row.get("zoneId");
            if (zoneIdObj == null) {
                continue;
            }
            long zoneId = ((Number) zoneIdObj).longValue();
            int total = row.get("totalCount") == null ? 0 : ((Number) row.get("totalCount")).intValue();
            int archived = row.get("archivedCount") == null ? 0 : ((Number) row.get("archivedCount")).intValue();
            counts.put(zoneId, new int[]{total, archived});
        }
        for (GarageZone zone : zones) {
            int[] c = counts.getOrDefault(zone.getId(), new int[]{0, 0});
            zone.setLightCount(c[0]);
            zone.setArchivedLightCount(c[1]);
        }
    }

    private Map<Long, GarageZone> indexById(List<GarageZone> zones) {
        Map<Long, GarageZone> map = new LinkedHashMap<>();
        for (GarageZone z : zones) {
            map.put(z.getId(), z);
        }
        return map;
    }

    /** 基于给定（已锁定的最新）层级收集某节点全部后代，不包含自身；带环保护 */
    private List<GarageZone> collectSubtree(Long rootId, Map<Long, GarageZone> byId) {
        List<GarageZone> result = new ArrayList<>();
        // parentId -> children
        Map<Long, List<GarageZone>> childrenMap = new HashMap<>();
        for (GarageZone z : byId.values()) {
            Long pid = z.getParentId();
            if (pid != null && pid != 0L) {
                childrenMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(z);
            }
        }
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(rootId);
        Set<Long> seen = new HashSet<>();
        seen.add(rootId);
        while (!stack.isEmpty()) {
            Long pid = stack.pop();
            for (GarageZone child : childrenMap.getOrDefault(pid, new ArrayList<>())) {
                if (seen.add(child.getId())) {
                    result.add(child);
                    stack.push(child.getId());
                }
            }
        }
        return result;
    }

    private long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(field + "不能为空");
        }
    }
}
