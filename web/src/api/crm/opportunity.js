import request from '@/utils/request'

export function listOpportunity(query) {
  return request({ url: '/crm/opportunity/list', method: 'get', params: query })
}

export function getOpportunity(id) {
  return request({ url: '/crm/opportunity/' + id, method: 'get' })
}

export function addOpportunity(data) {
  return request({ url: '/crm/opportunity', method: 'post', data })
}

export function updateOpportunity(data) {
  return request({ url: '/crm/opportunity', method: 'put', data })
}

export function delOpportunity(ids) {
  return request({ url: '/crm/opportunity/' + ids, method: 'delete' })
}

export function importOpportunity(data) {
  return request({
    url: '/crm/opportunity/import',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
