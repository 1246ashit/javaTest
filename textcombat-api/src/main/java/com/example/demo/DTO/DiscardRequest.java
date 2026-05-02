package com.example.demo.DTO;

import lombok.Data;

@Data
public class DiscardRequest {
    private Long inventoryItemId;
    private Integer quantity;
}
