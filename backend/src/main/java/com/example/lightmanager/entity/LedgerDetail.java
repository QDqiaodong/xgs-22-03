package com.example.lightmanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("ledger_detail")
public class LedgerDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ledgerId;

    private Long lightGroupId;

    private Long sourceZoneId;

    private Long targetZoneId;

    // ===== 流水详情关联展示（非表字段） =====

    /** 灯组编号：归档后编号不可编辑，因此与划转当时一致 */
    @TableField(exist = false)
    private String groupCode;

    /** 归档后编号不可编辑，当前编号即历史编号；此处仅用于前端显式渲染 */
    @TableField(exist = false)
    private String sourceZoneName;

    @TableField(exist = false)
    private String targetZoneName;

    /** 灯组当前归档状态：归档完成后流水详情共同显示“已归档” */
    @TableField(exist = false)
    private String archiveStatus;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }
    public Long getLightGroupId() { return lightGroupId; }
    public void setLightGroupId(Long lightGroupId) { this.lightGroupId = lightGroupId; }
    public Long getSourceZoneId() { return sourceZoneId; }
    public void setSourceZoneId(Long sourceZoneId) { this.sourceZoneId = sourceZoneId; }
    public Long getTargetZoneId() { return targetZoneId; }
    public void setTargetZoneId(Long targetZoneId) { this.targetZoneId = targetZoneId; }
    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
    public String getSourceZoneName() { return sourceZoneName; }
    public void setSourceZoneName(String sourceZoneName) { this.sourceZoneName = sourceZoneName; }
    public String getTargetZoneName() { return targetZoneName; }
    public void setTargetZoneName(String targetZoneName) { this.targetZoneName = targetZoneName; }
    public String getArchiveStatus() { return archiveStatus; }
    public void setArchiveStatus(String archiveStatus) { this.archiveStatus = archiveStatus; }
}
