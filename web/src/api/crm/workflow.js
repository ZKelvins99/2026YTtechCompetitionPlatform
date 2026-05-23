import request from '@/utils/request'

export function startWorkflow(data) {
  return request({ url: '/crm/workflow/start', method: 'post', data })
}

export function getWorkflowInstance(id) {
  return request({ url: '/crm/workflow/instance/' + id, method: 'get' })
}

export function getWorkflowBpmn(id) {
  return request({ url: '/crm/workflow/instance/' + id + '/bpmn', method: 'get' })
}

export function approveWorkflow(data) {
  return request({ url: '/crm/workflow/approve', method: 'put', data })
}

export function rejectWorkflow(data) {
  return request({ url: '/crm/workflow/reject', method: 'put', data })
}

export function countersignWorkflow(data) {
  return request({ url: '/crm/workflow/countersign', method: 'put', data })
}

export function transferWorkflow(data) {
  return request({ url: '/crm/workflow/transfer', method: 'put', data })
}

export function rollbackWorkflow(targetNodeId, data) {
  return request({ url: '/crm/workflow/rollback/' + targetNodeId, method: 'put', data })
}

export function terminateWorkflow(data) {
  return request({ url: '/crm/workflow/terminate', method: 'put', data })
}
