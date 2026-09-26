package com.example.lightmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 归档复核动作入参：退回补充（reason 必填） / 确认归档（opinion 必填）。
 */
public class ArchiveReviewDTO {

    /** 复核人 */
    @NotBlank(message = "操作人不能为空")
    private String operator;

    /** 打开待复核版本时的归档版本号，用于乐观并发控制 */
    @NotNull(message = "版本号不能为空")
    private Integer version;

    /** 退回原因（RETURN 必填） */
    private String reason;

    /** 归档确认意见（ARCHIVE 必填） */
    private String opinion;

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getOpinion() { return opinion; }
    public void setOpinion(String opinion) { this.opinion = opinion; }
}
