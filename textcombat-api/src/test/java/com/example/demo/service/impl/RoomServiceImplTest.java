package com.example.demo.service.impl;

import com.example.demo.dto.InventoryResponse;
import com.example.demo.dto.RoomAction;
import com.example.demo.dto.RoomDTO;
import com.example.demo.dto.RoomMemberDTO;
import com.example.demo.dto.RoomOutcome;
import com.example.demo.dto.RoomPhase;
import com.example.demo.entity.Boss;
import com.example.demo.entity.Item;
import com.example.demo.entity.ItemType;
import com.example.demo.entity.UserInventoryItem;
import com.example.demo.entity.UsersEntity;
import com.example.demo.repository.BossRepository;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.UserEquipmentRepository;
import com.example.demo.repository.UserInventoryItemRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.GoldService;
import com.example.demo.service.InventoryService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RoomServiceImpl 核心邏輯單元測試（不含 broadcast；那部分在 RoomServiceImplBroadcastTest）。
 */
@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock
    private RedisTemplate<String, RoomDTO> redis;
    @Mock
    private RedisTemplate<String, String> stringRedis;
    @Mock
    private ValueOperations<String, RoomDTO> valueOps;
    @Mock
    private SetOperations<String, String> setOps;
    @Mock
    private BossRepository bossRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private UserInventoryItemRepository inventoryRepo;
    @Mock
    private UserEquipmentRepository equipmentRepo;
    @Mock
    private ItemRepository itemRepo;
    @Mock
    private GoldService goldService;
    @Mock
    private SimpMessagingTemplate messaging;

    private RoomServiceImpl roomService;

    @BeforeEach
    void setUp() {
        roomService = new RoomServiceImpl(
                redis, stringRedis,
                bossRepo, userRepo,
                inventoryService, inventoryRepo, equipmentRepo, itemRepo,
                goldService, messaging);
    }

    // ============================================================
    //  helpers
    // ============================================================

    /** 開好兩個 redis ops 的 stub。需要 redis 寫入的 case 都會用到。 */
    private void stubRedisOps() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(stringRedis.opsForSet()).thenReturn(setOps);
    }

    private Boss boss(long id, String code, int hp, int atk, int def, long reward) {
        Boss b = new Boss();
        b.setId(id);
        b.setCode(code);
        b.setName("史萊姆");
        b.setHp(hp);
        b.setAttack(atk);
        b.setDefense(def);
        b.setRewardGold(reward);
        return b;
    }

    private UsersEntity user(long id, String username) {
        UsersEntity u = new UsersEntity();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    private RoomMemberDTO member(long userId, String name, int hp, int atk, int def,
                                  boolean alive, boolean fled, boolean acted) {
        RoomMemberDTO m = new RoomMemberDTO();
        m.setUserId(userId);
        m.setUsername(name);
        m.setHp(hp);
        m.setMaxHp(100);
        m.setAttack(atk);
        m.setDefense(def);
        m.setAlive(alive);
        m.setFled(fled);
        m.setActedThisRound(acted);
        return m;
    }

    private RoomDTO ongoingRoom(String roomId, RoomMemberDTO... members) {
        RoomDTO r = new RoomDTO();
        r.setRoomId(roomId);
        r.setBossId(1L);
        r.setBossCode("SLIME");
        r.setBossName("史萊姆");
        r.setBossMaxHp(100);
        r.setBossHp(100);
        r.setBossAttack(10);
        r.setBossDefense(2);
        r.setBossRewardGold(50L);
        r.setRound(1);
        r.setPhase(RoomPhase.PLAYER);
        r.setOutcome(RoomOutcome.ONGOING);
        r.setMembers(new ArrayList<>(List.of(members)));
        return r;
    }

    // ============================================================
    //  createRoom
    // ============================================================

    @Test
    @DisplayName("createRoom_case1: bossId = null → IllegalArgumentException")
    void createRoom_case1_bossIdNull_throws() {
        assertThatThrownBy(() -> roomService.createRoom(1L, null, "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("bossId 為必填");

        verify(redis, never()).opsForValue();
    }

    @Test
    @DisplayName("createRoom_case2: BOSS 不存在 → IllegalArgumentException")
    void createRoom_case2_bossNotFound_throws() {
        when(bossRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.createRoom(1L, 99L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BOSS 不存在");
    }

    @Test
    @DisplayName("createRoom_case3: 玩家不存在 → IllegalArgumentException")
    void createRoom_case3_userNotFound_throws() {
        when(bossRepo.findById(1L)).thenReturn(Optional.of(boss(1L, "SLIME", 100, 10, 2, 50L)));
        when(userRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.createRoom(99L, 1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("玩家不存在");
    }

    @Test
    @DisplayName("createRoom_case4: happy path 開房成功，並把建立者放進房 + 加入 lobby")
    void createRoom_case4_happyPath() {
        when(bossRepo.findById(1L)).thenReturn(Optional.of(boss(1L, "SLIME", 100, 10, 2, 50L)));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user(1L, "alice")));
        when(inventoryService.listInventory(1L))
                .thenReturn(new InventoryResponse(new HashMap<>(), List.of(), 30, 5));
        stubRedisOps();

        RoomDTO r = roomService.createRoom(1L, 1L, "alice 的隊伍");

        // 房間基本資料正確
        assertThat(r.getName()).isEqualTo("alice 的隊伍");
        assertThat(r.getBossId()).isEqualTo(1L);
        assertThat(r.getBossHp()).isEqualTo(100);
        assertThat(r.getOutcome()).isEqualTo(RoomOutcome.ONGOING);
        assertThat(r.getPhase()).isEqualTo(RoomPhase.PLAYER);
        assertThat(r.getRound()).isEqualTo(1);

        // 建立者已是成員
        assertThat(r.getMembers()).hasSize(1);
        RoomMemberDTO me = r.getMembers().get(0);
        assertThat(me.getUserId()).isEqualTo(1L);
        assertThat(me.isAlive()).isTrue();
        assertThat(me.isFled()).isFalse();
        assertThat(me.isActedThisRound()).isFalse();
        assertThat(me.getAttack()).isEqualTo(30);
        assertThat(me.getDefense()).isEqualTo(5);
        assertThat(me.getHp()).isEqualTo(RoomServiceImpl.PLAYER_MAX_HP);

        // 寫入 redis、加入 lobby、廣播
        verify(valueOps).set(eq("room:" + r.getRoomId()), eq(r), any(Duration.class));
        verify(setOps).add("lobby:rooms", r.getRoomId());
        verify(messaging).convertAndSend(eq("/topic/room/" + r.getRoomId()), eq(r));
    }

    // ============================================================
    //  joinRoom
    // ============================================================

    @Test
    @DisplayName("joinRoom_case1: 房間不存在 → IllegalArgumentException")
    void joinRoom_case1_roomNotFound_throws() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("room:nope")).thenReturn(null);

        assertThatThrownBy(() -> roomService.joinRoom(1L, "nope"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("房間不存在或已關閉");
    }

    @Test
    @DisplayName("joinRoom_case2: 房間已結束 → IllegalStateException")
    void joinRoom_case2_roomEnded_throws() {
        RoomDTO r = ongoingRoom("abc");
        r.setOutcome(RoomOutcome.VICTORY);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("room:abc")).thenReturn(r);

        assertThatThrownBy(() -> roomService.joinRoom(1L, "abc"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("房間已結束");
    }

    @Test
    @DisplayName("joinRoom_case3: 已在房內（未 fled）→ 視為重新進畫面、不做事")
    void joinRoom_case3_alreadyIn_noop() {
        RoomMemberDTO me = member(1L, "alice", 100, 10, 5, true, false, false);
        RoomDTO r = ongoingRoom("abc", me);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("room:abc")).thenReturn(r);

        RoomDTO out = roomService.joinRoom(1L, "abc");

        assertThat(out).isSameAs(r);
        assertThat(r.getMembers()).hasSize(1); // 沒被加第二筆
        verify(valueOps, never()).set(any(), any(), any(Duration.class));
        verify(messaging, never()).convertAndSend(anyString(), any(RoomDTO.class));
    }

    @Test
    @DisplayName("joinRoom_case4: 曾經 fled → IllegalStateException")
    void joinRoom_case4_previouslyFled_throws() {
        RoomMemberDTO me = member(1L, "alice", 0, 10, 5, false, true, true);
        RoomDTO r = ongoingRoom("abc", me);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("room:abc")).thenReturn(r);

        assertThatThrownBy(() -> roomService.joinRoom(1L, "abc"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("你已離開過這間房");
    }

    @Test
    @DisplayName("joinRoom_case5: 房間已滿 → IllegalStateException")
    void joinRoom_case5_roomFull_throws() {
        RoomDTO r = ongoingRoom("abc",
                member(2L, "b", 100, 10, 5, true, false, false),
                member(3L, "c", 100, 10, 5, true, false, false),
                member(4L, "d", 100, 10, 5, true, false, false),
                member(5L, "e", 100, 10, 5, true, false, false));
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("room:abc")).thenReturn(r);

        assertThatThrownBy(() -> roomService.joinRoom(1L, "abc"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("房間已滿");
    }

    @Test
    @DisplayName("joinRoom_case6: PLAYER 階段中途加入 → actedThisRound = true（本回合不能動）")
    void joinRoom_case6_midRoundJoiner_actedThisRoundTrue() {
        RoomDTO r = ongoingRoom("abc", member(2L, "b", 100, 10, 5, true, false, false));
        r.setPhase(RoomPhase.PLAYER);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("room:abc")).thenReturn(r);
        when(stringRedis.opsForSet()).thenReturn(setOps);
        when(setOps.members("lobby:rooms")).thenReturn(Set.of());
        when(userRepo.findById(1L)).thenReturn(Optional.of(user(1L, "alice")));
        when(inventoryService.listInventory(1L))
                .thenReturn(new InventoryResponse(new HashMap<>(), List.of(), 30, 5));

        roomService.joinRoom(1L, "abc");

        RoomMemberDTO newcomer = r.getMembers().stream()
                .filter(m -> m.getUserId().equals(1L))
                .findFirst().orElseThrow();
        assertThat(newcomer.isActedThisRound()).isTrue(); // ← 中途加入本回合不能再動
    }

    // ============================================================
    //  leaveRoom
    // ============================================================

    @Test
    @DisplayName("leaveRoom_case1: 不在房內 → IllegalStateException")
    void leaveRoom_case1_notInRoom_throws() {
        RoomDTO r = ongoingRoom("abc", member(2L, "b", 100, 10, 5, true, false, false));
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("room:abc")).thenReturn(r);

        assertThatThrownBy(() -> roomService.leaveRoom(99L, "abc"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("你不在這間房");
    }

    @Test
    @DisplayName("leaveRoom_case2: 已 fled → 直接回傳當下狀態，不再動")
    void leaveRoom_case2_alreadyFled_noop() {
        RoomMemberDTO me = member(1L, "alice", 0, 10, 5, false, true, true);
        RoomDTO r = ongoingRoom("abc", me);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("room:abc")).thenReturn(r);

        RoomDTO out = roomService.leaveRoom(1L, "abc");

        assertThat(out).isSameAs(r);
        verify(valueOps, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("leaveRoom_case3: 全部都離開 → outcome = ABANDONED、從 lobby 移除")
    void leaveRoom_case3_lastOneLeft_abandoned() {
        RoomMemberDTO me = member(1L, "alice", 100, 10, 5, true, false, false);
        RoomDTO r = ongoingRoom("abc", me);
        stubRedisOps();
        when(valueOps.get("room:abc")).thenReturn(r);

        RoomDTO out = roomService.leaveRoom(1L, "abc");

        assertThat(out.getOutcome()).isEqualTo(RoomOutcome.ABANDONED);
        assertThat(out.getEndedAt()).isNotNull();
        assertThat(me.isFled()).isTrue();
        verify(setOps).remove("lobby:rooms", "abc");
    }

    @Test
    @DisplayName("leaveRoom_case4: 仍有其他成員 → 自己 fled、房間繼續、不會 ABANDONED")
    void leaveRoom_case4_othersStay_continues() {
        RoomMemberDTO me = member(1L, "alice", 100, 10, 5, true, false, true);
        RoomMemberDTO other = member(2L, "bob", 100, 10, 5, true, false, false);
        RoomDTO r = ongoingRoom("abc", me, other);
        stubRedisOps();
        when(valueOps.get("room:abc")).thenReturn(r);
        when(setOps.members("lobby:rooms")).thenReturn(Set.of());

        roomService.leaveRoom(1L, "abc");

        assertThat(me.isFled()).isTrue();
        assertThat(me.isAlive()).isFalse();
        assertThat(r.getOutcome()).isEqualTo(RoomOutcome.ONGOING); // bob 還在
    }

    // ============================================================
    //  act
    // ============================================================

    @Test
    @DisplayName("act_case1: action = null → IllegalArgumentException")
    void act_case1_actionNull_throws() {
        assertThatThrownBy(() -> roomService.act(1L, "abc", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("action 為必填");
    }

    @Test
    @DisplayName("act_case2: 房間已結束 → IllegalStateException")
    void act_case2_roomEnded_throws() {
        RoomDTO r = ongoingRoom("abc", member(1L, "alice", 100, 10, 5, true, false, false));
        r.setOutcome(RoomOutcome.VICTORY);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("room:abc")).thenReturn(r);

        assertThatThrownBy(() -> roomService.act(1L, "abc", RoomAction.ATTACK, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("戰鬥已結束");
    }

    @Test
    @DisplayName("act_case3: 非 PLAYER 階段 → IllegalStateException")
    void act_case3_notPlayerPhase_throws() {
        RoomDTO r = ongoingRoom("abc", member(1L, "alice", 100, 10, 5, true, false, false));
        r.setPhase(RoomPhase.BOSS);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("room:abc")).thenReturn(r);

        assertThatThrownBy(() -> roomService.act(1L, "abc", RoomAction.ATTACK, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("非玩家階段");
    }

    @Test
    @DisplayName("act_case4: 不在房內 → IllegalStateException")
    void act_case4_notInRoom_throws() {
        RoomDTO r = ongoingRoom("abc", member(2L, "bob", 100, 10, 5, true, false, false));
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("room:abc")).thenReturn(r);

        assertThatThrownBy(() -> roomService.act(99L, "abc", RoomAction.ATTACK, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("你不在這間房");
    }

    @Test
    @DisplayName("act_case5: 角色已死 → IllegalStateException")
    void act_case5_alreadyDead_throws() {
        RoomMemberDTO me = member(1L, "alice", 0, 10, 5, false, false, false);
        RoomDTO r = ongoingRoom("abc", me);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("room:abc")).thenReturn(r);

        assertThatThrownBy(() -> roomService.act(1L, "abc", RoomAction.ATTACK, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("你已倒下，無法行動");
    }

    @Test
    @DisplayName("act_case6: 本回合已行動 → IllegalStateException")
    void act_case6_alreadyActed_throws() {
        RoomMemberDTO me = member(1L, "alice", 100, 10, 5, true, false, true); // acted=true
        RoomDTO r = ongoingRoom("abc", me);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("room:abc")).thenReturn(r);

        assertThatThrownBy(() -> roomService.act(1L, "abc", RoomAction.ATTACK, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("你本回合已行動過");
    }

    @Test
    @DisplayName("act_case7: ATTACK 對 BOSS 造成 max(1, atk-def) 傷害")
    void act_case7_attack_damagesBoss() {
        RoomMemberDTO me = member(1L, "alice", 100, 30, 5, true, false, false);
        RoomMemberDTO other = member(2L, "bob", 100, 30, 5, true, false, false); // 防止 round 推進
        RoomDTO r = ongoingRoom("abc", me, other);
        // boss: hp=100, def=2 → 我攻擊 30 - 2 = 28 傷害
        stubRedisOps();
        when(valueOps.get("room:abc")).thenReturn(r);

        roomService.act(1L, "abc", RoomAction.ATTACK, null);

        assertThat(r.getBossHp()).isEqualTo(72); // 100 - 28
        assertThat(me.isActedThisRound()).isTrue();
        // bob 沒動 → 還沒進 BOSS phase
        assertThat(r.getPhase()).isEqualTo(RoomPhase.PLAYER);
        assertThat(r.getOutcome()).isEqualTo(RoomOutcome.ONGOING);
    }

    @Test
    @DisplayName("act_case8: ATTACK 直接打死 BOSS → VICTORY、活著的成員拿到獎勵")
    void act_case8_attackKillsBoss_victoryAndReward() {
        RoomMemberDTO me = member(1L, "alice", 100, 200, 5, true, false, false);
        RoomDTO r = ongoingRoom("abc", me);
        r.setBossHp(50); // 一擊必殺
        r.setBossRewardGold(100L);
        stubRedisOps();
        when(valueOps.get("room:abc")).thenReturn(r);
        when(goldService.changeGold(eq(1L), eq(100L), eq("BATTLE_REWARD"), eq("SLIME"), anyString()))
                .thenReturn(200L);

        roomService.act(1L, "abc", RoomAction.ATTACK, null);

        assertThat(r.getBossHp()).isEqualTo(0);
        assertThat(r.getOutcome()).isEqualTo(RoomOutcome.VICTORY);
        assertThat(r.getEndedAt()).isNotNull();
        verify(goldService).changeGold(eq(1L), eq(100L), eq("BATTLE_REWARD"), eq("SLIME"), anyString());
        verify(setOps).remove("lobby:rooms", "abc"); // 結束後從 lobby 移除
    }

    @Test
    @DisplayName("act_case9: SKIP → 只標 acted，不傷 BOSS")
    void act_case9_skip_marksActedOnly() {
        RoomMemberDTO me = member(1L, "alice", 100, 30, 5, true, false, false);
        RoomMemberDTO other = member(2L, "bob", 100, 30, 5, true, false, false);
        RoomDTO r = ongoingRoom("abc", me, other);
        stubRedisOps();
        when(valueOps.get("room:abc")).thenReturn(r);

        int bossHpBefore = r.getBossHp();
        roomService.act(1L, "abc", RoomAction.SKIP, null);

        assertThat(r.getBossHp()).isEqualTo(bossHpBefore); // 沒掉血
        assertThat(me.isActedThisRound()).isTrue();
    }

    @Test
    @DisplayName("act_case10: USE_POTION 未帶 inventoryItemId → IllegalArgumentException")
    void act_case10_usePotionMissingItem_throws() {
        RoomMemberDTO me = member(1L, "alice", 50, 30, 5, true, false, false);
        RoomDTO r = ongoingRoom("abc", me);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("room:abc")).thenReturn(r);

        assertThatThrownBy(() -> roomService.act(1L, "abc", RoomAction.USE_POTION, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("USE_POTION 需要 inventoryItemId");
    }

    @Test
    @DisplayName("act_case11: USE_POTION 不是自己的物品 → IllegalStateException")
    void act_case11_usePotionNotOwner_throws() {
        RoomMemberDTO me = member(1L, "alice", 50, 30, 5, true, false, false);
        RoomDTO r = ongoingRoom("abc", me);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("room:abc")).thenReturn(r);

        UserInventoryItem inv = new UserInventoryItem();
        inv.setId(500L);
        inv.setUserId(99L); // 別人的
        inv.setItemId(3L);
        inv.setQuantity(1);
        when(inventoryRepo.findById(500L)).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> roomService.act(1L, "abc", RoomAction.USE_POTION, 500L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("不是你的物品");
    }

    @Test
    @DisplayName("act_case12: USE_POTION 物品不是消耗品 → IllegalArgumentException")
    void act_case12_usePotionNotConsumable_throws() {
        RoomMemberDTO me = member(1L, "alice", 50, 30, 5, true, false, false);
        RoomDTO r = ongoingRoom("abc", me);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("room:abc")).thenReturn(r);

        UserInventoryItem inv = new UserInventoryItem();
        inv.setId(500L);
        inv.setUserId(1L);
        inv.setItemId(1L);
        inv.setQuantity(1);
        when(inventoryRepo.findById(500L)).thenReturn(Optional.of(inv));

        Item weapon = new Item();
        weapon.setId(1L);
        weapon.setType(ItemType.EQUIPMENT);
        when(itemRepo.findById(1L)).thenReturn(Optional.of(weapon));

        assertThatThrownBy(() -> roomService.act(1L, "abc", RoomAction.USE_POTION, 500L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("不是消耗品");
    }

    @Test
    @DisplayName("act_case13: USE_POTION 成功 → HP 回升、藥水數量 -1、不傷 BOSS")
    void act_case13_usePotionSuccess_healsAndDeducts() {
        RoomMemberDTO me = member(1L, "alice", 40, 30, 5, true, false, false);
        RoomMemberDTO other = member(2L, "bob", 100, 30, 5, true, false, false); // 防進 BOSS phase
        RoomDTO r = ongoingRoom("abc", me, other);
        stubRedisOps();
        when(valueOps.get("room:abc")).thenReturn(r);

        UserInventoryItem inv = new UserInventoryItem();
        inv.setId(500L);
        inv.setUserId(1L);
        inv.setItemId(3L);
        inv.setQuantity(5);
        when(inventoryRepo.findById(500L)).thenReturn(Optional.of(inv));

        Item potion = new Item();
        potion.setId(3L);
        potion.setType(ItemType.CONSUMABLE);
        potion.setName("藥水");
        potion.setHealAmount(30);
        when(itemRepo.findById(3L)).thenReturn(Optional.of(potion));

        int bossHpBefore = r.getBossHp();
        roomService.act(1L, "abc", RoomAction.USE_POTION, 500L);

        assertThat(me.getHp()).isEqualTo(70);             // 40 + 30
        assertThat(inv.getQuantity()).isEqualTo(4);       // 5 - 1
        assertThat(r.getBossHp()).isEqualTo(bossHpBefore); // BOSS 沒掉血
        verify(inventoryRepo).save(inv);                  // 走 save 不走 delete
        verify(inventoryRepo, never()).delete(inv);
        assertThat(me.isActedThisRound()).isTrue();
    }

    // ============================================================
    //  listOpen
    // ============================================================

    @Test
    @DisplayName("listOpen_case1: lobby 為空 → 回傳空 list")
    void listOpen_case1_empty_returnsEmpty() {
        when(stringRedis.opsForSet()).thenReturn(setOps);
        when(setOps.members("lobby:rooms")).thenReturn(Set.of());

        assertThat(roomService.listOpen()).isEmpty();
    }

    @Test
    @DisplayName("listOpen_case2: 已結束的房會被順手從 lobby 清掉")
    void listOpen_case2_cleansEndedRooms() {
        stubRedisOps();
        when(setOps.members("lobby:rooms")).thenReturn(Set.of("ended"));
        when(redis.opsForValue()).thenReturn(valueOps);

        RoomDTO ended = ongoingRoom("ended");
        ended.setOutcome(RoomOutcome.VICTORY);
        when(valueOps.multiGet(any())).thenReturn(List.of(ended));

        List<?> out = roomService.listOpen();

        assertThat(out).isEmpty();
        verify(setOps).remove("lobby:rooms", "ended");
    }

    // ============================================================
    //  getRoom
    // ============================================================

    @Test
    @DisplayName("getRoom_case1: 房間不存在 → IllegalArgumentException")
    void getRoom_case1_notFound_throws() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("room:nope")).thenReturn(null);

        assertThatThrownBy(() -> roomService.getRoom("nope"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("房間不存在或已關閉");
    }

    @Test
    @DisplayName("getRoom_case2: 房間存在 → 回傳該房")
    void getRoom_case2_found_returns() {
        RoomDTO r = ongoingRoom("abc");
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("room:abc")).thenReturn(r);

        assertThat(roomService.getRoom("abc")).isSameAs(r);
    }
}
