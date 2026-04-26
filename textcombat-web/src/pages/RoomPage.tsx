import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  Alert, Avatar, Button, Card, Col, List, Modal, Progress, Row, Space, Tag, Typography, message,
} from 'antd'
import { roomApi, type Room, type RoomMember } from '../api/room'
import { inventoryApi, type InventoryItem } from '../api/inventory'
import { wsClient } from '../api/ws'
import { useAuthStore } from '../store/authStore'

const { Title, Text } = Typography

function hpColor(pct: number): string {
  if (pct > 60) return '#52c41a'
  if (pct > 30) return '#faad14'
  return '#ff4d4f'
}

function nameOf(m: RoomMember): string {
  return m.displayName?.trim() ? m.displayName : m.username
}

export default function RoomPage() {
  const { roomId } = useParams<{ roomId: string }>()
  const navigate = useNavigate()
  const myUserId = useAuthStore(s => s.user?.id)

  const [room, setRoom] = useState<Room | null>(null)
  const [acting, setActing] = useState(false)
  const [potions, setPotions] = useState<InventoryItem[]>([])
  const [loading, setLoading] = useState(true)

  // 載入房間 + 訂閱
  useEffect(() => {
    if (!roomId) return
    let mounted = true

    const load = async () => {
      try {
        const r = await roomApi.get(roomId)
        if (mounted) setRoom(r)
      } catch (e: any) {
        message.error(e?.response?.data?.error ?? '進入房間失敗')
        navigate('/lobby')
      } finally {
        if (mounted) setLoading(false)
      }
    }
    load()

    const sub = wsClient.subscribe<Room>(`/topic/room/${roomId}`, r => {
      if (mounted) setRoom(r)
    })

    return () => {
      mounted = false
      sub.unsubscribe()
    }
  }, [roomId, navigate])

  // 拉藥水清單（喝藥用）
  const refreshPotions = async () => {
    try {
      const inv = await inventoryApi.list()
      setPotions(inv.items.filter(i => i.type === 'CONSUMABLE'))
    } catch { /* ignore */ }
  }
  useEffect(() => { refreshPotions() }, [])

  // 同步勝利後的金幣到 store
  const lastGoldSyncRef = useState<{ outcome?: string }>({ outcome: undefined })[0]
  useEffect(() => {
    if (!room || !myUserId) return
    if (room.outcome === 'VICTORY' && lastGoldSyncRef.outcome !== 'VICTORY') {
      lastGoldSyncRef.outcome = 'VICTORY'
      const me = room.members.find(m => m.userId === myUserId)
      if (me && me.alive && !me.fled && room.bossRewardGold) {
        useAuthStore.setState(prev => prev.user
          ? { user: { ...prev.user, gold: (prev.user.gold ?? 0) + (room.bossRewardGold ?? 0) } }
          : prev)
      }
    }
  }, [room, myUserId])

  const me = useMemo(
    () => room?.members.find(m => m.userId === myUserId) ?? null,
    [room, myUserId]
  )

  const canAct = !!(
    room && me &&
    room.outcome === 'ONGOING' &&
    room.phase === 'PLAYER' &&
    !me.fled &&
    me.alive &&
    !me.actedThisRound
  )

  const handleAttack = async () => {
    if (!roomId) return
    setActing(true)
    try {
      const r = await roomApi.action(roomId, 'ATTACK')
      setRoom(r)
    } catch (e: any) {
      message.error(e?.response?.data?.error ?? '行動失敗')
    } finally {
      setActing(false)
    }
  }

  const handleSkip = async () => {
    if (!roomId) return
    setActing(true)
    try {
      const r = await roomApi.action(roomId, 'SKIP')
      setRoom(r)
    } catch (e: any) {
      message.error(e?.response?.data?.error ?? '跳過失敗')
    } finally {
      setActing(false)
    }
  }

  const handleUsePotion = () => {
    if (!roomId) return
    if (potions.length === 0) {
      message.warning('沒有藥水可以喝')
      return
    }
    Modal.confirm({
      title: '選擇要喝的藥水',
      icon: null,
      width: 480,
      content: (
        <List
          dataSource={potions}
          renderItem={p => (
            <List.Item
              key={p.inventoryItemId}
              actions={[
                <Button
                  type="primary"
                  key="use"
                  onClick={async () => {
                    Modal.destroyAll()
                    setActing(true)
                    try {
                      const r = await roomApi.action(roomId, 'USE_POTION', p.inventoryItemId)
                      setRoom(r)
                      await refreshPotions()
                    } catch (e: any) {
                      message.error(e?.response?.data?.error ?? '使用失敗')
                    } finally {
                      setActing(false)
                    }
                  }}
                >喝</Button>,
              ]}
            >
              <List.Item.Meta
                avatar={<span style={{ fontSize: 24 }}>🧪</span>}
                title={<>{p.itemName} <Text type="secondary">×{p.quantity}</Text></>}
                description={p.description}
              />
            </List.Item>
          )}
        />
      ),
      footer: null,
    })
  }

  const handleLeave = async () => {
    if (!roomId) return
    Modal.confirm({
      title: room?.outcome === 'ONGOING' ? '確定要離開戰鬥？' : '離開房間？',
      content: room?.outcome === 'ONGOING' ? '中途離開不會獲得獎勵。' : undefined,
      okText: '離開',
      cancelText: '取消',
      onOk: async () => {
        try {
          if (room?.outcome === 'ONGOING') {
            await roomApi.leave(roomId)
          }
        } catch (e: any) {
          // ignore
        } finally {
          navigate('/lobby')
        }
      },
    })
  }

  if (loading || !room) {
    return <div style={{ padding: 24 }}><Card loading /></div>
  }

  const bossPct = Math.round((room.bossHp / room.bossMaxHp) * 100)
  const ended = room.outcome !== 'ONGOING'

  return (
    <div style={{ padding: 24, maxWidth: 1100, margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: 16 }}>
        <Space>
          <Button onClick={handleLeave}>← 離開</Button>
          <Title level={4} style={{ margin: 0 }}>{room.name}</Title>
          <Tag color={ended ? 'default' : 'blue'}>
            第 {room.round} 回合 · {room.phase === 'PLAYER' ? '玩家階段' : 'BOSS 階段'}
          </Tag>
        </Space>
      </div>

      {/* BOSS 區 */}
      <Card size="small" style={{ marginBottom: 16 }}>
        <Row gutter={16} align="middle">
          <Col flex="120px" style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 72, lineHeight: 1 }}>{room.bossIcon ?? '👹'}</div>
          </Col>
          <Col flex="auto">
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <Title level={4} style={{ margin: 0 }}>{room.bossName}</Title>
              <div style={{ flex: 1 }} />
              <Space>
                <Tag>⚔️ {room.bossAttack}</Tag>
                <Tag>🛡️ {room.bossDefense}</Tag>
                <Tag color="gold">💰 +{room.bossRewardGold}</Tag>
              </Space>
            </div>
            <Progress
              percent={bossPct}
              strokeColor={hpColor(bossPct)}
              format={() => `${room.bossHp} / ${room.bossMaxHp}`}
            />
          </Col>
        </Row>
      </Card>

      {/* 玩家列 */}
      <Card size="small" title={`隊伍（${room.members.filter(m => !m.fled).length}/4）`} style={{ marginBottom: 16 }}>
        <Row gutter={[12, 12]}>
          {room.members.map(m => {
            const pct = m.maxHp > 0 ? Math.round((m.hp / m.maxHp) * 100) : 0
            const isMe = m.userId === myUserId
            const dead = !m.alive && !m.fled
            const fled = m.fled
            return (
              <Col key={m.userId} xs={24} sm={12} md={6}>
                <Card
                  size="small"
                  style={{
                    opacity: dead || fled ? 0.5 : 1,
                    border: isMe ? '2px solid #1677ff' : undefined,
                  }}
                >
                  <div style={{ textAlign: 'center' }}>
                    <Avatar size={48}>{nameOf(m).charAt(0).toUpperCase()}</Avatar>
                    <div style={{ marginTop: 4, fontWeight: 600 }}>
                      {nameOf(m)} {isMe && <Tag color="blue">你</Tag>}
                    </div>
                    {fled && <Tag color="default">已離開</Tag>}
                    {dead && <Tag color="red">倒下</Tag>}
                    {!fled && !dead && m.actedThisRound && room.phase === 'PLAYER' && (
                      <Tag color="green">已行動</Tag>
                    )}
                  </div>
                  <Progress
                    percent={pct}
                    size="small"
                    strokeColor={hpColor(pct)}
                    format={() => `${m.hp}/${m.maxHp}`}
                    style={{ marginTop: 6 }}
                  />
                  <div style={{ marginTop: 4, textAlign: 'center', fontSize: 11 }}>
                    <Tag>⚔️ {m.attack}</Tag>
                    <Tag>🛡️ {m.defense}</Tag>
                  </div>
                </Card>
              </Col>
            )
          })}
        </Row>
      </Card>

      {/* 行動 / 結算 */}
      <Card size="small" style={{ marginBottom: 16 }}>
        {ended ? (
          <ResultPanel room={room} myId={myUserId} onLeave={() => navigate('/lobby')} />
        ) : me?.fled ? (
          <Alert type="warning" showIcon message="你已離開戰鬥" />
        ) : !me?.alive ? (
          <Alert type="error" showIcon message="你已倒下，等待戰鬥結束" />
        ) : room.phase !== 'PLAYER' ? (
          <Alert type="info" showIcon message="BOSS 階段，等待 BOSS 行動⋯⋯" />
        ) : me.actedThisRound ? (
          <Alert
            type="info"
            showIcon
            message={`你本回合已行動。等待其他人行動完，BOSS 才會行動`}
          />
        ) : (
          <Space wrap>
            <Button type="primary" size="large" loading={acting} disabled={!canAct} onClick={handleAttack}>
              ⚔️ 普攻
            </Button>
            <Button size="large" loading={acting} disabled={!canAct} onClick={handleUsePotion}>
              🧪 喝藥（{potions.length}）
            </Button>
            <Button size="large" loading={acting} disabled={!canAct} onClick={handleSkip}>
              ⏭ 跳過
            </Button>
          </Space>
        )}
      </Card>

      {/* Log */}
      <Card size="small" title="戰鬥紀錄">
        <BattleLog log={room.log} />
      </Card>
    </div>
  )
}

function BattleLog({ log }: { log: string[] }) {
  const reversed = useMemo(() => [...log].reverse(), [log])
  return (
    <div style={{ maxHeight: 240, overflowY: 'auto', fontSize: 13, lineHeight: 1.8 }}>
      {reversed.map((line, i) => (
        <div key={log.length - 1 - i} style={{ borderBottom: '1px dashed #f0f0f0', padding: '2px 0' }}>
          {line}
        </div>
      ))}
    </div>
  )
}

function ResultPanel({ room, myId, onLeave }: { room: Room; myId?: number; onLeave: () => void }) {
  const me = room.members.find(m => m.userId === myId)
  let type: 'success' | 'error' | 'warning' | 'info' = 'info'
  let title = '戰鬥結束'
  let desc = ''

  if (room.outcome === 'VICTORY') {
    type = 'success'
    title = '勝利！'
    if (me && !me.fled && me.alive && room.bossRewardGold) {
      desc = `你獲得金幣 💰 ${room.bossRewardGold}`
    } else {
      desc = '你沒有領到獎勵（已離開或倒下）'
    }
  } else if (room.outcome === 'DEFEAT') {
    type = 'error'
    title = '失敗⋯⋯'
    desc = '所有人都倒下了'
  } else if (room.outcome === 'ABANDONED') {
    type = 'warning'
    title = '戰鬥中止'
    desc = '所有玩家都離開了'
  }

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <Alert type={type} showIcon message={title} description={desc} />
      <Button type="primary" block onClick={onLeave}>回大廳</Button>
    </Space>
  )
}
