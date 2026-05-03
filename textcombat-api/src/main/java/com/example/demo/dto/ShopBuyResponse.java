package com.example.demo.dto;

public class ShopBuyResponse {
    private Long goldBalance;
    private Long inventoryItemId;   // 新的或更新的背包筆 id
    private Integer quantity;        // 此背包筆總量
    private String itemCode;
    private Integer bought;          // 這次買的數量

    public ShopBuyResponse(Long goldBalance, Long inventoryItemId, Integer quantity,
                           String itemCode, Integer bought) {
        this.goldBalance = goldBalance;
        this.inventoryItemId = inventoryItemId;
        this.quantity = quantity;
        this.itemCode = itemCode;
        this.bought = bought;
    }

    public Long getGoldBalance() { return goldBalance; }
    public Long getInventoryItemId() { return inventoryItemId; }
    public Integer getQuantity() { return quantity; }
    public String getItemCode() { return itemCode; }
    public Integer getBought() { return bought; }
}
