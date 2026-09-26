package com.example.lightmanager.controller;

import com.example.lightmanager.dto.ResponseDTO;
import com.example.lightmanager.entity.GarageZone;
import com.example.lightmanager.service.GarageZoneService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zone")
@CrossOrigin(origins = "*")
public class GarageZoneController {
    
    private final GarageZoneService garageZoneService;
    
    public GarageZoneController(GarageZoneService garageZoneService) {
        this.garageZoneService = garageZoneService;
    }
    
    @GetMapping("/tree")
    public ResponseDTO<List<GarageZone>> getZoneTree() {
        return ResponseDTO.success(garageZoneService.getZoneTree());
    }
    
    @GetMapping("/list")
    public ResponseDTO<List<GarageZone>> getAllZones() {
        return ResponseDTO.success(garageZoneService.getAllZones());
    }
    
    @GetMapping("/{id}")
    public ResponseDTO<GarageZone> getZoneById(@PathVariable Long id) {
        return ResponseDTO.success(garageZoneService.getZoneById(id));
    }
    
    @PostMapping
    public ResponseDTO<GarageZone> createZone(@RequestBody GarageZone zone) {
        return ResponseDTO.success(garageZoneService.createZone(zone));
    }
    
    @PutMapping
    public ResponseDTO<GarageZone> updateZone(@RequestBody GarageZone zone) {
        return ResponseDTO.success(garageZoneService.updateZone(zone));
    }
    
    @DeleteMapping("/{id}")
    public ResponseDTO<Void> deleteZone(@PathVariable Long id) {
        garageZoneService.deleteZone(id);
        return ResponseDTO.success(null);
    }
}