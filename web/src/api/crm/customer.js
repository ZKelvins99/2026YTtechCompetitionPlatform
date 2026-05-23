import request from '@/utils/request'

export function listCustomer(query) {
  return request({ url: '/crm/customer/list', method: 'get', params: query })
}

export function getCustomer(id) {
  return request({ url: '/crm/customer/' + id, method: 'get' })
}

export function addCustomer(data) {
  return request({ url: '/crm/customer', method: 'post', data })
}

export function updateCustomer(data) {
  return request({ url: '/crm/customer', method: 'put', data })
}

export function delCustomer(ids) {
  return request({ url: '/crm/customer/' + ids, method: 'delete' })
}

export function getCrmDict(type) {
  return request({ url: '/crm/dict/' + type, method: 'get' })
}
