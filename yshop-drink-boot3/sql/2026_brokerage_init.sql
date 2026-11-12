-- ============================================================
-- 推广分销 / 佣金分成 初始化脚本
-- 说明：
--   1) 佣金相关配置全部可后台配置（system_config 表），功能默认关闭。
--   2) yshop_user 需包含字段 brokerage_price / spread_uid / spread_time /
--      is_promoter / spread_count（标准 yshop 已自带；若缺失请按需 ALTER）。
-- ============================================================

-- 分销配置（INSERT IGNORE：已存在则不重复写入，方便重复执行）
INSERT IGNORE INTO system_config(config_key, config_value, name, remark, create_time, update_time, deleted)
VALUES
    ('store_brokerage_open',          '0',   '分销功能总开关',     '1=开启 0=关闭',        NOW(), NOW(), 0),
    ('store_brokerage_ratio',         '10',  '一级佣金比例(%)',   '商品未单独设佣金时按比例', NOW(), NOW(), 0),
    ('store_brokerage_two',           '0',   '二级分销开关',      '1=开启 0=关闭',        NOW(), NOW(), 0),
    ('store_brokerage_two_ratio',     '5',   '二级佣金比例(%)',   '商品未单独设佣金时按比例', NOW(), NOW(), 0),
    ('store_brokerage_settle',        '1',   '佣金结算时机',      '1=支付成功 2=确认收货',  NOW(), NOW(), 0),
    ('store_brokerage_withdraw_min',  '100', '最低提现门槛(元)',  '低于此金额不可提现',      NOW(), NOW(), 0);

-- 佣金提现记录表
CREATE TABLE IF NOT EXISTS `yshop_user_brokerage_withdraw` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid`          BIGINT       NOT NULL COMMENT '用户编号',
    `price`        DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '提现金额',
    `status`       INT          NOT NULL DEFAULT 0 COMMENT '审核状态：0待审核 1通过 -1拒绝',
    `mark`         VARCHAR(255)  DEFAULT '' COMMENT '审核备注',
    `tenant_id`    BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_uid` (`uid`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='佣金提现申请';
