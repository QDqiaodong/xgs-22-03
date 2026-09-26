package com.example.lightmanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lightmanager.entity.GarageZone;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface GarageZoneMapper extends BaseMapper<GarageZone> {

    List<GarageZone> selectTreeByParentId(Long parentId);

    List<GarageZone> selectAllZones();

    /** 各分区灯组总数与已归档数（key: zoneId / totalCount / archivedCount） */
    List<Map<String, Object>> selectZoneLightCounts();
}
