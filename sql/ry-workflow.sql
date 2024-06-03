-- 请假单信息
create table test_leave
(
    id          bigint                       not null comment '主键',
    leave_type  varchar(255)                 not null comment '请假类型',
    start_date   datetime                     not null comment '开始时间',
    end_date     datetime                     not null comment '结束时间',
    leave_days  int(10)                      not null comment '请假天数',
    remark      varchar(255)                 null comment '请假原因',
    status      varchar(255)                 null comment '状态',
    create_dept bigint                       null comment '创建部门',
    create_by   bigint                       null comment '创建者',
    create_time datetime                     null comment '创建时间',
    update_by   bigint                       null comment '更新者',
    update_time datetime                     null comment '更新时间',
    tenant_id   varchar(20)                  null comment '租户编号',
    PRIMARY KEY (id) USING BTREE
) ENGINE = InnoDB COMMENT = '请假申请表';

-- 流程分类信息表
create table wf_category
(
    id            bigint                       not null comment '主键'
        primary key,
    category_name varchar(255)                 null comment '分类名称',
    category_code varchar(255)                 null comment '分类编码',
    parent_id     bigint                       null comment '父级id',
    sort_num      int(19)                      null comment '排序',
    tenant_id     varchar(20)                  null comment '租户编号',
    create_dept   bigint                       null comment '创建部门',
    create_by     bigint                       null comment '创建者',
    create_time   datetime                     null comment '创建时间',
    update_by     bigint                       null comment '更新者',
    update_time   datetime                     null comment '更新时间',
    constraint uni_category_code
        unique (category_code)
) engine=innodb comment= '流程分类';
INSERT INTO wf_category values (1, 'OA', 'OA', 0, 0, '000000', 103, 1, sysdate(), 1, sysdate());

create table wf_task_back_node
(
    id          bigint                       not null
        primary key,
    node_id     varchar(255)                 not null comment '节点id',
    node_name   varchar(255)                 not null comment '节点名称',
    order_no    int                          not null comment '排序',
    instance_id varchar(255)                 null comment '流程实例id',
    task_type   varchar(255)                 not null comment '节点类型',
    assignee    varchar(2000)                not null comment '审批人',
    tenant_id   varchar(20)                  null comment '租户编号',
    create_dept bigint                       null comment '创建部门',
    create_by   bigint                       null comment '创建者',
    create_time datetime                     null comment '创建时间',
    update_by   bigint                       null comment '更新者',
    update_time datetime                     null comment '更新时间'
)
    comment '节点审批记录';

create table wf_definition_config
(
    id            bigint                        not null comment '主键'
        primary key,
    table_name    varchar(255)                  not null comment '表名',
    definition_id varchar(255)                  not null comment '流程定义ID',
    process_key   varchar(255)                  not null comment '流程KEY',
    version       int(10)                       not null comment '流程版本',
    create_dept   bigint                        null comment '创建部门',
    create_by     bigint                        null comment '创建者',
    create_time   datetime                      null comment '创建时间',
    update_by     bigint                        null comment '更新者',
    update_time   datetime                      null comment '更新时间',
    remark        varchar(500) default ''       null comment '备注',
    tenant_id     varchar(20)                   null comment '租户编号',
    constraint uni_definition_id
        unique (definition_id)
)
    comment '流程定义配置';

create table wf_form_manage
(
    id          bigint       not null comment '主键'
        primary key,
    form_name   varchar(255) not null comment '表单名称',
    form_type   varchar(255) not null comment '表单类型',
    router      varchar(255) not null comment '路由地址/表单ID',
    remark      varchar(500) null comment '备注',
    tenant_id   varchar(20)  null comment '租户编号',
    create_dept bigint       null comment '创建部门',
    create_by   bigint       null comment '创建者',
    create_time datetime     null comment '创建时间',
    update_by   bigint       null comment '更新者',
    update_time datetime     null comment '更新时间'
)
    comment '表单管理';

insert into wf_form_manage(id, form_name, form_type, router, remark, tenant_id, create_dept, create_by, create_time, update_by, update_time) VALUES (1, '请假申请', 'static', '/workflow/leaveEdit/index', NULL, '000000', 103, 1, sysdate(), 1, sysdate());

create table wf_node_config
(
    id               bigint       not null comment '主键'
        primary key,
    form_id          bigint       null comment '表单id',
    form_type        varchar(255) null comment '表单类型',
    node_name        varchar(255) not null comment '节点名称',
    node_id          varchar(255) not null comment '节点id',
    definition_id    varchar(255) not null comment '流程定义id',
    apply_user_task  char(1)      default '0'     comment '是否为申请人节点 （0是 1否）',
    create_dept      bigint       null comment '创建部门',
    create_by        bigint       null comment '创建者',
    create_time      datetime     null comment '创建时间',
    update_by        bigint       null comment '更新者',
    update_time      datetime     null comment '更新时间',
    tenant_id        varchar(20)  null comment '租户编号'
)
    comment '节点配置';
