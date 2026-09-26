package com.example.lightmanager.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lightmanager.constant.ArchiveConstants;
import com.example.lightmanager.dto.PageRequestDTO;
import com.example.lightmanager.entity.LightGroup;
import com.example.lightmanager.exception.BusinessException;
import com.example.lightmanager.mapper.LightGroupMapper;
import com.example.lightmanager.service.LightGroupService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LightGroupServiceImpl implements LightGroupService {

    private final LightGroupMapper lightGroupMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public LightGroupServiceImpl(LightGroupMapper lightGroupMapper, RedisTemplate<String, Object> redisTemplate) {
        this.lightGroupMapper = lightGroupMapper;
        this.redisTemplate = redisTemplate;
    }

    private static final String LIGHT_GROUP_POWER_KEY = "light_group:power:";

    @Override
    public LightGroup createLightGroup(LightGroup lightGroup) {
        // 新建灯组始终是正常台账状态，忽略客户端可能传入的归档字段
        lightGroup.setArchiveStatus(ArchiveConstants.STATUS_ACTIVE);
        lightGroup.setArchiveVersion(0);
        lightGroupMapper.insert(lightGroup);
        updateRedisPower(lightGroup);
        return lightGroup;
    }

    @Override
    public LightGroup updateLightGroup(LightGroup lightGroup) {
        LightGroup old = lightGroupMapper.selectById(lightGroup.getId());
        if (old == null) {
            throw new BusinessException("灯组不存在");
        }
        if (ArchiveConstants.STATUS_ARCHIVED.equals(old.getArchiveStatus())) {
            // 归档后只读：编号、分区、启用状态及其余字段都不能再被编辑或划转
            throw new BusinessException("该灯组已归档，台账记录只读，不能再编辑编号、分区或启用状态");
        }
        // 归档流程状态只能由归档专用接口驱动，普通编辑不得改写
        lightGroup.setArchiveStatus(old.getArchiveStatus());
        lightGroup.setArchiveVersion(old.getArchiveVersion());
        lightGroupMapper.updateById(lightGroup);
        if (!old.getZoneId().equals(lightGroup.getZoneId())) {
            removeRedisPower(old);
        }
        updateRedisPower(lightGroup);
        return lightGroup;
    }

    @Override
    public void deleteLightGroup(Long id) {
        LightGroup lightGroup = lightGroupMapper.selectById(id);
        if (lightGroup == null) {
            return;
        }
        if (ArchiveConstants.STATUS_ARCHIVED.equals(lightGroup.getArchiveStatus())) {
            throw new BusinessException("该灯组已归档，归档记录不可删除");
        }
        removeRedisPower(lightGroup);
        lightGroupMapper.deleteById(id);
    }

    @Override
    public LightGroup getLightGroupById(Long id) {
        return lightGroupMapper.selectById(id);
    }

    @Override
    public IPage<LightGroup> getLightGroupPage(PageRequestDTO request) {
        Page<LightGroup> page = new Page<>(request.getPageNum(), request.getPageSize());
        return lightGroupMapper.selectWithZoneNamePage(page, request.getZoneId(),
                request.getGroupCode(), request.getStatus(), request.getArchiveStatus());
    }

    @Override
    public List<LightGroup> getLightGroupsByIds(List<Long> ids) {
        return lightGroupMapper.selectByIds(ids);
    }

    @Override
    public List<LightGroup> getLightGroupsByZoneId(Long zoneId) {
        return lightGroupMapper.selectByZoneId(zoneId);
    }

    private void updateRedisPower(LightGroup lightGroup) {
        String key = LIGHT_GROUP_POWER_KEY + lightGroup.getZoneId();
        redisTemplate.opsForZSet().add(key, lightGroup.getGroupCode(), lightGroup.getPower());
    }

    private void removeRedisPower(LightGroup lightGroup) {
        String key = LIGHT_GROUP_POWER_KEY + lightGroup.getZoneId();
        redisTemplate.opsForZSet().remove(key, lightGroup.getGroupCode());
    }
}
