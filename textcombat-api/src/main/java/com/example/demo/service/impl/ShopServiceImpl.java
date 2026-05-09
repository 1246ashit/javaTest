package com.example.demo.service.impl;

import com.example.demo.dto.ShopBuyResponse;
import com.example.demo.dto.ShopItemDTO;
import com.example.demo.entity.Item;
import com.example.demo.entity.ItemType;
import com.example.demo.entity.UserInventoryItem;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.UserInventoryItemRepository;
import com.example.demo.service.GoldService;
import com.example.demo.service.ShopService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ShopServiceImpl implements ShopService {

    private static final Logger log = LoggerFactory.getLogger(ShopServiceImpl.class);

    private final ItemRepository itemRepository;
    private final UserInventoryItemRepository inventoryRepository;
    private final GoldService goldService;

    public ShopServiceImpl(ItemRepository itemRepository,
            UserInventoryItemRepository inventoryRepository,
            GoldService goldService) {
        this.itemRepository = itemRepository;
        this.inventoryRepository = inventoryRepository;
        this.goldService = goldService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopItemDTO> listShopItems() {
        return itemRepository.findByPriceIsNotNullOrderByPriceAsc().stream()
                .map(ShopItemDTO::from)
                .toList();
    }

    /**
     * 購買物品：原子扣金幣 + 放進背包 + 寫交易紀錄
     */
    @Override
    @Transactional
    public ShopBuyResponse buy(Long userId, Long itemId, int quantity) {
        if (itemId == null || quantity <= 0) {
            throw new IllegalArgumentException("itemId 與 quantity(>0) 為必填");
        }

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("物品不存在"));

        if (item.getPrice() == null) {
            throw new IllegalArgumentException("此物品不販售");
        }

        // 裝備類一次只能買 1 個（每件都要獨立占格）
        if (item.getType() == ItemType.EQUIPMENT && quantity != 1) {
            throw new IllegalArgumentException("裝備只能一次購買 1 件");
        }

        // 檢查單筆總堆疊不超過 maxStack（買消耗品時看合併後量）
        if (item.getMaxStack() != null && quantity > item.getMaxStack()) {
            throw new IllegalArgumentException("一次購買數量超過單筆堆疊上限 " + item.getMaxStack());
        }

        long totalCost = item.getPrice() * quantity;

        // 扣錢
        long balance = goldDeal(userId, totalCost, item, quantity, "BUY");

        // 放進背包
        UserInventoryItem inv = putInBag(userId, item, quantity);

        log.info("商店購買: userId={}, itemCode={}, qty={}, cost={}, newBalance={}",
                userId, item.getCode(), quantity, totalCost, balance);

        return new ShopBuyResponse(balance, inv.getId(), inv.getQuantity(),
                item.getCode(), quantity);
    }

    private long goldDeal(Long userId, long totalCost, Item item, int quantity, String action) {
        // 扣錢（GoldService 內會檢查餘額不足 → 拋 IllegalStateException）
        long balance;
        if (action.equals("BUY")) {
            balance = goldService.changeGold(userId, -totalCost, "SHOP_BUY", item.getCode(),
                    "購買 " + item.getName() + " x" + quantity);
        } else if (action.equals("SELL")) {
            balance = goldService.changeGold(userId, totalCost, "SHOP_SELL", item.getCode(),
                    "販售 " + item.getName() + " x" + quantity);
        } else {
            throw new IllegalArgumentException("無效的金幣交易類型: " + action);
        }
        return balance;
    }

    // 放進背包（同 itemId 且 enhancementLevel=0 就合併堆疊）
    private UserInventoryItem putInBag(Long userId, Item item, int quantity) {
        UserInventoryItem inv;
        if (item.getType() == ItemType.EQUIPMENT) {
            // 裝備類每件獨立一筆
            inv = new UserInventoryItem();
            inv.setUserId(userId);
            inv.setItemId(item.getId());
            inv.setQuantity(1);
            inv.setEnhancementLevel(0);
            inventoryRepository.save(inv);
        } else {
            inv = inventoryRepository
                    .findFirstByUserIdAndItemIdAndEnhancementLevel(userId, item.getId(), 0)
                    .orElseGet(() -> {
                        UserInventoryItem neu = new UserInventoryItem();
                        neu.setUserId(userId);
                        neu.setItemId(item.getId());
                        neu.setQuantity(0);
                        neu.setEnhancementLevel(0);
                        return neu;
                    });
            int newQty = inv.getQuantity() + quantity;
            if (item.getMaxStack() != null && newQty > item.getMaxStack()) {
                throw new IllegalStateException(
                        "合併後數量 " + newQty + " 超過單筆堆疊上限 " + item.getMaxStack());
            }
            inv.setQuantity(newQty);
            inventoryRepository.save(inv);
        }
        return inv;
    }
}
