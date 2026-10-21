-- Warm Flow 2.1.0 schema and stored-state migration.
-- ============================================================
-- Warm-Flow 2.1.0 MySQL 增量升级脚本
-- 适用：从 2.0.1（或更早）升级到 2.1.0。
-- ============================================================

-- ---- 原 2.0.2：节点配置列 ----
-- 服务任务等节点在 flow_node 上持久化 node_config，历史任务快照同一份配置。
ALTER TABLE `flow_node`
    ADD COLUMN `node_config` text NULL COMMENT '节点配置（服务任务、事件等引擎节点配置json）' AFTER `ext`;

ALTER TABLE `flow_his_task`
    ADD COLUMN `node_config` text NULL COMMENT '节点配置（服务任务、事件等引擎节点配置json）' AFTER `ext`;

-- ---- 原 2.0.2：流程定义配置列 ----
-- 自动审批等定义级引擎配置从业务 ext 迁至 def_config，ext 回归业务系统独占。
-- 引擎只读 def_config 中的 autoPass，不回退读 ext。
ALTER TABLE `flow_definition`
    ADD COLUMN `def_config` text NULL COMMENT '流程定义配置（自动审批等引擎配置与自定义扩展属性json）' AFTER `ext`;

-- ---- 2.1.0：节点事件列 ----
-- 事件节点在 flow_node 上持久化事件类型与消息名称；事件 JSON 配置复用 node_config 的 event 段，不新增 event_config 列。
-- 兼容：旧数据四个事件列均为 NULL，表示无事件，既有流程行为不变；固定 START / END 不承载事件。
ALTER TABLE `flow_node`
    ADD COLUMN `event_type` varchar(40) NULL COMMENT '事件类型（MESSAGE_START TIMER_START TIMER_CATCH MESSAGE_CATCH MESSAGE_THROW TIMER_BOUNDARY MESSAGE_BOUNDARY ERROR_CATCH ERROR_BOUNDARY TERMINATE_END MESSAGE_END）' AFTER `node_config`,
    ADD COLUMN `event_name` varchar(100) NULL COMMENT '事件名称（消息事件使用）' AFTER `event_type`,
    ADD COLUMN `attached_to_node_code` varchar(100) NULL COMMENT '边界事件附着的任务节点编码' AFTER `event_name`,
    ADD COLUMN `interrupting` tinyint(1) NULL COMMENT '边界事件是否中断型' AFTER `attached_to_node_code`;

-- node_config 注释定稿：自定义扩展属性实际在 node.ext（NodeServiceImpl.getExt 读 getExt()），node_config 是服务任务 / 事件等引擎节点配置容器。
-- 从 2.0.2 草稿升级过的库已带旧注释，此句统一改到定稿；全新从 2.0.1 升级的库上一句已建对，重复执行无害。
ALTER TABLE `flow_node`
    MODIFY COLUMN `node_config` text NULL COMMENT '节点配置（服务任务、事件等引擎节点配置json）';

-- 状态编码重排：v2.1.0 把状态拆成「实例状态 / 任务状态 / 协作方式」三个独立维度（映射见 StatusMapper），
-- 三个维度各用一套小写英文编码，落库即自解释。不同表是不同维度、各有独立编码空间，同名编码互不影响。
-- 已按 2.0.1 或更早落库的宿主，存量数字码在列类型改成 varchar 后会变成 '1' 这类字符串，
-- 引擎按新编码查不到——本脚本末尾附了存量数据迁移段，按顺序执行即可，无需再跑其它脚本。
--   flow_instance.flow_status：draft待提交 waiting审批中 termination终止 invalid作废 cancel撤销 finish已完成 back已退回 restart重启
--   flow_task / flow_his_task.flow_status：pending待审批 pass通过 auto_pass自动审批 back退回 invalid失效 take_back拿回 copy抄送
--   flow_his_task.cooperate_type：none无 transfer转办 delegate委派 add_signer加签 remove_signer减签（只记协作动作；会签 / 票签由 node_ratio 决定，抄送由 flow_status=copy 表达，均不落此列）

-- 列类型 / 注释同步为新编码（cooperate_type 由 tinyint 改为 varchar，编码从数字换成英文）
ALTER TABLE `flow_instance`
    MODIFY COLUMN `flow_status` varchar(20) NOT NULL COMMENT '实例状态（draft待提交 waiting审批中 termination终止 invalid作废 cancel撤销 finish已完成 back已退回 restart重启）';
ALTER TABLE `flow_task`
    MODIFY COLUMN `flow_status` varchar(20) NOT NULL COMMENT '任务状态（pending待审批 pass通过 auto_pass自动审批 back退回 invalid失效 take_back拿回 copy抄送；任务结果，勿套用实例状态编码）';
ALTER TABLE `flow_his_task`
    MODIFY COLUMN `flow_status` varchar(20) NOT NULL COMMENT '任务状态（pending待审批 pass通过 auto_pass自动审批 back退回 invalid失效 take_back拿回 copy抄送；任务结果，勿套用实例状态编码，唯一例外是撤回归档写 cancel）',
    MODIFY COLUMN `cooperate_type` varchar(20) NOT NULL DEFAULT 'none' COMMENT '协作方式（none无 transfer转办 delegate委派 add_signer加签 remove_signer减签；行为维度，抄送/会签/票签不落此列，结果见 flow_status）';

-- flow_user 新增抄送人类型：type 增加 4，associated 语义扩展为「关联id」——
-- 抄送人记录挂在抄送历史（flow_his_task.id）上，待办办理完被删除后抄送仍然留档。
ALTER TABLE `flow_user`
    MODIFY COLUMN `type` char(1) NOT NULL COMMENT '人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限 4待办任务的抄送人）',
    MODIFY COLUMN `associated` bigint NOT NULL COMMENT '关联id（待办任务id；抄送人记录为 flow_his_task.id）';

-- 事件等待表：定时 / 消息等需要“等待”的事件统一落在此表，状态独立于 flow_status。
-- 用途：承载定时启动、中间定时捕获、中间消息捕获、定时边界、消息边界等待；一次性成功后删除，DEAD / CANCELLED 按保留期清理。
-- 索引：event_wait_status_next / event_wait_status_retry 支撑到期与重试扫描；event_wait_tenant_status_* 支撑多租户扫描；
--       event_wait_instance_task、event_wait_name_instance_status、event_wait_def_node_status 支撑实例 / 消息 / 定义维度的查询与取消。
-- 兼容：全新表，旧流程数据无记录；事件能力默认关闭，未开启时不扫描本表。
-- 回滚：DROP TABLE `flow_event_wait`；再 ALTER TABLE `flow_node` DROP COLUMN event_type, DROP COLUMN event_name,
--       DROP COLUMN attached_to_node_code, DROP COLUMN interrupting（回滚前须确认无事件定义引用这些列）。
CREATE TABLE `flow_event_wait`
(
    `id`               bigint       NOT NULL COMMENT '主键id',
    `tenant_id`        varchar(40)  DEFAULT NULL COMMENT '租户id',
    `del_flag`         char(1)      DEFAULT '0' COMMENT '删除标志',
    `definition_id`    bigint       DEFAULT NULL COMMENT '流程定义id（定时启动事件必须有）',
    `instance_id`      bigint       DEFAULT NULL COMMENT '流程实例id（实例等待事件必须有）',
    `node_code`        varchar(100) DEFAULT NULL COMMENT '事件节点编码（定位沿用 definition_id + node_code）',
    `event_type`       varchar(40)  NOT NULL COMMENT '事件类型（MESSAGE_START TIMER_START TIMER_CATCH MESSAGE_CATCH MESSAGE_THROW TIMER_BOUNDARY MESSAGE_BOUNDARY ERROR_CATCH ERROR_BOUNDARY TERMINATE_END MESSAGE_END）',
    `event_name`       varchar(100) DEFAULT NULL COMMENT '事件名称（消息事件使用）',
    `attached_task_id` bigint       DEFAULT NULL COMMENT '边界事件关联的待办任务id',
    `trigger_type`     varchar(40)  DEFAULT NULL COMMENT '触发策略类型（AFTER FIXED POINT_IN_TIME）',
    `trigger_interval` varchar(100) DEFAULT NULL COMMENT '触发策略原始配置',
    `next_trigger_at`  datetime     DEFAULT NULL COMMENT '下次触发时间',
    `timezone`         varchar(40)  DEFAULT NULL COMMENT '定时触发使用的时区',
    `repeat_count`     int          DEFAULT NULL COMMENT '已触发次数',
    `max_repeat_count` int          DEFAULT NULL COMMENT '最大触发次数',
    `status`           varchar(20)  NOT NULL COMMENT '等待状态（WAITING RUNNING CANCELLED RETRY DEAD）',
    `retry_count`      int          DEFAULT NULL COMMENT '已重试次数',
    `max_retry_count`  int          DEFAULT NULL COMMENT '最大重试次数',
    `retry_at`         datetime     DEFAULT NULL COMMENT '下次重试时间',
    `lock_owner`       varchar(64)  DEFAULT NULL COMMENT '抢占持有者标识',
    `lock_until`       datetime     DEFAULT NULL COMMENT '抢占租约到期时间',
    `version`          int          DEFAULT NULL COMMENT '乐观锁版本号',
    `payload`          text         COMMENT '消息事件接收的业务载荷',
    `last_error`       varchar(500) DEFAULT NULL COMMENT '最近一次失败原因摘要',
    `create_time`      datetime     DEFAULT NULL COMMENT '创建时间',
    `update_time`      datetime     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `event_wait_status_next` (`status`, `next_trigger_at`, `id`) USING BTREE,
    KEY `event_wait_status_retry` (`status`, `retry_at`, `id`) USING BTREE,
    KEY `event_wait_tenant_status_next` (`tenant_id`, `status`, `next_trigger_at`, `id`) USING BTREE,
    KEY `event_wait_tenant_status_retry` (`tenant_id`, `status`, `retry_at`, `id`) USING BTREE,
    KEY `event_wait_instance_task` (`instance_id`, `attached_task_id`) USING BTREE,
    KEY `event_wait_name_instance_status` (`event_name`, `instance_id`, `status`) USING BTREE,
    KEY `event_wait_def_node_status` (`definition_id`, `node_code`, `status`) USING BTREE
) ENGINE = InnoDB COMMENT ='事件等待表';

-- ---- 2.1.0：状态编码存量数据迁移 ----
-- 已按 2.0.1 或更早落库的宿主，把三列存量数字码迁到新编码；全新库 WHERE 一行不命中，重复执行无害。
-- 前置：本文件上方的 ALTER 已把列改成 varchar，按顺序整篇执行即可，勿单独摘 UPDATE 到 ALTER 之前跑。

-- 实例维度：0待提交→draft 1审批中→waiting 4终止→termination 5作废→invalid 6撤销→cancel
--          8已完成→finish 9已退回→back 12重启→restart；已移除的 7取回→waiting（取回属任务维度）、13暂存→draft（与待提交合并）
UPDATE flow_instance
SET flow_status = CASE flow_status
                      WHEN '0' THEN 'draft'
                      WHEN '1' THEN 'waiting'
                      WHEN '4' THEN 'termination'
                      WHEN '5' THEN 'invalid'
                      WHEN '6' THEN 'cancel'
                      WHEN '7' THEN 'waiting'
                      WHEN '8' THEN 'finish'
                      WHEN '9' THEN 'back'
                      WHEN '12' THEN 'restart'
                      WHEN '13' THEN 'draft'
    END
WHERE flow_status IN ('0', '1', '4', '5', '6', '7', '8', '9', '12', '13');

-- 任务维度（flow_task 与 flow_his_task 同套编码）：1待办理→pending 2通过→pass 9退回→back
--          10失效→invalid 11拿回→take_back；已移除的 8结束→pass（结束留痕统一按通过归档，是否结束看实例状态）
UPDATE flow_task
SET flow_status = CASE flow_status
                      WHEN '1' THEN 'pending'
                      WHEN '2' THEN 'pass'
                      WHEN '8' THEN 'pass'
                      WHEN '9' THEN 'back'
                      WHEN '10' THEN 'invalid'
                      WHEN '11' THEN 'take_back'
    END
WHERE flow_status IN ('1', '2', '8', '9', '10', '11');

UPDATE flow_his_task
SET flow_status = CASE flow_status
                      WHEN '1' THEN 'pending'
                      WHEN '2' THEN 'pass'
                      WHEN '8' THEN 'pass'
                      WHEN '9' THEN 'back'
                      WHEN '10' THEN 'invalid'
                      WHEN '11' THEN 'take_back'
    END
WHERE flow_status IN ('1', '2', '8', '9', '10', '11');

-- 协作维度：0/1无→none 2转办→transfer 3委派→delegate 6加签→add_signer 7减签→remove_signer；
--          已移除的 4会签 / 5票签→none——会签票签改由 flow_node.node_ratio 表达、抄送由 flow_status=copy 表达。
--          注意：迁移后「该历史当初按会签/票签写入」这一信息丢失，依赖 cooperate_type=4/5 的报表要改用 node_ratio 口径。
UPDATE flow_his_task
SET cooperate_type = CASE cooperate_type
                         WHEN '0' THEN 'none'
                         WHEN '1' THEN 'none'
                         WHEN '2' THEN 'transfer'
                         WHEN '3' THEN 'delegate'
                         WHEN '4' THEN 'none'
                         WHEN '5' THEN 'none'
                         WHEN '6' THEN 'add_signer'
                         WHEN '7' THEN 'remove_signer'
    END
WHERE cooperate_type IN ('0', '1', '2', '3', '4', '5', '6', '7');

-- Cloud dictionary entries for the new task and instance states.
-- 1. 节点版本已统一由流程定义管理，flow_node.version 在引擎 1.8.9 起就不存在了
--    旧全量脚本里该列是 NOT NULL 且无默认值，引擎写节点会报「doesn't have a default value」
ALTER TABLE `flow_node` DROP COLUMN `version`;

-- 2. 任务状态字典改为引擎 TaskStatus 的 7 个码，外加撤回归档留痕的 cancel
DELETE FROM sys_dict_data WHERE dict_type = 'wf_task_status';
INSERT INTO sys_dict_data VALUES (1761600000000000060, 1, '待审批', 'pending', 'wf_task_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '待审批');
INSERT INTO sys_dict_data VALUES (1761600000000000049, 2, '通过', 'pass', 'wf_task_status', '', 'success', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '通过');
INSERT INTO sys_dict_data VALUES (1761600000000000061, 3, '自动审批', 'auto_pass', 'wf_task_status', '', 'success', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '自动审批');
INSERT INTO sys_dict_data VALUES (1761600000000000056, 4, '抄送', 'copy', 'wf_task_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '抄送');
INSERT INTO sys_dict_data VALUES (1761600000000000052, 5, '退回', 'back', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '退回');
INSERT INTO sys_dict_data VALUES (1761600000000000062, 6, '拿回', 'take_back', 'wf_task_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '拿回');
INSERT INTO sys_dict_data VALUES (1761600000000000051, 7, '失效', 'invalid', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '失效');
INSERT INTO sys_dict_data VALUES (1761600000000000048, 8, '撤销', 'cancel', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '撤销');
INSERT INTO sys_dict_data VALUES (1761600000000000064, 9, '转办', 'transfer', 'wf_task_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '转办');
INSERT INTO sys_dict_data VALUES (1761600000000000065, 10, '委派', 'delegate', 'wf_task_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '委派');
INSERT INTO sys_dict_data VALUES (1761600000000000066, 11, '加签', 'add_signer', 'wf_task_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '加签');
INSERT INTO sys_dict_data VALUES (1761600000000000067, 12, '减签', 'remove_signer', 'wf_task_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '减签');

-- 原来的 waiting / termination / transfer / depute / sign / sign_off / timeout 已随上面整组删除：
-- 前两个属实例维度（wf_business_status 有），其余属协作维度 cooperate_type（transfer / delegate /
-- add_signer / remove_signer），timeout 是 flow_event_wait 的状态，都不是任务状态。
-- take_back 目前没有写入方，是留给「拿回」功能的占位，别当冗余删掉。

-- 3. 实例状态字典补 restart（引擎 FlowStatus 有，字典缺）
INSERT INTO sys_dict_data VALUES (1761600000000000063, 8, '重启', 'restart', 'wf_business_status', '', 'info', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '重启');
