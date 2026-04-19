package com.example.demo.DTO;

import com.example.demo.Entities.EquipmentSlot;
import com.example.demo.Entities.Item;
import com.example.demo.Entities.ItemType;
import com.example.demo.Entities.UserInventoryItem;

public class InventoryItemDTO {

    private Long inventoryItemId;
    private String itemCode;
    private String itemName;
    private String description;
    private ItemType type;
    private EquipmentSlot equipmentSlot;   // 純顯示用（icon 分類），不再限制放哪格
    private Integer quantity;
    private Integer enhancementLevel;
    private Integer baseAttack;
    private Integer baseDefense;
    private String iconUrl;
    private boolean equipped;
    private Integer equippedSlotIndex;     // 如果有裝備中，放哪一格 (1~9)；否則 null

    public static InventoryItemDTO of(UserInventoryItem inv, Item item, Integer equippedSlotIndex) {
        InventoryItemDTO dto = new InventoryItemDTO();
        dto.inventoryItemId = inv.getId();
        dto.itemCode = item.getCode();
        dto.itemName = item.getName();
        dto.description = item.getDescription();
        dto.type = item.getType();
        dto.equipmentSlot = item.getEquipmentSlot();
        dto.quantity = inv.getQuantity();
        dto.enhancementLevel = inv.getEnhancementLevel();
        dto.baseAttack = item.getBaseAttack();
        dto.baseDefense = item.getBaseDefense();
        dto.iconUrl = item.getIconUrl();
        dto.equipped = equippedSlotIndex != null;
        dto.equippedSlotIndex = equippedSlotIndex;
        return dto;
    }

    public Long getInventoryItemId() { return inventoryItemId; }
    public String getItemCode() { return itemCode; }
    public String getItemName() { return itemName; }
    public String getDescription() { return description; }
    public ItemType getType() { return type; }
    public EquipmentSlot getEquipmentSlot() { return equipmentSlot; }
    public Integer getQuantity() { return quantity; }
    public Integer getEnhancementLevel() { return enhancementLevel; }
    public Integer getBaseAttack() { return baseAttack; }
    public Integer getBaseDefense() { return baseDefense; }
    public String getIconUrl() { return iconUrl; }
    public boolean isEquipped() { return equipped; }
    public Integer getEquippedSlotIndex() { return equippedSlotIndex; }
}
