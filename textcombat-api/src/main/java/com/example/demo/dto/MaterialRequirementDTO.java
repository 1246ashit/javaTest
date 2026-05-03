package com.example.demo.dto;

import lombok.Data;

@Data
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
}
