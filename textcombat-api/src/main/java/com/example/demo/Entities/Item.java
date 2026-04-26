package com.example.demo.Entities;

import jakarta.persistence.*;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_slot", length = 20)
    private EquipmentSlot equipmentSlot;

    @Column(name = "max_stack", nullable = false)
    private Integer maxStack = 1;

    @Column(name = "base_attack")
    private Integer baseAttack = 0;

    @Column(name = "base_defense")
    private Integer baseDefense = 0;

    @Column(name = "icon_url", length = 255)
    private String iconUrl;

    @Column(name = "heal_amount")
    private Integer healAmount;   // null/0 = 不回血；CONSUMABLE 用

    @Column
    private Long price;   // null = 不在商店販售

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }

    public EquipmentSlot getEquipmentSlot() { return equipmentSlot; }
    public void setEquipmentSlot(EquipmentSlot equipmentSlot) { this.equipmentSlot = equipmentSlot; }

    public Integer getMaxStack() { return maxStack; }
    public void setMaxStack(Integer maxStack) { this.maxStack = maxStack; }

    public Integer getBaseAttack() { return baseAttack; }
    public void setBaseAttack(Integer baseAttack) { this.baseAttack = baseAttack; }

    public Integer getBaseDefense() { return baseDefense; }
    public void setBaseDefense(Integer baseDefense) { this.baseDefense = baseDefense; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public Integer getHealAmount() { return healAmount; }
    public void setHealAmount(Integer healAmount) { this.healAmount = healAmount; }

    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }
}
