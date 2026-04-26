import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import { useAuthStore } from '../store/authStore'

/**
 * 單例 STOMP client。第一次 subscribe 時自動連線。
 *
 * 用法：
 *   const sub = wsClient.subscribe('/topic/lobby', msg => { ... })
 *   sub.unsubscribe()
 */
class WSClient {
  private client: Client | null = null
  private connected = false
  private pending: Array<() => void> = []
  private subs = new Map<string, { topic: string; cb: (m: any) => void; sub?: StompSubscription }>()
  private nextId = 1

  private ensureClient() {
    if (this.client) return
    const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const url = `${proto}://${window.location.host}/ws`
    this.client = new Client({
      brokerURL: undefined,   // 用 webSocketFactory
      webSocketFactory: () => {
        const token = useAuthStore.getState().token
        return new WebSocket(`${url}?token=${encodeURIComponent(token ?? '')}`)
      },
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        this.connected = true
        // 重新訂閱所有訂閱
        for (const entry of this.subs.values()) {
          entry.sub = this.client!.subscribe(entry.topic, (msg: IMessage) => {
            try { entry.cb(JSON.parse(msg.body)) } catch { entry.cb(msg.body) }
          })
        }
        const queue = this.pending
        this.pending = []
        queue.forEach(fn => fn())
      },
      onStompError: frame => {
        console.error('STOMP error', frame.headers['message'], frame.body)
      },
      onWebSocketClose: () => {
        this.connected = false
        for (const e of this.subs.values()) e.sub = undefined
      },
    })
    this.client.activate()
  }

  subscribe<T = any>(topic: string, cb: (msg: T) => void): { unsubscribe: () => void } {
    this.ensureClient()
    const id = String(this.nextId++)
    const entry = { topic, cb }
    this.subs.set(id, entry as any)

    if (this.connected && this.client?.connected) {
      ;(entry as any).sub = this.client.subscribe(topic, (m: IMessage) => {
        try { cb(JSON.parse(m.body) as T) } catch { cb(m.body as any) }
      })
    }
    return {
      unsubscribe: () => {
        const e = this.subs.get(id)
        if (e?.sub) e.sub.unsubscribe()
        this.subs.delete(id)
        // 如果完全沒人訂閱，可選擇斷線（v1 不斷，留著）
      },
    }
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate()
      this.client = null
      this.connected = false
      this.subs.clear()
    }
  }
}

export const wsClient = new WSClient()
