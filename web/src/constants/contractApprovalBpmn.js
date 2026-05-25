import contractApprovalBpmnRaw from '@/assets/bpmn/contract-approval.bpmn?raw'

/** 合同审批 BPMN（与后端 resources/bpmn/contract-approval.bpmn 保持一致） */
export const CONTRACT_APPROVAL_BPMN = contractApprovalBpmnRaw.trim()

export function resolveWorkflowBpmnXml(instance, bpmnApiRes) {
  const fromInstance = instance?.bpmnXml
  if (typeof fromInstance === 'string' && fromInstance.includes('bpmn:definitions')) {
    return fromInstance
  }
  const payload = bpmnApiRes?.data ?? bpmnApiRes
  if (typeof payload === 'string' && payload.includes('bpmn:definitions')) {
    return payload
  }
  if (payload?.bpmnXml) {
    return payload.bpmnXml
  }
  return CONTRACT_APPROVAL_BPMN
}
