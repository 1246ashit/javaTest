package com.example.demo.dto;

import lombok.Data;

@Data
public class EquipRequest {
    private Long inventoryItemId;
    private Integer slotIndex;    // 1~9；null 代表自動放第一格空位
}
