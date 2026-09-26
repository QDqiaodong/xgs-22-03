package com.example.lightmanager.exception;

/**
 * 报废归档并发冲突：乐观锁版本号不匹配 / 归档流程状态已被他人改变。
 * 携带最新归档详情（含最新意见与完整历史），前端据此提示并刷新，
 * 后提交者的动作不生效，也不会额外增加一条复核历史。
 */
public class ArchiveConflictException extends RuntimeException {

    private final transient Object latestData;

    public ArchiveConflictException(String message, Object latestData) {
        super(message);
        this.latestData = latestData;
    }

    public Object getLatestData() {
        return latestData;
    }
}
