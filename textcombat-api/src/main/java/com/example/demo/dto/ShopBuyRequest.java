package com.example.demo.dto;

import lombok.Data;

@Data
public class ShopBuyRequest {
    private Long itemId;
    private Integer quantity;
}
