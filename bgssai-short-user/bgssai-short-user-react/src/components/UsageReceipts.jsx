import { useEffect, useState } from 'react'
import { Alert, Button, Collapse, Table } from 'antd'
import api from '../utils/http'
import { isValidBillingMultiplier, receiptSummary } from '../modelPricing'
import { publicModelText } from '../publicModel'
export default function UsageReceipts({ workRef }) {
 const [rows,setRows] = useState([])
 const [error,setError] = useState('')
 const [loading,setLoading] = useState(false)
 async function load() {
  setLoading(true)
  try { const response = await api.get('/user/ai-usage/list', { work_ref: workRef }); setRows(response.data?.result?.items || []); setError('') } catch { setError('暂时无法读取用量对账，请稍后刷新') } finally { setLoading(false) }
 }
 useEffect(() => { load() }, [workRef])
 return <Collapse style={{marginBottom:16}} items={[{key:'usage',label:'模型任务实际用量',children:<>
  <p>按每次生成任务实时登记用量；待对账表示费用结算进行中，此处最多展示最近 100 条调用记录。</p>
  <Button loading={loading} onClick={load}>刷新对账</Button>
  {error && <Alert type="warning" message={error} />}
  <Table size="small" rowKey={(row)=>row.request_id || row.task_ref} dataSource={rows} scroll={{x:1100}} columns={[
   {title:'模型',width:140,render:(_,row)=>publicModelText(row.model, row.ability_type) || '-'},
   {title:'任务',width:160,render:(_,row)=>row.task_ref || '-'},
   {title:'倍率',width:80,render:(_,row)=>isValidBillingMultiplier(row.receipt?.billing_multiplier) ? `${Number(row.receipt.billing_multiplier)}×` : '待确认'},
   {title:'请求',width:220,render:(_,row)=>row.request_id ? <code>{row.request_id}</code> : '-'},
   {title:'实际用量与费用',render:(_,row)=>receiptSummary(row.receipt)}
  ]} />
 </>}]} />
}
