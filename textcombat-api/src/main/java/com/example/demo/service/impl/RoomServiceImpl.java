package com.example.demo.service.impl;

import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.service.GoldService;
import com.example.demo.service.InventoryService;
import com.example.demo.service.RoomService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RoomServiceImpl implements RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomServiceImpl.class);

    public static final int PLAYER_MAX_HP = 100;
    public static final Duration ACTIVE_TTL = Duration.ofMinutes(30);
    public static final Duration ENDED_TTL = Duration.ofMinutes(3);

    private static final String LOBBY_KEY = "lobby:rooms";

    private final RedisTemplate<String, RoomDTO> redis;
    private final RedisTemplate<String, String> stringRedis;
    private final BossRepository bossRepo;
    private final UserRepository userRepo;
    private final InventoryService inventoryService;
    private final UserInventoryItemRepository inventoryRepo;
    private final UserEquipmentRepository equipmentRepo;
    private final ItemRepository itemRepo;
    private final GoldService goldService;
    private final SimpMessagingTemplate messaging;

    public RoomServiceImpl(RedisTemplate<String, RoomDTO> redis,
            RedisTemplate<String, String> stringRedis,
            BossRepository bossRepo,
            UserRepository userRepo,
            InventoryService inventoryService,
            UserInventoryItemRepository inventoryRepo,
            UserEquipmentRepository equipmentRepo,
            ItemRepository itemRepo,
            GoldService goldService,
            SimpMessagingTemplate messaging) {
        this.redis = redis;
        this.stringRedis = stringRedis;
        this.bossRepo = bossRepo;
        this.userRepo = userRepo;
        this.inventoryService = inventoryService;
        this.inventoryRepo = inventoryRepo;
        this.equipmentRepo = equipmentRepo;
        this.itemRepo = itemRepo;
        this.goldService = goldService;
        this.messaging = messaging;
    }

    private String roomKey(String roomId) {
        return "room:" + roomId;
    }

    private RoomDTO load(String roomId) {
        RoomDTO r = redis.opsForValue().get(roomKey(roomId));
        if (r == null)
            throw new IllegalArgumentException("房間不存在或已關閉");
        return r;
    }

    private void save(RoomDTO r) {
        Duration ttl = r.getOutcome() == RoomOutcome.ONGOING ? ACTIVE_TTL : ENDED_TTL;
        redis.opsForValue().set(roomKey(r.getRoomId()), r, ttl);
    }

    private void remove(String roomId) {
        redis.delete(roomKey(roomId));
        stringRedis.opsForSet().remove(LOBBY_KEY, roomId);
    }

    /** 同 roomId 的字串拿來當鎖；單 server 並發安全。 */
    private Object lockFor(String roomId) {
        return ("__room_lock__:" + roomId).intern();
    }

    private void broadcastRoom(RoomDTO r) {
        messaging.convertAndSend("/topic/room/" + r.getRoomId(), r);
    }

    private void broadcastLobby() {
        messaging.convertAndSend("/topic/lobby", listOpen());
    }

    // ====================================================
    // 公開 API
    // ====================================================

    @Override
    @Transactional
    public RoomDTO createRoom(Long userId, Long bossId, String name) {
        if (bossId == null)
            throw new IllegalArgumentException("bossId 為必填");
        Boss boss = bossRepo.findById(bossId)
                .orElseThrow(() -> new IllegalArgumentException("BOSS 不存在"));
        UsersEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("玩家不存在"));

        String roomId = UUID.randomUUID().toString().substring(0, 8);

        RoomDTO r = new RoomDTO();
        r.setRoomId(roomId);
        r.setName(name == null || name.isBlank()
                ? (user.getDisplayName() != null ? user.getDisplayName() : user.getUsername()) + " 的隊伍"
                : name);
        r.setBossId(boss.getId());
        r.setBossCode(boss.getCode());
        r.setBossName(boss.getName());
        r.setBossIcon(boss.getIcon());
        r.setBossMaxHp(boss.getHp());
        r.setBossHp(boss.getHp());
        r.setBossAttack(boss.getAttack());
        r.setBossDefense(boss.getDefense());
        r.setBossRewardGold(boss.getRewardGold());
        r.setRound(1);
        r.setPhase(RoomPhase.PLAYER);
        r.setOutcome(RoomOutcome.ONGOING);
        r.setCreatedAt(Instant.now().toEpochMilli());
        r.getLog().add(r.getName() + " 開戰：對戰 " + boss.getName());

        // 加入建立者
        RoomMemberDTO m = newMember(user, false);
        r.getMembers().add(m);
        r.getLog().add(displayOf(user) + " 加入隊伍");

        save(r);
        stringRedis.opsForSet().add(LOBBY_KEY, roomId);

        log.info("開房: roomId={}, userId={}, bossId={}", roomId, userId, bossId);
        broadcastRoom(r);
        broadcastLobby();
        return r;
    }

    @Override
    @Transactional
    public RoomDTO joinRoom(Long userId, String roomId) {
        synchronized (lockFor(roomId)) {
            RoomDTO r = load(roomId);
            if (r.getOutcome() != RoomOutcome.ONGOING) {
                throw new IllegalStateException("房間已結束");
            }
            // 已在房間中（曾加入過）→ 視為重新進入畫面，不做事
            for (RoomMemberDTO m : r.getMembers()) {
                if (m.getUserId().equals(userId)) {
                    if (m.isFled())
                        throw new IllegalStateException("你已離開過這間房");
                    return r;
                }
            }
            int active = (int) r.getMembers().stream().filter(m -> !m.isFled()).count();
            if (active >= RoomDTO.MAX_MEMBERS) {
                throw new IllegalStateException("房間已滿");
            }

            UsersEntity user = userRepo.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("玩家不存在"));
            // 中途加入：本回合視為已動，下回合才能行動
            RoomMemberDTO m = newMember(user, r.getPhase() == RoomPhase.PLAYER);
            r.getMembers().add(m);
            r.getLog().add(String.format("第 %d 回合：%s 加入了戰鬥", r.getRound(), displayOf(user)));

            save(r);
            log.info("加入房間: roomId={}, userId={}", roomId, userId);
            broadcastRoom(r);
            broadcastLobby();
            return r;
        }
    }

    @Override
    @Transactional
    public RoomDTO leaveRoom(Long userId, String roomId) {
        synchronized (lockFor(roomId)) {
            RoomDTO r = load(roomId);
            RoomMemberDTO me = findMember(r, userId);
            if (me == null)
                throw new IllegalStateException("你不在這間房");
            if (me.isFled())
                return r; // 已經離開過

            me.setFled(true);
            me.setAlive(false);
            me.setActedThisRound(true);
            r.getLog().add(String.format("第 %d 回合：%s 離開了戰鬥", r.getRound(), displayOf(me)));

            // 結算/推進
            if (allLeft(r)) {
                r.setOutcome(RoomOutcome.ABANDONED);
                r.setEndedAt(Instant.now().toEpochMilli());
                r.getLog().add("沒有玩家在房間裡，戰鬥中止");
                save(r);
                stringRedis.opsForSet().remove(LOBBY_KEY, r.getRoomId());
                broadcastRoom(r);
                broadcastLobby();
                return r;
            }

            if (r.getOutcome() == RoomOutcome.ONGOING) {
                advanceIfRoundDone(r);
            }
            save(r);
            broadcastRoom(r);
            broadcastLobby();
            return r;
        }
    }

    @Override
    @Transactional
    public RoomDTO act(Long userId, String roomId, RoomAction action, Long inventoryItemId) {
        if (action == null)
            throw new IllegalArgumentException("action 為必填");
        synchronized (lockFor(roomId)) {
            RoomDTO r = load(roomId);
            if (r.getOutcome() != RoomOutcome.ONGOING) {
                throw new IllegalStateException("戰鬥已結束");
            }
            if (r.getPhase() != RoomPhase.PLAYER) {
                throw new IllegalStateException("非玩家階段");
            }
            RoomMemberDTO me = findMember(r, userId);
            if (me == null || me.isFled())
                throw new IllegalStateException("你不在這間房");
            if (!me.isAlive())
                throw new IllegalStateException("你已倒下，無法行動");
            if (me.isActedThisRound())
                throw new IllegalStateException("你本回合已行動過");

            switch (action) {
                case ATTACK -> doAttack(r, me);
                case USE_POTION -> doPotion(r, me, userId, inventoryItemId);
                case SKIP -> doSkip(r, me);
            }
            me.setActedThisRound(true);

            // 玩家可能直接打死 BOSS
            if (r.getBossHp() <= 0) {
                victory(r);
            } else {
                advanceIfRoundDone(r);
            }
            save(r);
            broadcastRoom(r);
            if (r.getOutcome() != RoomOutcome.ONGOING)
                broadcastLobby();
            return r;
        }
    }

    @Override
    public List<RoomSummaryDTO> listOpen() {
        Set<String> ids = stringRedis.opsForSet().members(LOBBY_KEY);

        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<RoomSummaryDTO> out = new ArrayList<>();
        List<String> idList = new ArrayList<>(ids);
        List<String> keys = idList.stream().map(this::roomKey).toList();
        List<RoomDTO> rooms = redis.opsForValue().multiGet(keys);

        for (int i = 0; i < idList.size(); i++) {
            String id = idList.get(i);
            RoomDTO r = rooms == null ? null : rooms.get(i);

            if (r == null || r.getOutcome() != RoomOutcome.ONGOING) {
                stringRedis.opsForSet().remove(LOBBY_KEY, id); // 順手清掉壞的
                continue;
            }
            out.add(RoomSummaryDTO.of(r));
        }
        out.sort(Comparator.comparingLong(s -> s.getCreatedAt() == null ? 0L : s.getCreatedAt()));
        return out;
    }

    @Override
    public RoomDTO getRoom(String roomId) {
        return load(roomId);
    }

    // ====================================================
    // 行動實作
    // ====================================================

    private void doAttack(RoomDTO r, RoomMemberDTO me) {
        int dmg = Math.max(1, me.getAttack() - r.getBossDefense());
        int newHp = Math.max(0, r.getBossHp() - dmg);
        r.getLog().add(String.format("第 %d 回合：%s 攻擊造成 %d 傷害（%s HP %d → %d）",
                r.getRound(), displayOf(me), dmg, r.getBossName(), r.getBossHp(), newHp));
        r.setBossHp(newHp);
    }

    private void doPotion(RoomDTO r, RoomMemberDTO me, Long userId, Long inventoryItemId) {
        if (inventoryItemId == null) {
            throw new IllegalArgumentException("USE_POTION 需要 inventoryItemId");
        }
        UserInventoryItem inv = inventoryRepo.findById(inventoryItemId)
                .orElseThrow(() -> new IllegalArgumentException("背包物品不存在"));
        if (!inv.getUserId().equals(userId)) {
            throw new IllegalStateException("不是你的物品");
        }
        Item item = itemRepo.findById(inv.getItemId())
                .orElseThrow(() -> new IllegalStateException("物品資料異常"));
        if (item.getType() != ItemType.CONSUMABLE) {
            throw new IllegalArgumentException("不是消耗品");
        }
        Integer heal = item.getHealAmount();
        if (heal == null || heal <= 0) {
            throw new IllegalArgumentException("此物品無回血效果");
        }

        int remain = inv.getQuantity() - 1;
        if (remain <= 0) {
            equipmentRepo.findByUserIdAndInventoryItemId(userId, inventoryItemId)
                    .ifPresent(equipmentRepo::delete);
            inventoryRepo.delete(inv);
        } else {
            inv.setQuantity(remain);
            inventoryRepo.save(inv);
        }

        int before = me.getHp();
        int after = Math.min(me.getMaxHp(), before + heal);
        me.setHp(after);
        r.getLog().add(String.format("第 %d 回合：%s 喝 %s 回 %d HP（%d → %d）",
                r.getRound(), displayOf(me), item.getName(), after - before, before, after));
    }

    private void doSkip(RoomDTO r, RoomMemberDTO me) {
        r.getLog().add(String.format("第 %d 回合：%s 選擇跳過", r.getRound(), displayOf(me)));
    }

    // ====================================================
    // 回合推進
    // ====================================================

    /** 如果所有 alive 成員都已行動 → 進入 BOSS 階段並執行，再回到下一 round。 */
    private void advanceIfRoundDone(RoomDTO r) {
        if (r.getOutcome() != RoomOutcome.ONGOING)
            return;
        List<RoomMemberDTO> active = r.getMembers().stream()
                .filter(m -> !m.isFled())
                .toList();
        if (active.isEmpty()) {
            r.setOutcome(RoomOutcome.ABANDONED);
            r.setEndedAt(Instant.now().toEpochMilli());
            r.getLog().add("沒有玩家在房間裡，戰鬥中止");
            stringRedis.opsForSet().remove(LOBBY_KEY, r.getRoomId());
            return;
        }
        boolean allAliveActed = active.stream()
                .filter(RoomMemberDTO::isAlive)
                .allMatch(RoomMemberDTO::isActedThisRound);
        if (!allAliveActed)
            return;
        // 已沒有活著的 → 全死
        boolean anyoneAlive = active.stream().anyMatch(RoomMemberDTO::isAlive);
        if (!anyoneAlive) {
            defeat(r);
            return;
        }

        // BOSS 階段
        r.setPhase(RoomPhase.BOSS);
        bossTurn(r);

        if (r.getOutcome() != RoomOutcome.ONGOING)
            return;

        // 進入下一回合
        r.setRound(r.getRound() + 1);
        r.setPhase(RoomPhase.PLAYER);
        for (RoomMemberDTO m : r.getMembers()) {
            if (!m.isFled())
                m.setActedThisRound(false);
        }
    }

    private void bossTurn(RoomDTO r) {
        List<RoomMemberDTO> targets = r.getMembers().stream()
                .filter(m -> !m.isFled() && m.isAlive())
                .toList();
        if (targets.isEmpty()) {
            defeat(r);
            return;
        }
        RoomMemberDTO target = targets.get(ThreadLocalRandom.current().nextInt(targets.size()));
        int dmg = Math.max(1, r.getBossAttack() - target.getDefense());
        int newHp = Math.max(0, target.getHp() - dmg);
        r.getLog().add(String.format("第 %d 回合：%s 攻擊 %s，造成 %d 傷害（%d → %d）",
                r.getRound(), r.getBossName(), displayOf(target), dmg, target.getHp(), newHp));
        target.setHp(newHp);
        if (newHp == 0) {
            target.setAlive(false);
            r.getLog().add(displayOf(target) + " 倒下了");
        }
        // BOSS 攻擊完判斷全死
        boolean anyoneAlive = r.getMembers().stream()
                .anyMatch(m -> !m.isFled() && m.isAlive());
        if (!anyoneAlive)
            defeat(r);
    }

    private void victory(RoomDTO r) {
        r.setOutcome(RoomOutcome.VICTORY);
        r.setEndedAt(Instant.now().toEpochMilli());
        r.getLog().add("勝利！" + r.getBossName() + " 倒下了");
        long reward = r.getBossRewardGold() == null ? 0 : r.getBossRewardGold();
        if (reward > 0) {
            for (RoomMemberDTO m : r.getMembers()) {
                if (m.isFled() || !m.isAlive())
                    continue;
                try {
                    long bal = goldService.changeGold(
                            m.getUserId(), reward, "BATTLE_REWARD", r.getBossCode(),
                            "擊敗 " + r.getBossName());
                    r.getLog().add(String.format("%s 獲得金幣 💰 +%d（餘額 %d）",
                            displayOf(m), reward, bal));
                } catch (Exception e) {
                    log.warn("發放戰鬥獎勵失敗 userId={}, err={}", m.getUserId(), e.getMessage());
                }
            }
        }
        stringRedis.opsForSet().remove(LOBBY_KEY, r.getRoomId());
    }

    private void defeat(RoomDTO r) {
        r.setOutcome(RoomOutcome.DEFEAT);
        r.setEndedAt(Instant.now().toEpochMilli());
        r.getLog().add("失敗⋯⋯所有人都倒下了");
        stringRedis.opsForSet().remove(LOBBY_KEY, r.getRoomId());
    }

    // ====================================================
    // helpers
    // ====================================================

    private RoomMemberDTO newMember(UsersEntity user, boolean actedThisRound) {
        InventoryResponse inv = inventoryService.listInventory(user.getId());
        RoomMemberDTO m = new RoomMemberDTO();
        m.setUserId(user.getId());
        m.setUsername(user.getUsername());
        m.setDisplayName(user.getDisplayName());
        m.setHp(PLAYER_MAX_HP);
        m.setMaxHp(PLAYER_MAX_HP);
        m.setAttack(inv.getTotalAttack());
        m.setDefense(inv.getTotalDefense());
        m.setAlive(true);
        m.setFled(false);
        m.setActedThisRound(actedThisRound);
        m.setJoinedAt(Instant.now().toEpochMilli());
        return m;
    }

    private RoomMemberDTO findMember(RoomDTO r, Long userId) {
        for (RoomMemberDTO m : r.getMembers()) {
            if (m.getUserId().equals(userId))
                return m;
        }
        return null;
    }

    private boolean allLeft(RoomDTO r) {
        return r.getMembers().stream().allMatch(RoomMemberDTO::isFled);
    }

    private static String displayOf(UsersEntity u) {
        return u.getDisplayName() != null && !u.getDisplayName().isBlank() ? u.getDisplayName() : u.getUsername();
    }

    private static String displayOf(RoomMemberDTO m) {
        return m.getDisplayName() != null && !m.getDisplayName().isBlank() ? m.getDisplayName() : m.getUsername();
    }
}
