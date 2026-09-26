package com.example.lightmanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("inspection_action_log")
public class InspectionActionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long anomalyId;

    /** SUBMIT上报 RETURN退回 SUPPLEMENT补充 CONFIRM确认 RESOLVE登记处理结果 CLOSE关闭 */
    private String actionType;

    private String content;

    private LocalDateTime deadline;

    private String operator;

    private LocalDateTime operatedAt;

    private Integer fromVersion;

    private Integer toVersion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAnomalyId() { return anomalyId; }
    public void setAnomalyId(Long anomalyId) { this.anomalyId = anomalyId; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public LocalDateTime getOperatedAt() { return operatedAt; }
    public void setOperatedAt(LocalDateTime operatedAt) { this.operatedAt = operatedAt; }
    public Integer getFromVersion() { return fromVersion; }
    public void setFromVersion(Integer fromVersion) { this.fromVersion = fromVersion; }
    public Integer getToVersion() { return toVersion; }
    public void setToVersion(Integer toVersion) { this.toVersion = toVersion; }
}
