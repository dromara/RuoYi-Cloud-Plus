ALTER TABLE sys_logininfor ADD (client_key VARCHAR(32) DEFAULT '');
COMMENT ON COLUMN sys_logininfor.client_key IS '客户端';

ALTER TABLE sys_logininfor ADD (device_type VARCHAR(32) DEFAULT '');
COMMENT ON COLUMN sys_logininfor.device_type IS '设备类型';

insert into sys_menu values('124',  '缓存监控',     '2',   '1',  'cache',           'monitor/cache/index',          '', 1, 0, 'C', '0', '0', 'monitor:cache:list',          'redis',         103, 1, sysdate, null, null, '缓存监控');
