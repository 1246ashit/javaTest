package com.example.demo.service.impl;

import com.example.demo.entity.Item;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.UserInventoryItemRepository;
import com.example.demo.service.GoldService;
import com.example.demo.service.impl.ShopServiceImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    //buy
    // case1 itemId=null 拋錯
    @Test
    @DisplayName("case1: itemId=null 應拋 IllegalArgumentException")
    void buy_itemIdIsNull_throws() {
        assertThatThrownBy(() -> shopService.buy(1L, null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("itemId 與 quantity(>0) 為必填");
    }

    // case2  quantity=0 拋錯
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

    
}