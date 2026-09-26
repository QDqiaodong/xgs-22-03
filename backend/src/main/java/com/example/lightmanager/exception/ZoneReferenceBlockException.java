package com.example.lightmanager.exception;

import com.example.lightmanager.dto.ZoneReferenceBlockDTO;

import java.util.List;

/**
 * 分区删除引用阻断：分区或其任意后代仍有子分区 / 灯组 / 未核销划转流水。
 * HTTP 412：前置条件不满足，携带具体阻断原因（含冲突分区），直接在页面呈现。
 */
public class ZoneReferenceBlockException extends RuntimeException {

    private final transient List<ZoneReferenceBlockDTO> blocks;

    public ZoneReferenceBlockException(String message, List<ZoneReferenceBlockDTO> blocks) {
        super(message);
        this.blocks = blocks;
    }

    public List<ZoneReferenceBlockDTO> getBlocks() {
        return blocks;
    }
}
