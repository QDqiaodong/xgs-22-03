package com.example.lightmanager.dto;

import java.time.LocalDateTime;

public class LedgerPageRequestDTO {
    
    private Integer pageNum = 1;
    
    private Integer pageSize = 10;
    
    private String operator;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private String batchNo;

    /** 流水核销状态筛选: OPEN未核销 CLOSED已核销 */
    private String settleStatus;

    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getSettleStatus() { return settleStatus; }
    public void setSettleStatus(String settleStatus) { this.settleStatus = settleStatus; }
}