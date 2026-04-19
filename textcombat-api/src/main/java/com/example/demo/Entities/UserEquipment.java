package com.example.demo.Entities;

import jakarta.persistence.*;

@Entity
@Table(
    name = "user_equipment",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_equipment_slot", columnNames = {"user_id", "slot_index"}),
        @UniqueConstraint(name = "uq_user_equipment_invitem", columnNames = {"user_id", "inventory_item_id"})
    }
)
public class UserEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 1 ~ 9
    @Column(name = "slot_index", nullable = false)
    private Integer slotIndex;

    @Column(name = "inventory_item_id")
    private Long inventoryItemId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getSlotIndex() { return slotIndex; }
    public void setSlotIndex(Integer slotIndex) { this.slotIndex = slotIndex; }

    public Long getInventoryItemId() { return inventoryItemId; }
    public void setInventoryItemId(Long inventoryItemId) { this.inventoryItemId = inventoryItemId; }
}
