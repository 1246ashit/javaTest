import { useEffect, useRef, useState, type DragEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Button, Card, Col, Descriptions, Empty, InputNumber, Modal, Row, Space, Statistic, Tag, Typography, message,
} from 'antd'
import {
  inventoryApi,
  type InventoryItem,
  type InventoryResponse,
  type ItemType,
} from '../api/inventory'
import { goldApi } from '../api/gold'
import { useAuthStore } from '../store/authStore'

type DragSource =
  | { kind: 'inventory'; inventoryItemId: number; itemName: string }
  | { kind: 'slot'; inventoryItemId: number; itemName: string; fromSlot: number }

const { Title, Text } = Typography

const SLOT_INDEXES = [1, 2, 3, 4, 5, 6, 7, 8, 9] as const

function iconOf(item: InventoryItem | null): string {
  if (!item) return ''
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

export default function InventoryPage() {
  const navigate = useNavigate()
  const gold = useAuthStore(s => s.user?.gold ?? 0)
  const setGold = useAuthStore(s => s.setGold)

  const [data, setData] = useState<InventoryResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [selectedInvId, setSelectedInvId] = useState<number | null>(null)
  const [selectedSlot, setSelectedSlot] = useState<number | null>(null)

  // 拖曳：用 ref 而不是 state，避免在 drag 事件中觸發 re-render 把節點重建
  const dragSourceRef = useRef<DragSource | null>(null)
  const [dragOverSlot, setDragOverSlot] = useState<number | null>(null)
  const [dragOverBag, setDragOverBag] = useState(false)

  // 選中的可能是背包裡的物品，也可能是某一格上的物品
  const selectedFromItems = data?.items.find(i => i.inventoryItemId === selectedInvId) ?? null
  const selectedFromSlot = selectedSlot != null ? (data?.slots[String(selectedSlot)] ?? null) : null
  const selected = selectedFromSlot ?? selectedFromItems

  const load = async () => {
    setLoading(true)
    try {
      const [inv, g] = await Promise.all([inventoryApi.list(), goldApi.get()])
      setData(inv)
      setGold(g.gold)
    } catch (e: any) {
      message.error(e?.response?.data?.error ?? '載入背包失敗')
    } finally {
      setLoading(false)
    }
  }
  useEffect(() => { load() }, [])

  const after = (next: InventoryResponse) => {
    setData(next)
    // 如果選中的東西不見了就清掉
    if (selectedInvId && !next.items.find(i => i.inventoryItemId === selectedInvId)) {
      setSelectedInvId(null)
    }
  }

  // 點背包裡的物品
  const handleClickInvItem = (it: InventoryItem) => {
    setSelectedInvId(it.inventoryItemId)
    setSelectedSlot(null)
  }

  // 點裝備欄某格
  const handleClickSlot = (slotIndex: number) => {
    setSelectedSlot(slotIndex)
    setSelectedInvId(null)
  }

  const handleEquipAuto = async () => {
    if (!selected) return
    try {
      after(await inventoryApi.equip(selected.inventoryItemId))
      message.success(`已裝備：${selected.itemName}`)
    } catch (e: any) {
      message.error(e?.response?.data?.error ?? '裝備失敗')
    }
  }

  const handleUnequip = async (slotIndex: number) => {
    try {
      after(await inventoryApi.unequip(slotIndex))
      message.success(`已卸下第 ${slotIndex} 格`)
    } catch (e: any) {
      message.error(e?.response?.data?.error ?? '卸下失敗')
    }
  }

  const handleUse = async (slotIndex: number) => {
    try {
      const res = await inventoryApi.use(slotIndex)
      after(res.inventory)
      message.success(res.message)
    } catch (e: any) {
      message.error(e?.response?.data?.error ?? '使用失敗')
    }
  }

  const handleDiscard = () => {
    if (!selected || selected.equipped) return
    let qty = 1
    Modal.confirm({
      title: `丟棄 ${selected.itemName}`,
      content: (
        <div>
          <p>此動作無法復原。</p>
          <InputNumber
            min={1}
            max={selected.quantity}
            defaultValue={1}
            onChange={v => { qty = typeof v === 'number' ? v : 1 }}
            addonBefore="數量"
            style={{ width: 200 }}
          />
        </div>
      ),
      okText: '丟棄', okButtonProps: { danger: true }, cancelText: '取消',
      onOk: async () => {
        try {
          after(await inventoryApi.discard(selected.inventoryItemId, qty))
          message.success('已丟棄')
        } catch (e: any) {
          message.error(e?.response?.data?.error ?? '丟棄失敗')
        }
      },
    })
  }

  // ========= 拖曳處理 =========
  const onDragStartInv = (e: DragEvent<HTMLDivElement>, it: InventoryItem) => {
    dragSourceRef.current = {
      kind: 'inventory',
      inventoryItemId: it.inventoryItemId,
      itemName: it.itemName,
    }
    e.dataTransfer.effectAllowed = 'move'
    // 有些瀏覽器需要 setData 才會觸發 dragstart
    e.dataTransfer.setData('text/plain', String(it.inventoryItemId))
  }

  const onDragStartSlot = (e: DragEvent<HTMLDivElement>, slotIndex: number, it: InventoryItem) => {
    dragSourceRef.current = {
      kind: 'slot',
      inventoryItemId: it.inventoryItemId,
      itemName: it.itemName,
      fromSlot: slotIndex,
    }
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', String(it.inventoryItemId))
  }

  const onDragEnd = () => {
    dragSourceRef.current = null
    setDragOverSlot(null)
    setDragOverBag(false)
  }

  const onDragOverSlot = (e: DragEvent<HTMLDivElement>, slotIndex: number) => {
    if (!dragSourceRef.current) return
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
    if (dragOverSlot !== slotIndex) setDragOverSlot(slotIndex)
  }

  const onDragLeaveSlot = (slotIndex: number) => {
    if (dragOverSlot === slotIndex) setDragOverSlot(null)
  }

  const onDropToSlot = async (e: DragEvent<HTMLDivElement>, slotIndex: number) => {
    e.preventDefault()
    const src = dragSourceRef.current
    onDragEnd()
    if (!src) return
    // 拖回原本格 → 不動作
    if (src.kind === 'slot' && src.fromSlot === slotIndex) return
    try {
      after(await inventoryApi.equip(src.inventoryItemId, slotIndex))
      message.success(
        src.kind === 'slot'
          ? `${src.itemName} 移到第 ${slotIndex} 格`
          : `已裝備 ${src.itemName} 到第 ${slotIndex} 格`,
      )
    } catch (err: any) {
      message.error(err?.response?.data?.error ?? '裝備失敗')
    }
  }

  // 從裝備格拖到背包區 → 卸下
  const onDragOverBag = (e: DragEvent<HTMLDivElement>) => {
    const src = dragSourceRef.current
    if (!src || src.kind !== 'slot') return
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
    if (!dragOverBag) setDragOverBag(true)
  }

  const onDragLeaveBag = () => {
    if (dragOverBag) setDragOverBag(false)
  }

  const onDropToBag = async (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault()
    const src = dragSourceRef.current
    onDragEnd()
    if (!src || src.kind !== 'slot') return
    try {
      after(await inventoryApi.unequip(src.fromSlot))
      message.success(`已卸下 ${src.itemName}`)
    } catch (err: any) {
      message.error(err?.response?.data?.error ?? '卸下失敗')
    }
  }

  return (
    <div style={{ padding: 24, maxWidth: 1100, margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: 16 }}>
        <Space>
          <Button onClick={() => navigate('/')}>← 返回</Button>
          <Title level={3} style={{ margin: 0 }}>背包</Title>
        </Space>
        <div style={{ flex: 1 }} />
        <Space>
          <Button onClick={() => navigate('/shop')}>前往商店</Button>
          <Tag color="gold" style={{ fontSize: 16, padding: '4px 12px' }}>
            💰 {gold.toLocaleString()}
          </Tag>
        </Space>
      </div>

      {/* 裝備欄（9 格）+ 總屬性 */}
      <Card
        title="裝備欄（9 格，可放任意物品；可拖曳裝備）"
        size="small"
        style={{ marginBottom: 16 }}
        loading={loading && !data}
      >
        <Row gutter={16}>
          <Col span={16}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8, maxWidth: 520 }}>
              {SLOT_INDEXES.map(i => {
                const it = data?.slots[String(i)] ?? null
                const isSelected = selectedSlot === i
                const isDragOver = dragOverSlot === i
                const borderColor = isDragOver
                  ? '#52c41a'
                  : isSelected ? '#1677ff' : it ? '#faad14' : '#d9d9d9'
                return (
                  <div
                    key={i}
                    onClick={() => handleClickSlot(i)}
                    draggable={!!it}
                    onDragStart={it ? e => onDragStartSlot(e, i, it) : undefined}
                    onDragEnd={onDragEnd}
                    onDragOver={e => onDragOverSlot(e, i)}
                    onDragLeave={() => onDragLeaveSlot(i)}
                    onDrop={e => onDropToSlot(e, i)}
                    style={{
                      position: 'relative',
                      border: `2px ${isDragOver ? 'dashed' : 'solid'} ${borderColor}`,
                      borderRadius: 8,
                      minHeight: 110,
                      padding: 8,
                      textAlign: 'center',
                      background: isDragOver ? '#f6ffed' : it ? '#fffbe6' : '#fafafa',
                      cursor: it ? 'grab' : 'pointer',
                      userSelect: 'none',
                      transition: 'background 0.15s, border-color 0.15s',
                    }}
                  >
                    <div style={{ position: 'absolute', top: 4, left: 6, fontSize: 10, color: '#888' }}>#{i}</div>
                    <div style={{ fontSize: 32, lineHeight: 1.4, marginTop: 4 }}>{iconOf(it)}</div>
                    <div style={{ fontSize: 12, minHeight: 18 }}>
                      {it ? (
                        <>
                          {it.itemName}
                          {it.enhancementLevel > 0 && <Text type="warning"> +{it.enhancementLevel}</Text>}
                        </>
                      ) : '—'}
                    </div>
                    {it && it.type === 'CONSUMABLE' && it.quantity > 1 && (
                      <div style={{
                        position: 'absolute', right: 4, bottom: 4,
                        background: 'rgba(0,0,0,0.7)', color: '#fff',
                        fontSize: 11, padding: '0 4px', borderRadius: 4,
                      }}>
                        ×{it.quantity}
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          </Col>
          <Col span={8}>
            <Card size="small" title="屬性總和">
              <Statistic title="攻擊" value={data?.totalAttack ?? 0} prefix="⚔️" />
              <Statistic title="防禦" value={data?.totalDefense ?? 0} prefix="🛡️" style={{ marginTop: 12 }} />
              <Text type="secondary" style={{ fontSize: 12 }}>
                （只計入 9 格中的裝備；消耗品不加成）
              </Text>
            </Card>
          </Col>
        </Row>
      </Card>

      <Row gutter={16}>
        {/* 背包列表 */}
        <Col span={14}>
          <Card
            title={`物品（${data?.items.length ?? 0}）`}
            size="small"
            loading={loading && !data}
            styles={{
              body: {
                outline: dragOverBag ? '2px dashed #ff7875' : 'none',
                outlineOffset: -4,
                background: dragOverBag ? '#fff1f0' : undefined,
                borderRadius: 8,
                transition: 'background 0.15s',
              },
            }}
          >
            <div
              onDragOver={onDragOverBag}
              onDragLeave={onDragLeaveBag}
              onDrop={onDropToBag}
            >
            {dragOverBag && (
              <div style={{ textAlign: 'center', color: '#ff4d4f', marginBottom: 8, fontSize: 13 }}>
                放開以卸下
              </div>
            )}
            {data && data.items.length === 0 ? (
              <Empty description="背包是空的（可從裝備格拖物品到此卸下）" />
            ) : (
              <Row gutter={[8, 8]}>
                {data?.items.map(it => {
                  const isSelected = selectedInvId === it.inventoryItemId
                  return (
                    <Col key={it.inventoryItemId} span={4}>
                      <div
                        onClick={() => handleClickInvItem(it)}
                        draggable={!it.equipped}
                        onDragStart={!it.equipped ? e => onDragStartInv(e, it) : undefined}
                        onDragEnd={onDragEnd}
                        style={{
                          position: 'relative',
                          border: `2px solid ${isSelected ? '#1677ff' : it.equipped ? '#faad14' : '#d9d9d9'}`,
                          borderRadius: 6,
                          padding: 6,
                          textAlign: 'center',
                          cursor: it.equipped ? 'pointer' : 'grab',
                          background: it.equipped ? '#fffbe6' : '#fff',
                          userSelect: 'none',
                          opacity: it.equipped ? 0.85 : 1,
                        }}
                      >
                        <div style={{ fontSize: 28, lineHeight: 1.2 }}>{iconOf(it)}</div>
                        <div style={{
                          fontSize: 11,
                          whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
                        }}>
                          {it.itemName}
                        </div>
                        {it.quantity > 1 && (
                          <div style={{
                            position: 'absolute', right: 2, bottom: 2,
                            background: 'rgba(0,0,0,0.7)', color: '#fff',
                            fontSize: 11, padding: '0 4px', borderRadius: 4,
                          }}>×{it.quantity}</div>
                        )}
                        {it.enhancementLevel > 0 && (
                          <div style={{
                            position: 'absolute', left: 2, top: 2,
                            background: '#faad14', color: '#fff',
                            fontSize: 10, padding: '0 4px', borderRadius: 4,
                          }}>+{it.enhancementLevel}</div>
                        )}
                        {it.equipped && (
                          <div style={{
                            position: 'absolute', left: 2, bottom: 2,
                            background: '#52c41a', color: '#fff',
                            fontSize: 10, padding: '0 4px', borderRadius: 4,
                          }}>#{it.equippedSlotIndex}</div>
                        )}
                      </div>
                    </Col>
                  )
                })}
              </Row>
            )}
            </div>
          </Card>
        </Col>

        {/* 詳情 + 操作 */}
        <Col span={10}>
          <Card title={selectedSlot ? `第 ${selectedSlot} 格詳情` : '物品詳細'} size="small">
            {!selected ? (
              <Empty description="點擊物品或裝備欄格子查看詳情" />
            ) : (
              <>
                <div style={{ textAlign: 'center', marginBottom: 12 }}>
                  <div style={{ fontSize: 56, lineHeight: 1 }}>{iconOf(selected)}</div>
                  <Title level={4} style={{ margin: '8px 0 4px' }}>
                    {selected.itemName}
                    {selected.enhancementLevel > 0 && <Text type="warning"> +{selected.enhancementLevel}</Text>}
                  </Title>
                  <Space>
                    <Tag color="blue">{typeLabel(selected.type)}</Tag>
                    {selected.equipped && <Tag color="gold">第 {selected.equippedSlotIndex} 格</Tag>}
                  </Space>
                </div>

                <Descriptions column={1} size="small" bordered>
                  <Descriptions.Item label="代碼">{selected.itemCode}</Descriptions.Item>
                  <Descriptions.Item label="數量">{selected.quantity}</Descriptions.Item>
                  {selected.type === 'EQUIPMENT' && (
                    <>
                      <Descriptions.Item label="攻擊">{selected.baseAttack}</Descriptions.Item>
                      <Descriptions.Item label="防禦">{selected.baseDefense}</Descriptions.Item>
                    </>
                  )}
                  <Descriptions.Item label="描述">{selected.description ?? '—'}</Descriptions.Item>
                </Descriptions>

                <Space style={{ marginTop: 16, width: '100%', justifyContent: 'center' }} wrap>
                  {/* 裝備中：卸下 */}
                  {selected.equipped && selected.equippedSlotIndex != null && (
                    <Button onClick={() => handleUnequip(selected.equippedSlotIndex!)}>
                      卸下
                    </Button>
                  )}

                  {/* 裝備中 + 消耗品：使用 */}
                  {selected.equipped && selected.type === 'CONSUMABLE' && selected.equippedSlotIndex != null && (
                    <Button type="primary" onClick={() => handleUse(selected.equippedSlotIndex!)}>
                      使用
                    </Button>
                  )}

                  {/* 未裝備：裝備到空格 / 指定格 */}
                  {!selected.equipped && (
                    <>
                      <Button type="primary" onClick={handleEquipAuto}>裝備（自動）</Button>
                    </>
                  )}

                  <Button danger onClick={handleDiscard} disabled={selected.equipped}>
                    丟棄
                  </Button>
                </Space>
              </>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}
