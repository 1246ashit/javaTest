import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Form, Input, Button, Card, message, Typography } from 'antd'
import { authApi } from '../api/auth'

const { Title } = Typography

export default function RegisterPage() {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const onFinish = async (values: { username: string; password: string; displayName: string }) => {
    setLoading(true)
    try {
      await authApi.register(values.username, values.password, values.displayName)
      message.success('註冊成功，請登入')
      navigate('/login')
    } catch (e: any) {
      const msg = e?.response?.data?.error ?? '註冊失敗'
      message.error(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
      <Card style={{ width: 380 }}>
        <Title level={3} style={{ textAlign: 'center' }}>註冊</Title>
        <Form layout="vertical" onFinish={onFinish}>
          <Form.Item label="帳號" name="username" rules={[
            { required: true, message: '請輸入帳號' },
            { min: 3, max: 32, message: '3-32 字元' },
          ]}>
            <Input autoFocus />
          </Form.Item>
          <Form.Item label="密碼" name="password" rules={[
            { required: true, message: '請輸入密碼' },
            { min: 6, message: '至少 6 字元' },
          ]}>
            <Input.Password />
          </Form.Item>
          <Form.Item label="顯示名稱" name="displayName">
            <Input placeholder="選填" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              註冊
            </Button>
          </Form.Item>
          <div style={{ textAlign: 'center' }}>
            已有帳號？ <Link to="/login">登入</Link>
          </div>
        </Form>
      </Card>
    </div>
  )
}
