package com.example.lightmanager.controller;

import com.example.lightmanager.dto.AnomalyActionDTO;
import com.example.lightmanager.dto.AnomalyPageRequestDTO;
import com.example.lightmanager.dto.ResponseDTO;
import com.example.lightmanager.entity.InspectionAnomaly;
import com.example.lightmanager.service.InspectionAnomalyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inspection-anomaly")
@CrossOrigin(origins = "*")
public class InspectionAnomalyController {

    private final InspectionAnomalyService anomalyService;

    public InspectionAnomalyController(InspectionAnomalyService anomalyService) {
        this.anomalyService = anomalyService;
    }

    /** 异常清单 */
    @GetMapping("/page")
    public ResponseDTO<List<InspectionAnomaly>> getAnomalyPage(AnomalyPageRequestDTO request) {
        var pageInfo = anomalyService.getAnomalyPage(request);
        return ResponseDTO.success(pageInfo.getRecords(), pageInfo.getTotal());
    }

    /** 异常详情（含连续处置记录） */
    @GetMapping("/{id}")
    public ResponseDTO<InspectionAnomaly> getAnomalyDetail(@PathVariable Long id) {
        return ResponseDTO.success(anomalyService.getAnomalyDetail(id));
    }

    /** 复核退回，补充后重新进入待复核 */
    @PostMapping("/{id}/return")
    public ResponseDTO<InspectionAnomaly> returnAnomaly(@PathVariable Long id,
                                                        @Valid @RequestBody AnomalyActionDTO request) {
        return ResponseDTO.success(anomalyService.returnAnomaly(id, request));
    }

    /** 巡检人补充说明 */
    @PostMapping("/{id}/supplement")
    public ResponseDTO<InspectionAnomaly> supplement(@PathVariable Long id,
                                                     @Valid @RequestBody AnomalyActionDTO request) {
        return ResponseDTO.success(anomalyService.supplement(id, request));
    }

    /** 复核确认异常（处理意见 + 处理时限） */
    @PostMapping("/{id}/confirm")
    public ResponseDTO<InspectionAnomaly> confirm(@PathVariable Long id,
                                                  @Valid @RequestBody AnomalyActionDTO request) {
        return ResponseDTO.success(anomalyService.confirm(id, request));
    }

    /** 登记处理结果 */
    @PostMapping("/{id}/resolve")
    public ResponseDTO<InspectionAnomaly> resolve(@PathVariable Long id,
                                                  @Valid @RequestBody AnomalyActionDTO request) {
        return ResponseDTO.success(anomalyService.resolve(id, request));
    }

    /** 关闭（终态只读） */
    @PostMapping("/{id}/close")
    public ResponseDTO<InspectionAnomaly> close(@PathVariable Long id,
                                                @Valid @RequestBody AnomalyActionDTO request) {
        return ResponseDTO.success(anomalyService.close(id, request));
    }
}
