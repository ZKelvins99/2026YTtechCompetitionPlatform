import request from '@/utils/request'

export function getWorkbenchLayout() {
  return request({ url: '/crm/workbench/layout', method: 'get' })
}

export function saveWorkbenchLayout(data) {
  return request({ url: '/crm/workbench/layout', method: 'post', data })
}

export function getWorkbenchWidgets() {
  return request({ url: '/crm/workbench/widgets', method: 'get' })
}

export function getWidgetData(widgetId) {
  return request({ url: '/crm/workbench/widget/data/' + widgetId, method: 'get' })
}
