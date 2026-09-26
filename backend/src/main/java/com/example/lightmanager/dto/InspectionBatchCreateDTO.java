package com.example.lightmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class InspectionBatchCreateDTO {

    @NotEmpty(message = "请至少选择一个巡检分区")
    private List<Long> zoneIds;

    @NotBlank(message = "巡检人不能为空")
    private String inspector;

    private String remark;

    public List<Long> getZoneIds() { return zoneIds; }
    public void setZoneIds(List<Long> zoneIds) { this.zoneIds = zoneIds; }
    public String getInspector() { return inspector; }
    public void setInspector(String inspector) { this.inspector = inspector; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
