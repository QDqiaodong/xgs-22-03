package com.example.lightmanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("batch_transfer_ledger")
public class BatchTransferLedger {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String batchNo;
    
    private String operator;
    
    private LocalDateTime transferTime;
    
    private String sourceZoneIds;
    
    private Long targetZoneId;
    
    private Integer lightCount;
    
    private String description;

    /** OPEN未核销 CLOSED已核销（核销后解除对归档的引用阻断） */
    private String settleStatus;

    private String settledBy;

    private LocalDateTime settledAt;

    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public LocalDateTime getTransferTime() { return transferTime; }
    public void setTransferTime(LocalDateTime transferTime) { this.transferTime = transferTime; }
    public String getSourceZoneIds() { return sourceZoneIds; }
    public void setSourceZoneIds(String sourceZoneIds) { this.sourceZoneIds = sourceZoneIds; }
    public Long getTargetZoneId() { return targetZoneId; }
    public void setTargetZoneId(Long targetZoneId) { this.targetZoneId = targetZoneId; }
    public Integer getLightCount() { return lightCount; }
    public void setLightCount(Integer lightCount) { this.lightCount = lightCount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSettleStatus() { return settleStatus; }
    public void setSettleStatus(String settleStatus) { this.settleStatus = settleStatus; }
    public String getSettledBy() { return settledBy; }
    public void setSettledBy(String settledBy) { this.settledBy = settledBy; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}