package com.example.demo.Repository;

import com.example.demo.Entities.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {
    Optional<Item> findByCode(String code);

    // 商店展示：price 不為 null 的物品
    List<Item> findByPriceIsNotNullOrderByPriceAsc();
}
