package com.example.demo.dto;

import java.util.List;
import lombok.Data;


@Data
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

}
