package com.example.demo.service.impl;

import com.example.demo.dto.EnhanceFailEffect;
import com.example.demo.dto.EnhancePreviewResponse;
import com.example.demo.dto.EnhanceResultResponse;
import com.example.demo.dto.MaterialRequirementDTO;
import com.example.demo.entity.Item;
import com.example.demo.entity.ItemType;
import com.example.demo.entity.UserEquipment;
import com.example.demo.entity.UserInventoryItem;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.UserEquipmentRepository;
import com.example.demo.repository.UserInventoryItemRepository;
import com.example.demo.service.EnhanceService;
import com.example.demo.service.GoldService;
import com.example.demo.service.InventoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class EnhanceServiceImpl implements EnhanceService {

    private static final Logger log = LoggerFactory.getLogger(EnhanceServiceImpl.class);

    /** 強化等級上限。等級從 0（未強化）到 MAX_LEVEL。 */
    public static final int MAX_LEVEL = 10;

    /** 強化素材的 itemCode。若 items 表不存在這筆，視為「不需要素材」。 */
    public static final String MATERIAL_CODE = "ENHANCE_STONE";

    private final UserInventoryItemRepository inventoryRepo;
    private final UserEquipmentRepository equipmentRepo;
    private final ItemRepository itemRepo;
    private final GoldService goldService;
    private final InventoryService inventoryService;

    public EnhanceServiceImpl(UserInventoryItemRepository inventoryRepo,
                              UserEquipmentRepository equipmentRepo,
                              ItemRepository itemRepo,
                              GoldService goldService,
                              InventoryService inventoryService) {
        this.inventoryRepo = inventoryRepo;
        this.equipmentRepo = equipmentRepo;
        this.itemRepo = itemRepo;
        this.goldService = goldService;
        this.inventoryService = inventoryService;
    }

    // ========================================================
    //  規則表（之後可抽到設定檔／資料表）
    // ========================================================

    /** 從等級 fromLevel 強化到 fromLevel+1 的金幣成本。 */
    private long goldCostFor(int fromLevel) {
        return 100L * (fromLevel + 1);   // +0→+1=100, +1→+2=200, ... +9→+10=1000
    }

    /** 成功率（0~1）。 */
    private double successRateFor(int fromLevel) {
        switch (fromLevel) {
            case 0: return 1.00;
            case 1: return 0.95;
            case 2: return 0.90;
            case 3: return 0.80;
            case 4: return 0.70;
            case 5: return 0.60;
            case 6: return 0.45;
            case 7: return 0.30;
            case 8: return 0.20;
            case 9: return 0.10;
            default: return 0.0;
        }
    }

    /** 失敗效果。 */
    private EnhanceFailEffect failEffectFor(int fromLevel) {
        if (fromLevel < 4) return EnhanceFailEffect.NOTHING;
        if (fromLevel < 7) return EnhanceFailEffect.DOWNGRADE;
        return EnhanceFailEffect.DESTROY;
    }

    /** 強化所需素材數。+0→+1 不要素材，之後 = fromLevel。 */
    private int materialQtyFor(int fromLevel) {
        return Math.max(0, fromLevel);   // +0→+1: 0, +1→+2: 1, ... +9→+10: 9
    }

    /** 強化後的攻擊（含 +N 加成）。公式跟 InventoryServiceImpl 一致：base + base*level/10。 */
    private int attackAt(int base, int level) {
        return base + base * level / 10;
    }

    private int defenseAt(int base, int level) {
        return base + base * level / 10;
    }

    // ========================================================
    //  Public API
    // ========================================================

    @Override
    @Transactional(readOnly = true)
    public EnhancePreviewResponse preview(Long userId, Long inventoryItemId) {
        UserInventoryItem inv = loadOwnedEquipment(userId, inventoryItemId);
        Item item = itemRepo.findById(inv.getItemId())
                .orElseThrow(() -> new IllegalStateException("物品資料異常"));

        int currentLevel = inv.getEnhancementLevel() == null ? 0 : inv.getEnhancementLevel();

        EnhancePreviewResponse r = new EnhancePreviewResponse();
        r.setInventoryItemId(inv.getId());
        r.setItemCode(item.getCode());
        r.setItemName(item.getName());
        r.setCurrentLevel(currentLevel);
        r.setMaxLevel(MAX_LEVEL);
        r.setCurrentAttack(attackAt(item.getBaseAttack(), currentLevel));
        r.setCurrentDefense(defenseAt(item.getBaseDefense(), currentLevel));
        r.setGoldOwned(goldService.getGold(userId));

        // 已達上限：直接回不能強化
        if (currentLevel >= MAX_LEVEL) {
            r.setNextAttack(r.getCurrentAttack());
            r.setNextDefense(r.getCurrentDefense());
            r.setSuccessRate(0.0);
            r.setGoldCost(0);
            r.setMaterialCosts(List.of());
            r.setOnFail(EnhanceFailEffect.NOTHING);
            r.setCanEnhance(false);
            r.setBlockReason("已達強化上限");
            return r;
        }

        // 一般情況：算下一級資料
        r.setNextAttack(attackAt(item.getBaseAttack(), currentLevel + 1));
        r.setNextDefense(defenseAt(item.getBaseDefense(), currentLevel + 1));
        r.setSuccessRate(successRateFor(currentLevel));
        r.setGoldCost(goldCostFor(currentLevel));
        r.setOnFail(failEffectFor(currentLevel));

        // 素材
        List<MaterialRequirementDTO> mats = buildMaterialCosts(userId, currentLevel);
        r.setMaterialCosts(mats);

        // 判斷能否強化
        String block = null;
        if (r.getGoldOwned() < r.getGoldCost()) {
            block = "金幣不足";
        } else {
            for (MaterialRequirementDTO m : mats) {
                if (m.getQuantityOwned() < m.getQuantityNeeded()) {
                    block = m.getItemName() + "不足";
                    break;
                }
            }
        }
        r.setCanEnhance(block == null);
        r.setBlockReason(block);
        return r;
    }

    @Override
    @Transactional
    public EnhanceResultResponse enhance(Long userId, Long inventoryItemId) {
        UserInventoryItem inv = loadOwnedEquipment(userId, inventoryItemId);
        Item item = itemRepo.findById(inv.getItemId())
                .orElseThrow(() -> new IllegalStateException("物品資料異常"));

        int currentLevel = inv.getEnhancementLevel() == null ? 0 : inv.getEnhancementLevel();
        if (currentLevel >= MAX_LEVEL) {
            throw new IllegalArgumentException("已達強化上限");
        }

        long goldCost = goldCostFor(currentLevel);
        double rate = successRateFor(currentLevel);
        EnhanceFailEffect fail = failEffectFor(currentLevel);

        // 檢查金幣
        long goldOwned = goldService.getGold(userId);
        if (goldOwned < goldCost) {
            throw new IllegalArgumentException("金幣不足");
        }

        // 檢查＋扣素材
        int matQty = materialQtyFor(currentLevel);
        Optional<Item> matItemOpt = itemRepo.findByCode(MATERIAL_CODE);
        UserInventoryItem matInv = null;
        if (matQty > 0 && matItemOpt.isPresent()) {
            Item matItem = matItemOpt.get();
            matInv = inventoryRepo.findFirstByUserIdAndItemIdAndEnhancementLevel(userId, matItem.getId(), 0)
                    .orElseThrow(() -> new IllegalArgumentException(matItem.getName() + "不足"));
            if (matInv.getQuantity() < matQty) {
                throw new IllegalArgumentException(matItem.getName() + "不足");
            }
            int remain = matInv.getQuantity() - matQty;
            if (remain == 0) {
                inventoryRepo.delete(matInv);
            } else {
                matInv.setQuantity(remain);
                inventoryRepo.save(matInv);
            }
        }

        // 扣金幣（保留交易紀錄）
        long goldBalance = goldService.changeGold(
                userId, -goldCost, "ENHANCE", item.getCode(),
                "強化 " + item.getName() + " +" + currentLevel + "→+" + (currentLevel + 1)
        );

        // 判定成功 / 失敗
        boolean success = ThreadLocalRandom.current().nextDouble() < rate;

        EnhanceResultResponse out = new EnhanceResultResponse();
        out.setPreviousLevel(currentLevel);
        out.setGoldSpent(goldCost);
        out.setGoldBalance(goldBalance);
        out.setSuccess(success);
        out.setDestroyed(false);

        if (success) {
            int newLevel = currentLevel + 1;
            inv.setEnhancementLevel(newLevel);
            inventoryRepo.save(inv);
            out.setNewLevel(newLevel);
            out.setMessage("強化成功！+" + currentLevel + " → +" + newLevel);
            log.info("強化成功: userId={}, invItemId={}, {}→{}", userId, inventoryItemId, currentLevel, newLevel);
        } else {
            switch (fail) {
                case NOTHING: {
                    out.setNewLevel(currentLevel);
                    out.setMessage("強化失敗，等級不變（仍為 +" + currentLevel + "）");
                    break;
                }
                case DOWNGRADE: {
                    int newLevel = Math.max(0, currentLevel - 1);
                    inv.setEnhancementLevel(newLevel);
                    inventoryRepo.save(inv);
                    out.setNewLevel(newLevel);
                    out.setMessage("強化失敗，等級下降為 +" + newLevel);
                    break;
                }
                case DESTROY: {
                    // 銷毀：先清掉裝備格、再刪背包列
                    Optional<UserEquipment> ue = equipmentRepo.findByUserIdAndInventoryItemId(userId, inventoryItemId);
                    ue.ifPresent(equipmentRepo::delete);
                    if (ue.isPresent()) equipmentRepo.flush();
                    inventoryRepo.delete(inv);
                    out.setNewLevel(-1);
                    out.setDestroyed(true);
                    out.setMessage("強化失敗，" + item.getName() + " 消失了…");
                    break;
                }
            }
            log.info("強化失敗: userId={}, invItemId={}, fromLevel={}, effect={}, destroyed={}",
                    userId, inventoryItemId, currentLevel, fail, out.isDestroyed());
        }

        out.setInventory(inventoryService.listInventory(userId));
        return out;
    }

    // ========================================================
    //  helpers
    // ========================================================

    /** 取出某背包列，並驗證是該玩家的、是裝備類。 */
    private UserInventoryItem loadOwnedEquipment(Long userId, Long inventoryItemId) {
        if (inventoryItemId == null) {
            throw new IllegalArgumentException("inventoryItemId 為必填");
        }
        UserInventoryItem inv = inventoryRepo.findById(inventoryItemId)
                .orElseThrow(() -> new IllegalArgumentException("背包物品不存在"));
        if (!inv.getUserId().equals(userId)) {
            throw new IllegalStateException("不是你的物品");
        }
        Item item = itemRepo.findById(inv.getItemId())
                .orElseThrow(() -> new IllegalStateException("物品資料異常"));
        if (item.getType() != ItemType.EQUIPMENT) {
            throw new IllegalArgumentException("只有裝備能強化");
        }
        return inv;
    }

    /** 組成素材需求列表。若 items 表沒有強化石，回空 list。 */
    private List<MaterialRequirementDTO> buildMaterialCosts(Long userId, int currentLevel) {
        int qty = materialQtyFor(currentLevel);
        if (qty <= 0) return List.of();

        Optional<Item> matItemOpt = itemRepo.findByCode(MATERIAL_CODE);
        if (matItemOpt.isEmpty()) return List.of();   // 未配置素材 → 不要求

        Item matItem = matItemOpt.get();
        int owned = inventoryRepo.findFirstByUserIdAndItemIdAndEnhancementLevel(userId, matItem.getId(), 0)
                .map(UserInventoryItem::getQuantity)
                .orElse(0);

        List<MaterialRequirementDTO> list = new ArrayList<>(1);
        list.add(new MaterialRequirementDTO(
                matItem.getId(), matItem.getCode(), matItem.getName(), qty, owned
        ));
        return list;
    }
}
