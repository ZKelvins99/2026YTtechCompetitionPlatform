import request from '@/utils/request'

export function listContract(query) {
  return request({ url: '/crm/contract/list', method: 'get', params: query })
}

export function getContract(id) {
  return request({ url: '/crm/contract/' + id, method: 'get' })
}

export function addContract(data) {
  return request({ url: '/crm/contract', method: 'post', data })
}

export function updateContract(data) {
  return request({ url: '/crm/contract', method: 'put', data })
}

export function delContract(ids) {
  return request({ url: '/crm/contract/' + ids, method: 'delete' })
}
