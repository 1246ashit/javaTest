package com.example.demo.DTO;

public class MaterialRequirementDTO {
    private Long itemId;
    private String itemCode;
    private String itemName;
    private Integer quantityNeeded;
    private Integer quantityOwned;

    public MaterialRequirementDTO() {}

    public MaterialRequirementDTO(Long itemId, String itemCode, String itemName,
                                  Integer quantityNeeded, Integer quantityOwned) {
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.quantityNeeded = quantityNeeded;
        this.quantityOwned = quantityOwned;
    }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public Integer getQuantityNeeded() { return quantityNeeded; }
    public void setQuantityNeeded(Integer quantityNeeded) { this.quantityNeeded = quantityNeeded; }

    public Integer getQuantityOwned() { return quantityOwned; }
    public void setQuantityOwned(Integer quantityOwned) { this.quantityOwned = quantityOwned; }
}
