package com.example.demo.service.impl;


import com.example.demo.dto.EnhanceFailEffect;
import com.example.demo.dto.EnhancePreviewResponse;
import com.example.demo.dto.EnhanceResultResponse;
import com.example.demo.dto.InventoryResponse;
import com.example.demo.dto.MaterialRequirementDTO;
import com.example.demo.entity.UserInventoryItem;
import com.example.demo.entity.UserEquipment;
import com.example.demo.entity.Item;
import com.example.demo.entity.ItemType;
import com.example.demo.repository.GoldTransactionRepository;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.UserEquipmentRepository;
import com.example.demo.repository.UserInventoryItemRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.GoldService;
import com.example.demo.service.InventoryService;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

        // enhance: 失敗效果 NOTHING（fromLevel 0~3）
        @Test
        @DisplayName("enhance_case2: level 2 強化失敗 NOTHING（等級不變、素材仍扣、武器不 save）")
        void enhance_case2_fail_NOTHING() {
                // given
                Long userId = 100L;
                Long inventoryItemId = 101L;
                Long weaponItemId = 1L;
                Long stoneItemId = 201L;

                UserInventoryItem uit = new UserInventoryItem();
                uit.setId(inventoryItemId);
                uit.setUserId(userId);
                uit.setItemId(weaponItemId);
                uit.setEnhancementLevel(2);

                Item weapon = new Item();
                weapon.setId(weaponItemId);
                weapon.setCode("SWORD_01");
                weapon.setName("鐵劍");
                weapon.setType(ItemType.EQUIPMENT);
                weapon.setBaseAttack(200);
                weapon.setBaseDefense(200);

                Item stone = new Item();
                stone.setId(stoneItemId);
                stone.setCode("ENHANCE_STONE");
                stone.setName("強化石");

                UserInventoryItem stoneInv = new UserInventoryItem();
                stoneInv.setItemId(stoneItemId);
                stoneInv.setQuantity(5);

                when(inventoryRepo.findById(inventoryItemId)).thenReturn(Optional.of(uit));
                when(itemRepo.findById(weaponItemId)).thenReturn(Optional.of(weapon));
                when(goldService.getGold(userId)).thenReturn(1_000_000L);
                when(itemRepo.findByCode("ENHANCE_STONE")).thenReturn(Optional.of(stone));
                when(inventoryRepo.findFirstByUserIdAndItemIdAndEnhancementLevel(userId, stoneItemId, 0))
                                .thenReturn(Optional.of(stoneInv));
                when(goldService.changeGold(eq(userId), eq(-300L), eq("ENHANCE"), eq("SWORD_01"), anyString()))
                                .thenReturn(999_700L);
                InventoryResponse fakeInv = mock(InventoryResponse.class);
                when(inventoryService.listInventory(userId)).thenReturn(fakeInv);

                // 凍結隨機：0.99 >= 0.90 → 失敗
                ThreadLocalRandom fakeRandom = mock(ThreadLocalRandom.class);
                when(fakeRandom.nextDouble()).thenReturn(0.99);

                // Act
                try (MockedStatic<ThreadLocalRandom> mocked = mockStatic(ThreadLocalRandom.class)) {
                        mocked.when(ThreadLocalRandom::current).thenReturn(fakeRandom);

                        EnhanceResultResponse res = enhanceServiceImpl.enhance(userId, inventoryItemId);

                        // assert：失敗結果
                        assertThat(res.isSuccess()).isFalse();
                        assertThat(res.isDestroyed()).isFalse();
                        assertThat(res.getPreviousLevel()).isEqualTo(2);
                        assertThat(res.getNewLevel()).isEqualTo(2); // 等級不變
                        assertThat(res.getGoldSpent()).isEqualTo(300L);
                        assertThat(res.getGoldBalance()).isEqualTo(999_700L);
                        assertThat(res.getMessage()).isEqualTo("強化失敗，等級不變（仍為 +2）");

                        // 素材已扣（5 - 2 = 3）
                        assertThat(stoneInv.getQuantity()).isEqualTo(3);
                        verify(inventoryRepo).save(stoneInv);

                        // 武器本體沒被 save、沒被 delete
                        verify(inventoryRepo, never()).save(uit);
                        verify(inventoryRepo, never()).delete(uit);
                        assertThat(uit.getEnhancementLevel()).isEqualTo(2);

                        // 沒走到裝備格刪除
                        verify(equipmentRepo, never()).delete(any(UserEquipment.class));
                        verify(equipmentRepo, never()).flush();

                        assertThat(res.getInventory()).isSameAs(fakeInv);
                }
        }

        // enhance: 失敗效果 DOWNGRADE（fromLevel 4~6）
        @Test
        @DisplayName("enhance_case3: level 5 強化失敗 DOWNGRADE（等級 -1、武器要 save）")
        void enhance_case3_fail_DOWNGRADE() {
                // given
                Long userId = 100L;
                Long inventoryItemId = 101L;
                Long weaponItemId = 1L;
                Long stoneItemId = 201L;

                UserInventoryItem uit = new UserInventoryItem();
                uit.setId(inventoryItemId);
                uit.setUserId(userId);
                uit.setItemId(weaponItemId);
                uit.setEnhancementLevel(5);

                Item weapon = new Item();
                weapon.setId(weaponItemId);
                weapon.setCode("SWORD_01");
                weapon.setName("鐵劍");
                weapon.setType(ItemType.EQUIPMENT);
                weapon.setBaseAttack(200);
                weapon.setBaseDefense(200);

                Item stone = new Item();
                stone.setId(stoneItemId);
                stone.setCode("ENHANCE_STONE");
                stone.setName("強化石");

                UserInventoryItem stoneInv = new UserInventoryItem();
                stoneInv.setItemId(stoneItemId);
                stoneInv.setQuantity(10);

                when(inventoryRepo.findById(inventoryItemId)).thenReturn(Optional.of(uit));
                when(itemRepo.findById(weaponItemId)).thenReturn(Optional.of(weapon));
                when(goldService.getGold(userId)).thenReturn(1_000_000L);
                when(itemRepo.findByCode("ENHANCE_STONE")).thenReturn(Optional.of(stone));
                when(inventoryRepo.findFirstByUserIdAndItemIdAndEnhancementLevel(userId, stoneItemId, 0))
                                .thenReturn(Optional.of(stoneInv));
                when(goldService.changeGold(eq(userId), eq(-600L), eq("ENHANCE"), eq("SWORD_01"), anyString()))
                                .thenReturn(999_400L);
                InventoryResponse fakeInv = mock(InventoryResponse.class);
                when(inventoryService.listInventory(userId)).thenReturn(fakeInv);

                // 凍結隨機：0.99 >= 0.60 → 失敗
                ThreadLocalRandom fakeRandom = mock(ThreadLocalRandom.class);
                when(fakeRandom.nextDouble()).thenReturn(0.99);

                // Act
                try (MockedStatic<ThreadLocalRandom> mocked = mockStatic(ThreadLocalRandom.class)) {
                        mocked.when(ThreadLocalRandom::current).thenReturn(fakeRandom);

                        EnhanceResultResponse res = enhanceServiceImpl.enhance(userId, inventoryItemId);

                        // assert：失敗 + 降級
                        assertThat(res.isSuccess()).isFalse();
                        assertThat(res.isDestroyed()).isFalse();
                        assertThat(res.getPreviousLevel()).isEqualTo(5);
                        assertThat(res.getNewLevel()).isEqualTo(4);
                        assertThat(res.getGoldSpent()).isEqualTo(600L);
                        assertThat(res.getGoldBalance()).isEqualTo(999_400L);
                        assertThat(res.getMessage()).isEqualTo("強化失敗，等級下降為 +4");

                        // 武器等級真的被改寫 + save
                        assertThat(uit.getEnhancementLevel()).isEqualTo(4);
                        verify(inventoryRepo).save(uit);

                        // 武器沒被 delete、沒走到裝備格刪除
                        verify(inventoryRepo, never()).delete(uit);
                        verify(equipmentRepo, never()).delete(any(UserEquipment.class));
                        verify(equipmentRepo, never()).flush();

                        // 素材也扣了（10 - 5 = 5）
                        assertThat(stoneInv.getQuantity()).isEqualTo(5);
                        verify(inventoryRepo).save(stoneInv);
                }
        }

        // enhance: 失敗效果 DESTROY（fromLevel 7~9）
        @Test
        @DisplayName("enhance_case4: level 8 強化失敗 DESTROY（裝備格刪除 + 背包刪除 + destroyed=true）")
        void enhance_case4_fail_DESTROY() {
                // given
                Long userId = 100L;
                Long inventoryItemId = 101L;
                Long weaponItemId = 1L;
                Long stoneItemId = 201L;

                UserInventoryItem uit = new UserInventoryItem();
                uit.setId(inventoryItemId);
                uit.setUserId(userId);
                uit.setItemId(weaponItemId);
                uit.setEnhancementLevel(8);

                Item weapon = new Item();
                weapon.setId(weaponItemId);
                weapon.setCode("SWORD_01");
                weapon.setName("鐵劍");
                weapon.setType(ItemType.EQUIPMENT);
                weapon.setBaseAttack(200);
                weapon.setBaseDefense(200);

                Item stone = new Item();
                stone.setId(stoneItemId);
                stone.setCode("ENHANCE_STONE");
                stone.setName("強化石");

                UserInventoryItem stoneInv = new UserInventoryItem();
                stoneInv.setItemId(stoneItemId);
                stoneInv.setQuantity(10);

                // 已穿在裝備格上
                UserEquipment ue = new UserEquipment();
                ue.setUserId(userId);
                ue.setInventoryItemId(inventoryItemId);
                ue.setSlotIndex(1);

                when(inventoryRepo.findById(inventoryItemId)).thenReturn(Optional.of(uit));
                when(itemRepo.findById(weaponItemId)).thenReturn(Optional.of(weapon));
                when(goldService.getGold(userId)).thenReturn(1_000_000L);
                when(itemRepo.findByCode("ENHANCE_STONE")).thenReturn(Optional.of(stone));
                when(inventoryRepo.findFirstByUserIdAndItemIdAndEnhancementLevel(userId, stoneItemId, 0))
                                .thenReturn(Optional.of(stoneInv));
                when(goldService.changeGold(eq(userId), eq(-900L), eq("ENHANCE"), eq("SWORD_01"), anyString()))
                                .thenReturn(999_100L);
                when(equipmentRepo.findByUserIdAndInventoryItemId(userId, inventoryItemId))
                                .thenReturn(Optional.of(ue));
                InventoryResponse fakeInv = mock(InventoryResponse.class);
                when(inventoryService.listInventory(userId)).thenReturn(fakeInv);

                // 凍結隨機：0.99 >= 0.20 → 失敗
                ThreadLocalRandom fakeRandom = mock(ThreadLocalRandom.class);
                when(fakeRandom.nextDouble()).thenReturn(0.99);

                // Act
                try (MockedStatic<ThreadLocalRandom> mocked = mockStatic(ThreadLocalRandom.class)) {
                        mocked.when(ThreadLocalRandom::current).thenReturn(fakeRandom);

                        EnhanceResultResponse res = enhanceServiceImpl.enhance(userId, inventoryItemId);

                        // assert：失敗 + 銷毀
                        assertThat(res.isSuccess()).isFalse();
                        assertThat(res.isDestroyed()).isTrue();
                        assertThat(res.getPreviousLevel()).isEqualTo(8);
                        assertThat(res.getNewLevel()).isEqualTo(-1);
                        assertThat(res.getGoldSpent()).isEqualTo(900L);
                        assertThat(res.getGoldBalance()).isEqualTo(999_100L);
                        assertThat(res.getMessage()).isEqualTo("強化失敗，鐵劍 消失了…");

                        // 裝備格被清掉、flush
                        verify(equipmentRepo).delete(ue);
                        verify(equipmentRepo).flush();

                        // 背包列被刪除
                        verify(inventoryRepo).delete(uit);

                        // 不是 save（不該再保留一個 level 0 的 ghost）
                        verify(inventoryRepo, never()).save(uit);

                        // 素材一樣扣了（10 - 8 = 2）
                        assertThat(stoneInv.getQuantity()).isEqualTo(2);
                        verify(inventoryRepo).save(stoneInv);
                }
        }

        // enhance: 邊界 throw
        @Test
        @DisplayName("enhance_case5: 已達等級上限 → 拋 IllegalArgumentException、金幣與素材皆未動")
        void enhance_case5_atMaxLevel_throws() {
                // given
                Long userId = 100L;
                Long inventoryItemId = 101L;
                Long weaponItemId = 1L;

                UserInventoryItem uit = new UserInventoryItem();
                uit.setId(inventoryItemId);
                uit.setUserId(userId);
                uit.setItemId(weaponItemId);
                uit.setEnhancementLevel(10);

                Item weapon = new Item();
                weapon.setId(weaponItemId);
                weapon.setCode("SWORD_01");
                weapon.setName("鐵劍");
                weapon.setType(ItemType.EQUIPMENT);
                weapon.setBaseAttack(200);
                weapon.setBaseDefense(200);

                when(inventoryRepo.findById(inventoryItemId)).thenReturn(Optional.of(uit));
                when(itemRepo.findById(weaponItemId)).thenReturn(Optional.of(weapon));

                // Act + Assert
                assertThatThrownBy(() -> enhanceServiceImpl.enhance(userId, inventoryItemId))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("已達強化上限");

                // 任何金幣/素材/裝備動作都不該發生
                verify(goldService, never()).changeGold(anyLong(), anyLong(), anyString(), anyString(), anyString());
                verify(inventoryRepo, never()).save(any(UserInventoryItem.class));
                verify(inventoryRepo, never()).delete(any(UserInventoryItem.class));
                verify(equipmentRepo, never()).delete(any(UserEquipment.class));
        }

        @Test
        @DisplayName("enhance_case6: 金幣不足 → 拋 IllegalArgumentException、changeGold 未被呼叫")
        void enhance_case6_goldNotEnough_throws() {
                // given
                Long userId = 100L;
                Long inventoryItemId = 101L;
                Long weaponItemId = 1L;

                UserInventoryItem uit = new UserInventoryItem();
                uit.setId(inventoryItemId);
                uit.setUserId(userId);
                uit.setItemId(weaponItemId);
                uit.setEnhancementLevel(3); // 需要 400 金幣

                Item weapon = new Item();
                weapon.setId(weaponItemId);
                weapon.setCode("SWORD_01");
                weapon.setName("鐵劍");
                weapon.setType(ItemType.EQUIPMENT);
                weapon.setBaseAttack(200);
                weapon.setBaseDefense(200);

                when(inventoryRepo.findById(inventoryItemId)).thenReturn(Optional.of(uit));
                when(itemRepo.findById(weaponItemId)).thenReturn(Optional.of(weapon));
                when(goldService.getGold(userId)).thenReturn(100L); // 不夠

                // Act + Assert
                assertThatThrownBy(() -> enhanceServiceImpl.enhance(userId, inventoryItemId))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("金幣不足");

                verify(goldService, never()).changeGold(anyLong(), anyLong(), anyString(), anyString(), anyString());
                verify(inventoryRepo, never()).save(any(UserInventoryItem.class));
                verify(inventoryRepo, never()).delete(any(UserInventoryItem.class));
        }

        @Test
        @DisplayName("enhance_case7: 素材不足 → 拋 IllegalArgumentException、金幣未被扣")
        void enhance_case7_materialNotEnough_throws() {
                // given
                Long userId = 100L;
                Long inventoryItemId = 101L;
                Long weaponItemId = 1L;
                Long stoneItemId = 201L;

                UserInventoryItem uit = new UserInventoryItem();
                uit.setId(inventoryItemId);
                uit.setUserId(userId);
                uit.setItemId(weaponItemId);
                uit.setEnhancementLevel(3); // 需要 3 顆素材

                Item weapon = new Item();
                weapon.setId(weaponItemId);
                weapon.setCode("SWORD_01");
                weapon.setName("鐵劍");
                weapon.setType(ItemType.EQUIPMENT);
                weapon.setBaseAttack(200);
                weapon.setBaseDefense(200);

                Item stone = new Item();
                stone.setId(stoneItemId);
                stone.setCode("ENHANCE_STONE");
                stone.setName("強化石");

                UserInventoryItem stoneInv = new UserInventoryItem();
                stoneInv.setItemId(stoneItemId);
                stoneInv.setQuantity(1); // 只剩 1，不夠 3

                when(inventoryRepo.findById(inventoryItemId)).thenReturn(Optional.of(uit));
                when(itemRepo.findById(weaponItemId)).thenReturn(Optional.of(weapon));
                when(goldService.getGold(userId)).thenReturn(1_000_000L);
                when(itemRepo.findByCode("ENHANCE_STONE")).thenReturn(Optional.of(stone));
                when(inventoryRepo.findFirstByUserIdAndItemIdAndEnhancementLevel(userId, stoneItemId, 0))
                                .thenReturn(Optional.of(stoneInv));

                // Act + Assert
                assertThatThrownBy(() -> enhanceServiceImpl.enhance(userId, inventoryItemId))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("強化石不足");

                // 金幣完全沒被動到
                verify(goldService, never()).changeGold(anyLong(), anyLong(), anyString(), anyString(), anyString());

                // 素材數量沒被改、也沒 save / delete
                assertThat(stoneInv.getQuantity()).isEqualTo(1);
                verify(inventoryRepo, never()).save(any(UserInventoryItem.class));
                verify(inventoryRepo, never()).delete(any(UserInventoryItem.class));
        }

}
