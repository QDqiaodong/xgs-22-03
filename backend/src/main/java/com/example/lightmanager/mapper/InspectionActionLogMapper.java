package com.example.lightmanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lightmanager.entity.InspectionActionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InspectionActionLogMapper extends BaseMapper<InspectionActionLog> {

    /** 追加式查询：按时间正序，完整还原结论形成过程 */
    List<InspectionActionLog> selectByAnomalyId(@Param("anomalyId") Long anomalyId);
}
