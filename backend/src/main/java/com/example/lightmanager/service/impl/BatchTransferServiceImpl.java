package com.example.lightmanager.service.impl;

import com.example.lightmanager.dto.LedgerPageRequestDTO;
import com.example.lightmanager.dto.TransferRequestDTO;
import com.example.lightmanager.entity.BatchTransferLedger;
import com.example.lightmanager.entity.LedgerDetail;
import com.example.lightmanager.entity.LightGroup;
import com.example.lightmanager.exception.BusinessException;
import com.example.lightmanager.mapper.BatchTransferLedgerMapper;
import com.example.lightmanager.mapper.LedgerDetailMapper;
import com.example.lightmanager.mapper.LightGroupMapper;
import com.example.lightmanager.service.BatchTransferService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BatchTransferServiceImpl implements BatchTransferService {
    
    private final LightGroupMapper lightGroupMapper;
    private final BatchTransferLedgerMapper batchTransferLedgerMapper;
    private final LedgerDetailMapper ledgerDetailMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    
    public BatchTransferServiceImpl(LightGroupMapper lightGroupMapper, 
                                    BatchTransferLedgerMapper batchTransferLedgerMapper,
                                    LedgerDetailMapper ledgerDetailMapper,
                                    RedisTemplate<String, Object> redisTemplate) {
        this.lightGroupMapper = lightGroupMapper;
        this.batchTransferLedgerMapper = batchTransferLedgerMapper;
        this.ledgerDetailMapper = ledgerDetailMapper;
        this.redisTemplate = redisTemplate;
    }
    
    private static final String LIGHT_GROUP_POWER_KEY = "light_group:power:";
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchTransferLedger batchTransfer(TransferRequestDTO request) {
        List<Long> lightGroupIds = request.getLightGroupIds();
        Long targetZoneId = request.getTargetZoneId();

        // 归档后只读，编号/分区不可再被划转：先给出具体阻断灯组编号
        List<String> archivedCodes = lightGroupMapper.selectArchivedCodesByIds(lightGroupIds);
        if (!archivedCodes.isEmpty()) {
            throw new BusinessException("以下灯组已归档，不能再划转：" + String.join("、", archivedCodes));
        }

        List<LightGroup> lightGroups = lightGroupMapper.selectByIds(lightGroupIds);
        if (lightGroups.isEmpty()) {
            throw new BusinessException("未找到指定的灯组");
        }
        
        List<Long> sourceZoneIds = lightGroups.stream()
                .map(LightGroup::getZoneId)
                .distinct()
                .collect(Collectors.toList());
        
        String sourceZoneIdsStr = sourceZoneIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        
        String batchNo = generateBatchNo();
        
        BatchTransferLedger ledger = new BatchTransferLedger();
        ledger.setBatchNo(batchNo);
        ledger.setOperator(request.getOperator() != null ? request.getOperator() : "system");
        ledger.setTransferTime(LocalDateTime.now());
        ledger.setSourceZoneIds(sourceZoneIdsStr);
        ledger.setTargetZoneId(targetZoneId);
        ledger.setLightCount(lightGroups.size());
        ledger.setDescription(request.getDescription());
        batchTransferLedgerMapper.insert(ledger);
        
        List<LedgerDetail> details = new ArrayList<>();
        for (LightGroup lg : lightGroups) {
            LedgerDetail detail = new LedgerDetail();
            detail.setLedgerId(ledger.getId());
            detail.setLightGroupId(lg.getId());
            detail.setSourceZoneId(lg.getZoneId());
            detail.setTargetZoneId(targetZoneId);
            details.add(detail);
            
            String oldKey = LIGHT_GROUP_POWER_KEY + lg.getZoneId();
            redisTemplate.opsForZSet().remove(oldKey, lg.getGroupCode());
            
            String newKey = LIGHT_GROUP_POWER_KEY + targetZoneId;
            redisTemplate.opsForZSet().add(newKey, lg.getGroupCode(), lg.getPower());
        }
        
        if (!details.isEmpty()) {
            ledgerDetailMapper.batchInsert(details);
        }
        
        // 已归档灯组在前置校验中被整体拒绝；更新语句同样带归档条件做双保险
        int updated = lightGroupMapper.batchUpdateZoneIdActive(lightGroupIds, targetZoneId);
        if (updated != lightGroups.size()) {
            throw new BusinessException("划转失败：存在已归档或状态已变化的灯组，请刷新后重试");
        }

        return ledger;
    }
    
    @Override
    public IPage<BatchTransferLedger> getLedgerPage(LedgerPageRequestDTO request) {
        Page<BatchTransferLedger> page = new Page<>(request.getPageNum(), request.getPageSize());
        return batchTransferLedgerMapper.selectByConditionsPage(page,
                request.getOperator(), request.getStartTime(),
                request.getEndTime(), request.getBatchNo(), request.getSettleStatus());
    }
    
    @Override
    public BatchTransferLedger getLedgerById(Long id) {
        return batchTransferLedgerMapper.selectById(id);
    }
    
    @Override
    public List<LedgerDetail> getLedgerDetails(Long ledgerId) {
        return ledgerDetailMapper.selectByLedgerId(ledgerId);
    }

    @Override
    public BatchTransferLedger settleLedger(Long id, String operator) {
        if (operator == null || operator.trim().isEmpty()) {
            throw new BusinessException("核销操作人不能为空");
        }
        BatchTransferLedger ledger = batchTransferLedgerMapper.selectById(id);
        if (ledger == null) {
            throw new BusinessException("划转流水不存在");
        }
        int affected = batchTransferLedgerMapper.casSettle(id, operator.trim(), LocalDateTime.now());
        if (affected == 0) {
            throw new BusinessException("该划转流水已核销，请勿重复操作");
        }
        return batchTransferLedgerMapper.selectById(id);
    }

    private String generateBatchNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 10000);
        return "BT" + timestamp + String.format("%04d", random);
    }
}