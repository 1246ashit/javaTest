package com.example.demo.Services;

import com.example.demo.DTO.InventoryResponse;

public interface InventoryService {

    InventoryResponse listInventory(Long userId);

    void equip(Long userId, Long inventoryItemId, Integer slotIndex);

    void unequip(Long userId, Integer slotIndex);

    String useSlot(Long userId, Integer slotIndex);

    void discard(Long userId, Long inventoryItemId, Integer quantity);

}
