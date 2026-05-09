package com.example.demo.service.impl;

import com.example.demo.entity.GoldTransaction;
import com.example.demo.entity.UsersEntity;
import com.example.demo.repository.GoldTransactionRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.GoldService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoldServiceImpl implements GoldService {

    private static final Logger log = LoggerFactory.getLogger(GoldServiceImpl.class);

    private final UserRepository userRepository;
    private final GoldTransactionRepository txRepository;

    public GoldServiceImpl(UserRepository userRepository, GoldTransactionRepository txRepository) {
        this.userRepository = userRepository;
        this.txRepository = txRepository;
    }

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
    @Override
    @Transactional
    public long changeGold(Long userId, long amount, String reason, String refId, String note) {
        if (amount == 0) {
            throw new IllegalArgumentException("amount 不可為 0");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason 為必填");
        }

        UsersEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("玩家不存在"));

        long newBalance = user.getGold() + amount;
        if (newBalance < 0) {
            throw new IllegalStateException("金幣不足");
        }

        user.setGold(newBalance);
        userRepository.save(user);

        GoldTransaction tx = new GoldTransaction();
        tx.setUserId(userId);
        tx.setAmount(amount);
        tx.setBalanceAfter(newBalance);
        tx.setReason(reason);
        tx.setRefId(refId);
        tx.setNote(note);
        txRepository.save(tx);

        log.info("金幣異動: userId={}, amount={}, newBalance={}, reason={}, refId={}",
                userId, amount, newBalance, reason, refId);
        return newBalance;
    }

    @Override
    @Transactional(readOnly = true)
    public long getGold(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("玩家不存在"))
                .getGold();
    }
}
