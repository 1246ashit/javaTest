package com.example.demo.DTO;

public class EquipRequest {
    private Long inventoryItemId;
    private Integer slotIndex;    // 1~9；null 代表自動放第一格空位

    public Long getInventoryItemId() { return inventoryItemId; }
    public void setInventoryItemId(Long inventoryItemId) { this.inventoryItemId = inventoryItemId; }

    public Integer getSlotIndex() { return slotIndex; }
    public void setSlotIndex(Integer slotIndex) { this.slotIndex = slotIndex; }
}
