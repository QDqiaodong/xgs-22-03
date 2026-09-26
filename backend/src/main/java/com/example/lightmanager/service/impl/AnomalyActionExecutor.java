package com.example.lightmanager.service.impl;

import com.example.lightmanager.constant.InspectionConstants;
import com.example.lightmanager.dto.AnomalyActionDTO;
import com.example.lightmanager.entity.InspectionActionLog;
import com.example.lightmanager.entity.InspectionAnomaly;
import com.example.lightmanager.exception.BusinessException;
import com.example.lightmanager.exception.ConflictNeedsRefreshException;
import com.example.lightmanager.mapper.InspectionActionLogMapper;
import com.example.lightmanager.mapper.InspectionAnomalyMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 异常受控状态流转的事务执行器。
 * 每次调用只占用一个数据库连接：在同一事务内完成 CAS 条件更新 + 追加处置日志。
 * CAS 失败时不读取最新数据（避免高并发下嵌套事务占用第二个连接），直接抛出标记异常，
 * 由外层在事务结束、连接释放后再读取获胜方已提交的最新内容。
 */
@Component
public class AnomalyActionExecutor {

    private final InspectionAnomalyMapper anomalyMapper;
    private final InspectionActionLogMapper actionLogMapper;

    public AnomalyActionExecutor(InspectionAnomalyMapper anomalyMapper,
                                 InspectionActionLogMapper actionLogMapper) {
        this.anomalyMapper = anomalyMapper;
        this.actionLogMapper = actionLogMapper;
    }

    @FunctionalInterface
    public interface CasOperation {
        int execute(Long id, Integer version, String operator, LocalDateTime now);
    }

    @Transactional(rollbackFor = Exception.class)
    public void execute(Long id, AnomalyActionDTO request, String actionType,
                        String content, CasOperation cas) {
        InspectionAnomaly current = anomalyMapper.selectById(id);
        if (current == null) {
            throw new BusinessException("异常记录不存在");
        }
        if (InspectionConstants.STATUS_CLOSED.equals(current.getStatus())) {
            throw new BusinessException("该异常已关闭，记录只读，不能再进行任何操作");
        }

        LocalDateTime now = LocalDateTime.now();
        Integer fromVersion = current.getVersion();
        String operator = request.getOperator().trim();
        int affected = cas.execute(id, request.getVersion(), operator, now);

        if (affected == 0) {
            // 版本或期望状态已不匹配：放弃本次写入，回滚，交由外层读取最新内容提示
            throw new ConflictNeedsRefreshException(id, request.getVersion());
        }

        // 仅 CAS 成功才追加日志，因此并发下不会重复生成处理记录
        InspectionActionLog log = new InspectionActionLog();
        log.setAnomalyId(id);
        log.setActionType(actionType);
        log.setContent(content);
        if (InspectionConstants.ACTION_CONFIRM.equals(actionType)) {
            log.setDeadline(request.getDeadline());
        }
        log.setOperator(operator);
        log.setOperatedAt(now);
        log.setFromVersion(fromVersion);
        log.setToVersion(fromVersion + 1);
        actionLogMapper.insert(log);
    }
}
