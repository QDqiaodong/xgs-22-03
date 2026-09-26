package com.example.lightmanager.exception;

import com.example.lightmanager.dto.ArchiveReferenceBlockDTO;

import java.util.List;

/**
 * 归档引用阻断：确认归档时仍有未解除引用（未关闭巡检批次 / 异常处置记录 / 划转流水）。
 * HTTP 412：前置条件不满足，携带具体阻断记录，直接在页面呈现。
 */
public class ReferenceBlockException extends RuntimeException {

    private final transient List<ArchiveReferenceBlockDTO> blocks;

    public ReferenceBlockException(String message, List<ArchiveReferenceBlockDTO> blocks) {
        super(message);
        this.blocks = blocks;
    }

    public List<ArchiveReferenceBlockDTO> getBlocks() {
        return blocks;
    }
}
