package com.example.lightmanager.exception;

/**
 * 业务规则异常：状态不允许、必填项缺失等
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
