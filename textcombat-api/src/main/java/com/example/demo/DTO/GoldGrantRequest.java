package com.example.demo.DTO;

import lombok.Data;

@Data
public class GoldGrantRequest {
    private Long userId;
    private Long amount;   // 正數加、負數扣
    private String note;

}
