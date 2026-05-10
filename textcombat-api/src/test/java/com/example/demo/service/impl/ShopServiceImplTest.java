package com.example.demo.service.impl;

import com.example.demo.dto.ShopBuyResponse;
import com.example.demo.dto.ShopItemDTO;
import com.example.demo.entity.Item;
import com.example.demo.entity.ItemType;
import com.example.demo.entity.UserInventoryItem;
import com.example.demo.entity.UsersEntity;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.UserInventoryItemRepository;
import com.example.demo.service.GoldService;
import com.example.demo.service.impl.ShopServiceImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ShopServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserInventoryItemRepository inventoryRepository;

    @Mock
    private GoldService goldService; // ← 注意：mock 一個 service，不是 repository

    @InjectMocks
    private ShopServiceImpl shopService;

    ///// buy
    // case1 itemId=null 拋錯
    @Test
    @DisplayName("case1: itemId=null 應拋 IllegalArgumentException")
    void buy_itemIdIsNull_throws() {
        assertThatThrownBy(() -> shopService.buy(1L, null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("itemId 與 quantity(>0) 為必填");
    }

    // case2 quantity=0 拋錯
    @Test
    @DisplayName("case2: quantity=0 應拋 IllegalArgumentException")
    void buy_quantityIs0_throws() {
        assertThatThrownBy(() -> shopService.buy(1L, 1L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("itemId 與 quantity(>0) 為必填");
    }

    // case3 找不到itemId時
    @Test
    @DisplayName("case3: 找不到itemId時 應拋 IllegalArgumentException")
    void buy_ItemNotFound_throws() {

        when(itemRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shopService.buy(1L, 100L, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("物品不存在");
    }

    // case4 沒有標價的物品不能被賣
    @Test
    @DisplayName("case4: 沒有標價的物品不能被賣")
    void buy_youCantBuyItemWitchHaveNoPrice_throws() {
        Item item = new Item();
        item.setPrice(null);

        when(itemRepository.findById(100L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> shopService.buy(1L, 100L, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("此物品不販售");
    }

    // case5 裝備類一次只能買 1 個（每件都要獨立占格）
    @Test
    @DisplayName("case5: 裝備類一次只能買 1 個")
    void buy_equipmentItemOnlyBuyOneEveryTime_throws() {
        Item item = new Item();
        item.setPrice(100L);
        item.setType(ItemType.EQUIPMENT);

        when(itemRepository.findById(100L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> shopService.buy(1L, 100L, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("裝備只能一次購買 1 件");
    }

    // case6 檢查單筆總堆疊不超過 maxStack（買消耗品時看合併後量）
    @Test
    @DisplayName("case6: 不能買數量超過 maxStack的量")
    void buy_theItemYouBuythatAmountOfItCantOverMaxStackInOnce_throws() {
        Item item = new Item();
        item.setPrice(100L); // 補：通過 price 檢查
        item.setType(ItemType.CONSUMABLE); // 補：明確不是裝備（非必要但比較清楚）
        item.setMaxStack(20);

        when(itemRepository.findById(100L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> shopService.buy(1L, 100L, 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("超過單筆堆疊上限"); // 用 contains 比較穩
    }

    // case7 全過程確認-買裝備
    @Test
    @DisplayName("case7: 全過程確認-買裝備")
    void buy_FullPassCase_EQUIPMENT() {

        Item item = new Item();
        item.setPrice(100L);
        item.setType(ItemType.EQUIPMENT);
        item.setMaxStack(20);
        item.setCode("EQUIP_001");
        item.setName("鐵劍");

        when(itemRepository.findById(100L)).thenReturn(Optional.of(item));

        when(goldService.changeGold(1L, -100L, "SHOP_BUY", "EQUIP_001", "購買 鐵劍 x1"))
                .thenReturn(900L);

        // === Act (執行) ===
        ShopBuyResponse resp = shopService.buy(1L, 100L, 1);

        // === Assert (驗證行為與結果) ===

        assertThat(resp.getGoldBalance()).isEqualTo(900L);

        verify(goldService).changeGold(1L, -100L, "SHOP_BUY", "EQUIP_001", "購買 鐵劍 x1");

        ArgumentCaptor<UserInventoryItem> captor = ArgumentCaptor.forClass(UserInventoryItem.class);
        verify(inventoryRepository).save(captor.capture());
        UserInventoryItem saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getItemId()).isEqualTo(item.getId());
        assertThat(saved.getQuantity()).isEqualTo(1); // 裝備 = 1

    }

    // case8 全過程確認-買藥水
    @Test
    @DisplayName("case8: 全過程確認-買消耗品")
    void buy_FullPassCase_CONSUMABLE() {

        Item item = new Item();
        item.setPrice(20L);
        item.setType(ItemType.CONSUMABLE);
        item.setMaxStack(20);
        item.setCode("HEAL_001");
        item.setName("治療藥水");

        when(itemRepository.findById(101L)).thenReturn(Optional.of(item));

        when(goldService.changeGold(1L, -100L, "SHOP_BUY", "HEAL_001", "購買 治療藥水 x5"))
                .thenReturn(900L);

        when(inventoryRepository.findFirstByUserIdAndItemIdAndEnhancementLevel(1L, item.getId(), 0))
                .thenReturn(Optional.empty());

        // === Act (執行) ===
        ShopBuyResponse resp = shopService.buy(1L, 101L, 5);

        // === Assert (驗證行為與結果) ===

        assertThat(resp.getGoldBalance()).isEqualTo(900L);

        verify(goldService).changeGold(1L, -100L, "SHOP_BUY", "HEAL_001", "購買 治療藥水 x5");

        ArgumentCaptor<UserInventoryItem> captor = ArgumentCaptor.forClass(UserInventoryItem.class);
        verify(inventoryRepository).save(captor.capture());
        UserInventoryItem saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getItemId()).isEqualTo(item.getId());
        int existingQty = 0; // 首次購買，背包原本沒有
        int buyQty = 5;
        assertThat(saved.getQuantity()).isEqualTo(existingQty + buyQty);

    }

    // case9 — 買消耗品（背包已有 → 合併堆疊）
    @Test
    @DisplayName("case9: 買消耗品（背包已有 → 合併堆疊")
    void buy_FullPassCase_CONSUMABLE_butYouAlreadyHave() {

        Item item = new Item();
        item.setPrice(20L);
        item.setType(ItemType.CONSUMABLE);
        item.setMaxStack(999);
        item.setCode("HEAL_001");
        item.setName("治療藥水");
        item.setId(101L);

        UserInventoryItem userInventoryItem = new UserInventoryItem();
        userInventoryItem.setUserId(1L);
        userInventoryItem.setItemId(101L);
        userInventoryItem.setEnhancementLevel(0);
        userInventoryItem.setQuantity(20);

        when(itemRepository.findById(101L)).thenReturn(Optional.of(item));

        when(goldService.changeGold(1L, -100L, "SHOP_BUY", "HEAL_001", "購買 治療藥水 x5"))
                .thenReturn(900L);

        when(inventoryRepository.findFirstByUserIdAndItemIdAndEnhancementLevel(1L, item.getId(), 0))
                .thenReturn(Optional.of(userInventoryItem));

        // === Act (執行) ===
        ShopBuyResponse resp = shopService.buy(1L, 101L, 5);

        // === Assert (驗證行為與結果) ===

        assertThat(resp.getGoldBalance()).isEqualTo(900L);

        verify(goldService).changeGold(1L, -100L, "SHOP_BUY", "HEAL_001", "購買 治療藥水 x5");

        ArgumentCaptor<UserInventoryItem> captor = ArgumentCaptor.forClass(UserInventoryItem.class);
        verify(inventoryRepository).save(captor.capture());
        UserInventoryItem saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getItemId()).isEqualTo(item.getId());
        int existingQty = 20;
        int buyQty = 5;
        assertThat(saved.getQuantity()).isEqualTo(existingQty + buyQty);

        assertThat(saved).isSameAs(userInventoryItem);

    }

    // case10：合併後超過 maxStack → 拋 IllegalStateException
    @Test
    @DisplayName("case10: 不能買數量超過 你背包的限量(maxStack)")
    void buy_YouCantBuyOverTheMaxStack_throws() {
        Item item = new Item();
        item.setId(101L);
        item.setPrice(20L);
        item.setType(ItemType.CONSUMABLE);
        item.setMaxStack(20);
        item.setCode("HEAL_001");
        item.setName("治療藥水");

        UserInventoryItem userInventoryItem = new UserInventoryItem();
        userInventoryItem.setUserId(1L);
        userInventoryItem.setItemId(101L);
        userInventoryItem.setEnhancementLevel(0);
        userInventoryItem.setQuantity(18);

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        when(goldService.changeGold(1L, -100L, "SHOP_BUY", "HEAL_001", "購買 治療藥水 x5"))
                .thenReturn(900L);

        when(inventoryRepository.findFirstByUserIdAndItemIdAndEnhancementLevel(1L, item.getId(), 0))
                .thenReturn(Optional.of(userInventoryItem));

        // === Act (執行) ===
        // === Assert (驗證行為與結果) ===
        int quantity = 5;
        assertThatThrownBy(() -> shopService.buy(1L, item.getId(), quantity))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("合併後數量");

        verify(inventoryRepository, never()).save(any());

    }

    // case11：餘額不足
    @Test
    @DisplayName("case11：餘額不足")
    void buy_YouHaveNoMoney_throws() {
        Item item = new Item();
        item.setId(101L);
        item.setPrice(20L);
        item.setType(ItemType.CONSUMABLE);
        item.setMaxStack(20);
        item.setCode("HEAL_001");
        item.setName("治療藥水");

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        when(goldService.changeGold(1L, -100L, "SHOP_BUY", "HEAL_001", "購買 治療藥水 x5"))
                .thenThrow(new IllegalStateException("金幣不足"));

        // === Act (執行) ===
        // === Assert (驗證行為與結果) ===
        int quantity = 5;
        assertThatThrownBy(() -> shopService.buy(1L, item.getId(), quantity))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("金幣不足");

        verify(inventoryRepository, never()).save(any());

    }
    /////

    //// listShopItems
    @Test
    @DisplayName("case1: 回傳對應數量的 ShopItemDTO 並驗證欄位 mapping")
    void listShopItems_ShouldHaveListReturn() {
        List<Item> itemList = new ArrayList<>();

        Item item1 = new Item();
        item1.setId(101L);
        item1.setCode("HEAL_001");
        item1.setName("治療藥水");
        item1.setPrice(20L);
        itemList.add(item1);

        Item item2 = new Item();
        item2.setId(102L);
        item2.setCode("EQUIP_001");
        item2.setName("鐵劍");
        item2.setPrice(100L);
        itemList.add(item2);

        when(itemRepository.findByPriceIsNotNullOrderByPriceAsc())
                .thenReturn(itemList);

        List<ShopItemDTO> resp = shopService.listShopItems();

        assertThat(resp).hasSize(2); // 數量對

        assertThat(resp)
                .extracting("itemId", "itemCode", "itemName", "price")
                .containsExactly(
                        tuple(101L, "HEAL_001", "治療藥水", 20L),
                        tuple(102L, "EQUIP_001", "鐵劍", 100L));
    }

    @Test
    @DisplayName("case2：repository 回空 list 時，service 也回空 list（不會 NPE）")
    void listShopItems_EmptyList_returnsEmpty() {
        when(itemRepository.findByPriceIsNotNullOrderByPriceAsc())
                .thenReturn(Collections.emptyList());

        List<ShopItemDTO> resp = shopService.listShopItems();

        assertThat(resp).isEmpty(); // 不是 null、不是 NPE、就是 0 筆
    }

}