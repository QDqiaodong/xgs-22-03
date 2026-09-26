package com.example.lightmanager.exception;

/**
 * 归档 CAS 状态流转失败的内部标记：事务回滚后由外层读取最新归档详情并转换为 ArchiveConflictException。
 * 不在事务内读取，避免高并发时同线程占用两个数据库连接。
 */
public class ArchiveConflictNeedsRefreshException extends RuntimeException {

    private final Long lightGroupId;
    private final Integer clientVersion;

    public ArchiveConflictNeedsRefreshException(Long lightGroupId, Integer clientVersion) {
        super("NEEDS_REFRESH");
        this.lightGroupId = lightGroupId;
        this.clientVersion = clientVersion;
    }

    public Long getLightGroupId() {
        return lightGroupId;
    }

    public Integer getClientVersion() {
        return clientVersion;
    }
}
