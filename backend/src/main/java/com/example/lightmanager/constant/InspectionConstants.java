package com.example.lightmanager.constant;

/**
 * 灯组巡检异常处置相关常量
 */
public final class InspectionConstants {

    private InspectionConstants() {}

    /** 巡检结果 */
    public static final String RESULT_NORMAL = "NORMAL";
    public static final String RESULT_EXTINGUISHED = "EXTINGUISHED";
    public static final String RESULT_FLICKER = "FLICKER";
    public static final String RESULT_DIM = "DIM";

    /** 批次状态 */
    public static final String BATCH_DRAFT = "DRAFT";
    public static final String BATCH_SUBMITTED = "SUBMITTED";

    /** 异常状态 */
    public static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    public static final String STATUS_RETURNED = "RETURNED";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_CLOSED = "CLOSED";

    /** 处置动作 */
    public static final String ACTION_SUBMIT = "SUBMIT";
    public static final String ACTION_RETURN = "RETURN";
    public static final String ACTION_SUPPLEMENT = "SUPPLEMENT";
    public static final String ACTION_CONFIRM = "CONFIRM";
    public static final String ACTION_RESOLVE = "RESOLVE";
    public static final String ACTION_CLOSE = "CLOSE";
}
