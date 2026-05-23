import request from '@/utils/request'

// 消息模板
export function listTemplate(query) {
  return request({ url: '/crm/message/template/list', method: 'get', params: query })
}

export function getTemplate(id) {
  return request({ url: '/crm/message/template/' + id, method: 'get' })
}

export function addTemplate(data) {
  return request({ url: '/crm/message/template', method: 'post', data })
}

export function updateTemplate(data) {
  return request({ url: '/crm/message/template', method: 'put', data })
}

export function delTemplate(ids) {
  return request({ url: '/crm/message/template/' + ids, method: 'delete' })
}

// 消息记录
export function listRecord(query) {
  return request({ url: '/crm/message/record/list', method: 'get', params: query })
}

export function getSendTemplateList() {
  return request({ url: '/crm/message/record/send/template/list', method: 'get' })
}

export function sendMessage(data) {
  return request({ url: '/crm/message/record/send', method: 'post', data })
}

export function recallMessage(id) {
  return request({ url: '/crm/message/record/recall/' + id, method: 'put' })
}

export function resendMessage(id) {
  return request({ url: '/crm/message/record/resend/' + id, method: 'put' })
}

export function getUnreadCount() {
  return request({ url: '/crm/message/record/unread-count', method: 'get' })
}

export function getUnreadList() {
  return request({ url: '/crm/message/record/unread-list', method: 'get' })
}
