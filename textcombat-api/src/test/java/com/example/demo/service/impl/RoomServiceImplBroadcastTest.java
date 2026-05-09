package com.example.demo.service.impl;

import com.example.demo.dto.InventoryResponse;
import com.example.demo.dto.RoomDTO;
import com.example.demo.dto.RoomPhase;
import com.example.demo.dto.RoomOutcome;
import com.example.demo.entity.UsersEntity;
import com.example.demo.repository.*;
import com.example.demo.service.GoldService;
import com.example.demo.service.InventoryService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @BeforeEach
    void setUp() {
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
                messaging);
    }

    @Test
    @DisplayName("joinRoom 應廣播到 /topic/room/{roomId}")
    void joinRoom_broadcastsToRoomTopic() {
        // Arrange ①：準備一間進行中、空的房間，放在「假 redis」裡
        RoomDTO existing = new RoomDTO();
        existing.setRoomId("abc12345");
        existing.setOutcome(RoomOutcome.ONGOING);
        existing.setMembers(new ArrayList<>());
        existing.setRound(1);
        existing.setPhase(RoomPhase.PLAYER);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("room:abc12345")).thenReturn(existing);

        when(stringRedis.opsForSet()).thenReturn(setOps);
        when(setOps.members("lobby:rooms")).thenReturn(Set.of());

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

        // Assert：驗證廣播到對應的 topic
        verify(messaging).convertAndSend(eq("/topic/room/abc12345"), any(RoomDTO.class));
    }
}