package com.hcs.rpgcore.dismantle;

import com.hcs.rpgcore.RPGCorePlugin;

import java.sql.SQLException;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class EquipmentDismantleListener
        implements Listener {

    private final RPGCorePlugin plugin;

    private final EquipmentDismantleService dismantleService;

    private final EquipmentDismantleReturnRepository
            returnRepository;

    private final EquipmentDismantleRecoveryStore
            recoveryStore;

    private final Map<UUID, Inventory> activeInventories =
            new HashMap<>();


    public EquipmentDismantleListener(
            RPGCorePlugin plugin
    ) {

        this.plugin = plugin;
        this.dismantleService =
                new EquipmentDismantleService(plugin);

        this.returnRepository =
                plugin.getDismantleReturnRepository();

        this.recoveryStore =
                new EquipmentDismantleRecoveryStore(
                        plugin.getDataFolder().toPath()
                );
    }

    /*
     * =========================================================
     * CLICK
     * =========================================================
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {

        Inventory top = event.getView().getTopInventory();

        if (!(top.getHolder() instanceof EquipmentDismantleHolder)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }

        /*
         * 기존 분해 GUI에 장비가 남아 있으면 새 GUI가
         * 추적 정보를 덮어쓰지 못하도록 차단한다.
         */
        UUID playerId = player.getUniqueId();
        Inventory existing = activeInventories.get(playerId);

        if (existing != null && existing != top) {

            ItemStack existingInput = existing.getItem(
                    EquipmentDismantleGui.INPUT_SLOT
            );

            if (!isEmpty(existingInput)) {

                event.setCancelled(true);

                Bukkit.getScheduler().runTask(
                        plugin,
                        () -> {

                            if (!player.isOnline()
                                    || !plugin.isEnabled()) {
                                return;
                            }

                            if (activeInventories.get(playerId)
                                    != existing) {
                                return;
                            }

                            if (isEmpty(existing.getItem(
                                    EquipmentDismantleGui.INPUT_SLOT
                            ))) {
                                return;
                            }

                            player.openInventory(existing);
                        }
                );

                return;
            }

            activeInventories.remove(playerId, existing);
        }

        activeInventories.put(playerId, top);

        int rawSlot = event.getRawSlot();

        /*
         * 플레이어 인벤토리에서는 일반적인 아이템 선택을 허용한다.
         * 단, 상단 GUI로 아이템이 자동 이동하는 동작은 차단한다.
         */
        if (rawSlot >= top.getSize()) {

            if (event.isShiftClick()
                    || event.getAction()
                            == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    || event.getAction()
                            == InventoryAction.COLLECT_TO_CURSOR) {

                event.setCancelled(true);
            }

            return;
        }

        /*
         * GUI 외부 및 상단 슬롯의 기본 동작을 차단한다.
         * 투입 슬롯만 아래에서 직접 처리한다.
         */
        if (rawSlot < 0) {
            return;
        }

        event.setCancelled(true);

        if (rawSlot == EquipmentDismantleGui.DISMANTLE_SLOT) {

            if (event.getClick() != ClickType.LEFT
                    && event.getClick() != ClickType.RIGHT) {
                return;
            }

            ItemStack equipment = top.getItem(
                    EquipmentDismantleGui.INPUT_SLOT
            );

            if (isEmpty(equipment)) {
                player.sendMessage(
                        "§e[분해] 분해할 장비를 올려주세요."
                );
                return;
            }

            EquipmentDismantleService.DismantlePreview preview;

            try {
                preview = dismantleService.preview(equipment);

            } catch (SQLException | RuntimeException exception) {

                plugin.getLogger().warning(
                        "Equipment dismantle preview failed: "
                                + exception.getMessage()
                );

                player.sendMessage(
                        "§c[분해] 장비 정보를 확인하지 못했습니다."
                );
                return;
            }

            if (preview == null) {
                player.sendMessage(
                        "§c[분해] 분해할 수 없는 장비입니다."
                );
                return;
            }

            /*
             * 제작서 1개를 지급할 공간이 있는지 확인한다.
             * 공간이 없으면 장비를 소비하지 않는다.
             */
            ItemStack reward = preview.recipe().clone();
            reward.setAmount(1);

            Map<Integer, ItemStack> leftovers =
                    player.getInventory().addItem(reward);

            if (!leftovers.isEmpty()) {
                player.sendMessage(
                        "§e[분해] 제작서를 받을 인벤토리 공간이 없습니다."
                );
                return;
            }

            /*
             * 제작서 지급 후 투입한 장비 1개를 소비한다.
             */
            top.setItem(
                    EquipmentDismantleGui.INPUT_SLOT,
                    null
            );

            player.sendMessage(
                    "§a[분해] 장비를 분해하고 제작서 1개를 획득했습니다."
            );

            return;
        }

        if (rawSlot != EquipmentDismantleGui.INPUT_SLOT) {
            return;
        }

        /*
         * 첫 단계에서는 일반적인 좌클릭·우클릭만 허용한다.
         * 숫자키, Shift 클릭, 더블클릭 등은 처리하지 않는다.
         */
        if (event.getClick() != ClickType.LEFT
                && event.getClick() != ClickType.RIGHT) {
            return;
        }

        ItemStack input = top.getItem(
                EquipmentDismantleGui.INPUT_SLOT
        );

        ItemStack cursor = event.getCursor();

        boolean inputEmpty = isEmpty(input);
        boolean cursorEmpty = isEmpty(cursor);

        /*
         * 빈 투입 슬롯에 커서의 아이템 1개를 넣는다.
         */
        if (inputEmpty && !cursorEmpty) {

            ItemStack singleItem = cursor.clone();
            singleItem.setAmount(1);

            top.setItem(
                    EquipmentDismantleGui.INPUT_SLOT,
                    singleItem
            );

            if (cursor.getAmount() == 1) {

                player.setItemOnCursor(null);

            } else {

                ItemStack remaining = cursor.clone();

                remaining.setAmount(
                        cursor.getAmount() - 1
                );

                player.setItemOnCursor(remaining);
            }

            return;
        }

        /*
         * 빈 커서로 투입 슬롯을 클릭하면 장비를 회수한다.
         * 커서에 다른 아이템이 있으면 교체하지 않는다.
         */
        if (!inputEmpty && cursorEmpty) {

            top.setItem(
                    EquipmentDismantleGui.INPUT_SLOT,
                    null
            );

            player.setItemOnCursor(input);
        }
    }


    /*
     * =========================================================
     * DRAG
     * =========================================================
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {

        Inventory top = event.getView().getTopInventory();

        if (!(top.getHolder() instanceof EquipmentDismantleHolder)) {
            return;
        }

        int topSize = top.getSize();

        boolean touchesTop = event.getRawSlots()
                .stream()
                .anyMatch(slot -> slot >= 0 && slot < topSize);

        if (touchesTop) {
            event.setCancelled(true);
        }
    }


    /*
     * =========================================================
     * CLOSE - RETURN INPUT ITEM
     * =========================================================
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClose(InventoryCloseEvent event) {

        Inventory top = event.getView().getTopInventory();

        if (!(top.getHolder() instanceof EquipmentDismantleHolder)) {
            return;
        }

        if (event.getPlayer() instanceof Player player) {
            activeInventories.put(
                    player.getUniqueId(),
                    top
            );
        }

        ItemStack input = top.getItem(
                EquipmentDismantleGui.INPUT_SLOT
        );

        if (isEmpty(input)) {
            if (event.getPlayer() instanceof Player player) {
                activeInventories.remove(
                        player.getUniqueId(),
                        top
                );
            }
            return;
        }

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        /*
         * 접속 종료로 닫히는 경우에는 반환이나 GUI 재개방을
         * 시도하지 않는다. 장비는 기존 GUI에 유지하고,
         * PlayerQuitEvent에서 DB에 저장한다.
         */
        if (event.getReason()
                == InventoryCloseEvent.Reason.DISCONNECT) {
            return;
        }

        /*
         * 일반적인 ESC 종료 외에는 GUI를 강제로 다시 열지 않는다.
         * 장비는 기존 GUI 및 activeInventories에 유지한다.
         */
        if (event.getReason()
                != InventoryCloseEvent.Reason.PLAYER) {
            return;
        }

        /*
         * 먼저 인벤토리에 반환을 시도한다.
         * 반환에 실패한 아이템은 기존 GUI에 유지한다.
         */
        Map<Integer, ItemStack> leftovers =
                player.getInventory().addItem(input.clone());

        if (leftovers.isEmpty()) {

            top.setItem(
                    EquipmentDismantleGui.INPUT_SLOT,
                    null
            );

            activeInventories.remove(
                    player.getUniqueId(),
                    top
            );

            return;
        }

        /*
         * 분해 GUI에는 일반적으로 아이템 1개가 들어간다.
         * 일부만 반환된 경우에도 남은 수량만 GUI에 보관한다.
         */
        ItemStack remaining =
                leftovers.values().iterator().next().clone();

        top.setItem(
                EquipmentDismantleGui.INPUT_SLOT,
                remaining
        );

        /*
         * 접속 종료나 플러그인 종료 중에는 GUI를 다시 열 수 없다.
         * 이 경우에는 장비 유실을 피하기 위해 기존 반환 방식을 사용한다.
         */
        if (!player.isOnline() || !plugin.isEnabled()) {

            /*
             * GUI를 다시 열 수 없는 상황에서는 영구 보관한다.
             * 저장 성공 시에만 GUI에서 장비를 제거한다.
             */
            if (persistPendingEquipment(
                    player.getUniqueId(),
                    remaining
            )) {

                top.setItem(
                        EquipmentDismantleGui.INPUT_SLOT,
                        null
                );

                activeInventories.remove(
                        player.getUniqueId(),
                        top
                );

            } else {

                plugin.getLogger().severe(
                        "CRITICAL: Dismantle equipment could not be "
                                + "persisted during inventory close: "
                                + player.getUniqueId()
                );
            }

            return;
        }

        player.sendMessage(
                "§e[분해] 장비를 반환할 공간이 없습니다. "
                        + "인벤토리를 정리한 후 창을 닫아주세요."
        );

        /*
         * 닫힌 GUI의 동일한 Inventory 객체를 다음 틱에 다시 연다.
         */
        Bukkit.getScheduler().runTask(
                plugin,
                () -> {

                    if (!player.isOnline()) {
                        return;
                    }

                    if (!plugin.isEnabled()) {
                        return;
                    }

                    /*
                     * 로그아웃·서버 종료·다른 반환 처리로
                     * 추적 상태가 변경됐다면 다시 열지 않는다.
                     */
                    if (activeInventories.get(
                            player.getUniqueId()
                    ) != top) {
                        return;
                    }

                    if (isEmpty(top.getItem(
                            EquipmentDismantleGui.INPUT_SLOT
                    ))) {
                        return;
                    }

                    /*
                     * 그사이에 다른 GUI가 열렸으면 덮어쓰지 않는다.
                     * 분해 장비는 추적 상태에 계속 보관한다.
                     */
                    if (player.getOpenInventory()
                            .getTopInventory()
                            .getType()
                            != org.bukkit.event.inventory.InventoryType.CRAFTING) {
                        return;
                    }

                    player.openInventory(top);
                }
        );
    }


    /*
     * =========================================================
     * QUIT - PERSIST REGISTERED EQUIPMENT
     * =========================================================
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        Inventory top = activeInventories.get(playerId);

        if (top == null) {
            return;
        }

        ItemStack input = top.getItem(
                EquipmentDismantleGui.INPUT_SLOT
        );

        if (isEmpty(input)) {
            activeInventories.remove(playerId, top);
            return;
        }

        if (persistPendingEquipment(playerId, input)) {

            top.setItem(
                    EquipmentDismantleGui.INPUT_SLOT,
                    null
            );

            activeInventories.remove(playerId, top);

            plugin.getLogger().info(
                    "Dismantle equipment persisted on quit: "
                            + playerId
            );

        } else {

            plugin.getLogger().severe(
                    "CRITICAL: Dismantle equipment remains "
                            + "unpersisted on quit: "
                            + playerId
            );
        }

    }


    /*
     * =========================================================
     * SHUTDOWN - PERSIST PENDING EQUIPMENT
     * =========================================================
     */
    public void savePendingOnShutdown() {

        for (Map.Entry<UUID, Inventory> entry
                : new HashMap<>(activeInventories).entrySet()) {

            UUID playerId = entry.getKey();
            Inventory top = entry.getValue();

            ItemStack input = top.getItem(
                    EquipmentDismantleGui.INPUT_SLOT
            );

            if (isEmpty(input)) {
                activeInventories.remove(playerId, top);
                continue;
            }

            if (persistPendingEquipment(playerId, input)) {

                top.setItem(
                        EquipmentDismantleGui.INPUT_SLOT,
                        null
                );

                activeInventories.remove(playerId, top);

                plugin.getLogger().info(
                        "Dismantle equipment persisted on shutdown: "
                                + playerId
                );

            } else {

                plugin.getLogger().severe(
                        "CRITICAL: Dismantle equipment remains "
                                + "unpersisted on shutdown: "
                                + playerId
                );
            }

        }
    }


    /*
     * =========================================================
     * NPC 17 - OPEN OR CHECK PENDING RETURN
     * =========================================================
     */
    public void openOrReturn(Player player) {

        try {

            ItemStack pending = returnRepository.load(
                    player.getUniqueId()
            );

            /*
             * DB와 복구 파일에 모두 기록이 있으면
             * 중복 지급 가능성이 있으므로 자동 반환하지 않는다.
             */
            if (!isEmpty(pending)
                    && recoveryStore.exists(player.getUniqueId())) {

                plugin.getLogger().severe(
                        "Dismantle equipment exists in both DB and "
                                + "recovery file: "
                                + player.getUniqueId()
                );

                player.sendMessage(
                        "§c[분해] 보관 장비 기록이 중복되어 "
                                + "자동 반환을 중단했습니다. "
                                + "관리자에게 문의해주세요."
                );

                return;
            }

            if (!isEmpty(pending)) {

                /*
                 * 일반 인벤토리 0~35번 중 빈칸 한 곳에만 반환한다.
                 * 공간이 없으면 DB에 보관된 장비를 유지한다.
                 */
                int emptySlot = -1;

                for (int slot = 0; slot < 36; slot++) {

                    if (isEmpty(player.getInventory().getItem(slot))) {
                        emptySlot = slot;
                        break;
                    }
                }

                if (emptySlot < 0) {

                    player.sendMessage(
                            "§e[분해] 반환 대기 중인 장비가 있습니다. "
                                    + "인벤토리에 빈칸 1개를 확보한 뒤 "
                                    + "NPC를 다시 클릭해주세요."
                    );

                    return;
                }

                /*
                 * DB 기록 삭제에 실패하면 장비 지급을 되돌린다.
                 */
                player.getInventory().setItem(
                        emptySlot,
                        pending.clone()
                );

                try {

                    int deleted = returnRepository.delete(
                            player.getUniqueId()
                    );

                    if (deleted != 1) {
                        throw new SQLException(
                                "반환 장비 DB 기록 삭제 건수가 1이 아닙니다: "
                                        + deleted
                        );
                    }

                } catch (SQLException | RuntimeException exception) {

                    player.getInventory().setItem(
                            emptySlot,
                            null
                    );

                    throw exception;
                }

                player.sendMessage(
                        "§a[분해] 보관 중이던 장비를 인벤토리로 반환했습니다."
                );

                return;
            }

        } catch (SQLException | RuntimeException exception) {

            plugin.getLogger().severe(
                    "Failed to check pending dismantle equipment: "
                            + player.getUniqueId()
                            + " / "
                            + exception.getMessage()
            );

            player.sendMessage(
                    "§c[분해] 보관 장비를 확인하지 못했습니다. "
                            + "잠시 후 다시 시도해주세요."
            );

            return;
        }

        /*
         * 이전 분해 GUI에 장비가 남아 있으면
         * 새 GUI를 생성하지 않고 동일한 Inventory를 다시 연다.
         */
        UUID playerId = player.getUniqueId();
        Inventory existing = activeInventories.get(playerId);

        if (existing != null) {

            ItemStack existingInput = existing.getItem(
                    EquipmentDismantleGui.INPUT_SLOT
            );

            if (!isEmpty(existingInput)) {

                if (player.getOpenInventory()
                        .getTopInventory() != existing) {

                    player.openInventory(existing);
                }

                return;
            }

            activeInventories.remove(playerId, existing);
        }

        /*
         * DB 반환 장비가 없는 경우 복구 파일을 확인한다.
         */
        try {

            ItemStack recovered = recoveryStore.load(
                    player.getUniqueId()
            );

            if (!isEmpty(recovered)) {

                int emptySlot = -1;

                for (int slot = 0; slot < 36; slot++) {

                    if (isEmpty(player.getInventory().getItem(slot))) {
                        emptySlot = slot;
                        break;
                    }
                }

                if (emptySlot < 0) {

                    player.sendMessage(
                            "§e[분해] 반환 대기 중인 장비가 있습니다. "
                                    + "인벤토리에 빈칸 1개를 확보한 뒤 "
                                    + "NPC를 다시 클릭해주세요."
                    );

                    return;
                }

                player.getInventory().setItem(
                        emptySlot,
                        recovered.clone()
                );

                try {

                    recoveryStore.delete(
                            player.getUniqueId()
                    );

                } catch (IOException | RuntimeException exception) {

                    /*
                     * 파일 삭제에 실패하면 지급을 되돌린다.
                     */
                    player.getInventory().setItem(
                            emptySlot,
                            null
                    );

                    throw exception;
                }

                player.sendMessage(
                        "§a[분해] 보관 중이던 장비를 인벤토리로 반환했습니다."
                );

                return;
            }

        } catch (IOException | RuntimeException exception) {

            plugin.getLogger().severe(
                    "Failed to return dismantle recovery item: "
                            + player.getUniqueId()
                            + " / "
                            + exception.getMessage()
            );

            player.sendMessage(
                    "§c[분해] 복구 장비를 확인하지 못했습니다. "
                            + "관리자에게 문의해주세요."
            );

            return;
        }

        new EquipmentDismantleGui().open(player);
    }


    /*
     * =========================================================
     * PERSIST PENDING EQUIPMENT
     * =========================================================
     */
    private boolean persistPendingEquipment(
            UUID playerId,
            ItemStack equipment
    ) {

        /*
         * DB 저장을 우선한다.
         */
        try {

            returnRepository.save(
                    playerId,
                    equipment.clone()
            );

            return true;

        } catch (SQLException | RuntimeException exception) {

            plugin.getLogger().warning(
                    "Dismantle DB save failed; trying recovery file: "
                            + playerId
                            + " / "
                            + exception.getMessage()
            );
        }

        /*
         * DB 저장 실패 시에만 복구 파일을 사용한다.
         */
        try {

            recoveryStore.save(
                    playerId,
                    equipment.clone()
            );

            plugin.getLogger().warning(
                    "Dismantle equipment saved to recovery file: "
                            + playerId
            );

            return true;

        } catch (IOException | RuntimeException exception) {

            plugin.getLogger().severe(
                    "CRITICAL: Both dismantle storage methods failed: "
                            + playerId
                            + " / "
                            + exception.getMessage()
            );

            return false;
        }
    }


    private boolean isEmpty(ItemStack item) {

        return item == null
                || item.getType() == Material.AIR
                || item.getAmount() <= 0;
    }
}
