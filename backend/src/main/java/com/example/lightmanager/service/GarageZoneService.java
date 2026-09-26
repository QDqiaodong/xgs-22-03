package com.example.lightmanager.service;

import com.example.lightmanager.entity.GarageZone;

import java.util.List;

public interface GarageZoneService {
    
    List<GarageZone> getZoneTree();
    
    GarageZone getZoneById(Long id);
    
    GarageZone createZone(GarageZone zone);
    
    GarageZone updateZone(GarageZone zone);
    
    void deleteZone(Long id);
    
    List<GarageZone> getAllZones();
}