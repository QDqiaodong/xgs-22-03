package com.example.lightmanager.dto;

/**
 * 分区删除阻断记录：目标分区（或其任意后代）仍存在引用，禁止删除。
 * 仅当所有类型均为空、且分区本身是叶子时才允许删除。
 */
public class ZoneReferenceBlockDTO {

    /** 阻断类型: CHILD_ZONE仍有子分区 LIGHT_GROUP灯组引用 TRANSFER_LEDGER未核销划转流水 */
    private String type;

    /** 类型中文名 */
    private String typeText;

    /** 引用记录ID（子分区ID/灯组ID/流水ID） */
    private Long refId;

    /** 引用记录编号（分区编码/灯组编号/划转批次号） */
    private String refNo;

    /** 被引用分区ID */
    private Long zoneId;

    /** 被引用分区名称（子分区可能位于后代，给出具体冲突分区） */
    private String zoneName;

    /** 当前状态码（流水核销状态等） */
    private String status;

    /** 当前状态中文名 */
    private String statusText;

    /** 阻断说明 */
    private String detail;

    /** 业务发生/创建时间 */
    private String bizTime;

    public ZoneReferenceBlockDTO() {}

    public ZoneReferenceBlockDTO(String type, String typeText, Long refId, String refNo,
                                 Long zoneId, String zoneName, String status, String statusText,
                                 String detail, String bizTime) {
        this.type = type;
        this.typeText = typeText;
        this.refId = refId;
        this.refNo = refNo;
        this.zoneId = zoneId;
        this.zoneName = zoneName;
        this.status = status;
        this.statusText = statusText;
        this.detail = detail;
        this.bizTime = bizTime;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTypeText() { return typeText; }
    public void setTypeText(String typeText) { this.typeText = typeText; }
    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }
    public String getRefNo() { return refNo; }
    public void setRefNo(String refNo) { this.refNo = refNo; }
    public Long getZoneId() { return zoneId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }
    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getBizTime() { return bizTime; }
    public void setBizTime(String bizTime) { this.bizTime = bizTime; }
}
