package com.example.demo.Services;

import com.example.demo.DTO.ShopBuyResponse;
import com.example.demo.DTO.ShopItemDTO;
import com.example.demo.Entities.Item;
import com.example.demo.Entities.ItemType;
import com.example.demo.Entities.UserInventoryItem;
import com.example.demo.Repository.ItemRepository;
import com.example.demo.Repository.UserInventoryItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ShopService {

    private static final Logger log = LoggerFactory.getLogger(ShopService.class);

    private final ItemRepository itemRepository;
    private final UserInventoryItemRepository inventoryRepository;
    private final GoldService goldService;

    public ShopService(ItemRepository itemRepository,
                       UserInventoryItemRepository inventoryRepository,
                       GoldService goldService) {
        this.itemRepository = itemRepository;
        this.inventoryRepository = inventoryRepository;
        this.goldService = goldService;
    }

    @Transactional(readOnly = true)
    public List<ShopItemDTO> listShopItems() {
        return itemRepository.findByPriceIsNotNullOrderByPriceAsc().stream()
                .map(ShopItemDTO::from)
                .toList();
    }

    /**
     * 購買物品：原子扣金幣 + 放進背包 + 寫交易紀錄
     */
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

        // 扣錢（GoldService 內會檢查餘額不足 → 拋 IllegalStateException）
        long balance = goldService.changeGold(userId, -totalCost, "SHOP_BUY", item.getCode(),
                "購買 " + item.getName() + " x" + quantity);

        // 放進背包（同 itemId 且 enhancementLevel=0 就合併堆疊）
        UserInventoryItem inv;
        if (item.getType() == ItemType.EQUIPMENT) {
            // 裝備類每件獨立一筆
            inv = new UserInventoryItem();
            inv.setUserId(userId);
            inv.setItemId(item.getId());
            inv.setQuantity(1);
            inv.setEnhancementLevel(0);
            inv = inventoryRepository.save(inv);
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
            inv = inventoryRepository.save(inv);
        }

        log.info("商店購買: userId={}, itemCode={}, qty={}, cost={}, newBalance={}",
                userId, item.getCode(), quantity, totalCost, balance);

        return new ShopBuyResponse(balance, inv.getId(), inv.getQuantity(),
                item.getCode(), quantity);
    }
}
