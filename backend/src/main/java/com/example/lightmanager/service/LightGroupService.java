package com.example.lightmanager.service;

import com.example.lightmanager.dto.PageRequestDTO;
import com.example.lightmanager.entity.LightGroup;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface LightGroupService {
    
    LightGroup createLightGroup(LightGroup lightGroup);
    
    LightGroup updateLightGroup(LightGroup lightGroup);
    
    void deleteLightGroup(Long id);
    
    LightGroup getLightGroupById(Long id);
    
    IPage<LightGroup> getLightGroupPage(PageRequestDTO request);
    
    List<LightGroup> getLightGroupsByIds(List<Long> ids);
    
    List<LightGroup> getLightGroupsByZoneId(Long zoneId);
}