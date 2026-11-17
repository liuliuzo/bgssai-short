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
    mode: withdraw ? 'withdraw' : 'submit',
    buttonLabel: withdraw ? '撤回上架申请' : '申请上架 Tube',
    enabled: withdraw ? true : submit,
    submitReason: submit ? '' : (status === 'APPROVED'
      ? '这一集已通过上架审核'
      : status === 'PENDING'
        ? '上架申请审核中，先撤回才能重新申请'
        : '成片就绪后才能申请上架 Tube'),
    withdrawReason: withdraw ? '' : '只有上架申请审核中的这一集可以撤回',
  }
}

export function formatTubePublishHint(hint) {
  const text = String(hint || '').trim()
  if (!text) return ''
  if (/review_status=NOT_SUBMITTED/i.test(text)) return '尚未申请上架 Tube'
  if (/review_status=PENDING/i.test(text)) return '上架申请审核中'
  if (/review_status=REJECTED/i.test(text)) return '上架申请未通过'
  if (/剧集审核未通过/.test(text)) return '上架审核通过后可发布到 Tube'
  return text
}
