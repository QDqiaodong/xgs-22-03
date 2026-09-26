package com.example.lightmanager.service.impl;

import com.example.lightmanager.entity.LightArchiveRecord;
import com.example.lightmanager.entity.LightGroup;
import com.example.lightmanager.exception.ArchiveConflictNeedsRefreshException;
import com.example.lightmanager.mapper.ArchiveFlowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 归档状态流转事务执行器：在同一事务内完成 行锁 -> 事务内校验 -> CAS 条件更新 -> 追加归档历史。
 * 采用 READ_COMMITTED：确认归档事务内复检引用时读取各引用表的最新已提交快照，
 * 避免 REPEATABLE_READ 下复检仍用事务开始时旧快照导致的 TOCTOU 漏检。
 * CAS 失败时不读取最新数据（避免高并发嵌套占用第二个连接），直接抛出标记异常，
 * 由外层在事务结束、连接释放后再读取获胜方已提交的最新内容。
 * 仅 CAS 成功才追加历史，因此两人同时复核时不会额外增加一条复核历史。
 */
@Component
public class ArchiveActionExecutor {

    private final ArchiveFlowMapper archiveFlowMapper;

    public ArchiveActionExecutor(ArchiveFlowMapper archiveFlowMapper) {
        this.archiveFlowMapper = archiveFlowMapper;
    }

    @FunctionalInterface
    public interface CasOperation {
        int execute();
    }

    /** 历史条目构造器：CAS 成功后在同一事务内生成 */
    @FunctionalInterface
    public interface ArchiveRecordBuilder {
        LightArchiveRecord build(LightGroup lockedGroup);
    }

    /** 事务内（行锁后、CAS 前）附加校验，如归档确认前的引用复检 */
    @FunctionalInterface
    public interface TransactionCheck {
        void verify(LightGroup lockedGroup);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void execute(Long lightGroupId, Integer clientVersion,
                        ArchiveRecordBuilder recordBuilder, CasOperation cas) {
        execute(lightGroupId, clientVersion, recordBuilder, cas, null);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void execute(Long lightGroupId, Integer clientVersion,
                        ArchiveRecordBuilder recordBuilder, CasOperation cas,
                        TransactionCheck check) {
        // 行锁：串行化并发复核，以及"确认归档"与"新引用写入"之间的竞态
        LightGroup locked = archiveFlowMapper.selectLightForUpdate(lightGroupId);
        if (locked == null) {
            throw new ArchiveConflictNeedsRefreshException(lightGroupId, clientVersion);
        }
        if (check != null) {
            check.verify(locked);
        }
        int affected = cas.execute();
        if (affected == 0) {
            // 版本或期望状态已不匹配：放弃写入并回滚，交由外层读取最新意见提示
            throw new ArchiveConflictNeedsRefreshException(lightGroupId, clientVersion);
        }
        archiveFlowMapper.insertRecord(recordBuilder.build(locked));
    }
}
