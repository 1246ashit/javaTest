package com.example.demo.service.impl;

import com.example.demo.entity.UserInventoryItem;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.UserEquipmentRepository;
import com.example.demo.repository.UserInventoryItemRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

        @Mock
        private UserEquipmentRepository equipmentRepo;

        @Mock
        private UserInventoryItemRepository inventoryRepository;

        @Mock
        private ItemRepository itemRepo;

        @InjectMocks
        private InventoryServiceImpl inventoryService;

        // discard
        @Test
        @DisplayName("discard_case1: 成功丟棄時，資料庫要有儲存到")
        void discard_DiscardSuccessAndSqlRecordSuccess() {
                // when
                UserInventoryItem inv1 = new UserInventoryItem();
                inv1.setId(500L);
                inv1.setUserId(10L);
                inv1.setItemId(101L);
                inv1.setQuantity(20);

                when(inventoryRepository.findById(inv1.getId()))
                                .thenReturn(Optional.of(inv1));

                when(equipmentRepo.findByUserIdAndInventoryItemId(inv1.getUserId(), inv1.getId()))
                                .thenReturn(Optional.empty());

                // act
                inventoryService.discard(10L, 500L, 1);

                // assert
                ArgumentCaptor<UserInventoryItem> captor = ArgumentCaptor.forClass(UserInventoryItem.class);
                verify(inventoryRepository).save(captor.capture());
                UserInventoryItem saved = captor.getValue();
                assertThat(saved.getUserId()).isEqualTo(10L);
                assertThat(saved.getItemId()).isEqualTo(101L);
                int holding = 20;
                int discardQty = 1;
                assertThat(saved.getQuantity()).isEqualTo(holding-discardQty);

                verify(inventoryRepository, never()).delete(any());// 「部分丟棄」應該走 save 不走 delete
        }

}