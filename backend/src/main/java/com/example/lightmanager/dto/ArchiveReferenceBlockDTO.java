package com.example.lightmanager.dto;

/**
 * 归档引用阻断记录：确认归档前仍指向该灯组的未关闭引用。
 */
public class ArchiveReferenceBlockDTO {

    /** 引用类型: INSPECTION_BATCH未关闭巡检批次 INSPECTION_ANOMALY异常处置记录 TRANSFER_LEDGER划转流水 */
    private String type;

    /** 类型中文名 */
    private String typeText;

    /** 引用记录ID */
    private Long refId;

    /** 引用记录编号（批次号/批次号/划转批次号） */
    private String refNo;

    /** 关联灯组编号（流水/条目快照编号） */
    private String groupCode;

    /** 当前状态码 */
    private String status;

    /** 当前状态中文名 */
    private String statusText;

    /** 阻断说明 */
    private String detail;

    /** 业务发生/创建时间 */
    private String bizTime;

    public ArchiveReferenceBlockDTO() {}

    public ArchiveReferenceBlockDTO(String type, String typeText, Long refId, String refNo,
                                    String groupCode, String status, String statusText,
                                    String detail, String bizTime) {
        this.type = type;
        this.typeText = typeText;
        this.refId = refId;
        this.refNo = refNo;
        this.groupCode = groupCode;
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
    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getBizTime() { return bizTime; }
    public void setBizTime(String bizTime) { this.bizTime = bizTime; }
}
