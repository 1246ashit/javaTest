# CLAUDE.md

本檔案提供指引給未來在此 repo 工作的 Claude Code (claude.ai/code) 實例。

## Repo 結構

兩個並列子專案，一起開發：

- `textcombat-api/` — Spring Boot 4.0.5 後端（Java 21、Maven）。遊戲邏輯、JPA、JWT、WebSocket。
- `textcombat-web/` — Vite + React 19 + TypeScript 前端（antd、zustand、react-router v7、axios、@stomp/stompjs）。

最上層沒有統一 build 工具 —— 兩邊各自啟動。

## 常用指令

### 後端（`textcombat-api/`）

需要在 `textcombat-api/.env` 中放 `POSTGRES_PASSWORD` 與 `JWT_SECRET`（透過 `spring.config.import=optional:file:.env[.properties]` 載入）。執行需要三個外部服務：**PostgreSQL**（真相來源）、**Redis**（房間狀態 + Redisson 分散式鎖）、**Kafka**（lobby 事件廣播）。連線設定在 `application.properties`（Redis host/password、`spring.kafka.bootstrap-servers`、consumer group `textcombat-lobby`）。Kafka 或 Redis 起不來，啟動會失敗。

```powershell
# 啟動 dev server（HTTPS 8443，self-signed 憑證在 keystore.p12）
mvn spring-boot:run

# 跑所有測試
mvn test

# 跑單一測試類別
mvn test "-Dtest=GoldServiceImplTest"

# 跑單一測試方法
mvn test "-Dtest=GoldServiceImplTest#changeGold_spendSuccess_updatesBalanceAndWritesTx"

# 打包 jar
mvn clean package
```

JPA 設定 `ddl-auto=validate` —— schema 變更只在資料庫端做，不要靠 Hibernate 建表或 migrate。

### 前端（`textcombat-web/`）

```powershell
npm run dev      # vite dev server，把 /api 和 /ws proxy 到 https://localhost:8443
npm run build    # tsc -b && vite build
npm run lint     # eslint .
```

`vite.config.ts` 的 proxy 設 `secure: false` 以接受後端 self-signed 憑證。前端要能用，後端必須先在 8443 跑起來。

## 架構

### 認證與請求流程（後端）

`JwtAuthInterceptor` 由 `WebMvcConfig` 註冊到 `/api/**`。對每一個 controller method：

1. 若 header 有 `Authorization: Bearer <token>`，解 token 並把 `UsersEntity` 放進 ThreadLocal（`CurrentUserHolder.set(...)`）。即使是 `@Public` endpoint 也會跑這步，讓 public endpoint 也能機會性地讀到 current user。
2. 若 handler 標 `@Public` → 直接放行。
3. 否則必須已登入。若 method 或 class 上有 `@RequirePermission("CODE")`，會用 `user.hasPermission(code)` 比對 role→permission 攤平後的集合（`getPermissionCodes()` 把 `roles -> permissions` 攤平）。
4. `afterCompletion` 會清掉 ThreadLocal —— 不要繞過這步；`CurrentUserHolder.get()` 只在 request 處理期間有效。

Controller 透過 `CurrentUserHolder.get()` 拿當前使用者，不要從 client 傳 userId。永遠從 JWT 推出來。

錯誤處理：service 丟 `IllegalArgumentException`（→ 400）或 `IllegalStateException`（→ 409），由 `GlobalExceptionHandler` 統一轉。Controller 不要對這些 case 再包 try/catch —— 讓它往上拋。（`AccountController` 是例外，它早於 handler 寫成，自己抓了。）

### 遊戲狀態：SQL vs Redis

- **PostgreSQL** 是真相來源：使用者、物品、背包、裝備、金幣餘額、金幣交易、Boss、role、permission。
- **Redis** 放*短暫戰鬥狀態*：每間房序列化成 `RoomDTO`，key 為 `room:<roomId>`；另有 `lobby:rooms` Set 記錄開放中的房 id。進行中的房 TTL 30 分鐘、已結束的房 TTL 3 分鐘。**房間沒有 DB table** —— Redis key 一掉，房間就沒了。

每房層級的並發控制用 **Redisson 分散式鎖**：`redisson.getLock("room_lock:" + roomId)` → `lock.lock()` / `finally { lock.unlock() }`（見 `joinRoom`、`leaveRoom`、`act`）。`RedissonConfig` 用 single-server 模式指到同一台 Redis。這跨 instance 生效，所以水平擴展安全。改 room 並發相關邏輯時一律用 Redisson `RLock`。

### 即時更新（WebSocket / STOMP）

`WebSocketConfig` 開放 `/ws` 端點，使用 in-memory simple broker 配 `/topic`。JWT 在 handshake 階段透過 query string `?token=...` 驗（**不是** 透過 header）。`StompPrincipal(userId, username)` 會綁到 session，讓 `convertAndSendToUser` 能用。

`RoomServiceImpl` 每次改 state 後會把房間狀態**直接** broadcast 到 `/topic/room/<roomId>`（單房訂閱者就在同一台 instance，不需跨 instance）。

**Lobby 廣播走 Kafka，不是直接 broadcast**（為了多 instance 一致性）：房間開/關/更新時，`broadcastLobbyChange` 透過 `LobbyEventPublisher` 發 `LobbyEvent` 到 Kafka topic（用 `roomId` 當 partition key 保證同房事件順序）。每個 instance 的 `LobbyEventConsumer`（`@KafkaListener`）收到事件後，各自 `convertAndSend("/topic/lobby", roomService.listOpen())` 推給自己連著的前端。所以改 lobby 推送邏輯要動 `messaging/` 目錄，不是在 service 裡直接呼叫 `messaging.convertAndSend`。`KafkaConfig` 設了 `TRUSTED_PACKAGES`（`com.example.demo.messaging,com.example.demo.dto`）做 JSON 反序列化白名單。

前端用 `@stomp/stompjs` 訂閱 `/topic/room/<roomId>` 與 `/topic/lobby`（見 `textcombat-web/src/api/ws.ts`）。

### Service 層

Service interface 放在 `com.example.demo.service`，implementation 放在 `com.example.demo.service.impl`（**小寫** `impl` —— 之前 rename 過，見 commit `5c42642`）。新增 service 時遵循這個拆分。測試直接針對 impl class（`*ServiceImplTest`）。

### 該知道的領域邏輯

- **金幣**：`GoldService.changeGold(userId, amount, reason, refId, note)` 是**唯一**改使用者金幣的合法方式。負值 = 花、正值 = 賺。它在同一個 transaction 內寫 `GoldTransaction` 記錄與餘額。`reason` 必填、`amount` 為 0 會被擋。
- **強化**：`EnhanceServiceImpl` 直接內嵌成功率／素材數量／失敗效果表（`successRateFor`、`materialQtyFor`、`failEffectFor`），用 `MATERIAL_CODE = "ENHANCE_STONE"`。攻防公式 `base + base*level/10` 在 `InventoryServiceImpl` 也有一份 —— 改公式時兩邊都要同步。
- **戰鬥**：`RoomServiceImpl.act` 是玩家行動的唯一入口。回合推進邏輯在 `advanceIfRoundDone` —— 只有當所有活著、未離開的成員都 `actedThisRound = true`，才會進 BOSS 階段。

### 前端結構

- `src/api/` —— 一個 domain 一支檔（auth、shop、room、inventory、enhance、gold、boss、admin、ws）。所有 HTTP 都走 `client.ts` 的 `apiClient`，會自動從 `useAuthStore` 注入 JWT、遇 401 自動登出。
- `src/store/authStore.ts` —— zustand store，存 token + user，持久化到 localStorage。
- `src/pages/` —— 一條 route 一支 component；路由設在 `App.tsx`，用 `RequireAuth` 守門。

## 測試慣例

- Unit test 用 JUnit 5 + Mockito（`@ExtendWith(MockitoExtension.class)`、`@Mock`、`@InjectMocks`），assertion 用 AssertJ。Service 測試**不**載 Spring context，純 mock。
- 測試名稱遵循 `methodName_scenario_expectedBehavior`，並用 `@DisplayName` 寫人類可讀的 case 標籤（常用 `caseN: ...`）。新測試請照這個風格 —— `GoldServiceImplTest` 是範本。
- WebSocket broadcast 的測試方式：mock `SimpMessagingTemplate`，verify `convertAndSend` 的呼叫（見 `RoomServiceImplBroadcastTest`）。

## 機密與設定

`.env`（任何位置）都已 gitignore。`keystore.p12` 也 gitignore。`application.properties` 引用 `${POSTGRES_PASSWORD}` 與 `${JWT_SECRET}` —— 不要把實際值寫死。如果 diff 裡看到 hard-coded 機密，那就是 regression。
