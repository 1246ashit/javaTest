import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Button, Card, Col, Descriptions, Empty, InputNumber, Modal, Row, Space, Tag, Typography, message,
} from 'antd'
import { shopApi, type ShopItem } from '../api/shop'
import { goldApi } from '../api/gold'
import { useAuthStore } from '../store/authStore'
import type { EquipmentSlot, ItemType } from '../api/inventory'

const { Title, Text } = Typography

const SLOT_LABEL: Record<EquipmentSlot, string> = {
  WEAPON: '武器', ARMOR: '防具', HELMET: '頭盔', BOOTS: '靴子', ACCESSORY: '飾品',
}

function iconOf(item: ShopItem): string {
  if (item.type === 'CONSUMABLE') return '🧪'
  if (item.type === 'MATERIAL') return '📦'
  switch (item.equipmentSlot) {
    case 'WEAPON': return '⚔️'
    case 'ARMOR': return '🛡️'
    case 'HELMET': return '🪖'
    case 'BOOTS': return '👢'
    case 'ACCESSORY': return '💍'
    default: return '❓'
  }
}

function typeLabel(t: ItemType): string {
  return t === 'CONSUMABLE' ? '消耗品' : t === 'EQUIPMENT' ? '裝備' : '素材'
}

export default function ShopPage() {
  const navigate = useNavigate()
  const gold = useAuthStore(s => s.user?.gold ?? 0)
  const setGold = useAuthStore(s => s.setGold)
  const [items, setItems] = useState<ShopItem[]>([])
  const [loading, setLoading] = useState(false)
  const [selectedId, setSelectedId] = useState<number | null>(null)

  const selected = items.find(i => i.itemId === selectedId) ?? null

  const load = async () => {
    setLoading(true)
    try {
      const [list, g] = await Promise.all([shopApi.list(), goldApi.get()])
      setItems(list)
      setGold(g.gold)
    } catch (e: any) {
      message.error(e?.response?.data?.error ?? '載入商店失敗')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const handleBuy = () => {
    if (!selected) return

    const isEquipment = selected.type === 'EQUIPMENT'
    let qty = 1

    Modal.confirm({
      title: `購買 ${selected.itemName}`,
      content: (
        <div>
          <p>單價：{selected.price} 金幣</p>
          {!isEquipment && (
            <InputNumber
              min={1}
              max={99}
              defaultValue={1}
              onChange={v => { qty = typeof v === 'number' ? v : 1 }}
              addonBefore="數量"
              style={{ width: 200 }}
            />
          )}
          {isEquipment && <p>裝備類一次只能購買 1 件。</p>}
        </div>
      ),
      okText: '購買', cancelText: '取消',
      onOk: async () => {
        try {
          const res = await shopApi.buy(selected.itemId, qty)
          setGold(res.goldBalance)
          message.success(`購買成功：${selected.itemName} ×${res.bought}`)
        } catch (e: any) {
          message.error(e?.response?.data?.error ?? '購買失敗')
        }
      },
    })
  }

  const canAfford = selected ? gold >= selected.price : false

  return (
    <div style={{ padding: 24, maxWidth: 1100, margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: 16 }}>
        <Space>
          <Button onClick={() => navigate('/')}>← 返回</Button>
          <Title level={3} style={{ margin: 0 }}>商店</Title>
        </Space>
        <div style={{ flex: 1 }} />
        <Space>
          <Button onClick={() => navigate('/inventory')}>前往背包</Button>
          <Tag color="gold" style={{ fontSize: 16, padding: '4px 12px' }}>
            💰 {gold.toLocaleString()}
          </Tag>
        </Space>
      </div>

      <Row gutter={16}>
        {/* 商品清單 */}
        <Col span={14}>
          <Card title={`商品（${items.length}）`} size="small" loading={loading && items.length === 0}>
            {!loading && items.length === 0 ? (
              <Empty description="商店沒有商品" />
            ) : (
              <Row gutter={[8, 8]}>
                {items.map(it => {
                  const isSelected = it.itemId === selectedId
                  return (
                    <Col key={it.itemId} span={4}>
                      <div
                        onClick={() => setSelectedId(it.itemId)}
                        style={{
                          position: 'relative',
                          border: `2px solid ${isSelected ? '#1677ff' : '#d9d9d9'}`,
                          borderRadius: 6,
                          padding: 6,
                          textAlign: 'center',
                          cursor: 'pointer',
                          background: '#fff',
                          userSelect: 'none',
                        }}
                      >
                        <div style={{ fontSize: 28, lineHeight: 1.2 }}>{iconOf(it)}</div>
                        <div style={{
                          fontSize: 11,
                          whiteSpace: 'nowrap',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                        }}>
                          {it.itemName}
                        </div>
                        <div style={{ fontSize: 11, color: '#faad14', fontWeight: 600 }}>
                          💰 {it.price}
                        </div>
                      </div>
                    </Col>
                  )
                })}
              </Row>
            )}
          </Card>
        </Col>

        {/* 詳情 + 購買 */}
        <Col span={10}>
          <Card title="商品詳細" size="small">
            {!selected ? (
              <Empty description="點擊左側商品查看詳情" />
            ) : (
              <>
                <div style={{ textAlign: 'center', marginBottom: 12 }}>
                  <div style={{ fontSize: 56, lineHeight: 1 }}>{iconOf(selected)}</div>
                  <Title level={4} style={{ margin: '8px 0 4px' }}>{selected.itemName}</Title>
                  <Space>
                    <Tag color="blue">{typeLabel(selected.type)}</Tag>
                    {selected.equipmentSlot && <Tag>{SLOT_LABEL[selected.equipmentSlot]}</Tag>}
                    <Tag color="gold">💰 {selected.price}</Tag>
                  </Space>
                </div>

                <Descriptions column={1} size="small" bordered>
                  <Descriptions.Item label="物品代碼">{selected.itemCode}</Descriptions.Item>
                  {selected.type === 'EQUIPMENT' && (
                    <>
                      <Descriptions.Item label="攻擊">{selected.baseAttack}</Descriptions.Item>
                      <Descriptions.Item label="防禦">{selected.baseDefense}</Descriptions.Item>
                    </>
                  )}
                  <Descriptions.Item label="描述">
                    {selected.description ?? '—'}
                  </Descriptions.Item>
                </Descriptions>

                <Space style={{ marginTop: 16, width: '100%', justifyContent: 'center' }} direction="vertical" align="center">
                  <Button type="primary" size="large" onClick={handleBuy} disabled={!canAfford}>
                    購買
                  </Button>
                  {!canAfford && <Text type="danger">金幣不足</Text>}
                </Space>
              </>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}
