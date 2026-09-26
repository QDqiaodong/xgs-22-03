package com.example.lightmanager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 异常处置动作统一入参：退回 / 补充 / 确认 / 登记处理结果 / 关闭
 * 各动作必填字段由 Service 按动作类型分别校验
 */
public class AnomalyActionDTO {

    /** 操作人 */
    @NotBlank(message = "操作人不能为空")
    private String operator;

    /** 打开记录时的版本号，用于乐观并发控制 */
    @NotNull(message = "版本号不能为空")
    private Integer version;

    /** 退回原因（RETURN 必填） */
    private String reason;

    /** 补充说明（SUPPLEMENT 必填） */
    private String supplementDescription;

    /** 处理意见（CONFIRM 必填） */
    private String reviewOpinion;

    /** 处理时限（CONFIRM 必填） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;

    /** 处理结果（RESOLVE 必填） */
    private String handlingResult;

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getSupplementDescription() { return supplementDescription; }
    public void setSupplementDescription(String supplementDescription) { this.supplementDescription = supplementDescription; }
    public String getReviewOpinion() { return reviewOpinion; }
    public void setReviewOpinion(String reviewOpinion) { this.reviewOpinion = reviewOpinion; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public String getHandlingResult() { return handlingResult; }
    public void setHandlingResult(String handlingResult) { this.handlingResult = handlingResult; }
}
