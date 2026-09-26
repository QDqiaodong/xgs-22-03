package com.example.lightmanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lightmanager.entity.GarageZone;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface GarageZoneMapper extends BaseMapper<GarageZone> {

    List<GarageZone> selectTreeByParentId(Long parentId);

    /** 启用分区，按排序返回（树展示/灯组计数） */
    List<GarageZone> selectAllZones();

    /**
     * 锁定全部分区行（FOR UPDATE）：分区表规模小，层级变更/删除前整体加写锁，
     * 把所有层级写操作串行化，从根上杜绝两个管理员并发移动相互成环；
     * 普通读（树/列表查询）不加锁，不受阻塞。返回值即锁后最新全量层级。
     */
    List<GarageZone> selectAllZonesForUpdate();

    /**
     * 带版本号的条件更新（CAS）：version 不匹配返回 0，调用方转成 409 版本冲突。
     * 只更新非空字段（名称/编码/上级/层级/排序/状态），version 每次 +1。
     */
    int casUpdateZone(@Param("zone") GarageZone zone);

    /** 子树整体平移层级：移动目录后所有后代 level 同步加 delta，全部成功或整体回滚 */
    int shiftSubtreeLevels(@Param("ids") List<Long> ids, @Param("delta") int delta);

    /** 各分区灯组总数与已归档数（key: zoneId / totalCount / archivedCount） */
    List<Map<String, Object>> selectZoneLightCounts();
}
