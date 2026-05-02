package com.example.demo.Entities;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Getter;

@Entity
@Table(name = "bosses")
@Getter                      
@Setter                      
@NoArgsConstructor
public class Boss {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    private String icon; // emoji 或圖示

    @Column(nullable = false)
    private Integer hp;

    @Column(nullable = false)
    private Integer attack;

    @Column(nullable = false)
    private Integer defense;

    @Column(name = "reward_gold", nullable = false)
    private Long rewardGold = 0L;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

}
