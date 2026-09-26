package com.example.lightmanager.service.impl;

import com.example.lightmanager.entity.GarageZone;
import com.example.lightmanager.mapper.GarageZoneMapper;
import com.example.lightmanager.service.GarageZoneService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GarageZoneServiceImpl implements GarageZoneService {

    private final GarageZoneMapper garageZoneMapper;

    public GarageZoneServiceImpl(GarageZoneMapper garageZoneMapper) {
        this.garageZoneMapper = garageZoneMapper;
    }

    @Override
    public List<GarageZone> getZoneTree() {
        List<GarageZone> allZones = garageZoneMapper.selectAllZones();
        fillLightCounts(allZones);
        return buildTree(allZones);
    }

    private List<GarageZone> buildTree(List<GarageZone> zones) {
        Map<Long, List<GarageZone>> childrenMap = zones.stream()
                .filter(z -> z.getParentId() != null && z.getParentId() != 0)
                .collect(Collectors.groupingBy(GarageZone::getParentId));

        List<GarageZone> rootZones = zones.stream()
                .filter(z -> z.getParentId() == null || z.getParentId() == 0)
                .collect(Collectors.toList());

        for (GarageZone root : rootZones) {
            root.setChildren(childrenMap.getOrDefault(root.getId(), new ArrayList<>()));
        }
        return rootZones;
    }

    @Override
    public GarageZone getZoneById(Long id) {
        return garageZoneMapper.selectById(id);
    }

    @Override
    public GarageZone createZone(GarageZone zone) {
        garageZoneMapper.insert(zone);
        return zone;
    }

    @Override
    public GarageZone updateZone(GarageZone zone) {
        garageZoneMapper.updateById(zone);
        return zone;
    }

    @Override
    public void deleteZone(Long id) {
        garageZoneMapper.deleteById(id);
    }

    @Override
    public List<GarageZone> getAllZones() {
        List<GarageZone> zones = garageZoneMapper.selectAllZones();
        fillLightCounts(zones);
        return zones;
    }

    /** 分区列表共同显示已归档：每个分区灯组总数与已归档数 */
    private void fillLightCounts(List<GarageZone> zones) {
        Map<Long, int[]> counts = new HashMap<>();
        for (Map<String, Object> row : garageZoneMapper.selectZoneLightCounts()) {
            Object zoneIdObj = row.get("zoneId");
            if (zoneIdObj == null) {
                continue;
            }
            long zoneId = ((Number) zoneIdObj).longValue();
            int total = row.get("totalCount") == null ? 0 : ((Number) row.get("totalCount")).intValue();
            int archived = row.get("archivedCount") == null ? 0 : ((Number) row.get("archivedCount")).intValue();
            counts.put(zoneId, new int[]{total, archived});
        }
        for (GarageZone zone : zones) {
            int[] c = counts.getOrDefault(zone.getId(), new int[]{0, 0});
            zone.setLightCount(c[0]);
            zone.setArchivedLightCount(c[1]);
        }
    }
}
