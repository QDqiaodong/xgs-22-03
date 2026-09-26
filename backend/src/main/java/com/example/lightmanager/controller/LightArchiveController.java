package com.example.lightmanager.controller;

import com.example.lightmanager.dto.ArchiveReferenceBlockDTO;
import com.example.lightmanager.dto.ArchiveReviewDTO;
import com.example.lightmanager.dto.ArchiveSubmitDTO;
import com.example.lightmanager.dto.ResponseDTO;
import com.example.lightmanager.entity.LightGroup;
import com.example.lightmanager.service.LightArchiveService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 灯组报废归档：单灯详情内发起、退回补充、再次送审、引用检查、确认归档。
 */
@RestController
@RequestMapping("/api/light-group/{lightGroupId}/archive")
@CrossOrigin(origins = "*")
public class LightArchiveController {

    private final LightArchiveService archiveService;

    public LightArchiveController(LightArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    /** 归档详情：当前状态 + 完整历史轨迹 */
    @GetMapping
    public ResponseDTO<LightGroup> getArchiveDetail(@PathVariable Long lightGroupId) {
        return ResponseDTO.success(archiveService.getArchiveDetail(lightGroupId));
    }

    /** 归档前引用检查（未关闭巡检批次 / 异常处置记录 / 划转流水） */
    @GetMapping("/references")
    public ResponseDTO<List<ArchiveReferenceBlockDTO>> checkReferences(@PathVariable Long lightGroupId) {
        return ResponseDTO.success(archiveService.checkReferences(lightGroupId));
    }

    /** 管理员填写报废理由、现场照片说明、生效日期后发起送审 */
    @PostMapping("/submit")
    public ResponseDTO<LightGroup> submit(@PathVariable Long lightGroupId,
                                          @Valid @RequestBody ArchiveSubmitDTO request) {
        return ResponseDTO.success(archiveService.submit(lightGroupId, request));
    }

    /** 退回补充后再次送审 */
    @PostMapping("/resubmit")
    public ResponseDTO<LightGroup> resubmit(@PathVariable Long lightGroupId,
                                            @Valid @RequestBody ArchiveSubmitDTO request) {
        return ResponseDTO.success(archiveService.resubmit(lightGroupId, request));
    }

    /** 复核人说明原因退回 */
    @PostMapping("/return")
    public ResponseDTO<LightGroup> reviewReturn(@PathVariable Long lightGroupId,
                                                @Valid @RequestBody ArchiveReviewDTO request) {
        return ResponseDTO.success(archiveService.reviewReturn(lightGroupId, request));
    }

    /** 复核人确认归档（引用全部解除后生效） */
    @PostMapping("/confirm")
    public ResponseDTO<LightGroup> confirmArchive(@PathVariable Long lightGroupId,
                                                  @Valid @RequestBody ArchiveReviewDTO request) {
        return ResponseDTO.success(archiveService.confirmArchive(lightGroupId, request));
    }
}
