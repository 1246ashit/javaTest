import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import HomePage from './pages/HomePage'
import InventoryPage from './pages/InventoryPage'
import ShopPage from './pages/ShopPage'
import EnhancePage from './pages/EnhancePage'
import AdminPage from './pages/AdminPage'
import RequireAuth from './components/RequireAuth'
import 'antd/dist/reset.css'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/" element={
          <RequireAuth>
            <HomePage />
          </RequireAuth>
        } />
        <Route path="/inventory" element={
          <RequireAuth>
            <InventoryPage />
          </RequireAuth>
        } />
        <Route path="/shop" element={
          <RequireAuth>
            <ShopPage />
          </RequireAuth>
        } />
        <Route path="/enhance" element={
          <RequireAuth>
            <EnhancePage />
          </RequireAuth>
        } />
        <Route path="/admin" element={
          <RequireAuth>
            <AdminPage />
          </RequireAuth>
        } />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
