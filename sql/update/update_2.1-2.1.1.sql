ALTER TABLE sys_logininfor
    ADD COLUMN client_key VARCHAR(32)  NULL DEFAULT NULL COMMENT '客户端' AFTER `user_name`,
    ADD COLUMN device_type VARCHAR(32) NULL DEFAULT NULL COMMENT '设备类型' AFTER `client_key`;

insert into sys_menu values('124',  '缓存监控',     '2',   '1',  'cache',           'monitor/cache/index',          '', 1, 0, 'C', '0', '0', 'monitor:cache:list',          'redis',         103, 1, sysdate(), null, null, '缓存监控');
