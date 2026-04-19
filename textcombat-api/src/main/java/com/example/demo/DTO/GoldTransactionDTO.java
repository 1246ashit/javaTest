package com.example.demo.DTO;

import com.example.demo.Entities.GoldTransaction;

import java.time.OffsetDateTime;

public class GoldTransactionDTO {
    private Long id;
    private Long userId;
    private String username;      // 為了 admin 介面好看，join 進來
    private Long amount;
    private Long balanceAfter;
    private String reason;
    private String refId;
    private String note;
    private OffsetDateTime createdAt;

    public static GoldTransactionDTO of(GoldTransaction tx, String username) {
        GoldTransactionDTO d = new GoldTransactionDTO();
        d.id = tx.getId();
        d.userId = tx.getUserId();
        d.username = username;
        d.amount = tx.getAmount();
        d.balanceAfter = tx.getBalanceAfter();
        d.reason = tx.getReason();
        d.refId = tx.getRefId();
        d.note = tx.getNote();
        d.createdAt = tx.getCreatedAt();
        return d;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public Long getAmount() { return amount; }
    public Long getBalanceAfter() { return balanceAfter; }
    public String getReason() { return reason; }
    public String getRefId() { return refId; }
    public String getNote() { return note; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
