package com.example.lightmanager.service.impl;

import com.example.lightmanager.constant.ArchiveConstants;
import com.example.lightmanager.dto.ArchiveReferenceBlockDTO;
import com.example.lightmanager.dto.ArchiveReviewDTO;
import com.example.lightmanager.dto.ArchiveSubmitDTO;
import com.example.lightmanager.entity.GarageZone;
import com.example.lightmanager.entity.LightArchiveRecord;
import com.example.lightmanager.entity.LightGroup;
import com.example.lightmanager.exception.ArchiveConflictException;
import com.example.lightmanager.exception.ArchiveConflictNeedsRefreshException;
import com.example.lightmanager.exception.BusinessException;
import com.example.lightmanager.exception.ReferenceBlockException;
import com.example.lightmanager.mapper.ArchiveFlowMapper;
import com.example.lightmanager.mapper.GarageZoneMapper;
import com.example.lightmanager.mapper.LightGroupMapper;
import com.example.lightmanager.service.LightArchiveService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LightArchiveServiceImpl implements LightArchiveService {

    private final ArchiveFlowMapper archiveFlowMapper;
    private final LightGroupMapper lightGroupMapper;
    private final GarageZoneMapper garageZoneMapper;
    private final ArchiveActionExecutor actionExecutor;

    public LightArchiveServiceImpl(ArchiveFlowMapper archiveFlowMapper,
                                   LightGroupMapper lightGroupMapper,
                                   GarageZoneMapper garageZoneMapper,
                                   ArchiveActionExecutor actionExecutor) {
        this.archiveFlowMapper = archiveFlowMapper;
        this.lightGroupMapper = lightGroupMapper;
        this.garageZoneMapper = garageZoneMapper;
        this.actionExecutor = actionExecutor;
    }

    @Override
    public LightGroup getArchiveDetail(Long lightGroupId) {
        return loadDetail(lightGroupId);
    }

    private LightGroup loadDetail(Long lightGroupId) {
        LightGroup group = lightGroupMapper.selectById(lightGroupId);
        if (group == null) {
            throw new BusinessException("灯组不存在");
        }
        fillZoneName(group);
        group.setArchiveRecords(archiveFlowMapper.selectRecordsByLightGroupId(lightGroupId));
        return group;
    }

    private void fillZoneName(LightGroup group) {
        GarageZone zone = garageZoneMapper.selectById(group.getZoneId());
        if (zone != null) {
            group.setZoneName(zone.getZoneName());
        }
    }

    @Override
    public List<ArchiveReferenceBlockDTO> checkReferences(Long lightGroupId) {
        requireGroup(lightGroupId);
        return collectBlocks(lightGroupId);
    }

    private List<ArchiveReferenceBlockDTO> collectBlocks(Long lightGroupId) {
        List<ArchiveReferenceBlockDTO> blocks = new ArrayList<>();
        blocks.addAll(archiveFlowMapper.selectOpenInspectionBatches(lightGroupId));
        blocks.addAll(archiveFlowMapper.selectOpenAnomalies(lightGroupId));
        blocks.addAll(archiveFlowMapper.selectOpenTransferLedgers(lightGroupId));
        return blocks;
    }

    @Override
    public LightGroup submit(Long lightGroupId, ArchiveSubmitDTO request) {
        requireText(request.getScrapReason(), "报废理由");
        requireText(request.getPhotoNote(), "现场照片说明");
        if (request.getEffectiveDate() == null) {
            throw new BusinessException("报废生效日期不能为空");
        }
        LightGroup group = requireGroup(lightGroupId);
        if (ArchiveConstants.STATUS_ARCHIVED.equals(group.getArchiveStatus())) {
            throw new BusinessException("该灯组已归档，记录只读");
        }
        // 是否为正常状态、版本是否匹配交由 CAS 判定：
        // 另一标签页已先送审时，本次提交拿到过期版本，返回 409 + 最新内容
        LocalDateTime now = LocalDateTime.now();
        String operator = request.getOperator().trim();
        String reason = request.getScrapReason().trim();
        String photoNote = request.getPhotoNote().trim();
        LocalDate effectiveDate = request.getEffectiveDate();

        try {
            actionExecutor.execute(lightGroupId, request.getVersion(),
                    locked -> buildSubmitRecord(locked, 1, ArchiveConstants.ACTION_SUBMIT, reason,
                            null, photoNote, effectiveDate, null, operator, now),
                    () -> archiveFlowMapper.casSubmit(lightGroupId, request.getVersion(),
                            reason, photoNote, effectiveDate, operator, now));
        } catch (ArchiveConflictNeedsRefreshException e) {
            throwConflict(e);
        }
        return loadDetail(lightGroupId);
    }

    @Override
    public LightGroup resubmit(Long lightGroupId, ArchiveSubmitDTO request) {
        requireText(request.getSupplementNote(), "补充内容");
        LightGroup group = requireGroup(lightGroupId);
        if (ArchiveConstants.STATUS_ARCHIVED.equals(group.getArchiveStatus())) {
            throw new BusinessException("该灯组已归档，记录只读");
        }

        LocalDateTime now = LocalDateTime.now();
        String operator = request.getOperator().trim();
        String supplement = request.getSupplementNote().trim();

        try {
            // 是否允许再送审（RETURNED）及版本是否匹配交由 CAS 判定：
            // 另一标签页已再送审时，本次拿到的是过期版本，返回 409 + 最新意见，而非误报业务错误
            actionExecutor.execute(lightGroupId, request.getVersion(),
                    // 行锁内计算新轮次，并以锁定行上的原始理由/照片/生效日期作为本次历史快照
                    locked -> {
                        int round = archiveFlowMapper.selectMaxRound(lightGroupId) + 1;
                        return buildSubmitRecord(locked, round, ArchiveConstants.ACTION_RESUBMIT,
                                locked.getScrapReason(), supplement, locked.getPhotoNote(),
                                locked.getEffectiveDate(), null, operator, now);
                    },
                    () -> archiveFlowMapper.casResubmit(lightGroupId, request.getVersion(),
                            supplement, operator, now));
        } catch (ArchiveConflictNeedsRefreshException e) {
            throwConflict(e);
        }
        return loadDetail(lightGroupId);
    }

    @Override
    public LightGroup reviewReturn(Long lightGroupId, ArchiveReviewDTO request) {
        requireText(request.getReason(), "退回原因");
        LightGroup group = requireGroup(lightGroupId);
        if (ArchiveConstants.STATUS_ARCHIVED.equals(group.getArchiveStatus())) {
            throw new BusinessException("该灯组已归档，记录只读");
        }

        LocalDateTime now = LocalDateTime.now();
        String operator = request.getOperator().trim();
        String reason = request.getReason().trim();

        try {
            // 是否处于待复核、版本是否匹配交由 CAS 判定：
            // 两人同时复核时后提交者拿到过期版本，返回 409 + 最新意见，且不增加历史
            actionExecutor.execute(lightGroupId, request.getVersion(),
                    locked -> buildReviewRecord(locked, archiveFlowMapper.selectMaxRound(lightGroupId),
                            ArchiveConstants.ACTION_RETURN, reason, operator, now),
                    () -> archiveFlowMapper.casReturn(lightGroupId, request.getVersion(),
                            operator, reason, now));
        } catch (ArchiveConflictNeedsRefreshException e) {
            throwConflict(e);
        }
        return loadDetail(lightGroupId);
    }

    @Override
    public LightGroup confirmArchive(Long lightGroupId, ArchiveReviewDTO request) {
        requireText(request.getOpinion(), "归档确认意见");
        LightGroup group = requireGroup(lightGroupId);
        if (ArchiveConstants.STATUS_ARCHIVED.equals(group.getArchiveStatus())) {
            throw new BusinessException("该灯组已归档，记录只读");
        }
        // 非待复核（如已退回、被另一标签页先处理）不做引用预检；
        // 是否待复核、版本是否匹配交由事务内 CAS 判定，失败返回 409 + 最新意见
        boolean pending = ArchiveConstants.STATUS_PENDING_REVIEW.equals(group.getArchiveStatus());
        if (pending) {
            // 确认前先在页面侧引用检查给用户预览；事务内、行锁后再复检一次，全部解除才允许归档
            List<ArchiveReferenceBlockDTO> preview = collectBlocks(lightGroupId);
            if (!preview.isEmpty()) {
                throw new ReferenceBlockException("仍存在 " + preview.size()
                        + " 条未解除引用，归档被阻断；请按下列记录逐一解除后再确认归档", preview);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        String operator = request.getOperator().trim();
        String opinion = request.getOpinion().trim();

        try {
            actionExecutor.execute(lightGroupId, request.getVersion(),
                    locked -> buildReviewRecord(locked, archiveFlowMapper.selectMaxRound(lightGroupId),
                            ArchiveConstants.ACTION_ARCHIVE, opinion, operator, now),
                    () -> archiveFlowMapper.casArchive(lightGroupId, request.getVersion(),
                            operator, opinion, now),
                    locked -> {
                        // 事务内复检：此时持有灯组行锁，引用集合为已提交的最新快照
                        List<ArchiveReferenceBlockDTO> blocks = collectBlocks(lightGroupId);
                        if (!blocks.isEmpty()) {
                            throw new ReferenceBlockException("仍存在 " + blocks.size()
                                    + " 条未解除引用，归档被阻断；请按下列记录逐一解除后再确认归档", blocks);
                        }
                    });
        } catch (ArchiveConflictNeedsRefreshException e) {
            throwConflict(e);
        }
        return loadDetail(lightGroupId);
    }

    private LightArchiveRecord buildSubmitRecord(LightGroup group, int round, String action,
                                                 String reason, String supplement, String photoNote,
                                                 LocalDate effectiveDate, String opinion,
                                                 String operator, LocalDateTime now) {
        LightArchiveRecord record = baseRecord(group, round, action, operator, now);
        record.setScrapReason(reason);
        record.setSupplementNote(supplement);
        record.setPhotoNote(photoNote);
        record.setEffectiveDate(effectiveDate);
        record.setOpinion(opinion);
        return record;
    }

    private LightArchiveRecord buildReviewRecord(LightGroup group, int round, String action,
                                                 String opinion, String operator, LocalDateTime now) {
        LightArchiveRecord record = baseRecord(group, round, action, operator, now);
        // 退回/确认动作保留当时版本的原始理由、补充与生效信息，历史条目自成快照
        record.setScrapReason(group.getScrapReason());
        record.setSupplementNote(group.getSupplementNote());
        record.setPhotoNote(group.getPhotoNote());
        record.setEffectiveDate(group.getEffectiveDate());
        record.setOpinion(opinion);
        return record;
    }

    private LightArchiveRecord baseRecord(LightGroup group, int round, String action,
                                          String operator, LocalDateTime now) {
        LightArchiveRecord record = new LightArchiveRecord();
        record.setLightGroupId(group.getId());
        record.setRound(round);
        record.setActionType(action);
        record.setOperator(operator);
        record.setOperatedAt(now);
        // 历史条目保留当时的编号和分区，不被后续台账状态改写
        record.setSnapshotGroupCode(group.getGroupCode());
        record.setSnapshotZoneId(group.getZoneId());
        GarageZone zone = garageZoneMapper.selectById(group.getZoneId());
        record.setSnapshotZoneName(zone != null ? zone.getZoneName() : ("分区#" + group.getZoneId()));
        record.setFromVersion(group.getArchiveVersion());
        record.setToVersion(group.getArchiveVersion() + 1);
        return record;
    }

    private LightGroup requireGroup(Long id) {
        LightGroup group = lightGroupMapper.selectById(id);
        if (group == null) {
            throw new BusinessException("灯组不存在");
        }
        return group;
    }

    /** CAS 失败：事务已回滚、连接已释放，再读取获胜方提交的最新内容返回冲突提示 */
    private void throwConflict(ArchiveConflictNeedsRefreshException e) {
        LightGroup latest = loadDetail(e.getLightGroupId());
        throw new ArchiveConflictException(buildConflictMessage(latest), latest);
    }

    private String buildConflictMessage(LightGroup latest) {
        return "该送审版本刚被其他人处理过，当前状态已变为「"
                + archiveStatusText(latest.getArchiveStatus())
                + "」；页面已刷新为最新意见与历史，您的提交未生效，也没有重复增加复核记录";
    }

    private String archiveStatusText(String status) {
        return switch (status) {
            case ArchiveConstants.STATUS_ACTIVE -> "正常";
            case ArchiveConstants.STATUS_PENDING_REVIEW -> "待复核";
            case ArchiveConstants.STATUS_RETURNED -> "已退回";
            case ArchiveConstants.STATUS_ARCHIVED -> "已归档";
            default -> status;
        };
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(field + "不能为空");
        }
    }
}
