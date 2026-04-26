import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Alert, Button, Card, Col, Descriptions, Empty, Modal, Progress, Row, Space, Statistic, Tag, Typography, message,
} from 'antd'
import { inventoryApi, type InventoryItem, type InventoryResponse } from '../api/inventory'
import { enhanceApi, type EnhancePreview, type EnhanceFailEffect } from '../api/enhance'
import { goldApi } from '../api/gold'
import { useAuthStore } from '../store/authStore'

const { Title, Text } = Typography

function iconOf(it: InventoryItem): string {
  switch (it.equipmentSlot) {
    case 'WEAPON': return '⚔️'
    case 'ARMOR': return '🛡️'
    case 'HELMET': return '🪖'
    case 'BOOTS': return '👢'
    case 'ACCESSORY': return '💍'
    default: return '❓'
  }
}

function failEffectText(eff: EnhanceFailEffect): string {
  switch (eff) {
    case 'NOTHING': return '失敗時不影響等級'
    case 'DOWNGRADE': return '失敗時等級 -1'
    case 'DESTROY': return '失敗時武器將消失'
  }
}

function failEffectColor(eff: EnhanceFailEffect): string {
  switch (eff) {
    case 'NOTHING': return 'green'
    case 'DOWNGRADE': return 'orange'
    case 'DESTROY': return 'red'
  }
}

export default function EnhancePage() {
  const navigate = useNavigate()
  const gold = useAuthStore(s => s.user?.gold ?? 0)
  const setGold = useAuthStore(s => s.setGold)

  const [inv, setInv] = useState<InventoryResponse | null>(null)
  const [loading, setLoading] = useState(false)

  const [selectedInvId, setSelectedInvId] = useState<number | null>(null)
  const [preview, setPreview] = useState<EnhancePreview | null>(null)
  const [previewLoading, setPreviewLoading] = useState(false)

  const [enhancing, setEnhancing] = useState(false)

  // 只列裝備類（消耗品/素材不能強化）
  const equipments = (inv?.items ?? []).filter(i => i.type === 'EQUIPMENT')
  const selected = equipments.find(i => i.inventoryItemId === selectedInvId) ?? null

  const loadInventory = async () => {
    setLoading(true)
    try {
      const [data, g] = await Promise.all([inventoryApi.list(), goldApi.get()])
      setInv(data)
      setGold(g.gold)
    } catch (e: any) {
      message.error(e?.response?.data?.error ?? '載入背包失敗')
    } finally {
      setLoading(false)
    }
  }

  const loadPreview = async (invId: number) => {
    setPreviewLoading(true)
    try {
      const p = await enhanceApi.preview(invId)
      setPreview(p)
    } catch (e: any) {
      setPreview(null)
      message.error(e?.response?.data?.error ?? '無法取得強化資訊')
    } finally {
      setPreviewLoading(false)
    }
  }

  useEffect(() => { loadInventory() }, [])

  useEffect(() => {
    if (selectedInvId == null) { setPreview(null); return }
    loadPreview(selectedInvId)
  }, [selectedInvId])

  const handleEnhance = () => {
    if (!preview || !selected) return

    const dangerous = preview.onFail === 'DESTROY'
    Modal.confirm({
      title: `強化 ${selected.itemName} +${preview.currentLevel} → +${preview.currentLevel + 1}`,
      content: (
        <div>
          <p>成功率：<b>{Math.round(preview.successRate * 100)}%</b></p>
          <p>消耗：💰 {preview.goldCost.toLocaleString()}
            {preview.materialCosts.length > 0 && (
              <> + {preview.materialCosts.map(m => `${m.itemName} ×${m.quantityNeeded}`).join('、')}</>
            )}
          </p>
          <Tag color={failEffectColor(preview.onFail)}>{failEffectText(preview.onFail)}</Tag>
          {dangerous && <p style={{ color: '#ff4d4f', marginTop: 8 }}>⚠️ 失敗武器會消失，請謹慎操作</p>}
        </div>
      ),
      okText: '強化',
      okButtonProps: { danger: dangerous },
      cancelText: '取消',
      onOk: async () => {
        setEnhancing(true)
        try {
          const r = await enhanceApi.enhance(selected.inventoryItemId)
          setInv(r.inventory)
          setGold(r.goldBalance)

          if (r.success) {
            message.success(r.message || `強化成功！+${r.previousLevel} → +${r.newLevel}`)
          } else {
            message.warning(r.message || '強化失敗')
          }

          // 若武器被銷毀就清空選擇；否則重抓 preview
          if (r.destroyed) {
            setSelectedInvId(null)
          } else {
            loadPreview(selected.inventoryItemId)
          }
        } catch (e: any) {
          message.error(e?.response?.data?.error ?? '強化失敗')
        } finally {
          setEnhancing(false)
        }
      },
    })
  }

  return (
    <div style={{ padding: 24, maxWidth: 1100, margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: 16 }}>
        <Space>
          <Button onClick={() => navigate('/')}>← 返回</Button>
          <Title level={3} style={{ margin: 0 }}>強化</Title>
        </Space>
        <div style={{ flex: 1 }} />
        <Space>
          <Button onClick={() => navigate('/inventory')}>背包</Button>
          <Tag color="gold" style={{ fontSize: 16, padding: '4px 12px' }}>
            💰 {gold.toLocaleString()}
          </Tag>
        </Space>
      </div>

      <Row gutter={16}>
        {/* 左側：可強化裝備列表 */}
        <Col span={10}>
          <Card title={`可強化裝備（${equipments.length}）`} size="small" loading={loading && !inv}>
            {equipments.length === 0 ? (
              <Empty description="背包裡沒有裝備" />
            ) : (
              <Row gutter={[8, 8]}>
                {equipments.map(it => {
                  const isSelected = selectedInvId === it.inventoryItemId
                  return (
                    <Col key={it.inventoryItemId} span={6}>
                      <div
                        onClick={() => setSelectedInvId(it.inventoryItemId)}
                        style={{
                          position: 'relative',
                          border: `2px solid ${isSelected ? '#1677ff' : '#d9d9d9'}`,
                          borderRadius: 6,
                          padding: 8,
                          textAlign: 'center',
                          cursor: 'pointer',
                          background: isSelected ? '#e6f4ff' : '#fff',
                          userSelect: 'none',
                        }}
                      >
                        <div style={{ fontSize: 28, lineHeight: 1.2 }}>{iconOf(it)}</div>
                        <div style={{
                          fontSize: 11,
                          whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
                        }}>
                          {it.itemName}
                        </div>
                        {it.enhancementLevel > 0 && (
                          <div style={{
                            position: 'absolute', left: 2, top: 2,
                            background: '#faad14', color: '#fff',
                            fontSize: 10, padding: '0 4px', borderRadius: 4,
                          }}>+{it.enhancementLevel}</div>
                        )}
                        {it.equipped && (
                          <div style={{
                            position: 'absolute', right: 2, top: 2,
                            background: '#52c41a', color: '#fff',
                            fontSize: 10, padding: '0 4px', borderRadius: 4,
                          }}>已裝</div>
                        )}
                      </div>
                    </Col>
                  )
                })}
              </Row>
            )}
          </Card>
        </Col>

        {/* 右側：強化面板 */}
        <Col span={14}>
          <Card title="強化面板" size="small" loading={previewLoading}>
            {!selected ? (
              <Empty description="請從左側選擇要強化的裝備" />
            ) : !preview ? (
              <Empty description="無強化資訊" />
            ) : (
              <>
                <div style={{ textAlign: 'center', marginBottom: 16 }}>
                  <div style={{ fontSize: 56, lineHeight: 1 }}>{iconOf(selected)}</div>
                  <Title level={4} style={{ margin: '8px 0 4px' }}>
                    {selected.itemName}
                    <Text type="warning"> +{preview.currentLevel}</Text>
                    {' → '}
                    <Text type="success">+{preview.currentLevel + 1}</Text>
                  </Title>
                  <Text type="secondary">
                    上限 +{preview.maxLevel}
                  </Text>
                </div>

                {/* 屬性變化 */}
                <Row gutter={16} style={{ marginBottom: 16 }}>
                  <Col span={12}>
                    <Card size="small">
                      <Statistic
                        title="攻擊"
                        value={preview.currentAttack}
                        suffix={
                          <Text style={{ color: preview.nextAttack > preview.currentAttack ? '#52c41a' : undefined }}>
                            {' → '}{preview.nextAttack}
                            {preview.nextAttack > preview.currentAttack && (
                              <Text type="success"> (+{preview.nextAttack - preview.currentAttack})</Text>
                            )}
                          </Text>
                        }
                        prefix="⚔️"
                      />
                    </Card>
                  </Col>
                  <Col span={12}>
                    <Card size="small">
                      <Statistic
                        title="防禦"
                        value={preview.currentDefense}
                        suffix={
                          <Text style={{ color: preview.nextDefense > preview.currentDefense ? '#52c41a' : undefined }}>
                            {' → '}{preview.nextDefense}
                            {preview.nextDefense > preview.currentDefense && (
                              <Text type="success"> (+{preview.nextDefense - preview.currentDefense})</Text>
                            )}
                          </Text>
                        }
                        prefix="🛡️"
                      />
                    </Card>
                  </Col>
                </Row>

                {/* 成功率 */}
                <div style={{ marginBottom: 16 }}>
                  <Text>成功率</Text>
                  <Progress
                    percent={Math.round(preview.successRate * 100)}
                    status={preview.successRate >= 0.7 ? 'success' : preview.successRate >= 0.3 ? 'normal' : 'exception'}
                  />
                </div>

                {/* 成本 */}
                <Descriptions
                  column={1}
                  size="small"
                  bordered
                  style={{ marginBottom: 16 }}
                  items={[
                    {
                      label: '💰 金幣',
                      children: (
                        <Text type={preview.goldOwned >= preview.goldCost ? undefined : 'danger'}>
                          {preview.goldCost.toLocaleString()}
                          <Text type="secondary"> （持有 {preview.goldOwned.toLocaleString()}）</Text>
                        </Text>
                      ),
                    },
                    ...preview.materialCosts.map(m => ({
                      label: `📦 ${m.itemName}`,
                      children: (
                        <Text type={m.quantityOwned >= m.quantityNeeded ? undefined : 'danger'}>
                          ×{m.quantityNeeded}
                          <Text type="secondary"> （持有 {m.quantityOwned}）</Text>
                        </Text>
                      ),
                    })),
                    {
                      label: '失敗效果',
                      children: <Tag color={failEffectColor(preview.onFail)}>{failEffectText(preview.onFail)}</Tag>,
                    },
                  ]}
                />

                {/* 不能強化原因 */}
                {!preview.canEnhance && preview.blockReason && (
                  <Alert
                    type="warning"
                    showIcon
                    message={preview.blockReason}
                    style={{ marginBottom: 16 }}
                  />
                )}

                <Button
                  type="primary"
                  block
                  size="large"
                  disabled={!preview.canEnhance}
                  loading={enhancing}
                  onClick={handleEnhance}
                  danger={preview.onFail === 'DESTROY'}
                >
                  強化
                </Button>
              </>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}
