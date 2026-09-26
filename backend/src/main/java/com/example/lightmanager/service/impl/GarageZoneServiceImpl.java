package com.example.lightmanager.service.impl;

import com.example.lightmanager.entity.GarageZone;
import com.example.lightmanager.entity.LightGroup;
import com.example.lightmanager.dto.ZoneReferenceBlockDTO;
import com.example.lightmanager.exception.BusinessException;
import com.example.lightmanager.exception.ConflictException;
import com.example.lightmanager.exception.ZoneReferenceBlockException;
import com.example.lightmanager.mapper.BatchTransferLedgerMapper;
import com.example.lightmanager.mapper.GarageZoneMapper;
import com.example.lightmanager.mapper.LightGroupMapper;
import com.example.lightmanager.service.GarageZoneService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GarageZoneServiceImpl implements GarageZoneService {

    private final GarageZoneMapper garageZoneMapper;
    private final LightGroupMapper lightGroupMapper;
    private final BatchTransferLedgerMapper batchTransferLedgerMapper;
    private final ZoneUpdateExecutor zoneUpdateExecutor;

    public GarageZoneServiceImpl(GarageZoneMapper garageZoneMapper,
                                 LightGroupMapper lightGroupMapper,
                                 BatchTransferLedgerMapper batchTransferLedgerMapper,
                                 ZoneUpdateExecutor zoneUpdateExecutor) {
        this.garageZoneMapper = garageZoneMapper;
        this.lightGroupMapper = lightGroupMapper;
        this.batchTransferLedgerMapper = batchTransferLedgerMapper;
        this.zoneUpdateExecutor = zoneUpdateExecutor;
    }

    @Override
    public List<GarageZone> getZoneTree() {
        List<GarageZone> allZones = garageZoneMapper.selectAllZones();
        fillLightCounts(allZones);
        return buildTree(allZones);
    }

    /**
     * 按任意深度递归装配树：
     * - 每层都按 sort_order、id 排序，深层级分区也会完整挂在各自父节点下（修复三级及更深丢失）；
     * - parent 不存在 / 已禁用导致挂不上的孤儿节点上提为根，保证节点不丢；
     * - visited 防御历史脏数据中的环，遇到回边即断开，保证不出现反复加载。
     */
    private List<GarageZone> buildTree(List<GarageZone> zones) {
        Map<Long, List<GarageZone>> childrenMap = new HashMap<>();
        List<GarageZone> declaredRoots = new ArrayList<>();

        for (GarageZone z : zones) {
            z.setChildren(null);
            Long pid = z.getParentId();
            if (pid == null || pid == 0L) {
                declaredRoots.add(z);
            } else {
                childrenMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(z);
            }
        }

        Set<Long> attached = new HashSet<>();
        List<GarageZone> roots = new ArrayList<>();

        // 先递归挂载每个声明的根；attach 返回该子树挂载到的全部节点 id
        for (GarageZone root : declaredRoots) {
            attachChildren(root, childrenMap, attached);
            roots.add(root);
        }

        // 父节点缺失/禁用的孤儿，以及历史环数据中未被任何根访问到的节点，上提为根，避免丢节点
        for (GarageZone z : zones) {
            if (!attached.contains(z.getId())) {
                attachChildren(z, childrenMap, attached);
                roots.add(z);
            }
        }

        sortNodes(roots);
        return roots;
    }

    private void attachChildren(GarageZone node, Map<Long, List<GarageZone>> childrenMap, Set<Long> attached) {
        // 已挂载过说明出现了环或多父引用：断开此回边，不重复挂第二次
        if (!attached.add(node.getId())) {
            return;
        }
        List<GarageZone> children = childrenMap.get(node.getId());
        if (children == null || children.isEmpty()) {
            return;
        }
        List<GarageZone> attachedChildren = new ArrayList<>();
        for (GarageZone child : children) {
            if (attached.contains(child.getId())) {
                continue;
            }
            attachedChildren.add(child);
            attachChildren(child, childrenMap, attached);
        }
        node.setChildren(attachedChildren);
    }

    private void sortNodes(List<GarageZone> nodes) {
        nodes.sort(Comparator
                .comparing((GarageZone z) -> z.getSortOrder() == null ? 0 : z.getSortOrder())
                .thenComparing(GarageZone::getId));
        for (GarageZone node : nodes) {
            if (node.getChildren() != null) {
                sortNodes(node.getChildren());
            }
        }
    }

    @Override
    public GarageZone getZoneById(Long id) {
        return garageZoneMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GarageZone createZone(GarageZone zone) {
        requireText(zone.getZoneCode(), "分区编码");
        requireText(zone.getZoneName(), "分区名称");
        if (zone.getSortOrder() == null) {
            zone.setSortOrder(0);
        }
        if (zone.getStatus() == null) {
            zone.setStatus(1);
        }
        zone.setId(null);
        Long parentId = zone.getParentId();
        if (parentId == null || parentId == 0L) {
            zone.setParentId(0L);
            zone.setLevel(1);
        } else {
            GarageZone parent = garageZoneMapper.selectById(parentId);
            if (parent == null) {
                throw new BusinessException("上级分区不存在或已被删除");
            }
            zone.setLevel(parent.getLevel() + 1);
        }
        zone.setVersion(0);
        garageZoneMapper.insert(zone);
        return garageZoneMapper.selectById(zone.getId());
    }

    @Override
    public GarageZone updateZone(GarageZone zone) {
        if (zone.getId() == null) {
            throw new BusinessException("分区ID不能为空");
        }
        requireText(zone.getZoneCode(), "分区编码");
        requireText(zone.getZoneName(), "分区名称");
        if (zone.getVersion() == null) {
            // 未携带版本号视为基于初始版本的过期表单，直接走版本冲突，禁止无锁覆盖
            zone.setVersion(0);
        }
        try {
            zoneUpdateExecutor.execute(zone);
        } catch (ZoneUpdateExecutor.ZoneVersionConflict e) {
            // 事务已回滚、锁已释放，此时重读获胜方已提交的最新分区与最新树返回给后确认者
            GarageZone latest = garageZoneMapper.selectById(e.getZoneId());
            if (latest == null) {
                throw new BusinessException("分区不存在或已被删除，请刷新分区树");
            }
            fillLightCounts(List.of(latest));
            List<GarageZone> latestTree = getZoneTree();
            throw new ConflictException(buildVersionConflictMessage(latest),
                    new com.example.lightmanager.dto.ZoneConflictPayload(latest, latestTree));
        }
        return garageZoneMapper.selectById(zone.getId());
    }

    private String buildVersionConflictMessage(GarageZone latest) {
        return "分区「" + latest.getZoneName()
                + "」刚被其他管理员调整过（当前上级或层级已变化），您的确认基于过期版本，未生效。"
                + "已为您刷新为最新父子关系，请核对后再决定是否重试";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteZone(Long id) {
        // 锁定全部分区行，与并发换父级 / 删除互斥，保证引用检查与删除之间层级不会被改动
        List<GarageZone> all = garageZoneMapper.selectAllZonesForUpdate();
        GarageZone target = all.stream()
                .filter(z -> z.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new BusinessException("分区不存在或已被删除"));

        // 层级判断含禁用分区：禁用的子分区仍是子分区
        Map<Long, List<GarageZone>> childrenMap = new HashMap<>();
        Map<Long, GarageZone> zoneMap = new HashMap<>();
        for (GarageZone z : all) {
            zoneMap.put(z.getId(), z);
            if (z.getParentId() != null && z.getParentId() != 0L) {
                childrenMap.computeIfAbsent(z.getParentId(), k -> new ArrayList<>()).add(z);
            }
        }

        List<Long> subtreeIds = collectSubtreeIds(id, childrenMap);
        List<ZoneReferenceBlockDTO> blocks = new ArrayList<>();

        // 1) 仍有子分区（含多层后代）：仅空叶子可删，逐条列出阻断的子分区
        for (GarageZone child : childrenMap.getOrDefault(id, List.of())) {
            blocks.add(new ZoneReferenceBlockDTO(
                    "CHILD_ZONE", "子分区", child.getId(), child.getZoneCode(),
                    child.getId(), child.getZoneName(), null, null,
                    "分区「" + target.getZoneName() + "」下仍存在子分区「" + child.getZoneName()
                            + "」，请先调整或删除其子分区后再删除",
                    null));
        }

        // 2) 本分区或任意后代仍存在灯组（含已归档）：列出归属分区与灯组编号
        List<LightGroup> groups = lightGroupMapper.selectByZoneIds(subtreeIds);
        Map<Long, List<LightGroup>> groupsByZone = groups.stream()
                .collect(Collectors.groupingBy(LightGroup::getZoneId));
        for (Long zoneId : subtreeIds) {
            List<LightGroup> zoneGroups = groupsByZone.getOrDefault(zoneId, List.of());
            GarageZone owner = zoneMap.get(zoneId);
            String ownerName = owner != null ? owner.getZoneName() : String.valueOf(zoneId);
            for (LightGroup lg : zoneGroups) {
                String statusText = "ARCHIVED".equals(lg.getArchiveStatus()) ? "已归档" : "在用";
                blocks.add(new ZoneReferenceBlockDTO(
                        "LIGHT_GROUP", "灯组引用", lg.getId(), lg.getGroupCode(),
                        zoneId, ownerName, lg.getArchiveStatus(), statusText,
                        "分区「" + ownerName + "」下仍存在灯组「" + lg.getGroupCode()
                                + "」（" + statusText + "），请先将灯组划转或处理后再删除分区",
                        null));
            }
        }

        // 3) 本分区或任意后代仍有未核销划转流水（源/目标任一引用）
        List<ZoneReferenceBlockDTO> ledgerBlocks =
                batchTransferLedgerMapper.selectOpenTransferBlocksByZoneIds(subtreeIds);
        blocks.addAll(ledgerBlocks);

        if (!blocks.isEmpty()) {
            throw new ZoneReferenceBlockException(
                    "分区「" + target.getZoneName() + "」不是可删除的空叶子分区，存在 "
                            + blocks.size() + " 项阻断引用，请逐项处理后再删除",
                    blocks);
        }

        garageZoneMapper.deleteById(id);
    }

    @Override
    public List<GarageZone> getAllZones() {
        List<GarageZone> zones = garageZoneMapper.selectAllZones();
        fillLightCounts(zones);
        return zones;
    }

    /** 收集分区及其全部后代 id（含自身，BFS，visited 防御历史环数据） */
    private List<Long> collectSubtreeIds(Long rootId, Map<Long, List<GarageZone>> childrenMap) {
        List<Long> ids = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        visited.add(rootId);
        ids.add(rootId);
        int head = 0;
        while (head < ids.size()) {
            Long cur = ids.get(head++);
            for (GarageZone child : childrenMap.getOrDefault(cur, List.of())) {
                if (visited.add(child.getId())) {
                    ids.add(child.getId());
                }
            }
        }
        return ids;
    }

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

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(field + "不能为空");
        }
    }
}
