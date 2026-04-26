import { useNavigate } from 'react-router-dom'
import { Button, Card, Descriptions, Tag, Typography, Space } from 'antd'
import { useAuthStore } from '../store/authStore'

const { Title } = Typography

export default function HomePage() {
  const { user, logout } = useAuthStore()
  const navigate = useNavigate()

  if (!user) return null

  const isAdmin = user.roles.includes('ADMIN')

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div style={{ padding: 32, maxWidth: 800, margin: '0 auto' }}>
      <Title level={2}>TextCombat</Title>
      <Card
        title="我的資料"
        extra={
          <Space>
            <Button onClick={() => navigate('/inventory')}>背包</Button>
            <Button onClick={() => navigate('/shop')}>商店</Button>
            <Button onClick={() => navigate('/enhance')}>強化</Button>
            {isAdmin && <Button danger onClick={() => navigate('/admin')}>管理員</Button>}
            <Button onClick={handleLogout}>登出</Button>
          </Space>
        }
        style={{ marginBottom: 16 }}
      >
        <Descriptions column={1}>
          <Descriptions.Item label="ID">{user.id}</Descriptions.Item>
          <Descriptions.Item label="帳號">{user.username}</Descriptions.Item>
          <Descriptions.Item label="顯示名稱">{user.displayName ?? '(未設定)'}</Descriptions.Item>
          <Descriptions.Item label="金幣">
            <Tag color="gold">💰 {user.gold?.toLocaleString() ?? 0}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="角色">
            <Space>
              {user.roles.map(r => <Tag color="blue" key={r}>{r}</Tag>)}
            </Space>
          </Descriptions.Item>
          {isAdmin && (
            <Descriptions.Item label="權限">
              <Space wrap>
                {user.permissions.map(p => <Tag color="green" key={p}>{p}</Tag>)}
              </Space>
            </Descriptions.Item>
          )}
        </Descriptions>
      </Card>
    </div>
  )
}
