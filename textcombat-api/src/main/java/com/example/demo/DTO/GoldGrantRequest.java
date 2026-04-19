package com.example.demo.DTO;

public class GoldGrantRequest {
    private Long userId;
    private Long amount;   // 正數加、負數扣
    private String note;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
