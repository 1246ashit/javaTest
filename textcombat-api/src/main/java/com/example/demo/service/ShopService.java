package com.example.demo.service;

import java.util.List;

import com.example.demo.dto.ShopBuyResponse;
import com.example.demo.dto.ShopItemDTO;

public interface ShopService {

    List<ShopItemDTO> listShopItems();

    ShopBuyResponse buy(Long userId, Long itemId, int quantity);
}
