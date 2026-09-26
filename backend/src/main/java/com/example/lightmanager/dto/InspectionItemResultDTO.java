package com.example.lightmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class InspectionItemResultDTO {

    @NotNull(message = "巡检条目ID不能为空")
    private Long itemId;

    /** NORMAL正常 EXTINGUISHED熄灭 FLICKER频闪 DIM亮度不足 */
    @NotBlank(message = "巡检结果不能为空")
    private String resultType;

    /** 异常时必填（由Service按结果类型强制校验） */
    private String description;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getResultType() { return resultType; }
    public void setResultType(String resultType) { this.resultType = resultType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
