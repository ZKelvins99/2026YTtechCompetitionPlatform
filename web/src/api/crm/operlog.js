import request from '@/utils/request'

export function listOperlog(query) {
  return request({ url: '/crm/operlog/list', method: 'get', params: query })
}

export function getOperlog(id) {
  return request({ url: '/crm/operlog/' + id, method: 'get' })
}
