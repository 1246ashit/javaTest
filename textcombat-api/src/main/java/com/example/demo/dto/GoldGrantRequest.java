package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class GoldGrantRequest {
    @NotNull(message = "userId 為必填")
    private Long userId;

     @NotNull(message = "amount 為必填")
    private Long amount;   // 正數加、負數扣

    @Size(max = 200, message = "note 不可超過 200 字")
    private String note;

}
