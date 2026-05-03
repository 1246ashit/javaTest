package com.example.demo.dto;

import lombok.Data;

@Data
public class RoomActionRequest {
    private RoomAction action;
    private Long inventoryItemId;   // USE_POTION 用
}
