package com.example.lightmanager.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 暂存巡检结果：允许部分灯组尚未登记，允许异常暂不填现场说明
 */
public class InspectionItemSaveDTO {

    @NotEmpty(message = "巡检结果不能为空")
    @Valid
    private List<InspectionItemResultDTO> items;

    /** 暂存操作人（可选，用于审计） */
    private String operator;

    public List<InspectionItemResultDTO> getItems() { return items; }
    public void setItems(List<InspectionItemResultDTO> items) { this.items = items; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
