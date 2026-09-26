package com.example.lightmanager.exception;

import com.example.lightmanager.dto.ZoneDeleteBlockDTO;

import java.util.List;

/**
 * 删除分区阻断：分区不是空叶子（仍有子分区，或本分区/任意后代下仍有灯组、
 * 未核销划转流水）。HTTP 412：携带逐条阻断原因，前端直接列出。
 */
public class ZoneDeleteBlockException extends RuntimeException {

    private final transient List<ZoneDeleteBlockDTO> blocks;

    public ZoneDeleteBlockException(String message, List<ZoneDeleteBlockDTO> blocks) {
        super(message);
        this.blocks = blocks;
    }

    public List<ZoneDeleteBlockDTO> getBlocks() {
        return blocks;
    }
}
