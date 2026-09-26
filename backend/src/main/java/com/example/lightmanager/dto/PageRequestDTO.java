package com.example.lightmanager.dto;

public class PageRequestDTO {
    
    private Integer pageNum = 1;
    
    private Integer pageSize = 10;
    
    private Long zoneId;
    
    private String groupCode;
    
    private Integer status;

    /** 归档生命周期: ACTIVE/PENDING_REVIEW/RETURNED/ARCHIVED */
    private String archiveStatus;

    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public Long getZoneId() { return zoneId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }
    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getArchiveStatus() { return archiveStatus; }
    public void setArchiveStatus(String archiveStatus) { this.archiveStatus = archiveStatus; }
}