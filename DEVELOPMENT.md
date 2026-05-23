# 2026 比武项目开发流程文档

> **选题**：企业客户关系管理系统（CRM）— 企业管理类  
> **框架**：Vue 3.5 + Element Plus 2.13（前端） + Spring Boot 若依 v3.9.2（后端） + Oracle 11g  
> **总分目标**：100分（系统设计 20 + 项目开发 75 + 项目部署 5）

---

## 一、技术架构

```
前端 (Vue 3 + Element Plus)    后端 (Spring Boot)         数据库 (Oracle 11g)
┌────────────────────────┐     ┌──────────────────┐      ┌──────────────┐
│  Vue 3.5.26 (Composition│     │  ruoyi-admin     │      │  Oracle 11g   │
│    API + <script setup>)│     │  ruoyi-system    │      │  (128.128.   │
│  Element Plus 2.13.1   │◄───►│  ruoyi-framework │◄────►│   9.3:1521)  │
│  Pinia 3.0 (状态管理)   │ axios│  ruoyi-common    │      │  用户: ZXH   │
│  Vue Router 4.6        │     │  ruoyi-quartz    │      └──────────────┘
│  ECharts 5.6 (图表)    │     └──────────────────┘
│  vuedraggable 4.1 (拖拽)│
│  Vite 6.4 (构建)        │
└────────────────────────┘
```

- **前端**：Vue 3.5 + Element Plus，基于若依 Vue 前端脚手架，CRM 页面放在 `web/src/views/crm/`，API 封装在 `web/src/api/crm/`
- **后端**：若依 Spring Boot 框架，CRM 业务代码放在 `ruoyi-system/src/main/java/com/ruoyi/system/crm/`
- **图表**：ECharts 5.6（已安装，用于大屏和用户画像）
- **拖拽**：vuedraggable 4.1（已安装，用于工作台布局）
- **流程图**：bpmn-js（需新增依赖，用于工作流 BPMN 可视化）

### 依赖状态

| 依赖 | 版本 | 状态 | 用途 |
|---|---|---|---|
| `vue` | 3.5.26 | 已安装 | 前端框架 |
| `element-plus` | 2.13.1 | 已安装 | UI 组件库 |
| `@element-plus/icons-vue` | 2.3.2 | 已安装 | 图标 |
| `echarts` | 5.6.0 | 已安装 | 图表（大屏/画像） |
| `vuedraggable` | 4.1.0 | 已安装 | 工作台拖拽 |
| `pinia` | 3.0.4 | 已安装 | 状态管理 |
| `vue-router` | 4.6.4 | 已安装 | 路由 |
| `axios` | 1.13.2 | 已安装 | HTTP 请求 |
| `@vueuse/core` | 14.1.0 | 已安装 | Vue 工具函数（滚动监听等） |
| `bpmn-js` | — | **需新增** | 工作流流程图渲染 |

---

## 二、业务场景设计（得分目标：4分）

### 业务描述

本系统面向企业销售与客户服务团队，解决**客户信息分散、商机跟进不及时、合同审批流程繁琐、服务接口管理混乱**等痛点，实现从客户录入 → 商机跟踪 → 合同审批 → 服务交付 → 数据分析的**完整业务闭环**。

### 业务闭环流程

```
客户录入 ──► 联系人关联 ──► 商机创建 ──► 合同审批（工作流4节点）
                                          │
                                          ▼
                                    合同归档 + 消息通知
                                          │
                    ┌─────────────────────┼─────────────────────┐
                    ▼                     ▼                     ▼
              API服务发布          用户画像联动更新        数据大屏实时监控
```

### 数据流转

1. 客户数据支持 Excel 批量导入（含校验与去重）
2. 合同数据通过工作流 4 节点审批（提交→部门审批→财务审批→归档）
3. 关键操作写入日志表，大屏实时消费
4. 合同量、商机转化率等汇聚到用户画像统计表

---

## 三、数据码表设计（得分目标：6分，不少于3张）

> 以下仅列出表元数据（字段名、类型、约束），完整建表 SQL 后续按阶段写入 `SQL/` 文件夹。

### 3.1 客户等级码表 `CRM_CUSTOMER_LEVEL`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| ID | NUMBER(10) | PK | 主键 |
| LEVEL_CODE | VARCHAR2(20) | NOT NULL, UNIQUE | 等级编码 |
| LEVEL_NAME | VARCHAR2(50) | NOT NULL | 等级名称（VIP/普通/潜在） |
| DISCOUNT_RATE | NUMBER(3,2) | DEFAULT 1.00 | 折扣率 |
| SORT_ORDER | NUMBER(5) | DEFAULT 0 | 排序 |
| STATUS | CHAR(1) | DEFAULT '0' | 状态（0正常 1停用） |
| CREATE_BY | VARCHAR2(64) | | 创建人 |
| CREATE_TIME | DATE | | 创建时间 |

### 3.2 商机阶段码表 `CRM_OPPORTUNITY_STAGE`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| ID | NUMBER(10) | PK | 主键 |
| STAGE_CODE | VARCHAR2(20) | NOT NULL, UNIQUE | 阶段编码 |
| STAGE_NAME | VARCHAR2(50) | NOT NULL | 阶段名称（初步接触/需求分析/报价/谈判/成交/丢单） |
| PROBABILITY | NUMBER(3) | DEFAULT 0 | 成交概率(%) |
| SORT_ORDER | NUMBER(5) | DEFAULT 0 | 排序 |
| STATUS | CHAR(1) | DEFAULT '0' | 状态 |
| CREATE_TIME | DATE | | 创建时间 |

### 3.3 联系方式类型码表 `CRM_CONTACT_TYPE`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| ID | NUMBER(10) | PK | 主键 |
| TYPE_CODE | VARCHAR2(20) | NOT NULL, UNIQUE | 类型编码 |
| TYPE_NAME | VARCHAR2(50) | NOT NULL | 类型名称（手机/座机/邮箱/微信/QQ） |
| ICON | VARCHAR2(100) | | 图标样式 |
| SORT_ORDER | NUMBER(5) | DEFAULT 0 | 排序 |
| STATUS | CHAR(1) | DEFAULT '0' | 状态 |
| CREATE_TIME | DATE | | 创建时间 |

### 3.4 合同状态码表 `CRM_CONTRACT_STATUS`（附加）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| ID | NUMBER(10) | PK | 主键 |
| STATUS_CODE | VARCHAR2(20) | NOT NULL, UNIQUE | 状态编码 |
| STATUS_NAME | VARCHAR2(50) | NOT NULL | 状态名称（草稿/审批中/已生效/已终止/已归档） |
| SORT_ORDER | NUMBER(5) | DEFAULT 0 | 排序 |
| STATUS | CHAR(1) | DEFAULT '0' | 状态 |
| CREATE_TIME | DATE | | 创建时间 |

---

## 四、功能菜单设计（得分目标：10分，不少于5个）

| 序号 | 一级菜单 | 路由路径 | 对应 Vue 视图 | 覆盖得分点 |
|---|---|---|---|---|
| 1 | **客户管理** | `/crm/customer` | `views/crm/customer/index.vue` | 2.1 基础CRUD + 排序/列自定义/Excel导出 |
| 2 | **商机管理** | `/crm/opportunity` | `views/crm/opportunity/index.vue` | 2.1 CRUD、2.2 Excel导入校验去重 |
| 3 | **合同管理** | `/crm/contract` | `views/crm/contract/index.vue` | 2.7 工作流（4节点+BPMN展示） |
| 4 | **消息中心** | `/crm/message` | `views/crm/message/` | 2.4 模板+发送+撤回+重发+首页提醒 |
| 5 | **数据大屏** | `/crm/dashboard` | `views/crm/dashboard/index.vue` | 2.3 日志可视化+监控、2.6 画像图表 |
| 6 | **API服务市场** | `/crm/api` | `views/crm/api/` | 2.10 发布/调试/上下架/编辑删除 |
| 7 | **工作台** | `/crm/workbench` | `views/crm/workbench/index.vue` | 2.9 拖拽布局+小组件+交互 |
| 8 | **海量数据** | `/crm/behavior` | `views/crm/behavior/index.vue` | 2.5 10万+异步导入+无限滚动 |
| 9 | **操作日志** | `/crm/operlog` | `views/crm/operlog/index.vue` | 2.3 多条件查询/链路追踪/SQL查看 |

> 满足「不少于5个核心功能菜单」，实际规划 9 个菜单。

---

## 五、数据库业务表设计（补充）

> 以下为 CRM 核心业务表元数据，与上述码表配合使用。

### 5.1 客户表 `CRM_CUSTOMER`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| ID | NUMBER(10) | PK | 主键 |
| CUSTOMER_NO | VARCHAR2(32) | NOT NULL, UNIQUE | 客户编号 |
| CUSTOMER_NAME | VARCHAR2(100) | NOT NULL | 客户名称 |
| LEVEL_ID | NUMBER(10) | FK→CRM_CUSTOMER_LEVEL.ID | 客户等级 |
| INDUSTRY | VARCHAR2(50) | | 所属行业 |
| PROVINCE | VARCHAR2(20) | | 省份 |
| CITY | VARCHAR2(20) | | 城市 |
| ADDRESS | VARCHAR2(200) | | 详细地址 |
| PHONE | VARCHAR2(20) | | 联系电话 |
| EMAIL | VARCHAR2(100) | | 邮箱 |
| REMARK | VARCHAR2(500) | | 备注 |
| CREATE_BY | VARCHAR2(64) | | 创建人 |
| CREATE_TIME | DATE | | 创建时间 |
| UPDATE_BY | VARCHAR2(64) | | 更新人 |
| UPDATE_TIME | DATE | | 更新时间 |

### 5.2 联系人表 `CRM_CONTACT`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| ID | NUMBER(10) | PK | 主键 |
| CUSTOMER_ID | NUMBER(10) | FK→CRM_CUSTOMER.ID, NOT NULL | 所属客户 |
| CONTACT_NAME | VARCHAR2(50) | NOT NULL | 联系人姓名 |
| CONTACT_TYPE_ID | NUMBER(10) | FK→CRM_CONTACT_TYPE.ID | 联系方式类型 |
| CONTACT_VALUE | VARCHAR2(100) | | 联系方式内容 |
| POSITION | VARCHAR2(50) | | 职位 |
| IS_PRIMARY | CHAR(1) | DEFAULT '0' | 是否首要联系人 |
| CREATE_TIME | DATE | | 创建时间 |

### 5.3 商机表 `CRM_OPPORTUNITY`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| ID | NUMBER(10) | PK | 主键 |
| OPPORTUNITY_NAME | VARCHAR2(100) | NOT NULL | 商机名称 |
| CUSTOMER_ID | NUMBER(10) | FK→CRM_CUSTOMER.ID, NOT NULL | 关联客户 |
| STAGE_ID | NUMBER(10) | FK→CRM_OPPORTUNITY_STAGE.ID | 当前阶段 |
| ESTIMATED_AMOUNT | NUMBER(15,2) | | 预计金额 |
| EXPECTED_CLOSE_DATE | DATE | | 预计成交日期 |
| CREATE_BY | VARCHAR2(64) | | 负责人 |
| CREATE_TIME | DATE | | 创建时间 |
| UPDATE_TIME | DATE | | 更新时间 |

### 5.4 合同表 `CRM_CONTRACT`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| ID | NUMBER(10) | PK | 主键 |
| CONTRACT_NO | VARCHAR2(32) | NOT NULL, UNIQUE | 合同编号 |
| CONTRACT_NAME | VARCHAR2(100) | NOT NULL | 合同名称 |
| CUSTOMER_ID | NUMBER(10) | FK→CRM_CUSTOMER.ID | 关联客户 |
| OPPORTUNITY_ID | NUMBER(10) | FK→CRM_OPPORTUNITY.ID | 关联商机 |
| CONTRACT_AMOUNT | NUMBER(15,2) | NOT NULL | 合同金额 |
| STATUS_ID | NUMBER(10) | FK→CRM_CONTRACT_STATUS.ID | 合同状态 |
| SIGN_DATE | DATE | | 签订日期 |
| START_DATE | DATE | | 生效日期 |
| END_DATE | DATE | | 到期日期 |
| CONTENT | CLOB | | 合同内容 |
| CREATE_BY | VARCHAR2(64) | | 创建人 |
| CREATE_TIME | DATE | | 创建时间 |
| UPDATE_TIME | DATE | | 更新时间 |

### 5.5 消息模板表 `CRM_MESSAGE_TEMPLATE`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| ID | NUMBER(10) | PK | 主键 |
| TEMPLATE_NAME | VARCHAR2(100) | NOT NULL | 模板名称 |
| TEMPLATE_TYPE | VARCHAR2(20) | NOT NULL | 模板类型（合同提醒/商机提醒/系统通知） |
| TITLE | VARCHAR2(200) | NOT NULL | 消息标题 |
| CONTENT | CLOB | NOT NULL | 消息内容（支持变量占位如 {客户名称}） |
| VARIABLES | VARCHAR2(500) | | 可用变量说明（JSON） |
| STATUS | CHAR(1) | DEFAULT '0' | 状态 |
| CREATE_TIME | DATE | | 创建时间 |

### 5.6 消息记录表 `CRM_MESSAGE_RECORD`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| ID | NUMBER(10) | PK | 主键 |
| TEMPLATE_ID | NUMBER(10) | FK→CRM_MESSAGE_TEMPLATE.ID | 引用模板 |
| SENDER_ID | NUMBER(10) | | 发送人 |
| RECEIVER_ID | NUMBER(10) | | 接收人 |
| TITLE | VARCHAR2(200) | NOT NULL | 消息标题 |
| CONTENT | CLOB | | 消息内容（变量已渲染） |
| STATUS | CHAR(1) | DEFAULT '0' | 状态（0已发送 1已撤回 2已读） |
| SEND_TIME | DATE | | 发送时间 |
| RECALL_TIME | DATE | | 撤回时间 |

### 5.7 API信息表 `CRM_API_INFO`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| ID | NUMBER(10) | PK | 主键 |
| API_NAME | VARCHAR2(100) | NOT NULL | API名称 |
| API_DESC | VARCHAR2(500) | | API描述 |
| API_URL | VARCHAR2(200) | NOT NULL | 接口地址 |
| API_METHOD | VARCHAR2(10) | NOT NULL | 请求方式（GET/POST） |
| REQUEST_EXAMPLE | CLOB | | 请求示例 |
| RESPONSE_EXAMPLE | CLOB | | 响应示例 |
| STATUS | CHAR(1) | DEFAULT '1' | 状态（0下架 1上架） |
| CREATE_BY | VARCHAR2(64) | | 创建人 |
| CREATE_TIME | DATE | | 创建时间 |
| UPDATE_TIME | DATE | | 更新时间 |

### 5.8 工作流实例表 `CRM_WORKFLOW_INSTANCE`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| ID | NUMBER(10) | PK | 主键 |
| PROCESS_TYPE | VARCHAR2(20) | NOT NULL | 流程类型（CONTRACT_APPROVAL） |
| BUSINESS_ID | NUMBER(10) | NOT NULL | 业务主键（合同ID） |
| BUSINESS_NO | VARCHAR2(32) | | 业务编号 |
| CURRENT_NODE_ID | NUMBER(10) | | 当前节点 |
| STATUS | CHAR(1) | DEFAULT '0' | 状态（0审批中 1已完成 2已终止） |
| START_USER_ID | NUMBER(10) | | 发起人 |
| START_TIME | DATE | | 发起时间 |
| END_TIME | DATE | | 结束时间 |

### 5.9 工作流节点表 `CRM_WORKFLOW_NODE`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| ID | NUMBER(10) | PK | 主键 |
| INSTANCE_ID | NUMBER(10) | FK→CRM_WORKFLOW_INSTANCE.ID | 流程实例 |
| NODE_NAME | VARCHAR2(50) | NOT NULL | 节点名称（提交/部门审批/财务审批/归档） |
| NODE_ORDER | NUMBER(3) | NOT NULL | 节点顺序 |
| APPROVER_ID | NUMBER(10) | | 审批人 |
| APPROVAL_TYPE | VARCHAR2(20) | DEFAULT 'SINGLE' | 审批类型（SINGLE/COUNTERSIGN） |
| STATUS | CHAR(1) | DEFAULT '0' | 状态（0待审批 1通过 2驳回 3转办 4回退） |
| OPINION | VARCHAR2(500) | | 审批意见 |
| APPROVE_TIME | DATE | | 审批时间 |

### 5.10 操作日志扩展表 `CRM_OPER_LOG`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| ID | NUMBER(10) | PK | 主键 |
| REQUEST_URL | VARCHAR2(200) | | 请求地址 |
| REQUEST_METHOD | VARCHAR2(10) | | 请求方式 |
| REQUEST_PARAMS | CLOB | | 入参（JSON） |
| RESPONSE_RESULT | CLOB | | 出参（JSON） |
| SQL_STATEMENTS | CLOB | | 执行的 SQL 语句（链路追踪用） |
| TRACE_ID | VARCHAR2(64) | | 链路追踪 ID |
| OPERATOR | VARCHAR2(64) | | 操作人 |
| OPERATE_TIME | DATE | | 操作时间 |
| COST_MILLIS | NUMBER(10) | | 耗时（毫秒） |
| STATUS | CHAR(1) | DEFAULT '0' | 状态（0成功 1失败） |
| ERROR_MSG | VARCHAR2(2000) | | 错误信息 |

### 5.11 客户行为记录表 `CRM_CUSTOMER_BEHAVIOR`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| ID | NUMBER(20) | PK | 主键（序列生成，支持海量数据） |
| CUSTOMER_ID | NUMBER(10) | FK→CRM_CUSTOMER.ID | 关联客户 |
| BEHAVIOR_TYPE | VARCHAR2(50) | | 行为类型（电话沟通/拜访/邮件/演示） |
| DESCRIPTION | VARCHAR2(500) | | 行为描述 |
| BEHAVIOR_TIME | DATE | | 行为发生时间 |
| CREATE_TIME | DATE | DEFAULT SYSDATE | 创建时间 |

### 5.12 用户画像统计表 `CRM_USER_PROFILE`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| ID | NUMBER(10) | PK | 主键 |
| USER_ID | NUMBER(10) | | 用户 ID |
| CUSTOMER_COUNT | NUMBER(10) | DEFAULT 0 | 负责客户数 |
| OPPORTUNITY_COUNT | NUMBER(10) | DEFAULT 0 | 商机数量 |
| WIN_RATE | NUMBER(5,2) | DEFAULT 0 | 赢单率(%) |
| TOTAL_AMOUNT | NUMBER(18,2) | DEFAULT 0 | 累计合同金额 |
| INDUSTRY_DISTRIBUTION | CLOB | | 行业分布（JSON） |
| REGION_DISTRIBUTION | CLOB | | 区域分布（JSON） |
| MONTHLY_PERFORMANCE | CLOB | | 月度业绩趋势（JSON） |
| UPDATE_TIME | DATE | | 更新时间 |

### 5.13 工作台布局表 `CRM_WORKBENCH_LAYOUT`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| ID | NUMBER(10) | PK | 主键 |
| USER_ID | NUMBER(10) | NOT NULL, UNIQUE | 用户 ID |
| LAYOUT_JSON | CLOB | NOT NULL | 布局配置（JSON） |
| UPDATE_TIME | DATE | | 更新时间 |

---

## 六、前端项目结构规划

```
web/src/
├── api/crm/                          # API 请求封装
│   ├── customer.js                   # 客户管理 API
│   ├── contact.js                    # 联系人管理 API
│   ├── opportunity.js                # 商机管理 API
│   ├── contract.js                   # 合同管理 API
│   ├── message.js                    # 消息中心 API
│   ├── dashboard.js                  # 大屏数据 API
│   ├── apiMarket.js                  # API 服务市场 API
│   ├── workbench.js                  # 工作台 API
│   ├── behavior.js                   # 海量数据 API
│   ├── workflow.js                   # 工作流 API
│   ├── profile.js                    # 用户画像 API
│   └── operlog.js                    # 操作日志 API
├── views/crm/                        # CRM 页面
│   ├── customer/
│   │   └── index.vue                 # 客户列表（CRUD + 排序 + 列自定义 + 导出）
│   ├── contact/
│   │   └── index.vue                 # 联系人管理
│   ├── opportunity/
│   │   └── index.vue                 # 商机列表 + Excel 导入
│   ├── contract/
│   │   └── index.vue                 # 合同列表 + 发起审批入口
│   ├── workflow/
│   │   └── index.vue                 # 审批流程页（BPMN + 审批操作）
│   ├── message/
│   │   ├── template.vue              # 消息模板管理
│   │   ├── send.vue                  # 消息发送
│   │   └── record.vue               # 消息记录（撤回/重发）
│   ├── dashboard/
│   │   ├── index.vue                 # 监控大屏（6+ 指标实时更新）
│   │   └── profile.vue              # 用户画像（标签云/环形图/雷达图）
│   ├── api/
│   │   └── index.vue                 # API 服务市场（管理 + 调试）
│   ├── workbench/
│   │   └── index.vue                 # 工作台（拖拽布局 + 小组件）
│   ├── behavior/
│   │   └── index.vue                 # 海量数据无限滚动
│   └── operlog/
│       └── index.vue                 # 操作日志（多条件查询/详情/SQL追踪）
├── stores/crm/                       # Pinia 状态管理
│   ├── message.js                    # 未读消息数（首页角标）
│   └── workbench.js                  # 工作台布局状态
└── router/
    └── index.js                      # 路由配置（追加 CRM 路由模块）
```

---

## 七、分阶段开发流程

开发按得分点优先级排列，每个阶段有明确的前端、后端任务。

---

### 阶段一：基础设施搭建 + 基础 CRUD（对应 2.1，得分目标：10分）

> **状态：✅ 已完成**（2026-05-23）  
> **简介**：完成 CRM 数据库全量建表脚本（13张业务表 + 4张码表 + 序列 + 码表初始数据）、菜单权限 SQL、后端客户/联系人/码表 REST 接口（含分页查询、动态排序、Excel 导出）、前端客户管理页（列排序 + 列显隐 localStorage 持久化 + CRUD + 导出）及联系人管理页。

#### 后端任务

- [x] 1. 在 `SQL/` 文件夹下创建 `01_init_tables.sql`，包含第五节全部建表语句与码表初始化数据，按模块分段注释
- [x] 2. 在 `springBoot/ruoyi-system/src/main/java/com/ruoyi/system/` 下新建 `crm` 包（Customer/Contact/Dict Controller、Service、Mapper、Domain）
- [x] 3. 客户管理 `/crm/customer` 接口：list / detail / add / edit / delete / export
- [x] 4. 联系人管理 `/crm/contact` 接口同上模式
- [x] 5. 复用若依 `@Log` 注解记录操作日志
- [x] 6. 码表通用接口：`GET /crm/dict/{type}`

#### 前端任务

- [x] 1. 新增 `web/src/api/crm/customer.js`、`contact.js`
- [x] 2. 新增 `web/src/views/crm/customer/index.vue`（列排序 + 列设置 + Excel 导出 + CRUD）
- [x] 3. 新增 `web/src/views/crm/contact/index.vue`
- [x] 4. 菜单通过 `SQL/02_crm_menu.sql` 写入 sys_menu，若依动态路由自动加载（component: `crm/customer/index`）

#### 得分覆盖

| 得分点 | 分值 | 状态 | 实现方式 |
|---|---|---|---|
| 查询 | 1分 | ✅ | 多条件组合查询 + 分页 |
| 新增 | 1分 | ✅ | el-dialog 表单新增 |
| 修改 | 1分 | ✅ | el-dialog 编辑模式 |
| 删除 | 1分 | ✅ | 批量删除 + 确认弹窗 |
| 表格列排序 | 2分 | ✅ | sortable="custom" + orderByColumn |
| 表头自定义显示 | 2分 | ✅ | el-checkbox-group + localStorage |
| Excel导出 | 2分 | ✅ | 后端 POI + proxy.download |

---

### 阶段二：Excel 批量导入（对应 2.2，得分目标：10分）

> **状态：✅ 已完成**（2026-05-23）  
> **简介**：完成商机管理 CRUD、Excel 批量导入（Apache POI 逐行解析）、完整校验（必填/数字/码表/日期）、Excel 内去重与数据库查重，前端导入对话框含汇总卡片与错误详情表格（行/列/字段/原因，失败红/跳过橙/成功绿高亮）。

#### 后端任务

- [x] 1. 商机管理 `/crm/opportunity` CRUD 接口
- [x] 2. `POST /crm/opportunity/import` — MultipartFile 导入，返回校验结果 JSON
- [x] 3. 数据校验：必填、金额数字、阶段码表、日期格式，错误项 `{row,col,field,message}`
- [x] 4. 重复处理：Excel 内「商机名称+客户名称」去重；数据库查重跳过

#### 前端任务

- [x] 1. `web/src/views/crm/opportunity/index.vue` — CRUD + 导入对话框 + 结果面板
- [x] 2. `web/src/api/crm/opportunity.js` — 含 import 接口

#### 得分覆盖

| 得分点 | 分值 | 状态 | 实现方式 |
|---|---|---|---|
| Excel批量导入 + 格式校验 + 精准错误位置提示 | 4分 | ✅ | POI 逐行校验 + 前端 el-table 展示 row/col/field |
| 重复数据自动识别 + 去重 + 无重复入库 | 6分 | ✅ | HashMap 首行保留 + countByNameAndCustomer 查重 |

---

### 阶段三：日志收集与数据可视化（对应 2.3，得分目标：10分）

#### 后端任务

1. **操作日志收集**（2分）：
   - 基于若依现有 `SysOperlog` + `@Log` AOP 机制扩展
   - 新增 `CrmOperLogAspect` 切面（或扩展现有 `LogAspect`）：
     - 入参记录：`joinPoint.getArgs()` 序列化为 JSON
     - 出参记录：`@Around` 环绕通知获取返回值
     - 写入 `CRM_OPER_LOG` 表（requestUrl, requestMethod, requestParams, responseResult, operator, operateTime, traceId, costMillis）
   - 多条件查询接口 `GET /crm/operlog/list`：操作人、时间范围、请求地址组合查询 + 分页
2. **SQL 链路追踪**（2分）：
   - 在若依 `TraceIdFilter`（或新建）中生成 `traceId = UUID.randomUUID().toString().replace("-","")`，放入 `ThreadLocal` + 响应 header
   - AOP 切面中通过 Druid `FilterEvent` 或拦截 `DataSource` 获取执行的 SQL 列表，关联 traceId 写入 `CRM_OPER_LOG.SQL_STATEMENTS`
   - 日志详情接口 `GET /crm/operlog/{id}` 返回完整信息（含 SQL 语句列表）
3. **大屏数据接口**（6分）：
   - `GET /crm/dashboard/system-status` — 系统状态，使用 **oshi-core**（Maven 依赖）获取：
     - CPU 使用率、内存使用率、磁盘使用率、JVM 堆内存
   - `GET /crm/dashboard/log-stats` — 日志统计：
     - 今日操作频次（按小时分布）、异常占比、TOP5 活跃用户、各模块调用量
   - 接口无缓存，每次实时计算

#### 前端任务

1. 新增 `web/src/views/crm/dashboard/index.vue`（数据大屏）：
   - 全屏深色背景布局，使用 CSS Grid 分区域
   - 至少 6 个指标项（2分）：
     1. CPU 使用率 — ECharts **仪表盘**（gauge）
     2. 内存使用率 — ECharts **仪表盘**
     3. 今日操作总次数 — **数字卡片**（带较昨日趋势箭头）
     4. 异常次数/异常率 — **数字卡片** + 迷你折线图
     5. 活跃用户 TOP5 — ECharts **柱状图**（bar）
     6. 模块调用分布 — ECharts **饼图**（pie）
   - 底部滚动日志列表：`<el-table>` 固定高度 + `setInterval` 滚动，展示最近 20 条操作记录
   - **实时更新**（2分）：使用 `setInterval` 每 10 秒调用 `/dashboard/system-status` 和 `/dashboard/log-stats` 刷新数据
   - 使用 `@vueuse/core` 的 `useIntervalFn` 简化轮询逻辑

2. 新增 `web/src/views/crm/operlog/index.vue`（操作日志）：
   - 搜索栏：操作人 `<el-input>`、时间范围 `<el-date-picker type="datetimerange">`、请求地址 `<el-input>` + 搜索/重置
   - `<el-table>` 展示日志列表（分页）
   - 点击行或「详情」按钮 → `<el-dialog>` 弹出日志详情：
     - 基本信息：操作人、时间、URL、耗时
     - 入参/出参：JSON 格式化展示（`<pre>` 标签或 Monaco Editor）
     - **SQL 语句列表**：该请求执行的全部 SQL（链路追踪核心）

3. 新增 `web/src/api/crm/dashboard.js`、`operlog.js`

#### 得分覆盖

| 得分点 | 分值 | 实现方式 |
|---|---|---|
| 日志收集（入参出参地址操作人时间）+ 多条件查询分页 | 2分 | AOP切面 + 后端查询接口 + `<el-date-picker>` 组合查询 |
| 日志链路追踪（关联业务、查看SQL） | 2分 | traceId ThreadLocal + Druid SQL 记录 + 详情弹窗展示 |
| 大屏图表（CPU内存 + 操作频次异常分布） | 4分 | ECharts gauge/bar/pie + oshi + 统计接口 |
| 至少6个指标 + 实时更新 | 2分 | 6个 ECharts 指标 + `useIntervalFn` 10s 轮询 |

---

### 阶段四：消息中心（对应 2.4，得分目标：5分）

#### 后端任务

1. 消息模板接口 `/crm/message/template`：
   - `GET /list` — 模板列表（分页、按名称搜索）
   - `POST /` — 新增模板
   - `PUT /` — 编辑模板
   - `DELETE /{ids}` — 删除模板
2. 消息发送接口 `/crm/message/record`：
   - `GET /send/template/list` — 查询可用模板供发送页选择
   - `POST /send` — 引用模板发送：接收 `templateId` + `receiverId` + `variables`（变量值映射），后端根据模板变量占位符渲染内容后写入消息记录表
   - `PUT /recall/{id}` — 撤回（仅 status='0' 允许）
   - `PUT /resend/{id}` — 重发（仅 status='1' 允许）
   - `GET /unread-count` — 当前用户未读消息数（供首页角标）
   - `GET /unread-list` — 最近 5 条未读消息（供首页下拉）

#### 前端任务

1. 新增 `web/src/views/crm/message/template.vue`（模板管理）：
   - `<el-table>` 展示模板列表，模板类型用 `<el-tag>` 区分
   - `<el-dialog>` 新增/编辑表单：模板名称、类型（下拉）、标题、内容（`<el-input type="textarea">`，提示可用变量）
2. 新增 `web/src/views/crm/message/send.vue`（消息发送）：
   - 表单：选择模板（`<el-select>` + 选中后预览渲染内容）、选择接收人（复用若依用户选择器）、预览变量渲染内容 → 发送
3. 新增 `web/src/views/crm/message/record.vue`（消息记录）：
   - 表格：标题、发送人、接收人、发送时间、状态（`<el-tag>` 颜色区分）
   - 操作列：撤回按钮（`v-if="row.status === '0'"`）、重发按钮（`v-if="row.status === '1'"`）
4. **首页消息提醒**（修改若依 `Navbar.vue` 或 `TopNav.vue`）：
   - 顶部导航栏右侧添加消息图标（`<el-icon><Bell /></el-icon>`）+ `<el-badge :value="unreadCount">`
   - 点击弹出 `<el-popover>` 展示最近 5 条未读消息列表
   - 使用 Pinia store（`web/src/stores/crm/message.js`）管理 `unreadCount`，定时轮询更新
5. 新增 `web/src/api/crm/message.js`

#### 得分覆盖

| 得分点 | 分值 | 实现方式 |
|---|---|---|
| 消息模板配置 | 1分 | `<el-dialog>` 表单 CRUD |
| 引用模板发送 | 1分 | 发送页 `<el-select>` 选择模板 + 变量渲染 |
| 首页消息提醒 | 1分 | `<el-badge>` 角标 + `<el-popover>` 下拉 + Pinia 状态 |
| 消息撤回 + 重发 | 2分 | 按钮 `v-if` 条件渲染 + 后端状态校验 |

---

### 阶段五：海量表格数据加载（对应 2.5，得分目标：10分）

#### 后端任务

1. 数据库已设计 `CRM_CUSTOMER_BEHAVIOR` 表（见 5.11），在 `SQL/` 中补建表语句
2. 数据生成接口 `POST /crm/behavior/generate`：
   - 参数：`{ count: 100000 }`
   - 使用 `@Async` + `ThreadPoolTaskExecutor` 异步执行
   - MyBatis 批量 insert（`<foreach collection="list">`），每批 500 条
   - 返回 `{ taskId: "uuid" }`，前端轮询任务状态
3. 任务状态查询 `GET /crm/behavior/task/{taskId}`：返回 `{ status: "RUNNING"/"DONE"/"FAILED", processed: 已处理数, total: 总数 }`
4. 游标分页查询 `GET /crm/behavior/scroll`：
   - 参数：`lastId`（上页最后 ID）、`pageSize`（默认 100）
   - SQL：`SELECT * FROM (SELECT * FROM CRM_CUSTOMER_BEHAVIOR WHERE ID > #{lastId} ORDER BY ID) WHERE ROWNUM <= #{pageSize}`
   - 返回：`{ list: [...], lastId: 最后一条ID, hasMore: true/false }`
   - 响应 header 加 `X-Response-Time: 耗时ms`

#### 前端任务

1. 新增 `web/src/views/crm/behavior/index.vue`（海量数据）：
   - 顶部：`<el-button>` 生成数据 → `<el-dialog>` 输入数量（默认 100000）→ 开始导入
   - `<el-progress>` 进度条，轮询 `task/{taskId}` 更新进度
   - 导入完成后主内容区：
     - 固定高度容器 `<div class="scroll-container" style="height: calc(100vh - 200px); overflow-y: auto">`
     - **无限滚动**：使用 `@vueuse/core` 的 `useScroll` + `useInfiniteScroll` 或手写：
       ```js
       const containerRef = ref(null)
       const { arrivedState } = useScroll(containerRef)
       watch(() => arrivedState.bottom, (v) => { if (v && hasMore.value) loadMore() })
       ```
     - 每次请求带上 `lastId`，追加数据到列表（`list.value.push(...newData)`），不替换
     - `<el-table>` 或手动渲染（若数据量极大，使用虚拟滚动或简单 DOM 追加）
     - 底部「已加载 X / 共 total 条」提示 + `<el-icon class="is-loading"><Loading /></el-icon>` 加载中
   - 页面顶部固定显示「接口响应时间：**XX ms**」（解析 header `X-Response-Time`）
2. 新增 `web/src/api/crm/behavior.js`

#### 得分覆盖

| 得分点 | 分值 | 实现方式 |
|---|---|---|
| 异步导入10万+数据 | 4分 | `@Async` 批量 insert + `<el-progress>` 进度条轮询 |
| 无分页无限滚动，无卡顿无空白 | 4分 | 游标分页 + 数据追加 + `useScroll` 底部检测 + 节流 |
| 页面展示接口响应耗时 | 2分 | 响应 header `X-Response-Time` + 页面顶部实时渲染 |

---

### 阶段六：用户画像（对应 2.6，得分目标：5分）

#### 后端任务

1. 用户画像接口 `GET /crm/profile/{userId}`：
   - 实时从 `CRM_USER_PROFILE` 表查询统计数据
   - 返回数据结构：
     ```json
     {
       "tagCloud": [
         { "name": "制造业", "value": 15 },
         { "name": "金融", "value": 10 },
         { "name": "电商", "value": 8 }
       ],
       "ringChart": [
         { "name": "初步接触", "value": 12 },
         { "name": "需求分析", "value": 8 },
         { "name": "报价", "value": 5 },
         { "name": "谈判", "value": 3 },
         { "name": "成交", "value": 2 },
         { "name": "丢单", "value": 4 }
       ],
       "radarChart": {
         "indicator": [
           { "name": "客户数", "max": 100 },
           { "name": "商机数", "max": 50 },
           { "name": "合同额", "max": 1000 },
           { "name": "赢单率", "max": 100 },
           { "name": "活跃度", "max": 100 },
           { "name": "回款率", "max": 100 }
         ],
         "data": [{ "value": [45, 20, 380, 35, 60, 80] }]
       }
     }
     ```
   - 接口不设缓存，每次实时查询统计（确保业务数据变更后联动更新）
2. 数据联动：在 CustomerService / OpportunityService / ContractService 的增删改方法中，异步调用 `CrmUserProfileService.refresh(userId)` 更新统计表

#### 前端任务

1. 新增 `web/src/views/crm/dashboard/profile.vue`（用户画像）：
   - 页面标题 + 当前用户信息
   - 使用 `<el-row>` + `<el-col>` 三栏布局：
     - **标签云**（左，`:span="8"`）：ECharts `wordCloud` 扩展（或 echarts-wordcloud 插件），字号/颜色按 value 映射
     - **环形图**（中，`:span="8"`）：ECharts `pie` + `radius: ['40%', '70%']`，展示商机阶段分布
     - **雷达图**（右，`:span="8"`）：ECharts `radar`，展示 6 维业绩指标
   - 在 `mounted` 中调用 `/crm/profile/{userId}` 获取数据初始化图表
   - 联动验证：切换到商机管理页修改阶段 → 回到画像页，图表数据自动更新（因无缓存）
2. 新增 `web/src/api/crm/profile.js`

> 若 ECharts 原生不支持词云，方案改为：使用 CSS 手工标签云（`<el-tag>` 不同 size），或安装 `echarts-wordcloud` 插件。

#### 得分覆盖

| 得分点 | 分值 | 实现方式 |
|---|---|---|
| 标签云 + 环形图 + 雷达图 | 5分 | ECharts wordCloud(或CSS标签云) + pie + radar |
| 业务数据变更后联动更新 | (含在5分) | 无缓存实时查询 + 异步统计更新 |

---

### 阶段七：工作流功能（对应 2.7，得分目标：10分）

> **状态：✅ 已完成**（2026-05-23）  
> **简介**：实现合同 CRUD、4 节点审批流程（提交→部门审批→财务审批→归档）、6 种审批操作（通过/驳回/会签/转办/回退/终止）、BPMN 流程图可视化（bpmn-js 节点染色）、审批历史时间线，终审通过后合同状态自动更新为已生效。

#### 后端任务

- [x] 1. `/crm/workflow` 全套接口（start/approve/reject/countersign/transfer/rollback/terminate/instance/bpmn）
- [x] 2. Service 层状态机校验
- [x] 3. 终审通过 → 合同状态 ACTIVE

#### 前端任务

- [x] 1. 安装 bpmn-js
- [x] 2. `crm/contract/index.vue` — CRUD + 发起审批
- [x] 3. `crm/workflow/index.vue` — BPMN 图 + 操作区 + 时间线
- [x] 4. `web/src/api/crm/workflow.js`

#### 得分覆盖

| 得分点 | 分值 | 状态 | 实现方式 |
|---|---|---|---|
| 4节点完整流转 | 4分 | ✅ | 提交→部门→财务→归档 |
| 6种审批操作 | 4分 | ✅ | 6 个 PUT 接口 + 状态校验 |
| BPMN 可视化 | 2分 | ✅ | bpmn-js BpmnViewer + 节点染色 |

---

### 阶段八：API 服务市场（对应 2.10，得分目标：5分）

#### 后端任务

1. API 管理接口 `/crm/api`：
   - `GET /list` — API 列表（按名称搜索、状态筛选、分页）
   - `POST /` — 新增发布 API
   - `PUT /` — 编辑 API
   - `DELETE /{ids}` — 删除 API
   - `PUT /online/{id}` — 上架（status → '1'）
   - `PUT /offline/{id}` — 下架（status → '0'）
2. 在线调试 `POST /crm/api/debug/{id}`：
   - 查询 API 配置 → 使用 `RestTemplate` 按配置的 URL + METHOD 代理转发请求
   - 仅 status='1' 允许调试，否则返回"接口已下架"
   - 返回：代理请求的响应状态码、响应体、响应时间
3. 示例 API `GET /crm/api/demo/customer-count` — 返回当前客户总数

#### 前端任务

1. 新增 `web/src/views/crm/api/index.vue`（API 服务市场，单页面双 Tab）：
   - `<el-tabs>` 切换：
     - **Tab1「API 市场」**：卡片式布局（`<el-row>` + `<el-col>` + `<el-card>`）
       - 每张卡片展示：名称、描述、请求方式 `<el-tag>`（GET=绿/POST=蓝）、接口地址
       - 卡片操作：`<el-button>` 在线调试 → 弹出调试弹窗
     - **Tab2「API 管理」**：`<el-table>` 展示全部 API（含未上架）
       - 状态列：`<el-switch>` 切换上架/下架
       - 操作列：编辑、删除
   - **在线调试弹窗**（`<el-dialog>`）：
     - 上半区：请求信息（URL、Method 不可编辑）
     - 请求参数 `<el-input type="textarea">`（预填 `REQUEST_EXAMPLE`）
     - 发送按钮 + 加载状态
     - 下半区：响应结果 `<el-input type="textarea" readonly>`，JSON 格式化展示
     - 响应时间展示
   - 下架 API 的调试按钮 disabled + tooltip "接口已下架，无法调试"
2. 新增 `web/src/api/crm/apiMarket.js`

#### 得分覆盖

| 得分点 | 分值 | 实现方式 |
|---|---|---|
| 新增发布 + 在线调试验证可用性 | 2分 | `<el-form>` 发布 + RestTemplate 代理调试弹窗 |
| 上下架操作 | 1分 | `<el-switch>` 切换 + 后端状态校验 |
| 编辑 + 删除 | 2分 | `<el-dialog>` 编辑 + `<el-popconfirm>` 删除 |

---

### 阶段九：工作台小组件（对应 2.9，得分目标：5分）

#### 后端任务

1. 工作台接口 `/crm/workbench`：
   - `GET /layout` — 查询当前用户布局（查 `CRM_WORKBENCH_LAYOUT`）
   - `POST /layout` — 保存布局（JSON 存入 `LAYOUT_JSON` 字段）
   - `GET /widgets` — 返回可用小组件元数据列表
   - `GET /widget/data/{widgetId}` — 获取指定小组件数据：
     - `approval` → 当前用户待审批合同列表
     - `customer-stats` → 负责客户总数、本月新增
     - `opportunity-funnel` → 各阶段商机数量
     - `unread-messages` → 最近 5 条未读消息
     - `quick-actions` → 快捷入口列表（静态）
2. 布局 JSON 结构：
   ```json
   {
     "widgets": [
       { "id": "approval", "x": 0, "y": 0, "w": 6, "h": 4 },
       { "id": "customer-stats", "x": 6, "y": 0, "w": 6, "h": 2 }
     ]
   }
   ```

#### 前端任务

1. 新增 `web/src/views/crm/workbench/index.vue`（工作台）：
   - 使用 **vuedraggable**（已安装）实现网格拖拽：
     ```vue
     <template>
       <div class="workbench">
         <div class="toolbar">
           <el-button @click="addWidget">添加组件</el-button>
           <el-button @click="saveLayout">保存布局</el-button>
           <el-button @click="resetLayout">重置布局</el-button>
         </div>
         <draggable v-model="widgets" item-key="id" @end="onDragEnd" :animation="200">
           <template #item="{ element }">
             <div class="widget-card" :style="{ gridColumn: `span ${element.w}` }">
               <div class="widget-header">{{ element.title }}</div>
               <div class="widget-body">
                 <!-- 根据 widget.id 动态渲染不同组件 -->
                 <ApprovalWidget v-if="element.id === 'approval'" />
                 <CustomerStatsWidget v-if="element.id === 'customer-stats'" />
                 <!-- ... -->
               </div>
             </div>
           </template>
         </draggable>
       </div>
     </template>
     ```
   - 功能实现（3分）：
     - 拖拽调整位置（vuedraggable 内置）
     - 「添加组件」按钮弹出面板，选择未添加的组件
     - 「保存布局」调用 `POST /layout` 持久化
     - 「重置布局」恢复默认 2×2 或 3×2 网格排列
   - 预定义 5 个小组件（1分）：
     1. **待办审批**：列表展示待审批合同，点击行跳转审批页 → **业务交互**（1分）
     2. **客户统计**：数字卡片（总数 + 本月新增）
     3. **商机漏斗**：迷你 ECharts 漏斗图
     4. **未读消息**：最近 5 条消息列表
     5. **快捷操作**：`<el-button>` 新建客户 / 新建商机
2. 新增 `web/src/api/crm/workbench.js`
3. 新增 `web/src/stores/crm/workbench.js`（Pinia），管理布局状态

#### 得分覆盖

| 得分点 | 分值 | 实现方式 |
|---|---|---|
| 拖拽布局 + 初始化 + 添加 + 保存 | 3分 | vuedraggable + 布局 JSON 持久化到后端 |
| 不少于5个小组件 | 1分 | 5 个预定义组件 |
| 至少1个组件有业务交互 | 1分 | 待办审批组件内点击跳转审批页 |

---

## 八、系统功能菜单最终结构

```
CRM系统
├── 🏠 工作台（/crm/workbench）                ← 2.9 工作台小组件 5分
├── 👤 客户管理（/crm/customer）                ← 2.1 基础CRUD 4分 + 高级表格 6分
│   └── 联系人管理（/crm/contact）
├── 💼 商机管理（/crm/opportunity）             ← 2.2 Excel导入 10分
├── 📋 合同管理（/crm/contract）                ← 2.7 工作流 10分
│   └── 审批流程（/crm/workflow/:instanceId）
├── 🔔 消息中心（/crm/message）                 ← 2.4 消息中心 5分
│   ├── 模板管理（/crm/message/template）
│   ├── 发送消息（/crm/message/send）
│   └── 消息记录（/crm/message/record）
├── 📊 数据大屏（/crm/dashboard）               ← 2.3 监控审计 10分 + 2.6 用户画像 5分
│   └── 用户画像（/crm/dashboard/profile）
├── 🔌 API服务市场（/crm/api）                  ← 2.10 API市场 5分
├── 📄 客户行为记录（/crm/behavior）            ← 2.5 海量数据 10分
└── 📝 操作日志（/crm/operlog）                 ← 2.3 日志收集/链路追踪
```

---

## 九、非 Web 端实现（放到最后开发）

### 9.1 浏览器插件（对应 2.8，得分目标：5分）

> 独立于 Web 项目，放在项目根目录 `browser-plugin/` 下。

#### 插件目录结构

```
browser-plugin/
├── manifest.json          # Chrome Extension v3 清单
├── popup.html             # 弹出窗口（模板管理）
├── popup.js               # 弹出逻辑
├── content.js             # 页面注入脚本（校验 + 填充）
├── styles.css             # 样式
└── icons/
    ├── icon16.png
    ├── icon48.png
    └── icon128.png
```

#### manifest.json 关键配置

```json
{
  "manifest_version": 3,
  "name": "CRM表单助手",
  "version": "1.0",
  "permissions": ["storage"],
  "content_scripts": [{
    "matches": ["*://localhost:*/crm/customer/*", "*://*/crm/customer/*"],
    "js": ["content.js"],
    "css": ["styles.css"]
  }],
  "action": {
    "default_popup": "popup.html"
  }
}
```

#### 功能实现

1. **表单校验按钮**（3分）：
   - `content.js` 注入到客户新增/编辑表单页面
   - 在表单底部注入一个「校验表单」按钮（Element Plus 风格）
   - 点击校验至少 5 个字段：
     - 客户名称（`.el-form-item:has(input[placeholder*='客户名称'])` 值非空）
     - 联系电话（手机号正则 `/^1[3-9]\d{9}$/`）
     - 邮箱（邮箱正则 `/^[\w.-]+@[\w.-]+\.\w+$/`）
     - 客户等级（`.el-select` 值非空）
     - 所在城市（输入值非空）
   - 校验结果：在当前页面顶部注入一个结果面板（绿色通过 + 红色错误 + 具体提示），用 DOM 操作创建
2. **模板保存与填充**（2分）：
   - 校验全部通过后，弹出「保存为模板」按钮
   - 保存：读取当前表单全部字段值 → `chrome.storage.local.set({ templateName: {...} })`
   - 填充：在新表单页面检测到 `chrome.storage.local` 中有模板 → 注入「选择模板」下拉按钮 → 选择后遍历 DOM 自动填入对应字段
3. **popup.html** 提供模板管理入口：查看已保存模板列表、删除模板

#### 得分覆盖

| 得分点 | 分值 | 实现方式 |
|---|---|---|
| 新增校验按钮 + ≥5字段校验 + 精准结果展示 | 3分 | content.js DOM 注入 + 正则校验 + 结果面板渲染 |
| 模板保存 + 一键填充 | 2分 | chrome.storage.local 存取 + DOM 自动填表 |

---

### 9.2 项目部署（对应第三部分，得分目标：5分）

> 比武当日独立完成部署。

#### 部署流程

1. **前端构建**：
   ```bash
   cd web
   npm run build:prod
   # 产出 dist/ 目录
   ```
   将 `dist/` 内容复制到 `springBoot/ruoyi-admin/src/main/resources/static/` 下，随 Spring Boot JAR 一同部署。

2. **后端打包**：
   ```bash
   cd springBoot
   mvn clean package -DskipTests
   # 产出 ruoyi-admin/target/ruoyi-admin.jar
   ```

3. **启动**：
   ```bash
   nohup java -jar ruoyi-admin.jar --spring.profiles.active=druid > app.log 2>&1 &
   ```

4. **数据库初始化**：
   - 按 `SQL/` 文件夹下脚本编号顺序，在 Oracle 11g 中执行建表与初始数据

5. **验证清单**：
   - [ ] 客户管理 CRUD + 列排序 + 列显隐 + Excel 导出
   - [ ] 商机 Excel 导入（校验 + 去重提示）
   - [ ] 合同审批流程（4 节点流转 + BPMN 图）
   - [ ] 消息模板 → 发送 → 撤回 → 重发
   - [ ] 数据大屏 6 指标实时刷新
   - [ ] 用户画像三图表联动
   - [ ] 海量数据 10 万+ 异步导入 + 无限滚动
   - [ ] API 市场发布/调试/上下架
   - [ ] 工作台拖拽 + 5 组件
   - [ ] 操作日志多条件查询 + SQL 链路追踪
   - [ ] 浏览器插件校验 + 模板填充
   - [ ] 系统无 500 / JS 报错

---

## 十、新增 NPM 依赖

```bash
cd web

# 工作流 BPMN 渲染（阶段七）
npm install bpmn-js

# （可选）词云图表扩展（阶段六，若不用CSS方案）
npm install echarts-wordcloud
```

---

## 十一、开发优先级与顺序

```
第1优先级（基础 + 高分，共 20 分）：阶段一（2.1, 10分）→ 阶段二（2.2, 10分）
第2优先级（核心业务，共 20 分）：阶段七（2.7, 10分）→ 阶段三（2.3, 10分）
第3优先级（体验功能，共 10 分）：阶段四（2.4, 5分）→ 阶段六（2.6, 5分）
第4优先级（高级功能，共 20 分）：阶段五（2.5, 10分）→ 阶段八（2.10, 5分）→ 阶段九（2.9, 5分）
第5优先级（独立模块）：浏览器插件（2.8, 5分）
第6优先级（收尾）：部署验证（第三部分, 5分）
```

**总分**：10 + 10 + 10 + 10 + 5 + 5 + 10 + 5 + 5 + 5 + 5 = **80 分（开发）** + 20 分（系统设计）= **100 分**

---

## 十二、开发约定

| 类别 | 约定 |
|---|---|
| 后端包路径 | `com.ruoyi.system.crm.{controller\|service\|service.impl\|mapper\|domain}` |
| 前端视图路径 | `web/src/views/crm/{module}/index.vue` |
| 前端 API 路径 | `web/src/api/crm/{module}.js` |
| 前端 Store 路径 | `web/src/stores/crm/{module}.js` |
| SQL 脚本路径 | `项目根目录/SQL/`，按 `01_xxx.sql` `02_xxx.sql` 编号 |
| 数据库表前缀 | CRM 业务表统一 `CRM_`，字段大写下划线 |
| 后端接口风格 | RESTful，统一返回若依 `AjaxResult` |
| 前端组件风格 | `<script setup>` + Composition API，Element Plus 组件 |
| 日志注解 | 所有 Controller 方法加 `@Log`，`businessType` 如实填写 |
| 安全 | MyBatis `#{}` 防注入、若依 XSS 过滤器（已启用）、CSRF Token |

---

## 十三、开发进度总览

| 阶段 | 对应得分 | 状态 | 完成日期 | 简要说明 |
|---|---|---|---|---|
| 阶段一：基础设施 + 基础 CRUD | 2.1（10分） | ✅ 已完成 | 2026-05-23 | 建表脚本、客户/联系人 CRUD、列排序/列显隐/Excel 导出 |
| 阶段二：Excel 批量导入 | 2.2（10分） | ✅ 已完成 | 2026-05-23 | 商机 CRUD + Excel 导入校验去重 + 错误行/列精准提示 |
| 阶段三：日志与数据可视化 | 2.3（10分） | ✅ 已完成 | 2026-05-23 | AOP日志+traceId/SQL追踪+oshi大屏+operlog查询详情 |
| 阶段四：消息中心 | 2.4（5分） | ✅ 已完成 | 2026-05-23 | 模板CRUD+引用模板发送+撤回重发+首页Bell角标 |
| 阶段五：海量数据加载 | 2.5（10分） | ✅ 已完成 | 2026-05-23 | @Async批量生成10万+游标无限滚动+X-Response-Time |
| 阶段六：用户画像 | 2.6（5分） | ⬜ 待开发 | — | — |
| 阶段七：工作流 | 2.7（10分） | ✅ 已完成 | 2026-05-23 | 合同4节点审批+BPMN图+6种操作 |
| 阶段八：API 服务市场 | 2.10（5分） | ⬜ 待开发 | — | — |
| 阶段九：工作台小组件 | 2.9（5分） | ⬜ 待开发 | — | — |
| 浏览器插件 | 2.8（5分） | ⬜ 待开发 | — | — |
| 项目部署 | 第三部分（5分） | ⬜ 待开发 | — | — |

### 阶段一交付物清单

| 类型 | 路径 |
|---|---|
| SQL 建表 | `SQL/01_init_tables.sql` |
| SQL 菜单 | `SQL/02_crm_menu.sql` |
| 后端 Controller | `springBoot/ruoyi-system/.../crm/controller/CrmCustomerController.java` 等 |
| 后端 Mapper XML | `springBoot/ruoyi-system/src/main/resources/mapper/crm/*.xml` |
| 前端 API | `web/src/api/crm/customer.js`、`contact.js` |
| 前端页面 | `web/src/views/crm/customer/index.vue`、`contact/index.vue` |

### 阶段三交付物清单

| 类型 | 路径 |
|---|---|
| 后端 AOP/Filter | `CrmOperLogAspect.java`、`CrmTraceIdFilter.java`、`CrmSqlInterceptor.java` |
| 后端 Controller | `CrmOperLogController.java`、`CrmDashboardController.java` |
| 后端 Service | `CrmOperLogServiceImpl.java`、`CrmDashboardServiceImpl.java`（oshi-core） |
| SQL 菜单 | `SQL/05_crm_dashboard_operlog_menu.sql` |
| 前端 API | `web/src/api/crm/dashboard.js`、`operlog.js` |
| 前端页面 | `web/src/views/crm/dashboard/index.vue`、`operlog/index.vue` |

### 阶段四交付物清单

| 类型 | 路径 |
|---|---|
| 后端 Controller | `CrmMessageTemplateController.java`、`CrmMessageRecordController.java` |
| 后端 Service/Mapper | `CrmMessageTemplateServiceImpl`、`CrmMessageRecordServiceImpl` + Mapper XML |
| SQL 菜单 | `SQL/06_crm_message_menu.sql` |
| 前端 API | `web/src/api/crm/message.js` |
| 前端页面 | `web/src/views/crm/message/template.vue`、`send.vue`、`record.vue` |
| Pinia Store | `web/src/stores/crm/message.js` |
| 首页提醒 | `web/src/layout/components/CrmHeaderMessage/index.vue` |

### 阶段五交付物清单

| 类型 | 路径 |
|---|---|
| 建表 | `crm_customer_behavior` 已在 `SQL/01_init_tables.sql` |
| 后端 Async | `CrmCustomerBehaviorAsyncService.java` + `CrmAsyncConfig.java` |
| 后端 Controller | `CrmCustomerBehaviorController.java` |
| SQL 菜单 | `SQL/07_crm_behavior_menu.sql` |
| 前端 API | `web/src/api/crm/behavior.js` |
| 前端页面 | `web/src/views/crm/behavior/index.vue` |

### 启动前需执行的 SQL（按顺序）

1. 若依基础库（已有则跳过）：`springBoot/sql/ry_20260417.sql`
2. CRM 建表：`SQL/01_init_tables.sql`
3. CRM 菜单：`SQL/02_crm_menu.sql`
4. 商机菜单：`SQL/03_crm_opportunity_menu.sql`
5. 合同/工作流菜单：`SQL/04_crm_contract_workflow_menu.sql`
6. 大屏/操作日志菜单：`SQL/05_crm_dashboard_operlog_menu.sql`
7. 消息中心菜单：`SQL/06_crm_message_menu.sql`
8. 海量行为数据菜单：`SQL/07_crm_behavior_menu.sql`
