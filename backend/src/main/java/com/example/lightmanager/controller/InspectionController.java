package com.example.lightmanager.controller;

import com.example.lightmanager.dto.InspectionBatchCreateDTO;
import com.example.lightmanager.dto.InspectionBatchPageRequestDTO;
import com.example.lightmanager.dto.InspectionItemSaveDTO;
import com.example.lightmanager.dto.ResponseDTO;
import com.example.lightmanager.entity.InspectionBatch;
import com.example.lightmanager.service.InspectionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inspection")
@CrossOrigin(origins = "*")
public class InspectionController {

    private final InspectionService inspectionService;

    public InspectionController(InspectionService inspectionService) {
        this.inspectionService = inspectionService;
    }

    /** 创建巡检批次（选择一个或多个分区，生成待登记条目及分区快照） */
    @PostMapping("/batch")
    public ResponseDTO<InspectionBatch> createBatch(@Valid @RequestBody InspectionBatchCreateDTO request) {
        return ResponseDTO.success(inspectionService.createBatch(request));
    }

    @GetMapping("/batch/page")
    public ResponseDTO<List<InspectionBatch>> getBatchPage(InspectionBatchPageRequestDTO request) {
        var pageInfo = inspectionService.getBatchPage(request);
        return ResponseDTO.success(pageInfo.getRecords(), pageInfo.getTotal());
    }

    @GetMapping("/batch/{id}")
    public ResponseDTO<InspectionBatch> getBatchDetail(@PathVariable Long id) {
        return ResponseDTO.success(inspectionService.getBatchDetail(id));
    }

    /** 暂存巡检结果，可多次补录 */
    @PutMapping("/batch/{id}/items")
    public ResponseDTO<InspectionBatch> saveItems(@PathVariable Long id,
                                                  @Valid @RequestBody InspectionItemSaveDTO request) {
        return ResponseDTO.success(inspectionService.saveItems(id, request));
    }

    /** 提交批次，冻结原始结果并形成异常清单 */
    @PostMapping("/batch/{id}/submit")
    public ResponseDTO<InspectionBatch> submitBatch(@PathVariable Long id,
                                                    @RequestBody(required = false) Map<String, String> body) {
        String operator = body != null ? body.get("operator") : null;
        return ResponseDTO.success(inspectionService.submitBatch(id, operator));
    }
}
