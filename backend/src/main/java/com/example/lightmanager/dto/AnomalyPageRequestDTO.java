package com.example.lightmanager.dto;

public class AnomalyPageRequestDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String batchNo;

    private String groupCode;

    private String anomalyType;

    private String status;

    private Long zoneId;

    /** 是否只看当前分区已变化 */
    private Boolean zoneChanged;

    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
    public String getAnomalyType() { return anomalyType; }
    public void setAnomalyType(String anomalyType) { this.anomalyType = anomalyType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getZoneId() { return zoneId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }
    public Boolean getZoneChanged() { return zoneChanged; }
    public void setZoneChanged(Boolean zoneChanged) { this.zoneChanged = zoneChanged; }
}
