package com.example.demo.Services;

import com.example.demo.DTO.EnhancePreviewResponse;
import com.example.demo.DTO.EnhanceResultResponse;

public interface EnhanceService {

    EnhancePreviewResponse preview(Long userId, Long inventoryItemId);

    EnhanceResultResponse enhance(Long userId, Long inventoryItemId);
}
