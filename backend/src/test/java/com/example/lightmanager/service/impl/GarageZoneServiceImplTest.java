package com.example.lightmanager.service.impl;

import com.example.lightmanager.dto.ZoneDeleteBlockDTO;
import com.example.lightmanager.entity.GarageZone;
import com.example.lightmanager.exception.BusinessException;
import com.example.lightmanager.exception.ConflictException;
import com.example.lightmanager.exception.ZoneDeleteBlockException;
import com.example.lightmanager.mapper.GarageZoneMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 分区层级核心规则单元测试：
 * 任意深度装配排序、自挂/挂后代环防护、版本冲突、子树层级原子重算、删除引用保护。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GarageZoneServiceImplTest {

    @Mock
    private GarageZoneMapper mapper;

    private GarageZoneServiceImpl service;

    private final Map<Long, GarageZone> db = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new GarageZoneServiceImpl(mapper);
        db.clear();
        when(mapper.selectAllZonesForUpdate()).thenAnswer(inv -> db.values().stream()
                .map(GarageZoneServiceImplTest::cloneZone).toList());
        when(mapper.selectById(any())).thenAnswer(inv -> {
            GarageZone z = db.get(inv.getArgument(0));
            return z == null ? null : cloneZone(z);
        });
        when(mapper.selectZoneLightCounts()).thenReturn(Collections.emptyList());
        when(mapper.casUpdateZone(any())).thenAnswer(inv -> {
            GarageZone req = inv.getArgument(0);
            GarageZone cur = db.get(req.getId());
            if (cur == null || !cur.getVersion().equals(req.getVersion())) {
                return 0;
            }
            cur.setZoneCode(req.getZoneCode());
            cur.setZoneName(req.getZoneName());
            cur.setParentId(req.getParentId());
            cur.setLevel(req.getLevel());
            cur.setSortOrder(req.getSortOrder());
            cur.setStatus(req.getStatus());
            cur.setVersion(cur.getVersion() + 1);
            return 1;
        });
        org.mockito.Mockito.lenient().when(mapper.updateLevel(any(), anyInt())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            Integer level = inv.getArgument(1);
            GarageZone z = db.get(id);
            if (z != null) {
                z.setLevel(level);
            }
            return 1;
        });
        org.mockito.Mockito.lenient().when(mapper.bumpVersionByIds(anyList())).thenAnswer(inv -> {
            List<Long> ids = inv.getArgument(0);
            ids.forEach(id -> {
                GarageZone z = db.get(id);
                if (z != null) {
                    z.setVersion(z.getVersion() + 1);
                }
            });
            return ids.size();
        });
        org.mockito.Mockito.lenient().when(mapper.selectChildZoneBlocks(any())).thenAnswer(inv -> {
            Long pid = inv.getArgument(0);
            List<ZoneDeleteBlockDTO> blocks = new ArrayList<>();
            db.values().stream()
                    .filter(z -> pid.equals(z.getParentId()))
                    .forEach(z -> blocks.add(new ZoneDeleteBlockDTO(
                            "CHILD_ZONE", "子分区", z.getId(), z.getZoneName(),
                            z.getZoneCode(), "仍存在子分区")));
            return blocks;
        });
    }

    private GarageZone zone(long id, String name, long parentId, int level, int sort) {
        GarageZone z = new GarageZone();
        z.setId(id);
        z.setZoneCode("Z" + id);
        z.setZoneName(name);
        z.setParentId(parentId);
        z.setLevel(level);
        z.setSortOrder(sort);
        z.setStatus(1);
        z.setVersion(0);
        return z;
    }

    private void put(GarageZone z) {
        db.put(z.getId(), z);
    }

    /** 模拟数据库读：返回独立副本，业务代码对快照的内存修改不会污染"已提交数据" */
    private static GarageZone cloneZone(GarageZone z) {
        GarageZone c = new GarageZone();
        c.setId(z.getId());
        c.setZoneCode(z.getZoneCode());
        c.setZoneName(z.getZoneName());
        c.setParentId(z.getParentId());
        c.setLevel(z.getLevel());
        c.setSortOrder(z.getSortOrder());
        c.setStatus(z.getStatus());
        c.setVersion(z.getVersion());
        return c;
    }

    // ===== 1. 任意深度按排序完整展开 =====

    @Test
    void getZoneTree_assemblesFourLevelsAndKeepsOrder() {
        // 四级树：R 下 A、B 两个子树（用 sort_order 验证同级排序）
        put(zone(1, "R", 0, 1, 1));
        put(zone(2, "B", 1, 2, 2));
        put(zone(3, "A", 1, 2, 1));
        put(zone(4, "B1", 2, 3, 1));
        put(zone(5, "A2", 3, 3, 2));
        put(zone(6, "A1", 3, 3, 1));
        put(zone(7, "A1x", 6, 4, 1));
        when(mapper.selectAllZones()).thenAnswer(inv -> new ArrayList<>(db.values()));

        List<GarageZone> roots = service.getZoneTree();

        assertThat(roots).hasSize(1);
        GarageZone r = roots.get(0);
        assertThat(r.getZoneName()).isEqualTo("R");
        // 第二级按 sort_order：A(1) 在 B(2) 前
        assertThat(r.getChildren()).extracting(GarageZone::getZoneName).containsExactly("A", "B");
        GarageZone a = r.getChildren().get(0);
        assertThat(a.getChildren()).extracting(GarageZone::getZoneName).containsExactly("A1", "A2");
        GarageZone a1 = a.getChildren().get(0);
        // 第四级必须出现在树中（旧实现三级以下会丢）
        assertThat(a1.getChildren()).extracting(GarageZone::getZoneName).containsExactly("A1x");
    }

    @Test
    void getZoneTree_orphanNodeIsShownAsRootInsteadOfDropped() {
        // 父分区 99 不存在：孤儿节点不能丢
        put(zone(1, "Root", 0, 1, 1));
        put(zone(2, "Orphan", 99, 2, 1));
        when(mapper.selectAllZones()).thenAnswer(inv -> db.values().stream()
                .map(GarageZoneServiceImplTest::cloneZone).toList());

        List<GarageZone> roots = service.getZoneTree();

        assertThat(roots).extracting(GarageZone::getZoneName).containsExactlyInAnyOrder("Root", "Orphan");
    }

    @Test
    void getZoneTree_persistedCycleDoesNotLoseNodesOrLoopForever() {
        // 历史脏数据：3 -> 4 -> 3 自成环且没有根；读取必须不丢节点、不死循环
        put(zone(1, "Root", 0, 1, 1));
        put(zone(3, "CyA", 4, 2, 1));
        put(zone(4, "CyB", 3, 2, 1));
        when(mapper.selectAllZones()).thenAnswer(inv -> db.values().stream()
                .map(GarageZoneServiceImplTest::cloneZone).toList());

        List<GarageZone> roots = service.getZoneTree();

        // 环上节点必须出现在树里（某个环节点被提升为根，其余作为其子节点）
        List<String> allNames = new ArrayList<>();
        collectNames(roots, allNames);
        assertThat(allNames).containsExactlyInAnyOrder("Root", "CyA", "CyB");
        // 每个节点恰好出现一次，不重复挂载
        assertThat(countNodes(roots)).isEqualTo(3);
    }

    private void collectNames(List<GarageZone> nodes, List<String> acc) {
        for (GarageZone z : nodes) {
            acc.add(z.getZoneName());
            if (z.getChildren() != null) {
                collectNames(z.getChildren(), acc);
            }
        }
    }

    private int countNodes(List<GarageZone> nodes) {
        int n = 0;
        for (GarageZone z : nodes) {
            n++;
            if (z.getChildren() != null) {
                n += countNodes(z.getChildren());
            }
        }
        return n;
    }

    // ===== 2. 换父级：自挂 / 挂后代形成环 =====

    private GarageZone moveRequest(long id, long newParentId, int version) {
        GarageZone req = new GarageZone();
        GarageZone cur = db.get(id);
        req.setId(id);
        req.setZoneCode(cur.getZoneCode());
        req.setZoneName(cur.getZoneName());
        req.setParentId(newParentId);
        req.setSortOrder(cur.getSortOrder());
        req.setStatus(1);
        req.setVersion(version);
        return req;
    }

    @Test
    void updateZone_rejectsSelfParent() {
        put(zone(1, "R", 0, 1, 1));
        assertThatThrownBy(() -> service.updateZone(moveRequest(1, 1, 0)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自身");
        verify(mapper, never()).casUpdateZone(any());
    }

    @Test
    void updateZone_rejectsMovingUnderDirectDescendant() {
        // R -> A -> A1；把 R 挂到 A1 必须拒绝并点名 A1
        put(zone(1, "R", 0, 1, 1));
        put(zone(2, "A", 1, 2, 1));
        put(zone(3, "A1", 2, 3, 1));

        assertThatThrownBy(() -> service.updateZone(moveRequest(1, 3, 0)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("A1")
                .hasMessageContaining("循环层级");
        verify(mapper, never()).casUpdateZone(any());
    }

    @Test
    void updateZone_rejectsMovingUnderDeepDescendant() {
        // 四级：1 -> 2 -> 3 -> 4；把 2 挂到 4（自己的孙节点的子节点）
        put(zone(1, "R", 0, 1, 1));
        put(zone(2, "A", 1, 2, 1));
        put(zone(3, "A1", 2, 3, 1));
        put(zone(4, "A1x", 3, 4, 1));

        assertThatThrownBy(() -> service.updateZone(moveRequest(2, 4, 0)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("A1x");
    }

    // ===== 3. 子树原子移动：层级整体重算，灯组不被触碰 =====

    @Test
    void updateZone_movesSubtreeAtomicallyAndRecomputesAllLevels() {
        // 1(R) -> 2(A) -> 3(A1) -> 4(A1x)；另一棵 5(B)；把 A(2) 挂到 B(5) 下
        // B 原 level=1 -> A 应为 2，A1 为 3，A1x 为 4（与原深度一致，但父链改变）
        // 再构造更深场景验证 level 偏移：1(L1)->2(L2)->3(L3)->4(L4) 与 10(L1)->11(L2)
        // 把 2 挂到 11 下：新层级 3/4/5
        put(zone(1, "R", 0, 1, 1));
        put(zone(2, "A", 1, 2, 1));
        put(zone(3, "A1", 2, 3, 1));
        put(zone(4, "A1x", 3, 4, 1));
        put(zone(10, "B", 0, 1, 2));
        put(zone(11, "B1", 10, 2, 1));

        service.updateZone(moveRequest(2, 11, 0));

        assertThat(db.get(2L).getParentId()).isEqualTo(11L);
        assertThat(db.get(2L).getLevel()).isEqualTo(3);
        assertThat(db.get(3L).getLevel()).isEqualTo(4);
        assertThat(db.get(4L).getLevel()).isEqualTo(5);
        // 子树节点版本都被推进，其他分区不动
        assertThat(db.get(3L).getVersion()).isEqualTo(1);
        assertThat(db.get(4L).getVersion()).isEqualTo(1);
        assertThat(db.get(10L).getVersion()).isEqualTo(0);
        // 移动后再次校验：2 的后代集合已在新位置，把 11 挂到 2 仍应被识别为挂后代
        assertThatThrownBy(() -> service.updateZone(moveRequest(11, 3, db.get(11L).getVersion())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("循环层级");
    }

    @Test
    void updateZone_doesNotTouchAnyLightGroup() {
        put(zone(1, "R", 0, 1, 1));
        put(zone(2, "A", 1, 2, 1));
        put(zone(3, "B", 0, 1, 2));
        service.updateZone(moveRequest(2, 3, 0));
        // 仅允许层级相关调用，灯组划转不应由分区移动触发（mapper 上不存在相关方法调用）
        verify(mapper, times(1)).casUpdateZone(any());
        // A 没有后代：不产生子树版本推进
        verify(mapper, never()).bumpVersionByIds(anyList());
        verify(mapper, never()).updateLevel(any(), anyInt());
    }

    // ===== 4. 版本冲突：过期树提交被拒并带最新数据 =====

    @Test
    void updateZone_staleVersionRaisesConflictWithLatest() {
        GarageZone z = zone(1, "R", 0, 1, 1);
        z.setVersion(2);
        put(z);

        GarageZone stale = moveRequest(1, 0, 1);
        stale.setZoneName("旧名称");

        assertThatThrownBy(() -> service.updateZone(stale))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("过期层级");
        // 最新数据随冲突返回，供前端刷新
        ConflictException ex = catchConflict(() -> service.updateZone(stale));
        GarageZone latest = (GarageZone) ex.getLatestData();
        assertThat(latest.getId()).isEqualTo(1L);
        assertThat(latest.getVersion()).isEqualTo(2);
        // 过期写入未生效
        assertThat(db.get(1L).getVersion()).isEqualTo(2);
    }

    private ConflictException catchConflict(Runnable r) {
        try {
            r.run();
            throw new AssertionError("expected conflict");
        } catch (ConflictException e) {
            return e;
        }
    }

    // ===== 5. 删除保护：子分区 / 灯组 / 未核销流水，仅空叶子可删 =====

    @Test
    void deleteZone_rejectsWhenChildZoneExists() {
        put(zone(1, "R", 0, 1, 1));
        put(zone(2, "A", 1, 2, 1));
        when(mapper.selectLightGroupBlocks(anyList(), anyInt())).thenReturn(Collections.emptyList());
        when(mapper.selectOpenLedgerBlocks(anyList(), anyInt())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.deleteZone(1L))
                .isInstanceOf(ZoneDeleteBlockException.class)
                .satisfies(e -> {
                    ZoneDeleteBlockException block = (ZoneDeleteBlockException) e;
                    assertThat(block.getBlocks()).isNotEmpty();
                    assertThat(block.getBlocks().get(0).getType()).isEqualTo("CHILD_ZONE");
                });
        verify(mapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteZone_rejectsWhenDescendantHasLightGroup() {
        // R -> A -> A1，灯组挂在最深的 A1 上，删除 R 必须被阻断
        put(zone(1, "R", 0, 1, 1));
        put(zone(2, "A", 1, 2, 1));
        put(zone(3, "A1", 2, 3, 1));
        when(mapper.selectLightGroupBlocks(anyList(), anyInt())).thenReturn(List.of(
                new ZoneDeleteBlockDTO("LIGHT_GROUP", "灯组引用", 3L, "A1", "LG-1", "仍登记灯组")));
        when(mapper.selectOpenLedgerBlocks(anyList(), anyInt())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.deleteZone(1L))
                .isInstanceOf(ZoneDeleteBlockException.class)
                .hasMessageContaining("灯组引用");
        verify(mapper, never()).deleteById(any(Long.class));
        // 灯组阻断查询必须覆盖整个子树 {1,2,3}
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(mapper).selectLightGroupBlocks(idsCaptor.capture(), anyInt());
        assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void deleteZone_rejectsWhenUnsettledLedgerReferencesSubtree() {
        put(zone(1, "Leaf", 0, 1, 1));
        when(mapper.selectLightGroupBlocks(anyList(), anyInt())).thenReturn(Collections.emptyList());
        when(mapper.selectOpenLedgerBlocks(anyList(), anyInt())).thenReturn(List.of(
                new ZoneDeleteBlockDTO("OPEN_TRANSFER_LEDGER", "未核销划转流水", 1L, "Leaf",
                        "BT202601", "流水未核销")));

        assertThatThrownBy(() -> service.deleteZone(1L))
                .isInstanceOf(ZoneDeleteBlockException.class)
                .hasMessageContaining("未核销划转流水");
        verify(mapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteZone_emptyLeafSucceeds() {
        put(zone(1, "Leaf", 0, 1, 1));
        when(mapper.selectLightGroupBlocks(anyList(), anyInt())).thenReturn(Collections.emptyList());
        when(mapper.selectOpenLedgerBlocks(anyList(), anyInt())).thenReturn(Collections.emptyList());

        service.deleteZone(1L);
        verify(mapper).deleteById((Long) eq(1L));
    }

    @Test
    void deleteZone_subtreeIdsCollectedInDepth() {
        // 4 级树：删除根时收集到全部后代
        put(zone(1, "R", 0, 1, 1));
        put(zone(2, "A", 1, 2, 1));
        put(zone(3, "A1", 2, 3, 1));
        put(zone(4, "A1x", 3, 4, 1));
        when(mapper.selectLightGroupBlocks(anyList(), anyInt())).thenReturn(List.of(
                new ZoneDeleteBlockDTO("LIGHT_GROUP", "灯组引用", 4L, "A1x", "LG-9", "仍登记灯组")));
        when(mapper.selectOpenLedgerBlocks(anyList(), anyInt())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.deleteZone(1L))
                .isInstanceOf(ZoneDeleteBlockException.class);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(mapper).selectLightGroupBlocks(idsCaptor.capture(), anyInt());
        assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
    }
}
