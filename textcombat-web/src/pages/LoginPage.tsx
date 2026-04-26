import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Form, Input, Button, Card, message, Typography } from 'antd'
import { authApi } from '../api/auth'
import { useAuthStore } from '../store/authStore'

const { Title } = Typography

export default function LoginPage() {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true)
    try {
      const { token, user } = await authApi.login(values.username, values.password)
      setAuth(token, user)
      message.success(`歡迎回來, ${user.displayName ?? user.username}`)
      navigate('/')
    } catch (e: any) {
      const msg = e?.response?.data?.error ?? '登入失敗'
      message.error(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
      <Card style={{ width: 380 }}>
        <Title level={3} style={{ textAlign: 'center' }}>異世界打怪遊戲 — 登入</Title>
        <Form layout="vertical" onFinish={onFinish}>
          <Form.Item label="帳號" name="username" rules={[{ required: true, message: '請輸入帳號' }]}>
            <Input autoFocus />
          </Form.Item>
          <Form.Item label="密碼" name="password" rules={[{ required: true, message: '請輸入密碼' }]}>
            <Input.Password />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              登入
            </Button>
          </Form.Item>
          <div style={{ textAlign: 'center' }}>
            還沒帳號？ <Link to="/register">註冊</Link>
          </div>
        </Form>
      </Card>
    </div>
  )
}
