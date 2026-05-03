package com.example.demo.dto;

/**
 * 強化失敗時的後果。
 * NOTHING   = 等級不變
 * DOWNGRADE = 等級 -1（最低 0）
 * DESTROY   = 武器消失
 */
public enum EnhanceFailEffect {
    NOTHING,
    DOWNGRADE,
    DESTROY
}
