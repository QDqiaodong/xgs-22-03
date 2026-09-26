package com.example.lightmanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.lightmanager.entity.BatchTransferLedger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BatchTransferLedgerMapper extends BaseMapper<BatchTransferLedger> {
    
    IPage<BatchTransferLedger> selectByConditionsPage(IPage<BatchTransferLedger> page,
                                                       @Param("operator") String operator,
                                                       @Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime,
                                                       @Param("batchNo") String batchNo,
                                                       @Param("settleStatus") String settleStatus);

    /** 核销流水：仅 OPEN -> CLOSED，重复核销幂等返回0 */
    int casSettle(@Param("id") Long id,
                  @Param("operator") String operator,
                  @Param("time") LocalDateTime time);
}