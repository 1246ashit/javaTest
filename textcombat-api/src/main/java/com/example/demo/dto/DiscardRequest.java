package com.example.demo.dto;

import lombok.Data;

@Data
public class DiscardRequest {
    private Long inventoryItemId;
    private Integer quantity;
}
