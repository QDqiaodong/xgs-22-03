package com.example.lightmanager.dto;

public class LightGroupDTO {
    
    private Long id;
    
    private String groupCode;
    
    private Integer power;
    
    private Long zoneId;
    
    private String zoneName;
    
    private String installationLocation;
    
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
    public Integer getPower() { return power; }
    public void setPower(Integer power) { this.power = power; }
    public Long getZoneId() { return zoneId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }
    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
    public String getInstallationLocation() { return installationLocation; }
    public void setInstallationLocation(String installationLocation) { this.installationLocation = installationLocation; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}