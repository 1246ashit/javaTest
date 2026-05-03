package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_inventory_items")
@Getter
@Setter
@NoArgsConstructor
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
}
