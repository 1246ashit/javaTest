package com.example.demo.Entities;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
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
    private Integer healAmount; // null/0 = 不回血；CONSUMABLE 用

    @Column
    private Long price; // null = 不在商店販售
}
