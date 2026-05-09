package com.example.demo.service.impl;

import com.example.demo.entity.UsersEntity;
import com.example.demo.entity.GoldTransaction;
import com.example.demo.repository.GoldTransactionRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.Impl.GoldServiceImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class GoldServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private GoldTransactionRepository txRepository;

    @InjectMocks
    private GoldServiceImpl goldService;

    // 測changeGold情境
    // 1,當amount 為0的時候會拋IllegalArgumentException("amount 不可為 0")
    @Test
    @DisplayName("case1: amount=0 時應拋 IllegalArgumentException")
    void changeGold_amountIsZero_throws() {
        // Act + Assert：呼叫 changeGold 並驗證它拋例外
        assertThatThrownBy(() -> goldService.changeGold(1L, 0L, "GRANT", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("amount 不可為 0");
    }

    // 2,當 reason 為null 或是isBlank的時候會拋IllegalArgumentException(reason 為必填)
    @Test
    @DisplayName("case2a: reason=null 應拋 IllegalArgumentException")
    void changeGold_reasonIsNull_throws() {
        // Act + Assert：呼叫 changeGold 並驗證它拋例外
        assertThatThrownBy(() -> goldService.changeGold(1L, 20L, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("reason 為必填");

    }

    @Test
    @DisplayName("case2b: reason 為純空白 應拋 IllegalArgumentException")
    void changeGold_reasonIsBlank_throws() {
        // Act + Assert：呼叫 changeGold 並驗證它拋例外
        assertThatThrownBy(() -> goldService.changeGold(1L, 20L, "", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("reason 為必填");
    }

    // 3,當 userId 找不到的時候會拋IllegalArgumentException("玩家不存在")
    @Test
    @DisplayName("case3: user 不存在 應拋 IllegalArgumentException")
    void changeGold_userNotFound_throws() {
        // Arrange：明確告訴 mock 「找這個 userId 時回 empty」
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> goldService.changeGold(99L, 100L, "GRANT", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("玩家不存在");
    }

    // 4. 當輸入要消費的錢大於使用者持有的錢時拋出IllegalStateException("金幣不足");
    @Test
    @DisplayName("case4: 消費金額大於餘額 應拋 IllegalStateException")
    void changeGold_insufficientGold_throws() {
        // Arrange：準備一個有 100 金幣的 user
        UsersEntity user = new UsersEntity();
        user.setGold(100L);

        // 教 mock：當有人問 userRepository.findById(1L)，回傳這個 user
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act + Assert：想消費 200，超過餘額
        assertThatThrownBy(() -> goldService.changeGold(1L, -200L, "SHOP_BUY", null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("金幣不足");
        
        //驗證 save 完全沒被呼叫過
        verify(userRepository, never()).save(any());
        verify(txRepository, never()).save(any());

        //user 本身的 gold 也不該被動到
        assertThat(user.getGold()).isEqualTo(100L);
    }

    // 5. 扣錢成功 → 回傳值正確 + user.gold 更新 + tx 紀錄正確
    @Test
    @DisplayName("case5: 扣錢成功 應更新餘額並寫入交易紀錄")
    void changeGold_spendSuccess_updatesBalanceAndWritesTx() {
        // Arrange：user 有 100 金幣
        UsersEntity user = new UsersEntity();
        user.setGold(100L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act：消費 50
        long balance = goldService.changeGold(1L, -50L, "SHOP_BUY", "物品ID001", "買藥水");

        // Assert ①：回傳值正確
        assertThat(balance).isEqualTo(50L);

        // Assert ②：user 物件本身的 gold 真的被改成 50
        assertThat(user.getGold()).isEqualTo(50L);

        // Assert ③：userRepository.save(user) 真的被呼叫
        verify(userRepository).save(user);

        // Assert ④：txRepository.save(...) 被呼叫，且傳進去的 GoldTransaction 內容正確
        ArgumentCaptor<GoldTransaction> txCaptor = ArgumentCaptor.forClass(GoldTransaction.class);
        verify(txRepository).save(txCaptor.capture());

        GoldTransaction savedTx = txCaptor.getValue();
        assertThat(savedTx.getUserId()).isEqualTo(1L);
        assertThat(savedTx.getAmount()).isEqualTo(-50L);
        assertThat(savedTx.getBalanceAfter()).isEqualTo(50L);
        assertThat(savedTx.getReason()).isEqualTo("SHOP_BUY");
        assertThat(savedTx.getRefId()).isEqualTo("物品ID001");
        assertThat(savedTx.getNote()).isEqualTo("買藥水");
    }

    // 6. 加錢成功 → 回傳值正確 + tx.amount 為正
    @Test
    @DisplayName("case6: 加錢成功 應更新餘額並寫入交易紀錄")
    void changeGold_earnSuccess_updatesBalanceAndWritesTx() {
        // Arrange：user 有 100 金幣
        UsersEntity user = new UsersEntity();
        user.setGold(100L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act：賺 50
        long balance = goldService.changeGold(1L, 50L, "使用者賺錢", "藥水ID", "賣藥水");

        // Assert ①：回傳值正確
        assertThat(balance).isEqualTo(150L);

        // Assert ②：user 物件本身的 gold 真的被改成 150
        assertThat(user.getGold()).isEqualTo(150L);

        // Assert ③：userRepository.save(user) 真的被呼叫
        verify(userRepository).save(user);

        // Assert ④：txRepository.save(...) 被呼叫，且傳進去的 GoldTransaction 內容正確
        ArgumentCaptor<GoldTransaction> txCaptor = ArgumentCaptor.forClass(GoldTransaction.class);
        verify(txRepository).save(txCaptor.capture());

        GoldTransaction savedTx = txCaptor.getValue();
        assertThat(savedTx.getUserId()).isEqualTo(1L);
        assertThat(savedTx.getAmount()).isEqualTo(50L);
        assertThat(savedTx.getBalanceAfter()).isEqualTo(150L);
        assertThat(savedTx.getReason()).isEqualTo("使用者賺錢");
        assertThat(savedTx.getRefId()).isEqualTo("藥水ID");
        assertThat(savedTx.getNote()).isEqualTo("賣藥水");
    }

    // 測getGold情境
    // 1,當 userId 找不到的時候會拋IllegalArgumentException("玩家不存在")
    @Test
    @DisplayName("getGold_case1: user 不存在 應拋 IllegalArgumentException")
    void getGold_userNotFound_throws() {
        // Arrange：明確告訴 mock 「找這個 userId 時回 empty」
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> goldService.getGold(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("玩家不存在");
    }

    // 2,user 存在時 應該要知道她有多少錢
    @Test
    @DisplayName("getGold_case2: user 存在時 應該要知道她有多少錢")
    void getGold_userShouldHaveMoney() {
        UsersEntity user = new UsersEntity();
        user.setGold(100L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        long res=goldService.getGold(1L);

        assertThat(res).isEqualTo(100L);
    }

}
