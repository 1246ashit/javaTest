package com.example.demo.service;

public interface GoldService {

    /**
     * 改變玩家金幣並留下交易紀錄。
     * 所有金幣異動（發放、購買、獎勵）都要走這個方法，確保紀錄完整。
     *
     * @param userId 玩家 id
     * @param amount 正數 = 加、負數 = 扣
     * @param reason "GRANT" / "SHOP_BUY" / "QUEST_REWARD" 等
     * @param refId  關聯物件（例如物品 code），可為 null
     * @param note   人類可讀說明，可為 null
     * @return 異動後餘額
     */
    long changeGold(Long userId, long amount, String reason, String refId, String note);

    long getGold(Long userId);
}
