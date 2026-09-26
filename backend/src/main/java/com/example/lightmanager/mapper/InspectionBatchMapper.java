package com.example.lightmanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.lightmanager.entity.InspectionBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface InspectionBatchMapper extends BaseMapper<InspectionBatch> {

    IPage<InspectionBatch> selectBatchPage(IPage<InspectionBatch> page,
                                           @Param("batchNo") String batchNo,
                                           @Param("inspector") String inspector,
                                           @Param("status") String status,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);
}
