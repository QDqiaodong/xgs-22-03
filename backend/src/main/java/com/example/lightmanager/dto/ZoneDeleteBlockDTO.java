package com.example.lightmanager.dto;

/**
 * 删除分区阻断原因：仅当分区为空叶子时才允许删除，
 * 只要本分区或任意后代下仍挂有灯组 / 未核销划转流水 / 子分区，即逐条返回阻断原因。
 */
public class ZoneDeleteBlockDTO {

    /** CHILD_ZONE子分区 LIGHT_GROUP灯组引用 OPEN_TRANSFER_LEDGER未核销划转流水 */
    private String type;

    /** 类型中文名 */
    private String typeText;

    /** 所在分区ID（子分区阻断时为该子分区自身，其余为灯组/流水引用所在分区） */
    private Long zoneId;

    /** 所在分区名称 */
    private String zoneName;

    /** 关联记录编号（子分区编码 / 灯组编号 / 划转批次号） */
    private String refNo;

    /** 阻断说明 */
    private String detail;

    public ZoneDeleteBlockDTO() {}

    public ZoneDeleteBlockDTO(String type, String typeText, Long zoneId, String zoneName,
                              String refNo, String detail) {
        this.type = type;
        this.typeText = typeText;
        this.zoneId = zoneId;
        this.zoneName = zoneName;
        this.refNo = refNo;
        this.detail = detail;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTypeText() { return typeText; }
    public void setTypeText(String typeText) { this.typeText = typeText; }
    public Long getZoneId() { return zoneId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }
    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
    public String getRefNo() { return refNo; }
    public void setRefNo(String refNo) { this.refNo = refNo; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
