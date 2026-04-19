import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Button, Card, Form, Input, InputNumber, Modal, Space, Table, Tabs, Tag, Typography, message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { adminApi, type GoldTransaction } from '../api/admin'
import { goldApi } from '../api/gold'
import { useAuthStore } from '../store/authStore'
import type { UserInfo } from '../api/auth'

const { Title } = Typography

export default function AdminPage() {
  const navigate = useNavigate()
  const currentUser = useAuthStore(s => s.user)
  const setGold = useAuthStore(s => s.setGold)
  const isAdmin = currentUser?.roles.includes('ADMIN') ?? false

  const [users, setUsers] = useState<UserInfo[]>([])
  const [txs, setTxs] = useState<GoldTransaction[]>([])
  const [filterUserId, setFilterUserId] = useState<number | undefined>(undefined)
  const [loadingUsers, setLoadingUsers] = useState(false)
  const [loadingTxs, setLoadingTxs] = useState(false)

  // 發金幣 modal
  const [grantTarget, setGrantTarget] = useState<UserInfo | null>(null)
  const [grantForm] = Form.useForm()

  const loadUsers = async () => {
    setLoadingUsers(true)
    try {
      setUsers(await adminApi.listUsers())
    } catch (e: any) {
      message.error(e?.response?.data?.error ?? '載入玩家列表失敗')
    } finally {
      setLoadingUsers(false)
    }
  }

  const loadTxs = async (userId?: number) => {
    setLoadingTxs(true)
    try {
      setTxs(await adminApi.listTransactions(userId))
    } catch (e: any) {
      message.error(e?.response?.data?.error ?? '載入交易紀錄失敗')
    } finally {
      setLoadingTxs(false)
    }
  }

  useEffect(() => {
    if (!isAdmin) return
    loadUsers()
    loadTxs()
  }, [isAdmin])

  const submitGrant = async () => {
    if (!grantTarget) return
    const values = await grantForm.validateFields()
    try {
      const res = await goldApi.grant(grantTarget.id, values.amount, values.note)
      message.success(`${grantTarget.username} 餘額：${res.balance}`)
      // 若發給自己，更新 store
      if (grantTarget.id === currentUser?.id) {
        setGold(res.balance)
      }
      setGrantTarget(null)
      grantForm.resetFields()
      await Promise.all([loadUsers(), loadTxs(filterUserId)])
    } catch (e: any) {
      message.error(e?.response?.data?.error ?? '發放失敗')
    }
  }

  const userColumns: ColumnsType<UserInfo> = useMemo(() => [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '帳號', dataIndex: 'username' },
    { title: '顯示名稱', dataIndex: 'displayName', render: (v) => v ?? '—' },
    {
      title: '角色', dataIndex: 'roles',
      render: (rs: string[]) => rs.map(r => <Tag key={r} color={r === 'ADMIN' ? 'red' : 'blue'}>{r}</Tag>),
    },
    {
      title: '金幣', dataIndex: 'gold',
      render: (v: number) => <Tag color="gold">💰 {v?.toLocaleString() ?? 0}</Tag>,
      sorter: (a, b) => (a.gold ?? 0) - (b.gold ?? 0),
    },
    {
      title: '操作', key: 'action',
      render: (_, u) => (
        <Space>
          <Button size="small" type="primary" onClick={() => setGrantTarget(u)}>
            發金幣
          </Button>
          <Button size="small" onClick={() => { setFilterUserId(u.id); loadTxs(u.id) }}>
            看紀錄
          </Button>
        </Space>
      ),
    },
  ], [])

  const txColumns: ColumnsType<GoldTransaction> = useMemo(() => [
    {
      title: '時間', dataIndex: 'createdAt', width: 180,
      render: (v: string) => new Date(v).toLocaleString(),
    },
    { title: '玩家', dataIndex: 'username' },
    {
      title: '金額', dataIndex: 'amount', width: 100,
      render: (v: number) => (
        <Tag color={v >= 0 ? 'green' : 'red'}>
          {v >= 0 ? '+' : ''}{v.toLocaleString()}
        </Tag>
      ),
    },
    { title: '餘額', dataIndex: 'balanceAfter', width: 100 },
    { title: '原因', dataIndex: 'reason', width: 120 },
    { title: '關聯', dataIndex: 'refId', render: (v: string | null) => v ?? '—' },
    { title: '備註', dataIndex: 'note', render: (v: string | null) => v ?? '—' },
  ], [])

  if (!isAdmin) {
    return (
      <div style={{ padding: 32, textAlign: 'center' }}>
        <Title level={3}>沒有權限</Title>
        <Button onClick={() => navigate('/')}>返回首頁</Button>
      </div>
    )
  }

  return (
    <div style={{ padding: 24, maxWidth: 1200, margin: '0 auto' }}>
      <Space style={{ marginBottom: 16 }}>
        <Button onClick={() => navigate('/')}>← 返回</Button>
        <Title level={3} style={{ margin: 0 }}>管理員面板</Title>
      </Space>

      <Tabs
        items={[
          {
            key: 'users',
            label: `玩家（${users.length}）`,
            children: (
              <Card size="small">
                <Table
                  rowKey="id"
                  columns={userColumns}
                  dataSource={users}
                  loading={loadingUsers}
                  pagination={{ pageSize: 20 }}
                  size="small"
                />
              </Card>
            ),
          },
          {
            key: 'txs',
            label: `交易紀錄${filterUserId ? `（過濾 userId=${filterUserId}）` : ''}`,
            children: (
              <Card
                size="small"
                extra={
                  <Space>
                    {filterUserId && (
                      <Button size="small" onClick={() => { setFilterUserId(undefined); loadTxs() }}>
                        清除過濾
                      </Button>
                    )}
                    <Button size="small" onClick={() => loadTxs(filterUserId)}>重新整理</Button>
                  </Space>
                }
              >
                <Table
                  rowKey="id"
                  columns={txColumns}
                  dataSource={txs}
                  loading={loadingTxs}
                  pagination={{ pageSize: 20 }}
                  size="small"
                />
              </Card>
            ),
          },
        ]}
      />

      {/* 發金幣 modal */}
      <Modal
        open={!!grantTarget}
        title={grantTarget ? `發金幣 → ${grantTarget.username}` : ''}
        onCancel={() => { setGrantTarget(null); grantForm.resetFields() }}
        onOk={submitGrant}
        okText="送出"
        cancelText="取消"
      >
        <Form form={grantForm} layout="vertical" initialValues={{ amount: 100 }}>
          <Form.Item
            name="amount"
            label="金額（正數加、負數扣）"
            rules={[{ required: true, message: '請輸入金額' },
                    { type: 'number', validator: (_, v) => v !== 0 ? Promise.resolve() : Promise.reject('不可為 0') }]}
          >
            <InputNumber style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="note" label="備註（選填）">
            <Input.TextArea rows={2} placeholder="例：活動補償、測試用" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
