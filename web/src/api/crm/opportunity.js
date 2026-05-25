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

/** 创建 Excel 样例所需的测试客户（CRM导入测试客户A/B/C） */
export function seedOpportunityImportCustomers() {
  return request({
    url: '/crm/opportunity/seedImportCustomers',
    method: 'post'
  })
}
