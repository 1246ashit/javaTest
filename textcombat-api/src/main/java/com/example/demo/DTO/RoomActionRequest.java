package com.example.demo.DTO;

public class RoomActionRequest {
    private RoomAction action;
    private Long inventoryItemId;   // USE_POTION 用

    public RoomAction getAction() { return action; }
    public void setAction(RoomAction action) { this.action = action; }
    public Long getInventoryItemId() { return inventoryItemId; }
    public void setInventoryItemId(Long inventoryItemId) { this.inventoryItemId = inventoryItemId; }
}
