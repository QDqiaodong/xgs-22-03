SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS light_manager DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE light_manager;

CREATE TABLE IF NOT EXISTS garage_zone (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分区ID',
    zone_code VARCHAR(50) NOT NULL UNIQUE COMMENT '分区编码',
    zone_name VARCHAR(100) NOT NULL COMMENT '分区名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父分区ID',
    level INT DEFAULT 1 COMMENT '层级',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    version INT NOT NULL DEFAULT 0 COMMENT '层级版本号(乐观锁): 换父级/编辑后自增, 过期树提交返回409',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='车库分区表';

CREATE TABLE IF NOT EXISTS light_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '灯组ID',
    group_code VARCHAR(50) NOT NULL UNIQUE COMMENT '灯组编号',
    power INT NOT NULL COMMENT '功率(W)',
    zone_id BIGINT NOT NULL COMMENT '所属分区ID',
    installation_location VARCHAR(200) COMMENT '安装位置',
    status TINYINT DEFAULT 1 COMMENT '启用状态: 0停用 1启用',
    archive_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '报废归档生命周期: ACTIVE正常 PENDING_REVIEW待复核 RETURNED已退回 ARCHIVED已归档',
    archive_version INT NOT NULL DEFAULT 0 COMMENT '归档流程乐观锁版本号',
    scrap_reason VARCHAR(1000) COMMENT '报废理由(原始理由,不被后续补充改写)',
    supplement_note VARCHAR(2000) COMMENT '最近一次补充说明',
    photo_note VARCHAR(1000) COMMENT '现场照片说明',
    effective_date DATE COMMENT '报废生效日期',
    archive_applicant VARCHAR(50) COMMENT '最近送审操作人',
    archive_applied_at DATETIME COMMENT '最近送审时间',
    archive_reviewer VARCHAR(50) COMMENT '最近复核人',
    archive_review_opinion VARCHAR(1000) COMMENT '最近复核意见(退回原因或确认意见)',
    archive_archived_at DATETIME COMMENT '归档生效时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_zone_id (zone_id),
    INDEX idx_group_code (group_code),
    INDEX idx_status (status),
    INDEX idx_archive_status (archive_status),
    CONSTRAINT fk_light_zone FOREIGN KEY (zone_id) REFERENCES garage_zone(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='照明灯组表';

CREATE TABLE IF NOT EXISTS batch_transfer_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '台账ID',
    batch_no VARCHAR(64) NOT NULL UNIQUE COMMENT '批次号',
    operator VARCHAR(50) NOT NULL COMMENT '操作人',
    transfer_time DATETIME NOT NULL COMMENT '划转时间',
    source_zone_ids TEXT COMMENT '源分区ID列表',
    target_zone_id BIGINT NOT NULL COMMENT '目标分区ID',
    light_count INT NOT NULL COMMENT '划转灯组数量',
    description VARCHAR(500) COMMENT '操作描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_batch_no (batch_no),
    INDEX idx_transfer_time (transfer_time),
    INDEX idx_operator (operator)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='批量操作台账表';

CREATE TABLE IF NOT EXISTS ledger_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '明细ID',
    ledger_id BIGINT NOT NULL COMMENT '台账ID',
    light_group_id BIGINT NOT NULL COMMENT '灯组ID',
    source_zone_id BIGINT NOT NULL COMMENT '原所属分区ID',
    target_zone_id BIGINT NOT NULL COMMENT '新所属分区ID',
    INDEX idx_ledger_id (ledger_id),
    INDEX idx_light_group_id (light_group_id),
    CONSTRAINT fk_detail_ledger FOREIGN KEY (ledger_id) REFERENCES batch_transfer_ledger(id),
    CONSTRAINT fk_detail_light FOREIGN KEY (light_group_id) REFERENCES light_group(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='台账明细表';

CREATE TABLE IF NOT EXISTS inspection_batch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '巡检批次ID',
    batch_no VARCHAR(64) NOT NULL UNIQUE COMMENT '巡检批次号',
    inspector VARCHAR(50) NOT NULL COMMENT '巡检人',
    zone_ids TEXT NOT NULL COMMENT '巡检分区ID列表(逗号分隔)',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT暂存 SUBMITTED已提交',
    total_count INT NOT NULL DEFAULT 0 COMMENT '灯组总数',
    abnormal_count INT NOT NULL DEFAULT 0 COMMENT '异常数量',
    remark VARCHAR(500) COMMENT '批次备注',
    started_at DATETIME NOT NULL COMMENT '巡检开始时间(分区快照时点)',
    submitted_at DATETIME COMMENT '提交时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_batch_no (batch_no),
    INDEX idx_status (status),
    INDEX idx_inspector (inspector)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='灯组巡检批次表';

CREATE TABLE IF NOT EXISTS inspection_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '巡检条目ID',
    batch_id BIGINT NOT NULL COMMENT '巡检批次ID',
    light_group_id BIGINT NOT NULL COMMENT '灯组ID',
    group_code VARCHAR(50) NOT NULL COMMENT '灯组编号(快照)',
    result_type VARCHAR(20) COMMENT '巡检结果: NORMAL正常 EXTINGUISHED熄灭 FLICKER频闪 DIM亮度不足',
    description VARCHAR(1000) COMMENT '现场说明(异常必填)',
    snapshot_zone_id BIGINT NOT NULL COMMENT '巡检开始时所属分区ID(现场快照)',
    snapshot_zone_name VARCHAR(100) NOT NULL COMMENT '巡检开始时所属分区名称(现场快照)',
    snapshot_location VARCHAR(200) COMMENT '巡检开始时安装位置(现场快照)',
    recorded_at DATETIME COMMENT '登记时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_batch_light (batch_id, light_group_id),
    INDEX idx_batch_id (batch_id),
    INDEX idx_light_group_id (light_group_id),
    CONSTRAINT fk_item_batch FOREIGN KEY (batch_id) REFERENCES inspection_batch(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='巡检结果条目表(原始结果不可覆盖)';

CREATE TABLE IF NOT EXISTS inspection_anomaly (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '异常ID',
    batch_id BIGINT NOT NULL COMMENT '巡检批次ID',
    batch_no VARCHAR(64) NOT NULL COMMENT '巡检批次号(冗余)',
    item_id BIGINT NOT NULL COMMENT '巡检条目ID',
    light_group_id BIGINT NOT NULL COMMENT '灯组ID',
    group_code VARCHAR(50) NOT NULL COMMENT '灯组编号',
    anomaly_type VARCHAR(20) NOT NULL COMMENT '异常类型: EXTINGUISHED熄灭 FLICKER频闪 DIM亮度不足',
    original_description VARCHAR(1000) NOT NULL COMMENT '原始现场说明(不可修改)',
    supplement_description VARCHAR(1000) COMMENT '补充说明(退回补充时更新)',
    snapshot_zone_id BIGINT NOT NULL COMMENT '巡检时分区ID(快照)',
    snapshot_zone_name VARCHAR(100) NOT NULL COMMENT '巡检时分区名称(快照)',
    snapshot_location VARCHAR(200) COMMENT '巡检时安装位置(快照)',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT '状态: PENDING_REVIEW待复核 RETURNED已退回 CONFIRMED已确认 PROCESSING处理中 CLOSED已关闭',
    reporter VARCHAR(50) NOT NULL COMMENT '上报人',
    reported_at DATETIME NOT NULL COMMENT '上报时间',
    reviewer VARCHAR(50) COMMENT '复核确认人',
    review_opinion VARCHAR(1000) COMMENT '处理意见(确认时填写)',
    handling_deadline DATETIME COMMENT '处理时限(确认时填写)',
    confirmed_at DATETIME COMMENT '确认时间',
    handler VARCHAR(50) COMMENT '处理结果登记人',
    handling_result VARCHAR(1000) COMMENT '处理结果',
    handled_at DATETIME COMMENT '处理结果登记时间',
    closer VARCHAR(50) COMMENT '关闭人',
    closed_at DATETIME COMMENT '关闭时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_batch_id (batch_id),
    INDEX idx_light_group_id (light_group_id),
    INDEX idx_status (status),
    INDEX idx_group_code (group_code),
    CONSTRAINT fk_anomaly_batch FOREIGN KEY (batch_id) REFERENCES inspection_batch(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='巡检异常处置表';

CREATE TABLE IF NOT EXISTS inspection_action_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    anomaly_id BIGINT NOT NULL COMMENT '异常ID',
    action_type VARCHAR(30) NOT NULL COMMENT '动作: SUBMIT上报 RETURN退回 SUPPLEMENT补充 CONFIRM确认 RESOLVE登记处理结果 CLOSE关闭',
    content VARCHAR(2000) COMMENT '操作内容',
    deadline DATETIME COMMENT '处理时限(确认动作记录)',
    operator VARCHAR(50) NOT NULL COMMENT '操作人',
    operated_at DATETIME NOT NULL COMMENT '操作时间',
    from_version INT COMMENT '操作前版本号',
    to_version INT COMMENT '操作后版本号',
    INDEX idx_anomaly_id (anomaly_id),
    INDEX idx_operated_at (operated_at),
    CONSTRAINT fk_log_anomaly FOREIGN KEY (anomaly_id) REFERENCES inspection_anomaly(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异常处置操作日志表(追加不可改)';

CREATE TABLE IF NOT EXISTS light_archive_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '归档历史ID',
    light_group_id BIGINT NOT NULL COMMENT '灯组ID',
    round INT NOT NULL COMMENT '复核轮次: 首次送审为1, 每次退回后再送审加1',
    action_type VARCHAR(20) NOT NULL COMMENT '动作: SUBMIT送审 RETURN退回 RESUBMIT再送审 ARCHIVE确认归档',
    scrap_reason VARCHAR(1000) COMMENT '本次送审报废理由(送审/再送审时保存, 首次后不可改)',
    supplement_note VARCHAR(2000) COMMENT '本次补充内容(再送审时保存)',
    photo_note VARCHAR(1000) COMMENT '现场照片说明(送审时保存)',
    effective_date DATE COMMENT '报废生效日期(送审时保存)',
    opinion VARCHAR(1000) COMMENT '复核意见(退回原因/确认意见)',
    operator VARCHAR(50) NOT NULL COMMENT '操作人',
    operated_at DATETIME NOT NULL COMMENT '操作时间',
    snapshot_group_code VARCHAR(50) NOT NULL COMMENT '操作时灯组编号(快照, 不被后续台账状态改写)',
    snapshot_zone_id BIGINT NOT NULL COMMENT '操作时所属分区ID(快照)',
    snapshot_zone_name VARCHAR(100) NOT NULL COMMENT '操作时所属分区名称(快照)',
    from_version INT COMMENT '操作前归档版本号',
    to_version INT COMMENT '操作后归档版本号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_light_group_id (light_group_id),
    INDEX idx_operated_at (operated_at),
    CONSTRAINT fk_archive_log_light FOREIGN KEY (light_group_id) REFERENCES light_group(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='灯组报废归档历史表(同一条链路追加不可改)';

-- 既有库升级脚本：以下 ALTER 在全新库上跳过、在旧库上补列，重复执行安全
-- 1) 划转流水核销字段（旧库补列）
SET @ddl := (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'batch_transfer_ledger'
       AND COLUMN_NAME = 'settle_status') = 0,
    "ALTER TABLE batch_transfer_ledger
        ADD COLUMN settle_status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT '流水核销状态: OPEN未核销 CLOSED已核销(解除归档引用)',
        ADD COLUMN settled_by VARCHAR(50) COMMENT '核销人',
        ADD COLUMN settled_at DATETIME COMMENT '核销时间',
        ADD INDEX idx_settle_status (settle_status)",
    'SELECT 1'));
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 分区层级乐观锁版本号（旧库补列）
SET @ddl := (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'garage_zone'
       AND COLUMN_NAME = 'version') = 0,
    "ALTER TABLE garage_zone
        ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '层级版本号(乐观锁): 换父级/编辑后自增, 过期树提交返回409' AFTER status",
    'SELECT 1'));
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO garage_zone (zone_code, zone_name, parent_id, level, sort_order, status) VALUES
('ZONE-A', 'A区车库', 0, 1, 1, 1),
('ZONE-B', 'B区车库', 0, 1, 2, 1),
('ZONE-C', 'C区车库', 0, 1, 3, 1),
('ZONE-A1', 'A区-一层', 1, 2, 1, 1),
('ZONE-A2', 'A区-二层', 1, 2, 2, 1),
('ZONE-B1', 'B区-一层', 2, 2, 1, 1),
('ZONE-B2', 'B区-二层', 2, 2, 2, 1);

INSERT IGNORE INTO light_group (group_code, power, zone_id, installation_location, status) VALUES
('LG-A1-001', 30, 4, 'A区一层-入口处', 1),
('LG-A1-002', 30, 4, 'A区一层-主通道', 1),
('LG-A1-003', 25, 4, 'A区一层-停车位A1', 1),
('LG-A1-004', 25, 4, 'A区一层-停车位A2', 1),
('LG-A1-005', 30, 4, 'A区一层-电梯口', 1),
('LG-A2-001', 30, 5, 'A区二层-入口处', 1),
('LG-A2-002', 30, 5, 'A区二层-主通道', 1),
('LG-A2-003', 25, 5, 'A区二层-停车位B1', 1),
('LG-A2-004', 25, 5, 'A区二层-停车位B2', 1),
('LG-A2-005', 30, 5, 'A区二层-电梯口', 1),
('LG-B1-001', 30, 6, 'B区一层-入口处', 1),
('LG-B1-002', 30, 6, 'B区一层-主通道', 1),
('LG-B1-003', 25, 6, 'B区一层-停车位C1', 1),
('LG-B1-004', 25, 6, 'B区一层-停车位C2', 1),
('LG-B2-001', 30, 7, 'B区二层-入口处', 1),
('LG-B2-002', 30, 7, 'B区二层-主通道', 1),
('LG-B2-003', 25, 7, 'B区二层-停车位D1', 1),
('LG-C-001', 30, 3, 'C区车库-入口处', 1),
('LG-C-002', 30, 3, 'C区车库-主通道', 1),
('LG-C-003', 25, 3, 'C区车库-停车位E1', 1);
