package com.example.demo.service;

import com.example.demo.dto.InventoryResponse;

public interface InventoryService {

    InventoryResponse listInventory(Long userId);

    void equip(Long userId, Long inventoryItemId, Integer slotIndex);

    void unequip(Long userId, Integer slotIndex);

    String useSlot(Long userId, Integer slotIndex);

    void discard(Long userId, Long inventoryItemId, Integer quantity);

}
