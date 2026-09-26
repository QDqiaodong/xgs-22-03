package com.example.lightmanager.exception;

/**
 * CAS 状态流转失败的内部标记：事务回滚后由外层读取最新数据并转换为 ConflictException。
 * 不在事务内读取，避免高并发时同线程占用两个数据库连接造成连接池饥饿。
 */
public class ConflictNeedsRefreshException extends RuntimeException {

    private final Long anomalyId;
    private final Integer clientVersion;

    public ConflictNeedsRefreshException(Long anomalyId, Integer clientVersion) {
        super("NEEDS_REFRESH");
        this.anomalyId = anomalyId;
        this.clientVersion = clientVersion;
    }

    public Long getAnomalyId() {
        return anomalyId;
    }

    public Integer getClientVersion() {
        return clientVersion;
    }
}
