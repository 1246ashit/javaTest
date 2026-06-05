package com.example.demo.service.impl;

import com.example.demo.dto.InventoryResponse;
import com.example.demo.dto.RoomAction;
import com.example.demo.dto.RoomDTO;
import com.example.demo.dto.RoomMemberDTO;
import com.example.demo.dto.RoomPhase;
import com.example.demo.dto.RoomOutcome;
import com.example.demo.dto.RoomSummaryDTO;
import com.example.demo.entity.UsersEntity;
import com.example.demo.messaging.LobbyEventPublisher;
import com.example.demo.repository.*;
import com.example.demo.service.GoldService;
import com.example.demo.service.InventoryService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplBroadcastTest {

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

        @Mock
        private RedissonClient redisson;
        @Mock
        private RLock rLock;

        @Mock
        private LobbyEventPublisher lobbyEventPublisher;

        @BeforeEach
        void setUp() {
                lenient().when(redisson.getLock(anyString())).thenReturn(rLock);
                roomService = new RoomServiceImpl(
                                redis,
                                stringRedis,
                                bossRepo,
                                userRepo,
                                inventoryService,
                                inventoryRepo,
                                equipmentRepo,
                                itemRepo,
                                goldService,
                                messaging,
                                redisson,
                                lobbyEventPublisher);
        }

        // joinRoom
        @Test
        @DisplayName("joinRoom case1:應廣播到 /topic/room/{roomId}")
        void joinRoom_case1() {
                // Arrange ①：準備一間進行中、空的房間，放在「假 redis」裡
                RoomDTO existing = new RoomDTO();
                existing.setRoomId("abc12345");
                existing.setOutcome(RoomOutcome.ONGOING);
                existing.setMembers(new ArrayList<>());
                existing.setRound(1);
                existing.setPhase(RoomPhase.PLAYER);
                when(redis.opsForValue()).thenReturn(valueOps);
                when(valueOps.get("room:abc12345")).thenReturn(existing);

                // Arrange ②：玩家
                UsersEntity user = new UsersEntity();
                user.setId(1L);
                user.setUsername("alice");
                when(userRepo.findById(1L)).thenReturn(Optional.of(user));

                // Arrange ③：玩家背包（newMember 會讀 totalAttack / totalDefense）
                InventoryResponse inv = new InventoryResponse(new HashMap<>(), List.of(), 0, 0);
                when(inventoryService.listInventory(1L)).thenReturn(inv);

                // Act
                roomService.joinRoom(1L, "abc12345");

                // Assert：驗證廣播到對應的 room topic、lobby 發 ROOM_UPDATED 事件
                verify(messaging).convertAndSend(eq("/topic/room/abc12345"), any(RoomDTO.class));
                verify(lobbyEventPublisher, times(1)).publishUpdated(any(RoomSummaryDTO.class));
        }

        // act
        @Test
        @DisplayName("act case1:ATTACK 成功、Boss 活著 → 廣播 room 1 次、不廣播 lobby")
        void act_case1() {
                // Arrange ①：造一個已經在房裡的玩家（不是用 joinRoom 加進來的）
                // → 所以這裡不需要 mock userRepo / inventoryService
                RoomMemberDTO member = new RoomMemberDTO();
                member.setUserId(1L);
                member.setUsername("alice");
                member.setHp(100);
                member.setMaxHp(100);
                member.setAttack(10);
                member.setDefense(5);
                member.setAlive(true);
                member.setFled(false);
                member.setActedThisRound(false); // 還沒行動，才能 act

                // Arrange ②：房間（進行中、玩家階段、Boss 血很多打不死）
                RoomDTO existing = new RoomDTO();
                existing.setRoomId("abc12345");
                existing.setOutcome(RoomOutcome.ONGOING);
                existing.setPhase(RoomPhase.PLAYER);
                existing.setRound(1);
                existing.setBossName("史萊姆");
                existing.setBossHp(100); // 100 血，打 10 打不死
                existing.setBossMaxHp(100);
                existing.setBossAttack(5); // Boss 反擊 5，玩家 100 血也死不了
                existing.setBossDefense(0);
                existing.setMembers(new ArrayList<>(List.of(member)));

                // Arrange ③：mock redis 讀寫
                when(redis.opsForValue()).thenReturn(valueOps);
                when(valueOps.get("room:abc12345")).thenReturn(existing);

                // Act：玩家攻擊
                roomService.act(1L, "abc12345", RoomAction.ATTACK, null);

                // Assert ①：room 廣播剛好 1 次
                verify(messaging, times(1))
                                .convertAndSend(eq("/topic/room/abc12345"), any(RoomDTO.class));

                // Assert ②：因為戰鬥還沒結束，不該廣播 lobby
                verify(messaging, never())
                                .convertAndSend(eq("/topic/lobby"), any(Object.class));

                // Assert ③：Boss 真的有被扣血（10 攻擊 - 0 防 = 10）
                assertThat(existing.getBossHp()).isEqualTo(90);

                // Assert ④：戰鬥仍在進行
                assertThat(existing.getOutcome()).isEqualTo(RoomOutcome.ONGOING);
        }

        @Test
        @DisplayName("act case2:ATTACK 打死 Boss → room + lobby 都廣播、結局是 VICTORY")
        void act_case2() {
                // Arrange ①：強壯的玩家
                RoomMemberDTO member = new RoomMemberDTO();
                member.setUserId(1L);
                member.setUsername("alice");
                member.setHp(100);
                member.setMaxHp(100);
                member.setAttack(999); // 一擊必殺
                member.setDefense(5);
                member.setAlive(true);
                member.setFled(false);
                member.setActedThisRound(false);

                // Arrange ②：殘血 Boss
                RoomDTO existing = new RoomDTO();
                existing.setRoomId("abc12345");
                existing.setOutcome(RoomOutcome.ONGOING);
                existing.setPhase(RoomPhase.PLAYER);
                existing.setRound(1);
                existing.setBossCode("SLIME");
                existing.setBossName("史萊姆");
                existing.setBossHp(10); // 一擊就死
                existing.setBossMaxHp(100);
                existing.setBossAttack(5);
                existing.setBossDefense(0);
                existing.setBossRewardGold(0L); // 不發獎勵，省掉 mock goldService
                existing.setMembers(new ArrayList<>(List.of(member)));

                // Arrange ③：mock redis（victory() 會去 stringRedis 移除 lobby key）
                when(redis.opsForValue()).thenReturn(valueOps);
                when(valueOps.get("room:abc12345")).thenReturn(existing);
                when(stringRedis.opsForSet()).thenReturn(setOps);

                // Act
                roomService.act(1L, "abc12345", RoomAction.ATTACK, null);

                // Assert ①：room 廣播 1 次
                verify(messaging, times(1))
                                .convertAndSend(eq("/topic/room/abc12345"), any(RoomDTO.class));

                // Assert ②：戰鬥結束 → lobby 發 ROOM_CLOSED 事件
                verify(lobbyEventPublisher, times(1)).publishClosed(eq("abc12345"));

                // Assert ③：結局
                assertThat(existing.getOutcome()).isEqualTo(RoomOutcome.VICTORY);
                assertThat(existing.getBossHp()).isEqualTo(0);
        }

        @Test
        @DisplayName("act case3:戰鬥已結束來 act → 拋例外、完全不廣播")
        void act_case3() {
                // Arrange：已結束的房間
                RoomMemberDTO member = new RoomMemberDTO();
                member.setUserId(1L);
                member.setAlive(true);

                RoomDTO existing = new RoomDTO();
                existing.setRoomId("abc12345");
                existing.setOutcome(RoomOutcome.VICTORY); // ← 已結束
                existing.setPhase(RoomPhase.PLAYER);
                existing.setMembers(new ArrayList<>(List.of(member)));

                when(redis.opsForValue()).thenReturn(valueOps);
                when(valueOps.get("room:abc12345")).thenReturn(existing);

                // Act + Assert ①：應該拋 IllegalStateException
                assertThatThrownBy(() -> roomService.act(1L, "abc12345", RoomAction.ATTACK, null))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("戰鬥已結束");

                // Assert ②：失敗時連一次廣播都不能發（messaging + Kafka 都沒）
                verify(messaging, never()).convertAndSend(any(String.class), any(Object.class));
                verify(lobbyEventPublisher, never()).publishCreated(any(RoomSummaryDTO.class));
                verify(lobbyEventPublisher, never()).publishUpdated(any(RoomSummaryDTO.class));
                verify(lobbyEventPublisher, never()).publishClosed(anyString());
        }

        // joinRoom
        @Test
        @DisplayName("joinRoom case2:房間已結束 → 拋例外、完全不廣播")
        void joinRoom_case2() {
                RoomDTO existing = new RoomDTO();
                existing.setRoomId("abc12345");
                existing.setOutcome(RoomOutcome.VICTORY); // ← 已結束
                existing.setMembers(new ArrayList<>());

                when(redis.opsForValue()).thenReturn(valueOps);
                when(valueOps.get("room:abc12345")).thenReturn(existing);

                assertThatThrownBy(() -> roomService.joinRoom(1L, "abc12345"))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("房間已結束");

                verify(messaging, never()).convertAndSend(any(String.class), any(Object.class));
                verify(lobbyEventPublisher, never()).publishCreated(any(RoomSummaryDTO.class));
                verify(lobbyEventPublisher, never()).publishUpdated(any(RoomSummaryDTO.class));
                verify(lobbyEventPublisher, never()).publishClosed(anyString());
        }

        // leaveRoom
        @Test
        @DisplayName("leaveRoom case1:還有其他人 → room + lobby 都廣播、房間仍 ONGOING")
        void leaveRoom_case1() {
                // Arrange：兩個玩家，alice 離開、bob 留下
                RoomMemberDTO alice = new RoomMemberDTO();
                alice.setUserId(1L);
                alice.setUsername("alice");
                alice.setAlive(true);
                alice.setFled(false);

                RoomMemberDTO bob = new RoomMemberDTO();
                bob.setUserId(2L);
                bob.setUsername("bob");
                bob.setAlive(true);
                bob.setFled(false);
                bob.setActedThisRound(false);

                RoomDTO existing = new RoomDTO();
                existing.setRoomId("abc12345");
                existing.setOutcome(RoomOutcome.ONGOING);
                existing.setPhase(RoomPhase.PLAYER);
                existing.setRound(1);
                existing.setBossHp(100);
                existing.setBossAttack(5);
                existing.setMembers(new ArrayList<>(List.of(alice, bob)));

                when(redis.opsForValue()).thenReturn(valueOps);
                when(valueOps.get("room:abc12345")).thenReturn(existing);

                // Act
                roomService.leaveRoom(1L, "abc12345");

                // Assert
                verify(messaging, times(1))
                                .convertAndSend(eq("/topic/room/abc12345"), any(RoomDTO.class));
                // 還有人留下 → 發 ROOM_UPDATED
                verify(lobbyEventPublisher, times(1)).publishUpdated(any(RoomSummaryDTO.class));
                assertThat(alice.isFled()).isTrue();
                assertThat(existing.getOutcome()).isEqualTo(RoomOutcome.ONGOING);
        }

        @Test
        @DisplayName("leaveRoom case2:最後一個離開 → 結局 ABANDONED、room + lobby 都廣播")
        void leaveRoom_case2() {
                RoomMemberDTO alice = new RoomMemberDTO();
                alice.setUserId(1L);
                alice.setUsername("alice");
                alice.setAlive(true);
                alice.setFled(false);

                RoomDTO existing = new RoomDTO();
                existing.setRoomId("abc12345");
                existing.setOutcome(RoomOutcome.ONGOING);
                existing.setPhase(RoomPhase.PLAYER);
                existing.setRound(1);
                existing.setMembers(new ArrayList<>(List.of(alice)));

                when(redis.opsForValue()).thenReturn(valueOps);
                when(valueOps.get("room:abc12345")).thenReturn(existing);
                when(stringRedis.opsForSet()).thenReturn(setOps);

                roomService.leaveRoom(1L, "abc12345");

                verify(messaging, times(1))
                                .convertAndSend(eq("/topic/room/abc12345"), any(RoomDTO.class));
                // 最後一人離開 → ABANDONED → 發 ROOM_CLOSED
                verify(lobbyEventPublisher, times(1)).publishClosed(eq("abc12345"));
                assertThat(existing.getOutcome()).isEqualTo(RoomOutcome.ABANDONED);
        }

}