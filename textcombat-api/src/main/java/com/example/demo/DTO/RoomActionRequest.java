package com.example.demo.DTO;

import lombok.Data;

@Data
public class RoomActionRequest {
    private RoomAction action;
    private Long inventoryItemId;   // USE_POTION 用
}
