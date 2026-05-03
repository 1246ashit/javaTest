package com.example.demo.repository;

import com.example.demo.entity.UserEquipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserEquipmentRepository extends JpaRepository<UserEquipment, Long> {

    List<UserEquipment> findByUserId(Long userId);

    Optional<UserEquipment> findByUserIdAndSlotIndex(Long userId, Integer slotIndex);

    Optional<UserEquipment> findByUserIdAndInventoryItemId(Long userId, Long inventoryItemId);
}
