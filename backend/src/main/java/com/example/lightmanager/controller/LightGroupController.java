package com.example.lightmanager.controller;

import com.alibaba.excel.EasyExcel;
import com.example.lightmanager.dto.PageRequestDTO;
import com.example.lightmanager.dto.ResponseDTO;
import com.example.lightmanager.entity.LightGroup;
import com.example.lightmanager.service.LightGroupService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/light-group")
@CrossOrigin(origins = "*")
public class LightGroupController {
    
    private final LightGroupService lightGroupService;
    
    public LightGroupController(LightGroupService lightGroupService) {
        this.lightGroupService = lightGroupService;
    }
    
    @GetMapping("/page")
    public ResponseDTO<List<LightGroup>> getLightGroupPage(PageRequestDTO request) {
        var pageInfo = lightGroupService.getLightGroupPage(request);
        return ResponseDTO.success(pageInfo.getRecords(), pageInfo.getTotal());
    }
    
    @GetMapping("/{id}")
    public ResponseDTO<LightGroup> getLightGroupById(@PathVariable Long id) {
        return ResponseDTO.success(lightGroupService.getLightGroupById(id));
    }
    
    @GetMapping("/by-zone/{zoneId}")
    public ResponseDTO<List<LightGroup>> getLightGroupsByZoneId(@PathVariable Long zoneId) {
        return ResponseDTO.success(lightGroupService.getLightGroupsByZoneId(zoneId));
    }
    
    @PostMapping
    public ResponseDTO<LightGroup> createLightGroup(@RequestBody LightGroup lightGroup) {
        return ResponseDTO.success(lightGroupService.createLightGroup(lightGroup));
    }
    
    @PutMapping
    public ResponseDTO<LightGroup> updateLightGroup(@RequestBody LightGroup lightGroup) {
        return ResponseDTO.success(lightGroupService.updateLightGroup(lightGroup));
    }
    
    @DeleteMapping("/{id}")
    public ResponseDTO<Void> deleteLightGroup(@PathVariable Long id) {
        lightGroupService.deleteLightGroup(id);
        return ResponseDTO.success(null);
    }
    
    @GetMapping("/export/{zoneId}")
    public ResponseEntity<byte[]> exportByZoneId(@PathVariable Long zoneId) throws IOException {
        List<LightGroup> lightGroups = lightGroupService.getLightGroupsByZoneId(zoneId);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        EasyExcel.write(outputStream, LightGroupExportData.class)
                .sheet("灯组资产")
                .doWrite(lightGroups.stream()
                        .map(LightGroupExportData::fromEntity)
                        .toList());
        
        byte[] bytes = outputStream.toByteArray();
        
        String fileName = "灯组资产_" + zoneId + "_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", 
                new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
        
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
    
    @GetMapping("/export-selected")
    public ResponseEntity<byte[]> exportSelected(@RequestParam String ids) throws IOException {
        List<Long> idList = java.util.Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        List<LightGroup> lightGroups = lightGroupService.getLightGroupsByIds(idList);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        EasyExcel.write(outputStream, LightGroupExportData.class)
                .sheet("灯组资产")
                .doWrite(lightGroups.stream()
                        .map(LightGroupExportData::fromEntity)
                        .toList());
        
        byte[] bytes = outputStream.toByteArray();
        
        String fileName = "灯组资产_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", 
                new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
        
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
    
    public static class LightGroupExportData {
        
        @com.alibaba.excel.annotation.ExcelProperty("灯组编号")
        private String groupCode;
        
        @com.alibaba.excel.annotation.ExcelProperty("功率(W)")
        private Integer power;
        
        @com.alibaba.excel.annotation.ExcelProperty("所属分区")
        private String zoneName;
        
        @com.alibaba.excel.annotation.ExcelProperty("安装位置")
        private String installationLocation;
        
        @com.alibaba.excel.annotation.ExcelProperty("状态")
        private String status;
        
        public static LightGroupExportData fromEntity(LightGroup entity) {
            LightGroupExportData data = new LightGroupExportData();
            data.setGroupCode(entity.getGroupCode());
            data.setPower(entity.getPower());
            data.setZoneName(entity.getZoneName());
            data.setInstallationLocation(entity.getInstallationLocation());
            data.setStatus(entity.getStatus() == 1 ? "启用" : "停用");
            return data;
        }
        
        public String getGroupCode() { return groupCode; }
        public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
        public Integer getPower() { return power; }
        public void setPower(Integer power) { this.power = power; }
        public String getZoneName() { return zoneName; }
        public void setZoneName(String zoneName) { this.zoneName = zoneName; }
        public String getInstallationLocation() { return installationLocation; }
        public void setInstallationLocation(String installationLocation) { this.installationLocation = installationLocation; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}