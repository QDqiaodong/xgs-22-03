package com.example.lightmanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lightmanager.entity.LedgerDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LedgerDetailMapper extends BaseMapper<LedgerDetail> {
    
    void batchInsert(@Param("details") List<LedgerDetail> details);
    
    List<LedgerDetail> selectByLedgerId(@Param("ledgerId") Long ledgerId);
}