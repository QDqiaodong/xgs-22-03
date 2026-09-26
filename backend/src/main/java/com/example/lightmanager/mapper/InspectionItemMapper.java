package com.example.lightmanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lightmanager.entity.InspectionItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InspectionItemMapper extends BaseMapper<InspectionItem> {

    void batchInsert(@Param("items") List<InspectionItem> items);

    List<InspectionItem> selectByBatchId(@Param("batchId") Long batchId);
}
