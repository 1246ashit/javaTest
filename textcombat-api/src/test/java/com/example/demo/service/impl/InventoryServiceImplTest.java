package com.example.demo.service.impl;

import com.example.demo.dto.InventoryResponse;
import com.example.demo.entity.Item;
import com.example.demo.entity.ItemType;
import com.example.demo.entity.UserEquipment;
import com.example.demo.entity.UserInventoryItem;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.UserEquipmentRepository;
import com.example.demo.repository.UserInventoryItemRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

        @Mock
        private UserEquipmentRepository equipmentRepo;

        @Mock
        private UserInventoryItemRepository inventoryRepository;

        @Mock
        private ItemRepository itemRepo;

        @InjectMocks
        private InventoryServiceImpl inventoryService;

        // discard
        @Test
        @DisplayName("discard_case1: 成功丟棄時，資料庫要有儲存到")
        void discard_DiscardSuccessAndSqlRecordSuccess() {
                // when
                UserInventoryItem inv1 = new UserInventoryItem();
                inv1.setId(500L);
                inv1.setUserId(10L);
                inv1.setItemId(101L);
                inv1.setQuantity(20);

                when(inventoryRepository.findById(inv1.getId()))
                                .thenReturn(Optional.of(inv1));

                when(equipmentRepo.findByUserIdAndInventoryItemId(inv1.getUserId(), inv1.getId()))
                                .thenReturn(Optional.empty());

                // act
                inventoryService.discard(10L, 500L, 1);

                // assert
                ArgumentCaptor<UserInventoryItem> captor = ArgumentCaptor.forClass(UserInventoryItem.class);
                verify(inventoryRepository).save(captor.capture());
                UserInventoryItem saved = captor.getValue();
                assertThat(saved.getUserId()).isEqualTo(10L);
                assertThat(saved.getItemId()).isEqualTo(101L);
                int holding = 20;
                int discardQty = 1;
                assertThat(saved.getQuantity()).isEqualTo(holding-discardQty);

                verify(inventoryRepository, never()).delete(any());// 「部分丟棄」應該走 save 不走 delete
        }

        // ============================================================
        //  listInventory
        // ============================================================

        @Test
        @DisplayName("listInventory_case1: 兩件裝備、一份藥水；總攻防只算裝備、藥水排除")
        void listInventory_case1_happyPath() {
                // given
                Long userId = 10L;

                // 武器 +2、防具 +0、藥水
                UserInventoryItem invWeapon = new UserInventoryItem();
                invWeapon.setId(501L);
                invWeapon.setUserId(userId);
                invWeapon.setItemId(1L);
                invWeapon.setQuantity(1);
                invWeapon.setEnhancementLevel(2);

                UserInventoryItem invArmor = new UserInventoryItem();
                invArmor.setId(502L);
                invArmor.setUserId(userId);
                invArmor.setItemId(2L);
                invArmor.setQuantity(1);
                invArmor.setEnhancementLevel(0);

                UserInventoryItem invPotion = new UserInventoryItem();
                invPotion.setId(503L);
                invPotion.setUserId(userId);
                invPotion.setItemId(3L);
                invPotion.setQuantity(5);

                Item weapon = new Item();
                weapon.setId(1L);
                weapon.setCode("SWORD_01");
                weapon.setName("鐵劍");
                weapon.setType(ItemType.EQUIPMENT);
                weapon.setBaseAttack(100);
                weapon.setBaseDefense(0);

                Item armor = new Item();
                armor.setId(2L);
                armor.setCode("ARMOR_01");
                armor.setName("鐵甲");
                armor.setType(ItemType.EQUIPMENT);
                armor.setBaseAttack(0);
                armor.setBaseDefense(50);

                Item potion = new Item();
                potion.setId(3L);
                potion.setCode("POTION_01");
                potion.setName("藥水");
                potion.setType(ItemType.CONSUMABLE);
                potion.setBaseAttack(999); // 故意給數字，驗證消耗品不被計入
                potion.setBaseDefense(999);

                // 武器穿在 1 號格、防具在 2 號格、藥水沒穿
                UserEquipment eqWeapon = new UserEquipment();
                eqWeapon.setUserId(userId);
                eqWeapon.setSlotIndex(1);
                eqWeapon.setInventoryItemId(501L);

                UserEquipment eqArmor = new UserEquipment();
                eqArmor.setUserId(userId);
                eqArmor.setSlotIndex(2);
                eqArmor.setInventoryItemId(502L);

                when(inventoryRepository.findByUserId(userId))
                                .thenReturn(List.of(invWeapon, invArmor, invPotion));
                when(equipmentRepo.findByUserId(userId))
                                .thenReturn(List.of(eqWeapon, eqArmor));
                when(itemRepo.findAllById(any()))
                                .thenReturn(List.of(weapon, armor, potion));

                // Act
                InventoryResponse res = inventoryService.listInventory(userId);

                // assert：總攻防 = 武器 100*1.2 + 防具 50*1 ＝ attack 120 / defense 50
                assertThat(res.getTotalAttack()).isEqualTo(120);
                assertThat(res.getTotalDefense()).isEqualTo(50);

                // slots 1~9 都有 key（值可為 null），但只有 1、2 有東西
                assertThat(res.getSlots()).hasSize(9);
                assertThat(res.getSlots().get(1).getItemCode()).isEqualTo("SWORD_01");
                assertThat(res.getSlots().get(2).getItemCode()).isEqualTo("ARMOR_01");
                assertThat(res.getSlots().get(3)).isNull();

                // items 列表三筆都在
                assertThat(res.getItems()).hasSize(3);
        }

        // ============================================================
        //  equip
        // ============================================================

        @Test
        @DisplayName("equip_case1: inventoryItemId = null → IllegalArgumentException")
        void equip_case1_invIdNull_throws() {
                assertThatThrownBy(() -> inventoryService.equip(10L, null, 1))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("inventoryItemId 為必填");

                verify(equipmentRepo, never()).save(any());
        }

        @Test
        @DisplayName("equip_case2: 背包物品不存在 → IllegalArgumentException")
        void equip_case2_itemNotFound_throws() {
                when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> inventoryService.equip(10L, 999L, 1))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("背包物品不存在");
        }

        @Test
        @DisplayName("equip_case3: 不是自己的物品 → IllegalStateException")
        void equip_case3_notOwner_throws() {
                UserInventoryItem inv = new UserInventoryItem();
                inv.setId(500L);
                inv.setUserId(99L); // 屬於別人
                when(inventoryRepository.findById(500L)).thenReturn(Optional.of(inv));

                assertThatThrownBy(() -> inventoryService.equip(10L, 500L, 1))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessage("不是你的物品");

                verify(equipmentRepo, never()).save(any());
        }

        @Test
        @DisplayName("equip_case4: slotIndex 超出 1~9 → IllegalArgumentException")
        void equip_case4_slotOutOfRange_throws() {
                UserInventoryItem inv = new UserInventoryItem();
                inv.setId(500L);
                inv.setUserId(10L);
                when(inventoryRepository.findById(500L)).thenReturn(Optional.of(inv));
                when(equipmentRepo.findByUserIdAndInventoryItemId(10L, 500L))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> inventoryService.equip(10L, 500L, 10))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("slotIndex 必須在");

                verify(equipmentRepo, never()).save(any());
        }

        @Test
        @DisplayName("equip_case5: 已經在目標格 → no-op、不該再 save")
        void equip_case5_alreadyInTargetSlot_noop() {
                UserInventoryItem inv = new UserInventoryItem();
                inv.setId(500L);
                inv.setUserId(10L);
                when(inventoryRepository.findById(500L)).thenReturn(Optional.of(inv));

                UserEquipment existing = new UserEquipment();
                existing.setUserId(10L);
                existing.setSlotIndex(3);
                existing.setInventoryItemId(500L);
                when(equipmentRepo.findByUserIdAndInventoryItemId(10L, 500L))
                                .thenReturn(Optional.of(existing));

                inventoryService.equip(10L, 500L, 3);

                verify(equipmentRepo, never()).save(any());
                verify(equipmentRepo, never()).delete(any());
                verify(equipmentRepo, never()).flush();
        }

        @Test
        @DisplayName("equip_case6: 目標格已有別的東西 → 舊的被踢、新的塞進去")
        void equip_case6_targetSlotOccupied_kicksOld() {
                UserInventoryItem inv = new UserInventoryItem();
                inv.setId(500L);
                inv.setUserId(10L);
                when(inventoryRepository.findById(500L)).thenReturn(Optional.of(inv));
                when(equipmentRepo.findByUserIdAndInventoryItemId(10L, 500L))
                                .thenReturn(Optional.empty()); // 此物件原本沒裝

                UserEquipment old = new UserEquipment();
                old.setUserId(10L);
                old.setSlotIndex(3);
                old.setInventoryItemId(888L); // 目標格現在有別的
                when(equipmentRepo.findByUserIdAndSlotIndex(10L, 3))
                                .thenReturn(Optional.of(old));

                inventoryService.equip(10L, 500L, 3);

                verify(equipmentRepo).delete(old); // 舊的被刪
                verify(equipmentRepo).flush();

                ArgumentCaptor<UserEquipment> cap = ArgumentCaptor.forClass(UserEquipment.class);
                verify(equipmentRepo).save(cap.capture());
                UserEquipment saved = cap.getValue();
                assertThat(saved.getUserId()).isEqualTo(10L);
                assertThat(saved.getSlotIndex()).isEqualTo(3);
                assertThat(saved.getInventoryItemId()).isEqualTo(500L);
        }

        @Test
        @DisplayName("equip_case7: 物件原本在別格 → 舊格刪除、新格塞入")
        void equip_case7_movesFromOldSlot() {
                UserInventoryItem inv = new UserInventoryItem();
                inv.setId(500L);
                inv.setUserId(10L);
                when(inventoryRepository.findById(500L)).thenReturn(Optional.of(inv));

                UserEquipment existingForInv = new UserEquipment();
                existingForInv.setUserId(10L);
                existingForInv.setSlotIndex(2);
                existingForInv.setInventoryItemId(500L);
                when(equipmentRepo.findByUserIdAndInventoryItemId(10L, 500L))
                                .thenReturn(Optional.of(existingForInv));

                when(equipmentRepo.findByUserIdAndSlotIndex(10L, 5))
                                .thenReturn(Optional.empty()); // 目標格是空的

                inventoryService.equip(10L, 500L, 5);

                verify(equipmentRepo).delete(existingForInv); // 從舊格移除
                ArgumentCaptor<UserEquipment> cap = ArgumentCaptor.forClass(UserEquipment.class);
                verify(equipmentRepo).save(cap.capture());
                assertThat(cap.getValue().getSlotIndex()).isEqualTo(5);
        }

        @Test
        @DisplayName("equip_case8: slotIndex = null → 自動找第一個空格")
        void equip_case8_autoFindEmptySlot() {
                UserInventoryItem inv = new UserInventoryItem();
                inv.setId(500L);
                inv.setUserId(10L);
                when(inventoryRepository.findById(500L)).thenReturn(Optional.of(inv));
                when(equipmentRepo.findByUserIdAndInventoryItemId(10L, 500L))
                                .thenReturn(Optional.empty());

                // 1、2 已占用 → 第一個空的是 3
                UserEquipment u1 = new UserEquipment();
                u1.setSlotIndex(1);
                UserEquipment u2 = new UserEquipment();
                u2.setSlotIndex(2);
                when(equipmentRepo.findByUserId(10L)).thenReturn(List.of(u1, u2));
                when(equipmentRepo.findByUserIdAndSlotIndex(10L, 3))
                                .thenReturn(Optional.empty());

                inventoryService.equip(10L, 500L, null);

                ArgumentCaptor<UserEquipment> cap = ArgumentCaptor.forClass(UserEquipment.class);
                verify(equipmentRepo).save(cap.capture());
                assertThat(cap.getValue().getSlotIndex()).isEqualTo(3);
        }

        // ============================================================
        //  unequip
        // ============================================================

        @Test
        @DisplayName("unequip_case1: slotIndex = null → IllegalArgumentException")
        void unequip_case1_slotNull_throws() {
                assertThatThrownBy(() -> inventoryService.unequip(10L, null))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("slotIndex 為必填");
        }

        @Test
        @DisplayName("unequip_case2: 該格本來就空 → 不丟例外、也不該呼叫 delete")
        void unequip_case2_slotEmpty_noop() {
                when(equipmentRepo.findByUserIdAndSlotIndex(10L, 5))
                                .thenReturn(Optional.empty());

                inventoryService.unequip(10L, 5);

                verify(equipmentRepo, never()).delete(any());
        }

        @Test
        @DisplayName("unequip_case3: 該格有裝備 → delete 被呼叫")
        void unequip_case3_slotOccupied_deletes() {
                UserEquipment ue = new UserEquipment();
                ue.setUserId(10L);
                ue.setSlotIndex(5);
                ue.setInventoryItemId(500L);
                when(equipmentRepo.findByUserIdAndSlotIndex(10L, 5))
                                .thenReturn(Optional.of(ue));

                inventoryService.unequip(10L, 5);

                verify(equipmentRepo).delete(ue);
        }

        // ============================================================
        //  useSlot
        // ============================================================

        @Test
        @DisplayName("useSlot_case1: slotIndex = null → IllegalArgumentException")
        void useSlot_case1_slotNull_throws() {
                assertThatThrownBy(() -> inventoryService.useSlot(10L, null))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("slotIndex 為必填");
        }

        @Test
        @DisplayName("useSlot_case2: 該格是空的 → IllegalArgumentException")
        void useSlot_case2_slotEmpty_throws() {
                when(equipmentRepo.findByUserIdAndSlotIndex(10L, 1))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> inventoryService.useSlot(10L, 1))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("該格是空的");

                verify(inventoryRepository, never()).save(any());
                verify(inventoryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("useSlot_case3: 該格放的不是消耗品 → IllegalArgumentException")
        void useSlot_case3_notConsumable_throws() {
                UserEquipment ue = new UserEquipment();
                ue.setInventoryItemId(500L);
                when(equipmentRepo.findByUserIdAndSlotIndex(10L, 1))
                                .thenReturn(Optional.of(ue));

                UserInventoryItem inv = new UserInventoryItem();
                inv.setId(500L);
                inv.setItemId(1L);
                inv.setQuantity(1);
                when(inventoryRepository.findById(500L)).thenReturn(Optional.of(inv));

                Item weapon = new Item();
                weapon.setId(1L);
                weapon.setType(ItemType.EQUIPMENT);
                when(itemRepo.findById(1L)).thenReturn(Optional.of(weapon));

                assertThatThrownBy(() -> inventoryService.useSlot(10L, 1))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("此格的物品不能使用（只有消耗品能使用）");

                verify(inventoryRepository, never()).save(any());
                verify(inventoryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("useSlot_case4: 數量 > 1 → 數量 -1 並 save，不刪")
        void useSlot_case4_decrement() {
                UserEquipment ue = new UserEquipment();
                ue.setInventoryItemId(500L);
                when(equipmentRepo.findByUserIdAndSlotIndex(10L, 1))
                                .thenReturn(Optional.of(ue));

                UserInventoryItem inv = new UserInventoryItem();
                inv.setId(500L);
                inv.setItemId(3L);
                inv.setQuantity(5);
                when(inventoryRepository.findById(500L)).thenReturn(Optional.of(inv));

                Item potion = new Item();
                potion.setId(3L);
                potion.setCode("POTION_01");
                potion.setName("藥水");
                potion.setType(ItemType.CONSUMABLE);
                when(itemRepo.findById(3L)).thenReturn(Optional.of(potion));

                String msg = inventoryService.useSlot(10L, 1);

                assertThat(msg).isEqualTo("已使用 藥水");
                assertThat(inv.getQuantity()).isEqualTo(4);
                verify(inventoryRepository).save(inv);
                verify(inventoryRepository, never()).delete(any());
                verify(equipmentRepo, never()).delete(any());
        }

        @Test
        @DisplayName("useSlot_case5: 數量 = 1 → 用完即清裝備格 + 刪背包列")
        void useSlot_case5_lastOne_deletesBoth() {
                UserEquipment ue = new UserEquipment();
                ue.setInventoryItemId(500L);
                when(equipmentRepo.findByUserIdAndSlotIndex(10L, 1))
                                .thenReturn(Optional.of(ue));

                UserInventoryItem inv = new UserInventoryItem();
                inv.setId(500L);
                inv.setItemId(3L);
                inv.setQuantity(1);
                when(inventoryRepository.findById(500L)).thenReturn(Optional.of(inv));

                Item potion = new Item();
                potion.setId(3L);
                potion.setCode("POTION_01");
                potion.setName("藥水");
                potion.setType(ItemType.CONSUMABLE);
                when(itemRepo.findById(3L)).thenReturn(Optional.of(potion));

                inventoryService.useSlot(10L, 1);

                verify(equipmentRepo).delete(ue);
                verify(inventoryRepository).delete(inv);
                verify(inventoryRepository, never()).save(any());
        }

        // ============================================================
        //  discard（追加錯誤分支）
        // ============================================================

        @Test
        @DisplayName("discard_case2: inventoryItemId = null → IllegalArgumentException")
        void discard_case2_invIdNull_throws() {
                assertThatThrownBy(() -> inventoryService.discard(10L, null, 1))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("inventoryItemId 與 quantity");
        }

        @Test
        @DisplayName("discard_case3: quantity = 0 → IllegalArgumentException")
        void discard_case3_qtyZero_throws() {
                assertThatThrownBy(() -> inventoryService.discard(10L, 500L, 0))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("quantity(>0)");
        }

        @Test
        @DisplayName("discard_case4: 背包物品不存在 → IllegalArgumentException")
        void discard_case4_itemNotFound_throws() {
                when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> inventoryService.discard(10L, 999L, 1))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("背包物品不存在");
        }

        @Test
        @DisplayName("discard_case5: 不是自己的物品 → IllegalStateException")
        void discard_case5_notOwner_throws() {
                UserInventoryItem inv = new UserInventoryItem();
                inv.setId(500L);
                inv.setUserId(99L);
                inv.setQuantity(10);
                when(inventoryRepository.findById(500L)).thenReturn(Optional.of(inv));

                assertThatThrownBy(() -> inventoryService.discard(10L, 500L, 1))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessage("不是你的物品");
        }

        @Test
        @DisplayName("discard_case6: 丟棄數量超過持有 → IllegalArgumentException")
        void discard_case6_qtyExceedsHolding_throws() {
                UserInventoryItem inv = new UserInventoryItem();
                inv.setId(500L);
                inv.setUserId(10L);
                inv.setQuantity(3);
                when(inventoryRepository.findById(500L)).thenReturn(Optional.of(inv));

                assertThatThrownBy(() -> inventoryService.discard(10L, 500L, 5))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("丟棄數量大於持有數量");
        }

        @Test
        @DisplayName("discard_case7: 物品正在裝備中 → IllegalStateException、不該刪也不該 save")
        void discard_case7_itemEquipped_throws() {
                UserInventoryItem inv = new UserInventoryItem();
                inv.setId(500L);
                inv.setUserId(10L);
                inv.setQuantity(1);
                when(inventoryRepository.findById(500L)).thenReturn(Optional.of(inv));

                UserEquipment ue = new UserEquipment();
                ue.setUserId(10L);
                ue.setInventoryItemId(500L);
                when(equipmentRepo.findByUserIdAndInventoryItemId(10L, 500L))
                                .thenReturn(Optional.of(ue));

                assertThatThrownBy(() -> inventoryService.discard(10L, 500L, 1))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("裝備中");

                verify(inventoryRepository, never()).delete(any());
                verify(inventoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("discard_case8: 全部丟棄（qty == holding）→ inventoryRepo.delete 被呼叫")
        void discard_case8_fullDiscard_deletes() {
                UserInventoryItem inv = new UserInventoryItem();
                inv.setId(500L);
                inv.setUserId(10L);
                inv.setQuantity(3);
                when(inventoryRepository.findById(500L)).thenReturn(Optional.of(inv));
                when(equipmentRepo.findByUserIdAndInventoryItemId(10L, 500L))
                                .thenReturn(Optional.empty());

                inventoryService.discard(10L, 500L, 3);

                verify(inventoryRepository).delete(inv);
                verify(inventoryRepository, never()).save(any());
        }

}