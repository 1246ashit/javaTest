package com.example.demo.dto;

import lombok.Data;

@Data
public class EnhanceResultResponse {
    private boolean success;
    private Integer previousLevel;
    private Integer newLevel;
    private boolean destroyed;
    private long goldSpent;
    private long goldBalance;
    private String message;
    private InventoryResponse inventory;
}
