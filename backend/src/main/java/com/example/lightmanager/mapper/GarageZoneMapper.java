package com.example.lightmanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lightmanager.dto.ZoneDeleteBlockDTO;
import com.example.lightmanager.entity.GarageZone;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface GarageZoneMapper extends BaseMapper<GarageZone> {

    List<GarageZone> selectTreeByParentId(Long parentId);

    List<GarageZone> selectAllZones();

    /** 各分区灯组总数与已归档数（key: zoneId / totalCount / archivedCount） */
    List<Map<String, Object>> selectZoneLightCounts();

    /**
     * 事务内对全表分区加共享快照锁（含禁用分区），串行化换父级/删除等层级变更，
     * 使层级环校验始终基于已提交的最新父子关系，消除 TOCTOU 竞态。
     */
    List<GarageZone> selectAllZonesForUpdate();

    /** 乐观锁更新分区：仅当 version 未变时生效并自增，避免过期树覆盖他人结果 */
    int casUpdateZone(@Param("zone") GarageZone zone);

    /** 换父级后批量重算子树层级：按 id 逐个更新 level */
    int updateLevel(@Param("id") Long id, @Param("level") Integer level);

    /** 子树内任一节点被移动后，对其全部节点版本号 +1，使持有旧树的并发编辑收到 409 */
    int bumpVersionByIds(@Param("ids") List<Long> ids);

    // ===== 删除分区引用保护：删除前收集阻断原因 =====

    /** 直接子分区（含禁用）：存在即阻断，删除只允许空叶子 */
    List<ZoneDeleteBlockDTO> selectChildZoneBlocks(@Param("parentId") Long parentId);

    /** 分区集合（本分区+全部后代）下仍存在的灯组，返回前若干条具体编号 */
    List<ZoneDeleteBlockDTO> selectLightGroupBlocks(@Param("zoneIds") List<Long> zoneIds,
                                                    @Param("limit") int limit);

    /** 分区集合相关的未核销划转流水（源/目标/明细任一指向集合内分区），返回前若干条 */
    List<ZoneDeleteBlockDTO> selectOpenLedgerBlocks(@Param("zoneIds") List<Long> zoneIds,
                                                    @Param("limit") int limit);
}
