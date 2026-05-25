/** 各 CRM 页面「功能介绍」文案（用户向，非技术说明） */
export const CRM_PAGE_INTRO = {
  customer: {
    tagline: '维护企业客户档案',
    features: [
      '按客户名称、等级、行业查询',
      '新增、修改、删除客户',
      '表格列排序、自定义显示列',
      '导出 Excel'
    ]
  },
  contact: {
    tagline: '维护客户对接人',
    features: [
      '按客户、联系人姓名查询',
      '新增、修改、删除联系人',
      '记录手机、邮箱、职务等信息'
    ]
  },
  opportunity: {
    tagline: '跟进销售商机',
    features: [
      '按商机名称、阶段、客户查询',
      '新增、修改、删除商机',
      'Excel 批量导入',
      '导入时自动校验格式与重复数据'
    ]
  },
  contract: {
    tagline: '管理销售合同',
    features: [
      '按合同名称、状态查询',
      '新增、修改、删除合同',
      '发起审批并进入流程办理'
    ]
  },
  behavior: {
    tagline: '查看客户行为记录',
    features: [
      '按条件浏览行为数据',
      'Excel 批量导入',
      '下载导入模板与样例',
      '游标分页加载大量行为记录，支持一键刷新'
    ]
  },
  workbench: {
    tagline: '个人首页一览',
    features: [
      '拖拽调整组件位置与大小',
      '添加、移除工作台组件',
      '保存自己的布局',
      '待办事项点击即可办理'
    ]
  },
  api: {
    tagline: '管理与调试开放接口',
    features: [
      '浏览已发布接口',
      '在线调试请求',
      '发布、编辑、上下架、删除接口'
    ]
  },
  messageTemplate: {
    tagline: '配置消息模板',
    features: ['新建、编辑、删除模板', '供「发送消息」页面选用']
  },
  messageSend: {
    tagline: '向用户发送通知',
    features: ['选择模板并填写内容', '选择接收人', '一键发送']
  },
  messageRecord: {
    tagline: '查看已发消息',
    features: ['按标题、状态查询', '撤回未读消息', '失败消息可重发']
  },
  operlog: {
    tagline: '查看系统操作记录',
    features: [
      '按操作人、时间筛选',
      '查看操作内容与结果',
      '便于核对谁何时做了什么'
    ]
  },
  profile: {
    features: [
      '行业分布标签云',
      '区域分布环形图',
      '业绩与能力雷达图',
      '业务数据变更后点击刷新即可更新'
    ]
  }
}
