package com.example.demo.DTO;

public class EnhanceResultResponse {
    private boolean success;
    private Integer previousLevel;
    private Integer newLevel;
    private boolean destroyed;
    private long goldSpent;
    private long goldBalance;
    private String message;
    private InventoryResponse inventory;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public Integer getPreviousLevel() { return previousLevel; }
    public void setPreviousLevel(Integer previousLevel) { this.previousLevel = previousLevel; }

    public Integer getNewLevel() { return newLevel; }
    public void setNewLevel(Integer newLevel) { this.newLevel = newLevel; }

    public boolean isDestroyed() { return destroyed; }
    public void setDestroyed(boolean destroyed) { this.destroyed = destroyed; }

    public long getGoldSpent() { return goldSpent; }
    public void setGoldSpent(long goldSpent) { this.goldSpent = goldSpent; }

    public long getGoldBalance() { return goldBalance; }
    public void setGoldBalance(long goldBalance) { this.goldBalance = goldBalance; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public InventoryResponse getInventory() { return inventory; }
    public void setInventory(InventoryResponse inventory) { this.inventory = inventory; }
}
