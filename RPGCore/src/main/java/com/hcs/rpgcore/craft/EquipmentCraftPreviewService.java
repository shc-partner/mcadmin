package com.hcs.rpgcore.craft;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.item.CustomItemFactory;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/**
 * NPC 18번 전용 장비 제작 미리보기 서비스.
 *
 * NPC를 새로 생성하거나 삭제하지 않는다.
 * 기존 NPC 18번의 장비만 일시적으로 교체한다.
 *
 * 모든 메서드는 서버 메인 스레드에서 호출해야 한다.
 */
public final class EquipmentCraftPreviewService {

    private static final int PREVIEW_NPC_ID = 18;

    private final RPGCorePlugin plugin;
    private final CustomItemFactory itemFactory;

    private boolean previewActive;

    private final Map<EquipmentSlot, ItemStack>
            originalEquipment =
            new EnumMap<>(EquipmentSlot.class);

    private static final EquipmentSlot[] PREVIEW_SLOTS = {
            EquipmentSlot.HELMET,
            EquipmentSlot.CHESTPLATE,
            EquipmentSlot.LEGGINGS,
            EquipmentSlot.BOOTS,
            EquipmentSlot.HAND,
            EquipmentSlot.OFF_HAND
    };

    public EquipmentCraftPreviewService(
            RPGCorePlugin plugin
    ) {
        this.plugin = plugin;
        this.itemFactory = new CustomItemFactory(plugin);
    }

    /**
     * 선택한 장비 하나를 미리보기한다.
     *
     * 무기: HAND
     * 방패/날개: OFF_HAND
     * 방어구: 해당 방어구 슬롯
     */
    public boolean previewSingle(
            Player player,
            String itemId,
            EquipmentSlot slot
    ) {
        if (itemId == null || slot == null) {
            return false;
        }

        Map<EquipmentSlot, String> items =
                new EnumMap<>(EquipmentSlot.class);

        items.put(slot, itemId);

        return preview(player, items);
    }

    /**
     * 여러 장비를 동시에 미리보기한다.
     *
     * 향후 set_id로 조회한 방어구와 날개를
     * 이 메서드에 전달한다.
     */
    public boolean preview(
            Player player,
            Map<EquipmentSlot, String> itemIds
    ) {
        requireMainThread();

        if (player == null
                || itemIds == null
                || itemIds.isEmpty()) {
            return false;
        }

        NPC npc = getPreviewNpc();

        if (npc == null || !npc.isSpawned()) {
            player.sendMessage(
                    "장비 미리보기 NPC 18번을 찾을 수 없습니다."
            );
            return false;
        }

        Equipment equipment =
                npc.getOrAddTrait(Equipment.class);

        /*
         * 기존 NPC 장비를 변경하기 전에
         * 모든 미리보기 아이템을 먼저 생성한다.
         */
        Map<EquipmentSlot, ItemStack> previewItems =
                new EnumMap<>(EquipmentSlot.class);

        for (Map.Entry<EquipmentSlot, String> entry
                : itemIds.entrySet()) {

            EquipmentSlot slot = entry.getKey();
            String itemId = entry.getValue();

            if (!isPreviewSlot(slot)
                    || itemId == null
                    || itemId.startsWith("mal_nyun_")) {

                player.sendMessage(
                        "미리보기할 수 없는 장비입니다."
                );
                return false;
            }

            ItemStack item = itemFactory.create(itemId);

            if (item == null || item.getType().isAir()) {
                player.sendMessage(
                        "장비 아이템을 생성하지 못했습니다: "
                                + itemId
                );
                return false;
            }

            previewItems.put(
                    slot,
                    item.clone()
            );
        }

        /*
         * 최초 미리보기에서만 NPC 18번의 원래 장비를 보존한다.
         * 다른 플레이어가 장비를 교체해도 원본을 덮어쓰지 않는다.
         */
        if (!previewActive) {

            originalEquipment.clear();

            for (EquipmentSlot slot : PREVIEW_SLOTS) {

                ItemStack original = equipment.get(slot);

                originalEquipment.put(
                        slot,
                        original == null
                                ? null
                                : original.clone()
                );
            }
        }

        previewActive = true;

        /*
         * 선택한 장비의 슬롯만 교체한다.
         * 다른 슬롯의 기존 미리보기 장비는 유지한다.
         */
        for (Map.Entry<EquipmentSlot, ItemStack> entry
                : previewItems.entrySet()) {

            equipment.set(
                    entry.getKey(),
                    entry.getValue()
            );
        }

        return true;
    }

    /**
     * 누가 실행했는지와 관계없이 공용 미리보기를 종료한다.
     *
     * NPC 18번은 유지하고,
     * 최초 미리보기 이전의 장비를 복원한다.
     */
    public void release(
            Player player
    ) {
        requireMainThread();

        if (player == null || !previewActive) {
            return;
        }

        restoreOriginalEquipment();
    }

    /**
     * NPC 18번의 장비를 전부 해제한다.
     *
     * 저장된 원래 장비 정보도 폐기하여
     * 미리보기 종료나 서버 종료 시 다시 복원되지 않게 한다.
     */
    public boolean resetEquipment() {

        requireMainThread();

        NPC npc = getPreviewNpc();

        if (npc == null || !npc.isSpawned()) {
            return false;
        }

        Equipment equipment =
                npc.getOrAddTrait(Equipment.class);

        clearEquipment(equipment);

        originalEquipment.clear();
        previewActive = false;

        return true;
    }

    /**
     * 플러그인 종료 시 호출한다.
     */
    public void shutdown() {
        requireMainThread();

        restoreOriginalEquipment();
    }

    private void restoreOriginalEquipment() {

        if (!previewActive) {
            return;
        }

        NPC npc = getPreviewNpc();

        if (npc != null && npc.isSpawned()) {

            Equipment equipment =
                    npc.getOrAddTrait(Equipment.class);

            clearEquipment(equipment);

            for (EquipmentSlot slot : PREVIEW_SLOTS) {

                ItemStack original =
                        originalEquipment.get(slot);

                if (original != null) {

                    equipment.set(
                            slot,
                            original.clone()
                    );
                }
            }
        }

        originalEquipment.clear();
        previewActive = false;
    }

    private void clearEquipment(
            Equipment equipment
    ) {
        for (EquipmentSlot slot : PREVIEW_SLOTS) {

            equipment.set(
                    slot,
                    null
            );
        }
    }

    private NPC getPreviewNpc() {

        return CitizensAPI
                .getNPCRegistry()
                .getById(PREVIEW_NPC_ID);
    }

    private boolean isPreviewSlot(
            EquipmentSlot slot
    ) {
        for (EquipmentSlot allowed : PREVIEW_SLOTS) {

            if (allowed == slot) {
                return true;
            }
        }

        return false;
    }

    private void requireMainThread() {

        if (!Bukkit.isPrimaryThread()) {

            throw new IllegalStateException(
                    "Equipment preview must run "
                            + "on the server main thread."
            );
        }
    }
}
