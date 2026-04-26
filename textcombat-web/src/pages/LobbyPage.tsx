import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Button, Card, Col, Empty, Form, Input, Modal, Progress, Row, Select, Space, Tag, Typography, message,
} from 'antd'
import { roomApi, type RoomSummary } from '../api/room'
import { bossApi, type Boss } from '../api/boss'
import { wsClient } from '../api/ws'
import { useAuthStore } from '../store/authStore'

const { Title, Text } = Typography

export default function LobbyPage() {
  const navigate = useNavigate()
  const gold = useAuthStore(s => s.user?.gold ?? 0)

  const [rooms, setRooms] = useState<RoomSummary[]>([])
  const [bosses, setBosses] = useState<Boss[]>([])
  const [loading, setLoading] = useState(false)
  const [createOpen, setCreateOpen] = useState(false)
  const [createBossId, setCreateBossId] = useState<number | undefined>()
  const [createName, setCreateName] = useState('')
  const [creating, setCreating] = useState(false)

  const reload = async () => {
    setLoading(true)
    try {
      const [r, b] = await Promise.all([roomApi.list(), bossApi.list()])
      setRooms(r)
      setBosses(b)
    } catch (e: any) {
      message.error(e?.response?.data?.error ?? '載入失敗')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    reload()
    // 訂閱大廳變化
    const sub = wsClient.subscribe<RoomSummary[]>('/topic/lobby', list => {
      setRooms(list)
    })
    return () => sub.unsubscribe()
  }, [])

  const handleCreate = async () => {
    if (!createBossId) {
      message.warning('請選擇 BOSS')
      return
    }
    setCreating(true)
    try {
      const r = await roomApi.create(createBossId, createName.trim() || undefined)
      message.success('開房成功！')
      setCreateOpen(false)
      setCreateName('')
      navigate(`/room/${r.roomId}`)
    } catch (e: any) {
      message.error(e?.response?.data?.error ?? '開房失敗')
    } finally {
      setCreating(false)
    }
  }

  const handleJoin = async (roomId: string) => {
    try {
      await roomApi.join(roomId)
      navigate(`/room/${roomId}`)
    } catch (e: any) {
      message.error(e?.response?.data?.error ?? '加入失敗')
    }
  }

  return (
    <div style={{ padding: 24, maxWidth: 1100, margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: 16 }}>
        <Space>
          <Button onClick={() => navigate('/')}>← 返回</Button>
          <Title level={3} style={{ margin: 0 }}>戰鬥大廳</Title>
        </Space>
        <div style={{ flex: 1 }} />
        <Space>
          <Button type="primary" onClick={() => setCreateOpen(true)}>＋ 開房</Button>
          <Button onClick={reload}>重新整理</Button>
          <Tag color="gold" style={{ fontSize: 16, padding: '4px 12px' }}>
            💰 {gold.toLocaleString()}
          </Tag>
        </Space>
      </div>

      <Card size="small" title={`進行中的房間（${rooms.length}）`} loading={loading && rooms.length === 0}>
        {rooms.length === 0 ? (
          <Empty description="目前沒有公開房間，要不要開一個？">
            <Button type="primary" onClick={() => setCreateOpen(true)}>＋ 開房</Button>
          </Empty>
        ) : (
          <Row gutter={[16, 16]}>
            {rooms.map(r => {
              const pct = r.bossMaxHp > 0 ? Math.round((r.bossHp / r.bossMaxHp) * 100) : 0
              const full = r.memberCount >= r.maxMembers
              return (
                <Col key={r.roomId} xs={24} sm={12}>
                  <Card hoverable size="small">
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                      <div style={{ fontSize: 40 }}>{r.bossIcon ?? '👹'}</div>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontWeight: 600 }}>{r.name}</div>
                        <Text type="secondary" style={{ fontSize: 12 }}>對手：{r.bossName}</Text>
                        <Progress
                          percent={pct}
                          size="small"
                          status={pct > 50 ? 'normal' : 'exception'}
                          format={() => `BOSS ${r.bossHp}/${r.bossMaxHp}`}
                        />
                        <div style={{ marginTop: 4 }}>
                          <Tag color={full ? 'red' : 'blue'}>
                            👥 {r.memberCount} / {r.maxMembers}
                          </Tag>
                        </div>
                      </div>
                      <Button
                        type="primary"
                        disabled={full}
                        onClick={() => handleJoin(r.roomId)}
                      >
                        {full ? '滿了' : '加入'}
                      </Button>
                    </div>
                  </Card>
                </Col>
              )
            })}
          </Row>
        )}
      </Card>

      {/* 開房 Modal */}
      <Modal
        title="開新房間"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={handleCreate}
        confirmLoading={creating}
        okText="開房並進入"
        cancelText="取消"
      >
        <Form layout="vertical">
          <Form.Item label="房名（選填）">
            <Input
              placeholder="留空 → 自動用「{你}的隊伍」"
              value={createName}
              onChange={e => setCreateName(e.target.value)}
              maxLength={30}
            />
          </Form.Item>
          <Form.Item label="挑戰 BOSS" required>
            <Select
              placeholder="選一個對手"
              value={createBossId}
              onChange={setCreateBossId}
              options={bosses.map(b => ({
                value: b.id,
                label: (
                  <span>
                    {b.icon ?? '👹'} {b.name} — HP {b.hp} / ATK {b.attack} / DEF {b.defense}（💰 {b.rewardGold}）
                  </span>
                ),
              }))}
            />
          </Form.Item>
          <Text type="secondary" style={{ fontSize: 12 }}>
            房間最多 4 人，戰鬥中也可以中途加入。
          </Text>
        </Form>
      </Modal>
    </div>
  )
}
