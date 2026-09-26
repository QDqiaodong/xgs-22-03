package com.example.lightmanager.exception;

/**
 * 分区层级冲突：自挂、挂到自身后代等会形成环的换父级操作。
 * HTTP 400：消息中直接给出具体冲突分区，调用方据此提示管理员。
 */
public class ZoneHierarchyConflictException extends RuntimeException {

    public ZoneHierarchyConflictException(String message) {
        super(message);
    }
}
