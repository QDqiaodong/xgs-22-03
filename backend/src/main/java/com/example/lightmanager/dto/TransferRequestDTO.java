package com.example.lightmanager.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class TransferRequestDTO {
    
    @NotEmpty(message = "灯组ID列表不能为空")
    private List<Long> lightGroupIds;
    
    @NotNull(message = "目标分区ID不能为空")
    private Long targetZoneId;
    
    private String operator;
    
    private String description;

    public List<Long> getLightGroupIds() { return lightGroupIds; }
    public void setLightGroupIds(List<Long> lightGroupIds) { this.lightGroupIds = lightGroupIds; }
    public Long getTargetZoneId() { return targetZoneId; }
    public void setTargetZoneId(Long targetZoneId) { this.targetZoneId = targetZoneId; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}