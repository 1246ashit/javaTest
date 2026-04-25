package com.example.demo.Services;

import java.util.List;

import com.example.demo.DTO.ShopBuyResponse;
import com.example.demo.DTO.ShopItemDTO;

public interface ShopService {

    List<ShopItemDTO> listShopItems();

    ShopBuyResponse buy(Long userId, Long itemId, int quantity);
}
