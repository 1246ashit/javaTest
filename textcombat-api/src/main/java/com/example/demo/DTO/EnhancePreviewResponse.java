package com.example.demo.DTO;

import java.util.List;

public class EnhancePreviewResponse {
    private Long inventoryItemId;
    private String itemCode;
    private String itemName;
    private Integer currentLevel;
    private Integer maxLevel;
    private Integer currentAttack;
    private Integer currentDefense;
    private Integer nextAttack;
    private Integer nextDefense;
    private double successRate;            // 0 ~ 1
    private long goldCost;
    private long goldOwned;
    private List<MaterialRequirementDTO> materialCosts;
    private EnhanceFailEffect onFail;
    private boolean canEnhance;
    private String blockReason;            // canEnhance=false 時的原因，否則 null

    public Long getInventoryItemId() { return inventoryItemId; }
    public void setInventoryItemId(Long inventoryItemId) { this.inventoryItemId = inventoryItemId; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public Integer getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(Integer currentLevel) { this.currentLevel = currentLevel; }

    public Integer getMaxLevel() { return maxLevel; }
    public void setMaxLevel(Integer maxLevel) { this.maxLevel = maxLevel; }

    public Integer getCurrentAttack() { return currentAttack; }
    public void setCurrentAttack(Integer currentAttack) { this.currentAttack = currentAttack; }

    public Integer getCurrentDefense() { return currentDefense; }
    public void setCurrentDefense(Integer currentDefense) { this.currentDefense = currentDefense; }

    public Integer getNextAttack() { return nextAttack; }
    public void setNextAttack(Integer nextAttack) { this.nextAttack = nextAttack; }

    public Integer getNextDefense() { return nextDefense; }
    public void setNextDefense(Integer nextDefense) { this.nextDefense = nextDefense; }

    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }

    public long getGoldCost() { return goldCost; }
    public void setGoldCost(long goldCost) { this.goldCost = goldCost; }

    public long getGoldOwned() { return goldOwned; }
    public void setGoldOwned(long goldOwned) { this.goldOwned = goldOwned; }

    public List<MaterialRequirementDTO> getMaterialCosts() { return materialCosts; }
    public void setMaterialCosts(List<MaterialRequirementDTO> materialCosts) { this.materialCosts = materialCosts; }

    public EnhanceFailEffect getOnFail() { return onFail; }
    public void setOnFail(EnhanceFailEffect onFail) { this.onFail = onFail; }

    public boolean isCanEnhance() { return canEnhance; }
    public void setCanEnhance(boolean canEnhance) { this.canEnhance = canEnhance; }

    public String getBlockReason() { return blockReason; }
    public void setBlockReason(String blockReason) { this.blockReason = blockReason; }
}
