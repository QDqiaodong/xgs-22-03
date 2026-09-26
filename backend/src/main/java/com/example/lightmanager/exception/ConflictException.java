package com.example.lightmanager.exception;

/**
 * 并发冲突异常：乐观锁版本号不匹配 / 记录状态已被他人改变。
 * 携带最新的异常详情，前端据此提示并刷新，禁止后提交者覆盖前一次结论。
 */
public class ConflictException extends RuntimeException {

    private final transient Object latestData;

    public ConflictException(String message, Object latestData) {
        super(message);
        this.latestData = latestData;
    }

    public Object getLatestData() {
        return latestData;
    }
}
