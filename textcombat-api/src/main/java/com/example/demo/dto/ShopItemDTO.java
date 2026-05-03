package com.example.demo.dto;

import com.example.demo.entity.EquipmentSlot;
import com.example.demo.entity.Item;
import com.example.demo.entity.ItemType;

public class ShopItemDTO {
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String description;
    private ItemType type;
    private EquipmentSlot equipmentSlot;
    private Integer baseAttack;
    private Integer baseDefense;
    private Long price;
    private String iconUrl;

    public static ShopItemDTO from(Item i) {
        ShopItemDTO d = new ShopItemDTO();
        d.itemId = i.getId();
        d.itemCode = i.getCode();
        d.itemName = i.getName();
        d.description = i.getDescription();
        d.type = i.getType();
        d.equipmentSlot = i.getEquipmentSlot();
        d.baseAttack = i.getBaseAttack();
        d.baseDefense = i.getBaseDefense();
        d.price = i.getPrice();
        d.iconUrl = i.getIconUrl();
        return d;
    }

    public Long getItemId() { return itemId; }
    public String getItemCode() { return itemCode; }
    public String getItemName() { return itemName; }
    public String getDescription() { return description; }
    public ItemType getType() { return type; }
    public EquipmentSlot getEquipmentSlot() { return equipmentSlot; }
    public Integer getBaseAttack() { return baseAttack; }
    public Integer getBaseDefense() { return baseDefense; }
    public Long getPrice() { return price; }
    public String getIconUrl() { return iconUrl; }
}
