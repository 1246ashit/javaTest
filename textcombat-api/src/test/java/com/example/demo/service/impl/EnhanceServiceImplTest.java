package com.example.demo.service.impl;

import com.example.demo.entity.UsersEntity;
import com.example.demo.dto.EnhanceFailEffect;
import com.example.demo.dto.EnhancePreviewResponse;
import com.example.demo.dto.EnhanceResultResponse;
import com.example.demo.dto.InventoryResponse;
import com.example.demo.dto.MaterialRequirementDTO;
import com.example.demo.entity.GoldTransaction;
import com.example.demo.entity.UserInventoryItem;
import com.example.demo.entity.Item;
import com.example.demo.entity.ItemType;
import com.example.demo.repository.GoldTransactionRepository;
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

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@ExtendWith(MockitoExtension.class)
public class EnhanceServiceImplTest {
        @Mock
        private UserRepository userRepository;

        @Mock
        private UserInventoryItemRepository inventoryRepo;

        @Mock
        private ItemRepository itemRepo;

        @Mock
        private GoldTransactionRepository txRepository;

        @Mock
        private GoldService goldService;

        @Mock
        private UserEquipmentRepository equipmentRepo;

        @Mock
        private InventoryService inventoryService;

        @InjectMocks
        private EnhanceServiceImpl enhanceServiceImpl;

        // preview
        @Test
        @DisplayName("case1: preview happy path 無強化素材強化")
        void preview_case1() {
                // given
                Long userId = 100L;
                Long inventoryItemId = 101L;
                Long itemId = 1L;

                UserInventoryItem uit = new UserInventoryItem();
                uit.setId(inventoryItemId);
                uit.setUserId(userId);
                uit.setItemId(itemId);
                uit.setQuantity(1);
                uit.setEnhancementLevel(1);

                Item item = new Item();
                item.setId(itemId);
                item.setCode("SWORD_01");
                item.setName("鐵劍");
                item.setType(ItemType.EQUIPMENT);
                item.setBaseAttack(200);
                item.setBaseDefense(200);

                when(inventoryRepo.findById(inventoryItemId)).thenReturn(Optional.of(uit));
                when(itemRepo.findById(itemId)).thenReturn(Optional.of(item));
                when(itemRepo.findByCode("ENHANCE_STONE")).thenReturn(Optional.empty());// 此次強化不需要素材
                when(goldService.getGold(userId)).thenReturn(1_000_000L);

                // Act
                EnhancePreviewResponse res = enhanceServiceImpl.preview(userId, inventoryItemId);

                // assert
                assertThat(res.getInventoryItemId()).isEqualTo(101L);
                assertThat(res.getItemCode()).isEqualTo("SWORD_01");
                assertThat(res.getItemName()).isEqualTo("鐵劍");
                assertThat(res.getCurrentLevel()).isEqualTo(1);
                assertThat(res.getMaxLevel()).isEqualTo(10);
                assertThat(res.getCurrentAttack()).isEqualTo(220);
                assertThat(res.getCurrentDefense()).isEqualTo(220);
                assertThat(res.getNextAttack()).isEqualTo(240);
                assertThat(res.getNextDefense()).isEqualTo(240);
                assertThat(res.getSuccessRate()).isEqualTo(0.95);
                assertThat(res.getGoldCost()).isEqualTo(200L);
                assertThat(res.getGoldOwned()).isEqualTo(1_000_000L);
                assertThat(res.getOnFail()).isEqualTo(EnhanceFailEffect.NOTHING);
                assertThat(res.getMaterialCosts()).isEmpty();
                assertThat(res.isCanEnhance()).isTrue();
                assertThat(res.getBlockReason()).isNull();
        }

        @Test
        @DisplayName("case2: preview happy path 有要強化素材強化，且有強化素材")
        void preview_case2() {
                // given
                Long userId = 100L;
                Long inventoryItemId = 101L;
                Long weaponItemId = 1L;
                Long stoneItemId = 201L;

                // 使用者背包的劍
                UserInventoryItem uit = new UserInventoryItem();
                uit.setId(inventoryItemId);
                uit.setUserId(userId);
                uit.setItemId(weaponItemId);
                uit.setQuantity(1);
                uit.setEnhancementLevel(8);

                // 使用者背包的強化石有幾顆
                UserInventoryItem uit2 = new UserInventoryItem();
                uit2.setItemId(stoneItemId);
                uit2.setQuantity(123456);

                // 要被強化的武器
                Item wepon = new Item();
                wepon.setId(weaponItemId);
                wepon.setCode("SWORD_01");
                wepon.setName("鐵劍");
                wepon.setType(ItemType.EQUIPMENT);
                wepon.setBaseAttack(200);
                wepon.setBaseDefense(200);

                // 強化素材
                Item enhanceMeterialItem = new Item();
                enhanceMeterialItem.setId(stoneItemId);
                enhanceMeterialItem.setCode("ENHANCE_STONE");
                enhanceMeterialItem.setName("強化石");

                when(inventoryRepo.findById(inventoryItemId))
                                .thenReturn(Optional.of(uit));
                when(itemRepo.findById(weaponItemId))
                                .thenReturn(Optional.of(wepon));
                when(itemRepo.findByCode("ENHANCE_STONE"))
                                .thenReturn(Optional.of(enhanceMeterialItem));// 此次強化要素材
                when(inventoryRepo.findFirstByUserIdAndItemIdAndEnhancementLevel(userId, stoneItemId, 0))
                                .thenReturn(Optional.of(uit2));
                when(goldService.getGold(userId))
                                .thenReturn(1_000_000L);

                // Act
                EnhancePreviewResponse res = enhanceServiceImpl.preview(userId, inventoryItemId);

                // assert
                assertThat(res.getInventoryItemId()).isEqualTo(101L);
                assertThat(res.getItemCode()).isEqualTo("SWORD_01");
                assertThat(res.getItemName()).isEqualTo("鐵劍");
                assertThat(res.getCurrentLevel()).isEqualTo(8);
                assertThat(res.getMaxLevel()).isEqualTo(10);
                assertThat(res.getCurrentAttack()).isEqualTo(360);
                assertThat(res.getCurrentDefense()).isEqualTo(360);
                assertThat(res.getNextAttack()).isEqualTo(380);
                assertThat(res.getNextDefense()).isEqualTo(380);
                assertThat(res.getSuccessRate()).isEqualTo(0.2);
                assertThat(res.getGoldCost()).isEqualTo(900L);
                assertThat(res.getGoldOwned()).isEqualTo(1_000_000L);
                assertThat(res.getOnFail()).isEqualTo(EnhanceFailEffect.DESTROY);

                // 預計強化素材清單
                // 素材會隨等級增加
                // 規格：level 8 → 需要 8 顆素材（公式 Math.max(0, level)）
                assertThat(res.getMaterialCosts()).containsExactly(
                                new MaterialRequirementDTO(201L, "ENHANCE_STONE", "強化石", 8, 123456));

                assertThat(res.isCanEnhance()).isTrue();
                assertThat(res.getBlockReason()).isNull();
        }

        @Test
        @DisplayName("case3: preview 素材不足 → 不能強化、blockReason 為「強化石不足」")
        void preview_case3() {
                // given
                Long userId = 100L;
                Long inventoryItemId = 101L;
                Long weaponItemId = 1L;
                Long stoneItemId = 201L;

                // 使用者背包的劍
                UserInventoryItem uit = new UserInventoryItem();
                uit.setId(inventoryItemId);
                uit.setUserId(userId);
                uit.setItemId(weaponItemId);
                uit.setQuantity(1);
                uit.setEnhancementLevel(8);

                // 使用者背包的強化石有幾顆
                UserInventoryItem uit2 = new UserInventoryItem();
                uit2.setItemId(stoneItemId);
                uit2.setQuantity(1);

                // 要被強化的武器
                Item wepon = new Item();
                wepon.setId(weaponItemId);
                wepon.setCode("SWORD_01");
                wepon.setName("鐵劍");
                wepon.setType(ItemType.EQUIPMENT);
                wepon.setBaseAttack(200);
                wepon.setBaseDefense(200);

                // 強化素材
                Item enhanceMeterialItem = new Item();
                enhanceMeterialItem.setId(stoneItemId);
                enhanceMeterialItem.setCode("ENHANCE_STONE");
                enhanceMeterialItem.setName("強化石");

                when(inventoryRepo.findById(inventoryItemId))
                                .thenReturn(Optional.of(uit));
                when(itemRepo.findById(weaponItemId))
                                .thenReturn(Optional.of(wepon));
                when(itemRepo.findByCode("ENHANCE_STONE"))
                                .thenReturn(Optional.of(enhanceMeterialItem));// 此次強化要素材
                when(inventoryRepo.findFirstByUserIdAndItemIdAndEnhancementLevel(userId, stoneItemId, 0))
                                .thenReturn(Optional.of(uit2));
                when(goldService.getGold(userId))
                                .thenReturn(1_000_000L);

                // Act
                EnhancePreviewResponse res = enhanceServiceImpl.preview(userId, inventoryItemId);

                // assert
                assertThat(res.getInventoryItemId()).isEqualTo(101L);
                assertThat(res.getItemCode()).isEqualTo("SWORD_01");
                assertThat(res.getItemName()).isEqualTo("鐵劍");
                assertThat(res.getCurrentLevel()).isEqualTo(8);
                assertThat(res.getMaxLevel()).isEqualTo(10);
                assertThat(res.getCurrentAttack()).isEqualTo(360);
                assertThat(res.getCurrentDefense()).isEqualTo(360);
                assertThat(res.getNextAttack()).isEqualTo(380);
                assertThat(res.getNextDefense()).isEqualTo(380);
                assertThat(res.getSuccessRate()).isEqualTo(0.2);
                assertThat(res.getGoldCost()).isEqualTo(900L);
                assertThat(res.getGoldOwned()).isEqualTo(1_000_000L);
                assertThat(res.getOnFail()).isEqualTo(EnhanceFailEffect.DESTROY);

                // 預計強化素材清單
                // 素材會隨等級增加
                // 規格：level 8 → 需要 8 顆素材（公式 Math.max(0, level)）
                assertThat(res.getMaterialCosts()).containsExactly(
                                new MaterialRequirementDTO(201L, "ENHANCE_STONE", "強化石", 8, 1));

                assertThat(res.isCanEnhance()).isFalse();
                assertThat(res.getBlockReason()).isEqualTo("強化石不足");
        }

        @Test
        @DisplayName("case4: preview 金幣不足 → 不能強化、blockReason 為「金幣不足」")
        void preview_case4() {
                // given
                Long userId = 100L;
                Long inventoryItemId = 101L;
                Long weaponItemId = 1L;
                Long stoneItemId = 201L;

                // 使用者背包的劍
                UserInventoryItem uit = new UserInventoryItem();
                uit.setId(inventoryItemId);
                uit.setUserId(userId);
                uit.setItemId(weaponItemId);
                uit.setQuantity(1);
                uit.setEnhancementLevel(8);

                // 使用者背包的強化石有幾顆
                UserInventoryItem uit2 = new UserInventoryItem();
                uit2.setItemId(stoneItemId);
                uit2.setQuantity(123456);

                // 要被強化的武器
                Item wepon = new Item();
                wepon.setId(weaponItemId);
                wepon.setCode("SWORD_01");
                wepon.setName("鐵劍");
                wepon.setType(ItemType.EQUIPMENT);
                wepon.setBaseAttack(200);
                wepon.setBaseDefense(200);

                // 強化素材
                Item enhanceMeterialItem = new Item();
                enhanceMeterialItem.setId(stoneItemId);
                enhanceMeterialItem.setCode("ENHANCE_STONE");
                enhanceMeterialItem.setName("強化石");

                when(inventoryRepo.findById(inventoryItemId))
                                .thenReturn(Optional.of(uit));
                when(itemRepo.findById(weaponItemId))
                                .thenReturn(Optional.of(wepon));
                when(itemRepo.findByCode("ENHANCE_STONE"))
                                .thenReturn(Optional.of(enhanceMeterialItem));// 此次強化要素材
                when(inventoryRepo.findFirstByUserIdAndItemIdAndEnhancementLevel(userId, stoneItemId, 0))
                                .thenReturn(Optional.of(uit2));
                when(goldService.getGold(userId))
                                .thenReturn(100L);

                // Act
                EnhancePreviewResponse res = enhanceServiceImpl.preview(userId, inventoryItemId);

                // assert
                assertThat(res.getBlockReason()).isEqualTo("金幣不足");
        }

        @Test
        @DisplayName("case5: preview 等級已達上限 → 不能強化、blockReason 為「已達強化上限」")
        void preview_case5() {
                // given
                Long userId = 100L;
                Long inventoryItemId = 101L;
                Long weaponItemId = 1L;

                // 使用者背包的劍
                UserInventoryItem uit = new UserInventoryItem();
                uit.setId(inventoryItemId);
                uit.setUserId(userId);
                uit.setItemId(weaponItemId);
                uit.setQuantity(1);
                uit.setEnhancementLevel(10);

                // 要被強化的武器
                Item wepon = new Item();
                wepon.setId(weaponItemId);
                wepon.setCode("SWORD_01");
                wepon.setName("鐵劍");
                wepon.setType(ItemType.EQUIPMENT);
                wepon.setBaseAttack(200);
                wepon.setBaseDefense(200);

                when(inventoryRepo.findById(inventoryItemId))
                                .thenReturn(Optional.of(uit));
                when(itemRepo.findById(weaponItemId))
                                .thenReturn(Optional.of(wepon));
                when(goldService.getGold(userId))
                                .thenReturn(10000000L);

                // Act
                EnhancePreviewResponse res = enhanceServiceImpl.preview(userId, inventoryItemId);

                // assert
                assertThat(res.getCurrentLevel()).isEqualTo(10);
                assertThat(res.getMaxLevel()).isEqualTo(10);
                assertThat(res.getCurrentAttack()).isEqualTo(400);
                assertThat(res.getNextAttack()).isEqualTo(400);
                assertThat(res.getCurrentDefense()).isEqualTo(400);
                assertThat(res.getNextDefense()).isEqualTo(400);
                assertThat(res.getSuccessRate()).isEqualTo(0.0);
                assertThat(res.getGoldCost()).isEqualTo(0L);
                assertThat(res.getMaterialCosts()).isEqualTo(List.of());
                assertThat(res.getOnFail()).isEqualTo(EnhanceFailEffect.NOTHING);
                assertThat(res.isCanEnhance()).isFalse();
                assertThat(res.getBlockReason()).isEqualTo("已達強化上限");
        }

        //

        // enhance
        @Test
        @DisplayName("enhance_case1: level 1 強化成功（凍結隨機 0.5 < 0.95）")
        void enhance_case1_success() {
                // given
                Long userId = 100L;
                Long inventoryItemId = 101L;
                Long weaponItemId = 1L;

                UserInventoryItem uit = new UserInventoryItem();
                uit.setId(inventoryItemId);
                uit.setUserId(userId);
                uit.setItemId(weaponItemId);
                uit.setEnhancementLevel(1);

                Item weapon = new Item();
                weapon.setId(weaponItemId);
                weapon.setCode("SWORD_01");
                weapon.setName("鐵劍");
                weapon.setType(ItemType.EQUIPMENT);
                weapon.setBaseAttack(200);
                weapon.setBaseDefense(200);

                when(inventoryRepo.findById(inventoryItemId)).thenReturn(Optional.of(uit));
                when(itemRepo.findById(weaponItemId)).thenReturn(Optional.of(weapon));
                when(itemRepo.findByCode("ENHANCE_STONE")).thenReturn(Optional.empty()); // 沒設定素材
                when(goldService.getGold(userId)).thenReturn(1_000_000L);
                when(goldService.changeGold(eq(userId), eq(-200L), eq("ENHANCE"), eq("SWORD_01"), anyString()))
                                .thenReturn(999_800L);
                InventoryResponse fakeInv = mock(InventoryResponse.class);
                when(inventoryService.listInventory(userId)).thenReturn(fakeInv);

                // 凍結隨機
                ThreadLocalRandom fakeRandom = mock(ThreadLocalRandom.class);
                when(fakeRandom.nextDouble()).thenReturn(0.5); // 0.5 < 0.95 → 成功

                // Act
                try (MockedStatic<ThreadLocalRandom> mocked = mockStatic(ThreadLocalRandom.class)) {
                        mocked.when(ThreadLocalRandom::current).thenReturn(fakeRandom);
                        
                        EnhanceResultResponse res = enhanceServiceImpl.enhance(userId, inventoryItemId);
                        // assert
                        assertThat(res.isSuccess()).isTrue();
                        assertThat(res.getPreviousLevel()).isEqualTo(1);
                        assertThat(res.getNewLevel()).isEqualTo(2);// 1等升2等
                        assertThat(res.isDestroyed()).isFalse();
                        assertThat(res.getGoldSpent()).isEqualTo(200L);
                        assertThat(res.getGoldBalance()).isEqualTo(999_800L);
                        assertThat(res.getMessage()).isEqualTo("強化成功！+1 → +2");

                        verify(inventoryService).listInventory(userId);
                        assertThat(res.getInventory()).isSameAs(fakeInv);
                }

        }

}
