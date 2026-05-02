package com.example.demo.DTO;

import lombok.Data;

@Data
public class EquipRequest {
    private Long inventoryItemId;
    private Integer slotIndex;    // 1~9；null 代表自動放第一格空位
}
