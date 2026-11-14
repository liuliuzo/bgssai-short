/**
 * Submit and withdraw are mutually exclusive.
 * Withdraw only while PENDING. Submit only when compose is ready and review is not pending or approved.
 */
export function reviewActionState({ reviewStatus, composeReady = false } = {}) {
  const status = reviewStatus || 'NOT_SUBMITTED'
  const withdraw = status === 'PENDING'
  const submit = Boolean(composeReady) && !withdraw && status !== 'APPROVED'
  return {
    submit,
    withdraw,
    submitReason: submit ? '' : (status === 'APPROVED'
      ? '这一集已经通过审核'
      : status === 'PENDING'
        ? '审核中，先撤回才能再提交'
        : '成片就绪后才能提交审核'),
    withdrawReason: withdraw ? '' : '只有审核中的这一集可以撤回',
  }
}
