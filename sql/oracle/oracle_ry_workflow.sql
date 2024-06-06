
-- 请假单信息
create table TEST_LEAVE
(
    ID          NUMBER(20) not null
        constraint PK_TEST_LEAVE
        primary key,
    LEAVE_TYPE  VARCHAR2(255),
    START_DATE  DATE,
    END_DATE    DATE,
    LEAVE_DAYS  NUMBER(10),
    REMARK      VARCHAR2(255),
    STATUS      VARCHAR2(255),
    CREATE_DEPT NUMBER(20),
    CREATE_BY   NUMBER(20),
    CREATE_TIME DATE,
    UPDATE_BY   NUMBER(20),
    UPDATE_TIME DATE,
    TENANT_ID   VARCHAR2(20)
);

comment on table TEST_LEAVE is '请假申请表';
comment on column TEST_LEAVE.ID is '主键';
comment on column TEST_LEAVE.LEAVE_TYPE is '请假类型';
comment on column TEST_LEAVE.START_DATE is '开始时间';
comment on column TEST_LEAVE.END_DATE is '结束时间';
comment on column TEST_LEAVE.LEAVE_DAYS is '请假天数';
comment on column TEST_LEAVE.REMARK is '请假原因';
comment on column TEST_LEAVE.STATUS is '状态';
comment on column TEST_LEAVE.CREATE_DEPT is '创建部门';
comment on column TEST_LEAVE.CREATE_BY is '创建者';
comment on column TEST_LEAVE.CREATE_TIME is '创建时间';
comment on column TEST_LEAVE.UPDATE_BY is '更新者';
comment on column TEST_LEAVE.UPDATE_TIME is '更新时间';
comment on column TEST_LEAVE.TENANT_ID is '租户编号';

-- 流程分类信息表
create table WF_CATEGORY
(
    ID            NUMBER(20) not null
        constraint PK_WF_CATEGORY
        primary key,
    CATEGORY_NAME VARCHAR2(255),
    CATEGORY_CODE VARCHAR2(255)
        constraint UNI_CATEGORY_CODE
        unique,
    PARENT_ID     NUMBER(20),
    SORT_NUM      NUMBER(10),
    TENANT_ID     VARCHAR2(20),
    CREATE_DEPT   NUMBER(20),
    CREATE_BY     NUMBER(20),
    CREATE_TIME   DATE,
    UPDATE_BY     NUMBER(20),
    UPDATE_TIME   DATE
);

comment on table WF_CATEGORY is '流程分类';
comment on column WF_CATEGORY.ID is '主键';
comment on column WF_CATEGORY.CATEGORY_NAME is '分类名称';
comment on column WF_CATEGORY.CATEGORY_CODE is '分类编码';
comment on column WF_CATEGORY.PARENT_ID is '父级id';
comment on column WF_CATEGORY.SORT_NUM is '排序';
comment on column WF_CATEGORY.TENANT_ID is '租户编号';
comment on column WF_CATEGORY.CREATE_DEPT is '创建部门';
comment on column WF_CATEGORY.CREATE_BY is '创建者';
comment on column WF_CATEGORY.CREATE_TIME is '创建时间';
comment on column WF_CATEGORY.UPDATE_BY is '更新者';
comment on column WF_CATEGORY.UPDATE_TIME is '更新时间';
INSERT INTO wf_category values (1, 'OA', 'OA', 0, 0, '000000', 103, 1, sysdate, 1, sysdate);

create table WF_TASK_BACK_NODE
(
    ID            NUMBER(20) not null
        constraint PK_WF_TASK_BACK_NODE
        primary key,
    NODE_ID       VARCHAR2(255) not null,
    NODE_NAME     VARCHAR2(255) not null,
    ORDER_NO      NUMBER(20) not null,
    INSTANCE_ID   VARCHAR2(255) not null,
    TASK_TYPE     VARCHAR2(255) not null,
    ASSIGNEE      VARCHAR2(2000) not null,
    TENANT_ID     VARCHAR2(20),
    CREATE_DEPT   NUMBER(20),
    CREATE_BY     NUMBER(20),
    CREATE_TIME   DATE,
    UPDATE_BY     NUMBER(20),
    UPDATE_TIME   DATE
);
comment on table WF_TASK_BACK_NODE is '节点审批记录';
comment on column WF_TASK_BACK_NODE.ID is '主键';
comment on column WF_TASK_BACK_NODE.NODE_ID is '节点id';
comment on column WF_TASK_BACK_NODE.NODE_NAME is '节点名称';
comment on column WF_TASK_BACK_NODE.ORDER_NO is '排序';
comment on column WF_TASK_BACK_NODE.INSTANCE_ID is '流程实例id';
comment on column WF_TASK_BACK_NODE.TASK_TYPE is '节点类型';
comment on column WF_TASK_BACK_NODE.ASSIGNEE is '审批人';
comment on column WF_TASK_BACK_NODE.TENANT_ID is '租户编号';
comment on column WF_TASK_BACK_NODE.CREATE_DEPT is '创建部门';
comment on column WF_TASK_BACK_NODE.CREATE_BY is '创建者';
comment on column WF_TASK_BACK_NODE.CREATE_TIME is '创建时间';
comment on column WF_TASK_BACK_NODE.UPDATE_BY is '更新者';
comment on column WF_TASK_BACK_NODE.UPDATE_TIME is '更新时间';

create table WF_DEFINITION_CONFIG
(
    ID            NUMBER(20) NOT NULL
        CONSTRAINT PK_WF_DEFINITION_CONFIG
        PRIMARY KEY,
    TABLE_NAME    VARCHAR2(255) NOT NULL,
    DEFINITION_ID VARCHAR2(255) NOT NULL,
    PROCESS_KEY   VARCHAR2(255) NOT NULL,
    VERSION       NUMBER(10)    NOT NULL,
    REMARK        VARCHAR2(500),
    TENANT_ID     VARCHAR2(20),
    CREATE_DEPT   NUMBER(20),
    CREATE_BY     NUMBER(20),
    CREATE_TIME   DATE,
    UPDATE_BY     NUMBER(20),
    UPDATE_TIME   DATE,
    constraint uni_definition_id
        unique (definition_id)
);
comment on table WF_DEFINITION_CONFIG is '流程定义配置';
comment on column WF_DEFINITION_CONFIG.ID is '主键';
comment on column WF_DEFINITION_CONFIG.TABLE_NAME is '表名';
comment on column WF_DEFINITION_CONFIG.DEFINITION_ID is '流程定义ID';
comment on column WF_DEFINITION_CONFIG.PROCESS_KEY is '流程KEY';
comment on column WF_DEFINITION_CONFIG.VERSION is '流程版本';
comment on column WF_DEFINITION_CONFIG.TENANT_ID is '租户编号';
comment on column WF_DEFINITION_CONFIG.REMARK is '备注';
comment on column WF_DEFINITION_CONFIG.CREATE_DEPT is '创建部门';
comment on column WF_DEFINITION_CONFIG.CREATE_BY is '创建者';
comment on column WF_DEFINITION_CONFIG.CREATE_TIME is '创建时间';
comment on column WF_DEFINITION_CONFIG.UPDATE_BY is '更新者';
comment on column WF_DEFINITION_CONFIG.UPDATE_TIME is '更新时间';

create table WF_FORM_MANAGE
(
    ID            NUMBER(20) NOT NULL
        CONSTRAINT PK_WF_FORM_MANAGE
        PRIMARY KEY,
    FORM_NAME     VARCHAR2(255) NOT NULL,
    FORM_TYPE     VARCHAR2(255) NOT NULL,
    ROUTER        VARCHAR2(255) NOT NULL,
    REMARK        VARCHAR2(500),
    TENANT_ID     VARCHAR2(20),
    CREATE_DEPT   NUMBER(20),
    CREATE_BY     NUMBER(20),
    CREATE_TIME   DATE,
    UPDATE_BY     NUMBER(20),
    UPDATE_TIME   DATE
);

comment on table WF_FORM_MANAGE is '表单管理';
comment on column WF_FORM_MANAGE.ID is '主键';
comment on column WF_FORM_MANAGE.FORM_NAME is '表单名称';
comment on column WF_FORM_MANAGE.FORM_TYPE is '表单类型';
comment on column WF_FORM_MANAGE.ROUTER is '路由地址/表单ID';
comment on column WF_FORM_MANAGE.REMARK is '备注';
comment on column WF_FORM_MANAGE.TENANT_ID is '租户编号';
comment on column WF_FORM_MANAGE.CREATE_DEPT is '创建部门';
comment on column WF_FORM_MANAGE.CREATE_BY is '创建者';
comment on column WF_FORM_MANAGE.CREATE_TIME is '创建时间';
comment on column WF_FORM_MANAGE.UPDATE_BY is '更新者';
comment on column WF_FORM_MANAGE.UPDATE_TIME is '更新时间';

insert into wf_form_manage(id, form_name, form_type, router, remark, tenant_id, create_dept, create_by, create_time, update_by, update_time) VALUES (1, '请假申请', 'static', '/workflow/leaveEdit/index', NULL, '000000', 103, 1, sysdate, 1, sysdate);

create table WF_NODE_CONFIG
(
    ID               NUMBER(20) NOT NULL
        CONSTRAINT PK_WF_NODE_CONFIG
        PRIMARY KEY,
    FORM_ID          NUMBER(20),
    FORM_TYPE        VARCHAR2(255),
    NODE_NAME        VARCHAR2(255) NOT NULL,
    NODE_ID          VARCHAR2(255) NOT NULL,
    DEFINITION_ID    VARCHAR2(255) NOT NULL,
    APPLY_USER_TASK  CHAR(1) DEFAULT '0',
    TENANT_ID        VARCHAR2(20),
    CREATE_DEPT      NUMBER(20),
    CREATE_BY        NUMBER(20),
    CREATE_TIME      DATE,
    UPDATE_BY        NUMBER(20),
    UPDATE_TIME      DATE
);

comment on table WF_NODE_CONFIG is '节点配置';
comment on column WF_NODE_CONFIG.ID is '主键';
comment on column WF_NODE_CONFIG.FORM_ID is '表单id';
comment on column WF_NODE_CONFIG.FORM_TYPE is '表单类型';
comment on column WF_NODE_CONFIG.NODE_ID is '节点id';
comment on column WF_NODE_CONFIG.NODE_NAME is '节点名称';
comment on column WF_NODE_CONFIG.DEFINITION_ID is '流程定义id';
comment on column WF_NODE_CONFIG.APPLY_USER_TASK is '是否为申请人节点 （0是 1否）';
comment on column WF_NODE_CONFIG.TENANT_ID is '租户编号';
comment on column WF_NODE_CONFIG.CREATE_DEPT is '创建部门';
comment on column WF_NODE_CONFIG.CREATE_BY is '创建者';
comment on column WF_NODE_CONFIG.CREATE_TIME is '创建时间';
comment on column WF_NODE_CONFIG.UPDATE_BY is '更新者';
comment on column WF_NODE_CONFIG.UPDATE_TIME is '更新时间';

