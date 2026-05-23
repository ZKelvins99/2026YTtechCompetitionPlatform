import request from '@/utils/request'

export function listApi(query) {
  return request({ url: '/crm/api/list', method: 'get', params: query })
}

export function getApi(id) {
  return request({ url: '/crm/api/' + id, method: 'get' })
}

export function addApi(data) {
  return request({ url: '/crm/api', method: 'post', data })
}

export function updateApi(data) {
  return request({ url: '/crm/api', method: 'put', data })
}

export function delApi(ids) {
  return request({ url: '/crm/api/' + ids, method: 'delete' })
}

export function onlineApi(id) {
  return request({ url: '/crm/api/online/' + id, method: 'put' })
}

export function offlineApi(id) {
  return request({ url: '/crm/api/offline/' + id, method: 'put' })
}

export function debugApi(id, data) {
  return request({ url: '/crm/api/debug/' + id, method: 'post', data: data || {} })
}
