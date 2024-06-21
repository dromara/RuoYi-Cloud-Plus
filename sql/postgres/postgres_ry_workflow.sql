-- 请假单信息
create table test_leave
(
    id          bigint not null
        constraint test_leave_pk
            primary key,
    leave_type  varchar(255),
    start_date  timestamp,
    end_date    timestamp,
    leave_days  bigint,
    remark      varchar(255),
    status      varchar(255),
    create_dept bigint,
    create_by   bigint,
    create_time timestamp,
    update_by   bigint,
    update_time timestamp,
    tenant_id   varchar(20)
);

comment on table test_leave is '请假申请表';
comment on column test_leave.id is '主键';
comment on column test_leave.leave_type is '请假类型';
comment on column test_leave.start_date is '开始时间';
comment on column test_leave.end_date is '结束时间';
comment on column test_leave.remark is '请假原因';
comment on column test_leave.status is '状态';
comment on column test_leave.create_dept is '创建部门';
comment on column test_leave.create_by is '创建者';
comment on column test_leave.create_time is '创建时间';
comment on column test_leave.update_by is '更新者';
comment on column test_leave.update_time is '更新时间';
comment on column test_leave.tenant_id is '租户编码';

alter table test_leave
    owner to postgres;

-- 流程分类信息表
create table wf_category
(
    id            bigint not null
        constraint wf_category_pk
            primary key,
    category_name varchar(255),
    category_code varchar(255),
    parent_id     bigint,
    sort_num      bigint,
    tenant_id     varchar(20),
    create_dept   bigint,
    create_by     bigint,
    create_time   timestamp,
    update_by     bigint,
    update_time   timestamp
);

comment on table wf_category is '流程分类';
comment on column wf_category.id is '主键';
comment on column wf_category.category_name is '分类名称';
comment on column wf_category.category_code is '分类编码';
comment on column wf_category.parent_id is '父级id';
comment on column wf_category.sort_num is '排序';
comment on column wf_category.tenant_id is '租户id';
comment on column wf_category.create_dept is '创建部门';
comment on column wf_category.create_by is '创建者';
comment on column wf_category.create_time is '创建时间';
comment on column wf_category.update_by is '修改者';
comment on column wf_category.update_time is '修改时间';

alter table wf_category
    owner to postgres;

create unique index uni_category_code
    on wf_category (category_code);

INSERT INTO wf_category values (1, 'OA', 'OA', 0, 0, '000000', 103, 1, now(), 1, now());

create table wf_task_back_node
(
    id            bigint not null
        constraint pk_wf_task_back_node
            primary key,
    node_id       varchar(255) not null,
    node_name     varchar(255) not null,
    order_no      bigint not null,
    instance_id   varchar(255) not null,
    task_type     varchar(255) not null,
    assignee      varchar(2000) not null,
    tenant_id     varchar(20),
    create_dept   bigint,
    create_by     bigint,
    create_time   timestamp,
    update_by     bigint,
    update_time   timestamp
);

comment on table wf_task_back_node is '节点审批记录';
comment on column wf_task_back_node.id is '主键';
comment on column wf_task_back_node.node_id is '节点id';
comment on column wf_task_back_node.node_name is '节点名称';
comment on column wf_task_back_node.order_no is '排序';
comment on column wf_task_back_node.instance_id is '流程实例id';
comment on column wf_task_back_node.task_type is '节点类型';
comment on column wf_task_back_node.assignee is '审批人';
comment on column wf_task_back_node.tenant_id is '租户id';
comment on column wf_task_back_node.create_dept is '创建部门';
comment on column wf_task_back_node.create_by is '创建者';
comment on column wf_task_back_node.create_time is '创建时间';
comment on column wf_task_back_node.update_by is '修改者';
comment on column wf_task_back_node.update_time is '修改时间';

alter table wf_task_back_node
    owner to postgres;

create table wf_definition_config
(
    id            bigint not null
        constraint pk_wf_definition_config
            primary key,
    table_name    varchar(255) not null,
    definition_id varchar(255) not null,
    process_key   varchar(255) not null,
    version       bigint       not null,
    tenant_id     varchar(20),
    remark        varchar(500),
    create_dept   bigint,
    create_by     bigint,
    create_time   timestamp,
    update_by     bigint,
    update_time   timestamp
);

comment on table wf_definition_config is '流程定义配置';
comment on column wf_definition_config.id is '主键';
comment on column wf_definition_config.table_name is '表名';
comment on column wf_definition_config.definition_id is '流程定义ID';
comment on column wf_definition_config.process_key is '流程KEY';
comment on column wf_definition_config.version is '流程版本';
comment on column wf_definition_config.tenant_id is '租户id';
comment on column wf_definition_config.remark is '备注';
comment on column wf_definition_config.create_dept is '创建部门';
comment on column wf_definition_config.create_by is '创建者';
comment on column wf_definition_config.create_time is '创建时间';
comment on column wf_definition_config.update_by is '修改者';
comment on column wf_definition_config.update_time is '修改时间';

alter table wf_definition_config
    owner to postgres;
create unique index uni_definition_id
    on wf_definition_config (definition_id);

create table wf_form_manage
(
    id            bigint not null
        constraint pk_wf_form_manage
            primary key,
    form_name     varchar(255) not null,
    form_type     varchar(255) not null,
    router        varchar(255) not null,
    remark        varchar(500),
    tenant_id     varchar(20),
    create_dept   bigint,
    create_by     bigint,
    create_time   timestamp,
    update_by     bigint,
    update_time   timestamp
);

comment on table wf_form_manage is '表单管理';
comment on column wf_form_manage.id is '主键';
comment on column wf_form_manage.form_name is '表单名称';
comment on column wf_form_manage.form_type is '表单类型';
comment on column wf_form_manage.router is '路由地址/表单ID';
comment on column wf_form_manage.remark is '备注';
comment on column wf_form_manage.tenant_id is '租户id';
comment on column wf_form_manage.create_dept is '创建部门';
comment on column wf_form_manage.create_by is '创建者';
comment on column wf_form_manage.create_time is '创建时间';
comment on column wf_form_manage.update_by is '修改者';
comment on column wf_form_manage.update_time is '修改时间';

insert into wf_form_manage(id, form_name, form_type, router, remark, tenant_id, create_dept, create_by, create_time, update_by, update_time) VALUES (1, '请假申请', 'static', '/workflow/leaveEdit/index', NULL, '000000', 103, 1, now(), 1, now());

create table wf_node_config
(
    id               bigint not null
        constraint pk_wf_node_config
            primary key,
    form_id          bigint,
    form_type        varchar(255),
    node_name        varchar(255) not null,
    node_id          varchar(255) not null,
    definition_id    varchar(255) not null,
    apply_user_task  char(1) default '0',
    tenant_id        varchar(20),
    create_dept      bigint,
    create_by        bigint,
    create_time      timestamp,
    update_by        bigint,
    update_time      timestamp
);

comment on table wf_node_config is '节点配置';
comment on column wf_node_config.id is '主键';
comment on column wf_node_config.form_id is '表单id';
comment on column wf_node_config.form_type is '表单类型';
comment on column wf_node_config.node_id is '节点id';
comment on column wf_node_config.node_name is '节点名称';
comment on column wf_node_config.definition_id is '流程定义id';
comment on column wf_node_config.apply_user_task is '是否为申请人节点 （0是 1否）';
comment on column wf_node_config.tenant_id is '租户id';
comment on column wf_node_config.create_dept is '创建部门';
comment on column wf_node_config.create_by is '创建者';
comment on column wf_node_config.create_time is '创建时间';
comment on column wf_node_config.update_by is '修改者';
comment on column wf_node_config.update_time is '修改时间';
