package com.example.demo.Repository;

import com.example.demo.Entities.UserInventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserInventoryItemRepository extends JpaRepository<UserInventoryItem, Long> {

    List<UserInventoryItem> findByUserId(Long userId);

    // 找可以堆疊的行（同玩家、同道具、同強化等級，才能 quantity 相加）
    Optional<UserInventoryItem> findFirstByUserIdAndItemIdAndEnhancementLevel(
        Long userId, Long itemId, Integer enhancementLevel);
}
