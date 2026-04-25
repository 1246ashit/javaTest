package com.example.demo.Services.Impl;

import com.example.demo.DTO.InventoryItemDTO;
import com.example.demo.DTO.InventoryResponse;
import com.example.demo.Entities.Item;
import com.example.demo.Entities.ItemType;
import com.example.demo.Entities.UserEquipment;
import com.example.demo.Entities.UserInventoryItem;
import com.example.demo.Repository.ItemRepository;
import com.example.demo.Repository.UserEquipmentRepository;
import com.example.demo.Repository.UserInventoryItemRepository;
import com.example.demo.Services.InventoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    public static final int SLOT_MIN = 1;
    public static final int SLOT_MAX = 9;

    private final UserInventoryItemRepository inventoryRepo;
    private final UserEquipmentRepository equipmentRepo;
    private final ItemRepository itemRepo;

    public InventoryServiceImpl(UserInventoryItemRepository inventoryRepo,
                            UserEquipmentRepository equipmentRepo,
                            ItemRepository itemRepo) {
        this.inventoryRepo = inventoryRepo;
        this.equipmentRepo = equipmentRepo;
        this.itemRepo = itemRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse listInventory(Long userId) {
        List<UserInventoryItem> invRows = inventoryRepo.findByUserId(userId);
        List<UserEquipment> equipRows = equipmentRepo.findByUserId(userId);

        Map<Long, Item> itemById = itemRepo.findAllById(
                invRows.stream().map(UserInventoryItem::getItemId).distinct().toList()
        ).stream().collect(Collectors.toMap(Item::getId, i -> i));

        // inventoryItemId → slotIndex
        Map<Long, Integer> invToSlot = new HashMap<>();
        for (UserEquipment ue : equipRows) {
            if (ue.getInventoryItemId() != null) {
                invToSlot.put(ue.getInventoryItemId(), ue.getSlotIndex());
            }
        }

        List<InventoryItemDTO> items = invRows.stream()
                .map(inv -> {
                    Item item = itemById.get(inv.getItemId());
                    return InventoryItemDTO.of(inv, item, invToSlot.get(inv.getId()));
                })
                .toList();

        // 組 slotIndex → DTO
        Map<Integer, InventoryItemDTO> slots = new HashMap<>();
        Map<Long, InventoryItemDTO> dtoByInvId = items.stream()
                .collect(java.util.stream.Collectors.toMap(InventoryItemDTO::getInventoryItemId, d -> d));
        for (UserEquipment ue : equipRows) {
            if (ue.getInventoryItemId() == null) continue;
            slots.put(ue.getSlotIndex(), dtoByInvId.get(ue.getInventoryItemId()));
        }

        // 計算總攻防（只算裝備，不算消耗品/素材）
        int totalAttack = 0, totalDefense = 0;
        for (InventoryItemDTO d : slots.values()) {
            if (d == null) continue;
            if (d.getType() != ItemType.EQUIPMENT) continue;
            int enh = d.getEnhancementLevel() == null ? 0 : d.getEnhancementLevel();
            // 強化每級 +10% 原數值（簡單規則；可自行調整）
            totalAttack  += d.getBaseAttack()  + (d.getBaseAttack()  * enh / 10);
            totalDefense += d.getBaseDefense() + (d.getBaseDefense() * enh / 10);
        }

        return new InventoryResponse(slots, items, totalAttack, totalDefense);
    }

    /**
     * 把背包物件放到第 slotIndex 格。
     * - 若 slotIndex 為 null，自動找第一格空位。
     * - 若該物件已裝備在別格，先從舊格移除。
     * - 若目標格已有其他物件，先把它踢下來。
     */
    @Override
    @Transactional
    public void equip(Long userId, Long inventoryItemId, Integer slotIndex) {
        if (inventoryItemId == null) {
            throw new IllegalArgumentException("inventoryItemId 為必填");
        }

        UserInventoryItem inv = inventoryRepo.findById(inventoryItemId)
                .orElseThrow(() -> new IllegalArgumentException("背包物品不存在"));
        if (!inv.getUserId().equals(userId)) {
            throw new IllegalStateException("不是你的物品");
        }

        // 找出目前這個背包物件是否已經在某一格
        UserEquipment existingForInv = equipmentRepo
                .findByUserIdAndInventoryItemId(userId, inventoryItemId)
                .orElse(null);

        // 決定目標 slotIndex
        int target;
        if (slotIndex == null) {
            target = findFirstEmptySlot(userId);
        } else {
            if (slotIndex < SLOT_MIN || slotIndex > SLOT_MAX) {
                throw new IllegalArgumentException("slotIndex 必須在 " + SLOT_MIN + "~" + SLOT_MAX);
            }
            target = slotIndex;
        }

        // 如果該物件已在 target 格 → 什麼都不用做
        if (existingForInv != null && Objects.equals(existingForInv.getSlotIndex(), target)) {
            return;
        }

        // 先清掉 target 格的舊內容
        equipmentRepo.findByUserIdAndSlotIndex(userId, target)
                .ifPresent(equipmentRepo::delete);
        equipmentRepo.flush();   // 確保 unique 不衝突

        // 如果這個物件原本在別格，移除舊紀錄
        if (existingForInv != null) {
            equipmentRepo.delete(existingForInv);
            equipmentRepo.flush();
        }

        UserEquipment ue = new UserEquipment();
        ue.setUserId(userId);
        ue.setSlotIndex(target);
        ue.setInventoryItemId(inventoryItemId);
        equipmentRepo.save(ue);

        log.info("裝備: userId={}, slotIndex={}, invItemId={}", userId, target, inventoryItemId);
    }

    @Override
    @Transactional
    public void unequip(Long userId, Integer slotIndex) {
        if (slotIndex == null) {
            throw new IllegalArgumentException("slotIndex 為必填");
        }
        equipmentRepo.findByUserIdAndSlotIndex(userId, slotIndex).ifPresent(ue -> {
            equipmentRepo.delete(ue);
            log.info("卸下: userId={}, slotIndex={}", userId, slotIndex);
        });
    }

    /**
     * 使用裝備欄第 slotIndex 格的消耗品。
     * 目前僅實作「數量 -1」與紀錄 log；未來可擴充藥品實際效果。
     */
    @Override
    @Transactional
    public String useSlot(Long userId, Integer slotIndex) {
        if (slotIndex == null) {
            throw new IllegalArgumentException("slotIndex 為必填");
        }
        UserEquipment ue = equipmentRepo.findByUserIdAndSlotIndex(userId, slotIndex)
                .orElseThrow(() -> new IllegalArgumentException("該格是空的"));

        UserInventoryItem inv = inventoryRepo.findById(ue.getInventoryItemId())
                .orElseThrow(() -> new IllegalStateException("背包資料異常"));

        Item item = itemRepo.findById(inv.getItemId())
                .orElseThrow(() -> new IllegalStateException("物品資料異常"));

        if (item.getType() != ItemType.CONSUMABLE) {
            throw new IllegalArgumentException("此格的物品不能使用（只有消耗品能使用）");
        }

        int remaining = inv.getQuantity() - 1;
        if (remaining <= 0) {
            // 數量耗盡：清背包列 + 清裝備格
            equipmentRepo.delete(ue);
            inventoryRepo.delete(inv);
        } else {
            inv.setQuantity(remaining);
            inventoryRepo.save(inv);
        }

        log.info("使用消耗品: userId={}, slotIndex={}, itemCode={}, remaining={}",
                userId, slotIndex, item.getCode(), Math.max(0, remaining));
        return "已使用 " + item.getName();
    }

    @Override
    @Transactional
    public void discard(Long userId, Long inventoryItemId, Integer quantity) {
        if (inventoryItemId == null || quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("inventoryItemId 與 quantity(>0) 為必填");
        }

        UserInventoryItem inv = inventoryRepo.findById(inventoryItemId)
                .orElseThrow(() -> new IllegalArgumentException("背包物品不存在"));

        if (!inv.getUserId().equals(userId)) {
            throw new IllegalStateException("不是你的物品");
        }

        if (quantity > inv.getQuantity()) {
            throw new IllegalArgumentException("丟棄數量大於持有數量");
        }

        // 裝備中的物品要先卸下
        equipmentRepo.findByUserIdAndInventoryItemId(userId, inventoryItemId)
                .ifPresent(ue -> { throw new IllegalStateException("裝備中的物品無法丟棄，請先卸下"); });

        int remaining = inv.getQuantity() - quantity;
        if (remaining == 0) {
            inventoryRepo.delete(inv);
        } else {
            inv.setQuantity(remaining);
            inventoryRepo.save(inv);
        }

        log.info("丟棄: userId={}, invItemId={}, qty={}, remaining={}",
                userId, inventoryItemId, quantity, remaining);
    }

    private int findFirstEmptySlot(Long userId) {
        List<UserEquipment> rows = equipmentRepo.findByUserId(userId);
        boolean[] used = new boolean[SLOT_MAX + 1];
        for (UserEquipment ue : rows) {
            if (ue.getSlotIndex() != null && ue.getSlotIndex() >= SLOT_MIN && ue.getSlotIndex() <= SLOT_MAX) {
                used[ue.getSlotIndex()] = true;
            }
        }
        for (int i = SLOT_MIN; i <= SLOT_MAX; i++) {
            if (!used[i]) return i;
        }
        throw new IllegalStateException("裝備欄已滿（9 格全滿）");
    }
}
