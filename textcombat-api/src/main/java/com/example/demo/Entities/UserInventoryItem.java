package com.example.demo.Entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_inventory_items")
public class UserInventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "enhancement_level", nullable = false)
    private Integer enhancementLevel = 0;

    @Column(name = "acquired_at", insertable = false, updatable = false)
    private LocalDateTime acquiredAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getEnhancementLevel() { return enhancementLevel; }
    public void setEnhancementLevel(Integer enhancementLevel) { this.enhancementLevel = enhancementLevel; }

    public LocalDateTime getAcquiredAt() { return acquiredAt; }
}
