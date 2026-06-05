package com.example.demo.service.battle;

import org.springframework.stereotype.Component;

import com.example.demo.dto.ActionContext;
import com.example.demo.dto.RoomAction;
import com.example.demo.dto.RoomDTO;
import com.example.demo.dto.RoomMemberDTO;
import com.example.demo.entity.Item;
import com.example.demo.entity.ItemType;
import com.example.demo.entity.UserInventoryItem;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.UserEquipmentRepository;
import com.example.demo.repository.UserInventoryItemRepository;

@Component
public class PotionStrategy implements ActionStrategy {
    private final UserInventoryItemRepository inventoryRepo;
    private final ItemRepository itemRepo;
    private final UserEquipmentRepository equipmentRepo;

    public PotionStrategy(UserInventoryItemRepository inventoryRepo,
            ItemRepository itemRepo,
            UserEquipmentRepository equipmentRepo) {
        this.inventoryRepo = inventoryRepo;
        this.itemRepo = itemRepo;
        this.equipmentRepo = equipmentRepo;
    }

    public RoomAction action() {
        return RoomAction.USE_POTION;
    }

    public void execute(RoomDTO r, RoomMemberDTO me, ActionContext ctx) {
        Long userId = ctx.userId();
        Long inventoryItemId = ctx.inventoryItemId();
        if (inventoryItemId == null) {
            throw new IllegalArgumentException("USE_POTION 需要 inventoryItemId");
        }
        UserInventoryItem inv = inventoryRepo.findById(inventoryItemId)
                .orElseThrow(() -> new IllegalArgumentException("背包物品不存在"));
        if (!inv.getUserId().equals(userId)) {
            throw new IllegalStateException("不是你的物品");
        }
        Item item = itemRepo.findById(inv.getItemId())
                .orElseThrow(() -> new IllegalStateException("物品資料異常"));
        if (item.getType() != ItemType.CONSUMABLE) {
            throw new IllegalArgumentException("不是消耗品");
        }
        Integer heal = item.getHealAmount();
        if (heal == null || heal <= 0) {
            throw new IllegalArgumentException("此物品無回血效果");
        }

        int remain = inv.getQuantity() - 1;
        if (remain <= 0) {
            equipmentRepo.findByUserIdAndInventoryItemId(userId, inventoryItemId)
                    .ifPresent(equipmentRepo::delete);
            inventoryRepo.delete(inv);
        } else {
            inv.setQuantity(remain);
            inventoryRepo.save(inv);
        }

        int before = me.getHp();
        int after = Math.min(me.getMaxHp(), before + heal);
        me.setHp(after);
        r.getLog().add(String.format("第 %d 回合：%s 喝 %s 回 %d HP（%d → %d）",
                r.getRound(), BattleText.displayOf(me), item.getName(), after - before, before, after));
    }
}
