import request from '@/utils/request'

export function listContact(query) {
  return request({ url: '/crm/contact/list', method: 'get', params: query })
}

export function getContact(id) {
  return request({ url: '/crm/contact/' + id, method: 'get' })
}

export function addContact(data) {
  return request({ url: '/crm/contact', method: 'post', data })
}

export function updateContact(data) {
  return request({ url: '/crm/contact', method: 'put', data })
}

export function delContact(ids) {
  return request({ url: '/crm/contact/' + ids, method: 'delete' })
}

export function listCustomerOptions() {
  return request({ url: '/crm/customer/list', method: 'get', params: { pageNum: 1, pageSize: 9999 } })
}
