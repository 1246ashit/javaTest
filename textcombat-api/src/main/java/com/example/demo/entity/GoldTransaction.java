package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Getter;

@Entity
@Table(name = "gold_transactions")
@Getter                      
@Setter                      
@NoArgsConstructor
public class GoldTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 正數 = 加、負數 = 扣
    @Column(nullable = false)
    private Long amount;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    // "GRANT" / "SHOP_BUY" / "QUEST_REWARD" 等
    @Column(nullable = false, length = 50)
    private String reason;

    // 關聯 id（例如 shop item code / quest id）
    @Column(name = "ref_id", length = 100)
    private String refId;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

}
