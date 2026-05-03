package com.example.demo.service;

import com.example.demo.dto.EnhancePreviewResponse;
import com.example.demo.dto.EnhanceResultResponse;

public interface EnhanceService {

    EnhancePreviewResponse preview(Long userId, Long inventoryItemId);

    EnhanceResultResponse enhance(Long userId, Long inventoryItemId);
}
