package com.example.lightmanager.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lightmanager.constant.InspectionConstants;
import com.example.lightmanager.dto.AnomalyActionDTO;
import com.example.lightmanager.dto.AnomalyPageRequestDTO;
import com.example.lightmanager.entity.InspectionActionLog;
import com.example.lightmanager.entity.InspectionAnomaly;
import com.example.lightmanager.entity.LightGroup;
import com.example.lightmanager.exception.BusinessException;
import com.example.lightmanager.exception.ConflictException;
import com.example.lightmanager.exception.ConflictNeedsRefreshException;
import com.example.lightmanager.mapper.InspectionActionLogMapper;
import com.example.lightmanager.mapper.InspectionAnomalyMapper;
import com.example.lightmanager.mapper.LightGroupMapper;
import com.example.lightmanager.service.InspectionAnomalyService;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InspectionAnomalyServiceImpl implements InspectionAnomalyService {

    private static final DateTimeFormatter DEADLINE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final InspectionAnomalyMapper anomalyMapper;
    private final InspectionActionLogMapper actionLogMapper;
    private final LightGroupMapper lightGroupMapper;
    private final AnomalyActionExecutor actionExecutor;

    public InspectionAnomalyServiceImpl(InspectionAnomalyMapper anomalyMapper,
                                        InspectionActionLogMapper actionLogMapper,
                                        LightGroupMapper lightGroupMapper,
                                        AnomalyActionExecutor actionExecutor) {
        this.anomalyMapper = anomalyMapper;
        this.actionLogMapper = actionLogMapper;
        this.lightGroupMapper = lightGroupMapper;
        this.actionExecutor = actionExecutor;
    }

    @Override
    public IPage<InspectionAnomaly> getAnomalyPage(AnomalyPageRequestDTO request) {
        Page<InspectionAnomaly> page = new Page<>(request.getPageNum(), request.getPageSize());
        // 当前分区与 zone_changed 已在 SQL 中通过 JOIN 灯组/分区实时算出
        return anomalyMapper.selectAnomalyPage(page, request);
    }

    @Override
    public InspectionAnomaly getAnomalyDetail(Long id) {
        return loadAnomalyDetail(id);
    }

    @Override
    public InspectionAnomaly getAnomalyDetailFresh(Long id) {
        return loadAnomalyDetail(id);
    }

    private InspectionAnomaly loadAnomalyDetail(Long id) {
        InspectionAnomaly anomaly = anomalyMapper.selectById(id);
        if (anomaly == null) {
            throw new BusinessException("异常记录不存在");
        }
        fillCurrentZone(anomaly);
        anomaly.setZoneChanged(
                anomaly.getCurrentZoneId() == null
                        || !anomaly.getCurrentZoneId().equals(anomaly.getSnapshotZoneId()));
        List<InspectionActionLog> logs = actionLogMapper.selectByAnomalyId(id);
        anomaly.setLogs(logs);
        return anomaly;
    }

    /** 补全当前分区信息并标记分区是否已相对现场快照发生变化 */
    private void fillCurrentZone(InspectionAnomaly anomaly) {
        List<LightGroup> groups = lightGroupMapper.selectByIds(List.of(anomaly.getLightGroupId()));
        if (!groups.isEmpty()) {
            LightGroup lg = groups.get(0);
            anomaly.setCurrentZoneId(lg.getZoneId());
            anomaly.setCurrentZoneName(lg.getZoneName());
        } else {
            anomaly.setCurrentZoneId(null);
            anomaly.setCurrentZoneName(null);
        }
    }

    @Override
    public InspectionAnomaly returnAnomaly(Long id, AnomalyActionDTO request) {
        requireText(request.getReason(), "退回原因");
        return runAction(id, request, InspectionConstants.ACTION_RETURN, request.getReason(),
                (anomalyId, version, operator, now) ->
                        anomalyMapper.casReturn(anomalyId, version, operator, request.getReason(), now));
    }

    @Override
    public InspectionAnomaly supplement(Long id, AnomalyActionDTO request) {
        requireText(request.getSupplementDescription(), "补充说明");
        String supplement = request.getSupplementDescription().trim();
        String content = "补充现场说明：" + supplement;
        return runAction(id, request, InspectionConstants.ACTION_SUPPLEMENT, content,
                (anomalyId, version, operator, now) ->
                        anomalyMapper.casSupplement(anomalyId, version, supplement, operator, now));
    }

    @Override
    public InspectionAnomaly confirm(Long id, AnomalyActionDTO request) {
        requireText(request.getReviewOpinion(), "处理意见");
        if (request.getDeadline() == null) {
            throw new BusinessException("确认异常时必须填写处理时限");
        }
        String opinion = request.getReviewOpinion().trim();
        String deadlineText = request.getDeadline().format(DEADLINE_FORMATTER);
        String content = "确认异常。处理意见：" + opinion + "；处理时限：" + deadlineText;
        return runAction(id, request, InspectionConstants.ACTION_CONFIRM, content,
                (anomalyId, version, operator, now) ->
                        anomalyMapper.casConfirm(anomalyId, version, operator, opinion, request.getDeadline(), now));
    }

    @Override
    public InspectionAnomaly resolve(Long id, AnomalyActionDTO request) {
        requireText(request.getHandlingResult(), "处理结果");
        String result = request.getHandlingResult().trim();
        String content = "登记处理结果：" + result;
        return runAction(id, request, InspectionConstants.ACTION_RESOLVE, content,
                (anomalyId, version, operator, now) ->
                        anomalyMapper.casResolve(anomalyId, version, operator, result, now));
    }

    @Override
    public InspectionAnomaly close(Long id, AnomalyActionDTO request) {
        return runAction(id, request, InspectionConstants.ACTION_CLOSE, "关闭异常，处置收口",
                (anomalyId, version, operator, now) ->
                        anomalyMapper.casClose(anomalyId, version, operator, now));
    }

    /**
     * 外层编排（非事务）：
     * - 调用执行器在单个事务内完成 CAS + 日志；
     * - CAS 失败事务回滚后，连接已释放，此时再读取获胜方已提交的最新数据返回给后提交者，
     *   既不覆盖前一次结论，也不会重复生成处理记录。
     */
    private InspectionAnomaly runAction(Long id, AnomalyActionDTO request, String actionType,
                                        String content, AnomalyActionExecutor.CasOperation cas) {
        try {
            actionExecutor.execute(id, request, actionType, content, cas);
        } catch (ConflictNeedsRefreshException e) {
            InspectionAnomaly latest = loadAnomalyDetail(e.getAnomalyId());
            throw new ConflictException(buildConflictMessage(latest), latest);
        }
        return loadAnomalyDetail(id);
    }

    private String buildConflictMessage(InspectionAnomaly latest) {
        return "该异常刚被其他人操作过，当前状态已变为「" + statusText(latest.getStatus())
                + "」，页面显示的是最新内容；您的提交未生效，也未重复生成处理记录，请刷新后查看";
    }

    private String statusText(String status) {
        return switch (status) {
            case InspectionConstants.STATUS_PENDING_REVIEW -> "待复核";
            case InspectionConstants.STATUS_RETURNED -> "已退回";
            case InspectionConstants.STATUS_CONFIRMED -> "已确认";
            case InspectionConstants.STATUS_PROCESSING -> "处理中";
            case InspectionConstants.STATUS_CLOSED -> "已关闭";
            default -> status;
        };
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(field + "不能为空");
        }
    }
}
