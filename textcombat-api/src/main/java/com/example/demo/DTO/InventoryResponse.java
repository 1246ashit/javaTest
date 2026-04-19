package com.example.demo.DTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryResponse {

    // 9 格；key = 1~9，值 = 放在該格的物品（可能 null 表示空格）
    private Map<Integer, InventoryItemDTO> slots = new HashMap<>();

    private List<InventoryItemDTO> items;

    // 裝備總和（消耗品不計）
    private int totalAttack;
    private int totalDefense;

    public InventoryResponse(Map<Integer, InventoryItemDTO> slots,
                             List<InventoryItemDTO> items,
                             int totalAttack, int totalDefense) {
        // 確保 1~9 都有 key（可能 null），前端不用再判斷
        for (int i = 1; i <= 9; i++) {
            this.slots.put(i, slots.get(i));
        }
        this.items = items;
        this.totalAttack = totalAttack;
        this.totalDefense = totalDefense;
    }

    public Map<Integer, InventoryItemDTO> getSlots() { return slots; }
    public List<InventoryItemDTO> getItems() { return items; }
    public int getTotalAttack() { return totalAttack; }
    public int getTotalDefense() { return totalDefense; }
}
