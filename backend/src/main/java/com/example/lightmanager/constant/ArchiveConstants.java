package com.example.lightmanager.constant;

/**
 * 灯组报废归档相关常量
 */
public final class ArchiveConstants {

    private ArchiveConstants() {}

    /** 归档生命周期状态 */
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    public static final String STATUS_RETURNED = "RETURNED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    /** 归档历史动作 */
    public static final String ACTION_SUBMIT = "SUBMIT";
    public static final String ACTION_RETURN = "RETURN";
    public static final String ACTION_RESUBMIT = "RESUBMIT";
    public static final String ACTION_ARCHIVE = "ARCHIVE";

    /** 划转流水核销状态 */
    public static final String SETTLE_OPEN = "OPEN";
    public static final String SETTLE_CLOSED = "CLOSED";

    /** 引用检查类型 */
    public static final String REF_INSPECTION_BATCH = "INSPECTION_BATCH";
    public static final String REF_INSPECTION_ANOMALY = "INSPECTION_ANOMALY";
    public static final String REF_TRANSFER_LEDGER = "TRANSFER_LEDGER";
}
