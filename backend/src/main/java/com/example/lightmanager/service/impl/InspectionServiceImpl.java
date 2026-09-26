package com.example.lightmanager.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lightmanager.constant.InspectionConstants;
import com.example.lightmanager.dto.InspectionBatchCreateDTO;
import com.example.lightmanager.dto.InspectionBatchPageRequestDTO;
import com.example.lightmanager.dto.InspectionItemResultDTO;
import com.example.lightmanager.dto.InspectionItemSaveDTO;
import com.example.lightmanager.entity.GarageZone;
import com.example.lightmanager.entity.InspectionActionLog;
import com.example.lightmanager.entity.InspectionAnomaly;
import com.example.lightmanager.entity.InspectionBatch;
import com.example.lightmanager.entity.InspectionItem;
import com.example.lightmanager.entity.LightGroup;
import com.example.lightmanager.exception.BusinessException;
import com.example.lightmanager.mapper.GarageZoneMapper;
import com.example.lightmanager.mapper.InspectionActionLogMapper;
import com.example.lightmanager.mapper.InspectionAnomalyMapper;
import com.example.lightmanager.mapper.InspectionBatchMapper;
import com.example.lightmanager.mapper.InspectionItemMapper;
import com.example.lightmanager.mapper.LightGroupMapper;
import com.example.lightmanager.service.InspectionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InspectionServiceImpl implements InspectionService {

    private final InspectionBatchMapper batchMapper;
    private final InspectionItemMapper itemMapper;
    private final InspectionAnomalyMapper anomalyMapper;
    private final InspectionActionLogMapper actionLogMapper;
    private final LightGroupMapper lightGroupMapper;
    private final GarageZoneMapper garageZoneMapper;

    public InspectionServiceImpl(InspectionBatchMapper batchMapper,
                                 InspectionItemMapper itemMapper,
                                 InspectionAnomalyMapper anomalyMapper,
                                 InspectionActionLogMapper actionLogMapper,
                                 LightGroupMapper lightGroupMapper,
                                 GarageZoneMapper garageZoneMapper) {
        this.batchMapper = batchMapper;
        this.itemMapper = itemMapper;
        this.anomalyMapper = anomalyMapper;
        this.actionLogMapper = actionLogMapper;
        this.lightGroupMapper = lightGroupMapper;
        this.garageZoneMapper = garageZoneMapper;
    }

    private static final Set<String> RESULT_TYPES = Set.of(
            InspectionConstants.RESULT_NORMAL,
            InspectionConstants.RESULT_EXTINGUISHED,
            InspectionConstants.RESULT_FLICKER,
            InspectionConstants.RESULT_DIM);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionBatch createBatch(InspectionBatchCreateDTO request) {
        // 选择的分区及其子孙分区都纳入巡检范围
        List<GarageZone> allZones = garageZoneMapper.selectAllZones();
        Map<Long, GarageZone> zoneMap = allZones.stream()
                .collect(Collectors.toMap(GarageZone::getId, Function.identity()));
        Set<Long> selectedZoneIds = new HashSet<>(request.getZoneIds());
        for (Long zoneId : request.getZoneIds()) {
            collectChildZones(zoneId, allZones, selectedZoneIds);
        }

        List<LightGroup> lightGroups = lightGroupMapper.selectByZoneIds(new ArrayList<>(selectedZoneIds));
        // 已归档灯组为只读终态，不再纳入新的巡检批次（历史批次/异常中的引用不受影响）
        lightGroups = lightGroups.stream()
                .filter(lg -> !com.example.lightmanager.constant.ArchiveConstants.STATUS_ARCHIVED
                        .equals(lg.getArchiveStatus()))
                .collect(Collectors.toList());
        if (lightGroups.isEmpty()) {
            throw new BusinessException("所选分区下暂无可巡检灯组（灯组均已归档或为空），无法创建巡检批次");
        }

        LocalDateTime now = LocalDateTime.now();
        InspectionBatch batch = new InspectionBatch();
        batch.setBatchNo(generateBatchNo());
        batch.setInspector(request.getInspector().trim());
        batch.setZoneIds(request.getZoneIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        batch.setStatus(InspectionConstants.BATCH_DRAFT);
        batch.setTotalCount(lightGroups.size());
        batch.setAbnormalCount(0);
        batch.setRemark(request.getRemark());
        batch.setStartedAt(now);
        batchMapper.insert(batch);

        // 创建批次时即固化每个灯组的分区现场快照，之后划转不会改写历史
        List<InspectionItem> items = new ArrayList<>();
        for (LightGroup lg : lightGroups) {
            InspectionItem item = new InspectionItem();
            item.setBatchId(batch.getId());
            item.setLightGroupId(lg.getId());
            item.setGroupCode(lg.getGroupCode());
            item.setSnapshotZoneId(lg.getZoneId());
            item.setSnapshotZoneName(zoneMap.containsKey(lg.getZoneId())
                    ? zoneMap.get(lg.getZoneId()).getZoneName() : ("分区#" + lg.getZoneId()));
            item.setSnapshotLocation(lg.getInstallationLocation());
            items.add(item);
        }
        itemMapper.batchInsert(items);

        return getBatchDetail(batch.getId());
    }

    private void collectChildZones(Long parentId, List<GarageZone> allZones, Set<Long> acc) {
        for (GarageZone zone : allZones) {
            if (parentId.equals(zone.getParentId()) && acc.add(zone.getId())) {
                collectChildZones(zone.getId(), allZones, acc);
            }
        }
    }

    @Override
    public IPage<InspectionBatch> getBatchPage(InspectionBatchPageRequestDTO request) {
        Page<InspectionBatch> page = new Page<>(request.getPageNum(), request.getPageSize());
        return batchMapper.selectBatchPage(page, request.getBatchNo(), request.getInspector(),
                request.getStatus(), request.getStartTime(), request.getEndTime());
    }

    @Override
    public InspectionBatch getBatchDetail(Long batchId) {
        InspectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException("巡检批次不存在");
        }
        batch.setItems(itemMapper.selectByBatchId(batchId));
        return batch;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionBatch saveItems(Long batchId, InspectionItemSaveDTO request) {
        InspectionBatch batch = requireBatch(batchId);
        if (!InspectionConstants.BATCH_DRAFT.equals(batch.getStatus())) {
            throw new BusinessException("批次已提交，原始巡检结果不能再修改");
        }

        List<InspectionItem> dbItems = itemMapper.selectByBatchId(batchId);
        Map<Long, InspectionItem> itemMap = dbItems.stream()
                .collect(Collectors.toMap(InspectionItem::getId, Function.identity()));

        LocalDateTime now = LocalDateTime.now();
        for (InspectionItemResultDTO dto : request.getItems()) {
            InspectionItem item = itemMap.get(dto.getItemId());
            if (item == null) {
                throw new BusinessException("巡检条目不存在或不属于该批次");
            }
            if (!RESULT_TYPES.contains(dto.getResultType())) {
                throw new BusinessException("灯组" + item.getGroupCode() + "的巡检结果不合法");
            }
            // 暂存允许异常不填现场说明，之后继续补录
            item.setResultType(dto.getResultType());
            item.setDescription(dto.getDescription());
            item.setRecordedAt(now);
            itemMapper.updateById(item);
        }
        return getBatchDetail(batchId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionBatch submitBatch(Long batchId, String operator) {
        InspectionBatch batch = requireBatch(batchId);
        if (!InspectionConstants.BATCH_DRAFT.equals(batch.getStatus())) {
            throw new BusinessException("批次已提交，不能重复提交");
        }

        List<InspectionItem> items = itemMapper.selectByBatchId(batchId);
        List<InspectionAnomaly> anomalies = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (InspectionItem item : items) {
            if (item.getResultType() == null) {
                throw new BusinessException("灯组" + item.getGroupCode() + "尚未登记巡检结果，请补录后再提交");
            }
            if (!InspectionConstants.RESULT_NORMAL.equals(item.getResultType())
                    && (item.getDescription() == null || item.getDescription().trim().isEmpty())) {
                throw new BusinessException("灯组" + item.getGroupCode() + "登记为异常，必须填写现场说明");
            }
            if (!InspectionConstants.RESULT_NORMAL.equals(item.getResultType())) {
                InspectionAnomaly anomaly = new InspectionAnomaly();
                anomaly.setBatchId(batch.getId());
                anomaly.setBatchNo(batch.getBatchNo());
                anomaly.setItemId(item.getId());
                anomaly.setLightGroupId(item.getLightGroupId());
                anomaly.setGroupCode(item.getGroupCode());
                anomaly.setAnomalyType(item.getResultType());
                anomaly.setOriginalDescription(item.getDescription().trim());
                anomaly.setSnapshotZoneId(item.getSnapshotZoneId());
                anomaly.setSnapshotZoneName(item.getSnapshotZoneName());
                anomaly.setSnapshotLocation(item.getSnapshotLocation());
                anomaly.setStatus(InspectionConstants.STATUS_PENDING_REVIEW);
                anomaly.setReporter(batch.getInspector());
                anomaly.setReportedAt(now);
                anomaly.setVersion(0);
                anomalies.add(anomaly);
            }
        }

        // 批次提交后冻结原始结果，进入异常处置流程
        if (!anomalies.isEmpty()) {
            anomalyMapper.batchInsert(anomalies);

            // 为每条异常写入"上报"动作，形成处置记录起点
            for (InspectionAnomaly anomaly : anomalies) {
                InspectionActionLog log = new InspectionActionLog();
                log.setAnomalyId(anomaly.getId());
                log.setActionType(InspectionConstants.ACTION_SUBMIT);
                log.setContent("批次[" + batch.getBatchNo() + "]上报异常：" + anomaly.getOriginalDescription());
                log.setOperator(batch.getInspector());
                log.setOperatedAt(now);
                log.setFromVersion(0);
                log.setToVersion(0);
                actionLogMapper.insert(log);
            }
        }

        batch.setStatus(InspectionConstants.BATCH_SUBMITTED);
        batch.setAbnormalCount(anomalies.size());
        batch.setSubmittedAt(now);
        batchMapper.updateById(batch);

        return getBatchDetail(batchId);
    }

    private InspectionBatch requireBatch(Long batchId) {
        InspectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException("巡检批次不存在");
        }
        return batch;
    }

    private String generateBatchNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 10000);
        return "XC" + timestamp + String.format("%04d", random);
    }
}
