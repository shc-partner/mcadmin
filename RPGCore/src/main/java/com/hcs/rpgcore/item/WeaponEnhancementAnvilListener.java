package com.hcs.rpgcore.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.kyori.adventure.text.Component;

import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.InventoryView;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;


public final class WeaponEnhancementAnvilListener
        implements Listener {

    private final WeaponEnhancementService
            enhancementService;

    /*
     * NPC 16이 연 모루만 별도로 식별한다.
     * 일반 모루 강화와 분리하기 위한 정보.
     */
    private final Map<UUID, InventoryView> npcAnvilViews =
            new HashMap<>();


    public WeaponEnhancementAnvilListener(
            WeaponEnhancementService enhancementService
    ) {

        this.enhancementService =
                enhancementService;
    }


    /*
     * =========================================================
     * NPC 16 - WEAPON ENHANCEMENT ANVIL
     * =========================================================
     */
    @EventHandler
    public void onNpcRightClick(
            NPCRightClickEvent event
    ) {

        if (event.getNPC().getId() != 16) {
            return;
        }

        Player player = event.getClicker();

        InventoryView view =
                MenuType.ANVIL.create(
                        player,
                        Component.text("무기 강화")
                );

        player.openInventory(view);

        npcAnvilViews.put(
                player.getUniqueId(),
                player.getOpenInventory()
        );
    }


    /*
     * NPC 전용 모루 여부.
     * 후속 강화 이벤트 분리 단계에서 사용한다.
     */
    private boolean isNpcAnvil(
            Player player,
            InventoryView view
    ) {

        InventoryView trackedView =
                npcAnvilViews.get(
                        player.getUniqueId()
                );

        return trackedView != null
                && trackedView.getTopInventory()
                        == view.getTopInventory();
    }


    /*
     * NPC 모루를 닫으면 식별 정보를 정리한다.
     */
    @EventHandler
    public void onNpcAnvilClose(
            InventoryCloseEvent event
    ) {

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (isNpcAnvil(player, event.getView())) {
            npcAnvilViews.remove(
                    player.getUniqueId()
            );
        }
    }


    /*
     * =========================================================
     * PREPARE
     * =========================================================
     *
     * LEFT  : 강화 가능 무기
     * RIGHT : 강화석
     */
    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onPrepare(
            PrepareAnvilEvent event
    ) {

        Inventory inventory =
                event.getInventory();


        ItemStack weapon =
                inventory.getItem(
                        0
                );


        ItemStack stone =
                inventory.getItem(
                        1
                );


        /*
         * 오른쪽 슬롯이 강화석이 아니면
         * 기존 바닐라 모루 동작에는 개입하지 않는다.
         */
        boolean normalStone =
                enhancementService.isEnhancementStone(stone);

        boolean npcBetterStone =
                event.getView().getPlayer() instanceof Player player
                && isNpcAnvil(player, event.getView())
                && enhancementService.isBetterEnhancementStone(stone);

        if (!normalStone && !npcBetterStone) {
            return;
        }


        /*
         * 강화석을 넣었는데 올바른 강화 무기가 아니면
         * 결과를 생성하지 않는다.
         */
        if (
                !enhancementService
                        .canEnhance(
                                weapon
                        )
        ) {

            event.setResult(
                    null
            );

            return;
        }


        ItemStack preview =
                weapon.clone();


        /*
         * 결과 슬롯은 강화 "시도" 버튼 역할.
         *
         * 실제 단계 변경은 클릭 순간에 한다.
         */
        event.setResult(
                preview
        );


        event.getView()
                .setRepairCost(
                        0
                );
    }


    /*
     * =========================================================
     * RESULT CLICK
     * =========================================================
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = false
    )
    public void onResultClick(
            InventoryClickEvent event
    ) {

        if (
                event.getView()
                        .getTopInventory()
                        .getType()
                        != InventoryType.ANVIL
        ) {
            return;
        }


        /*
         * 모루 결과 슬롯.
         */
        if (
                event.getRawSlot()
                        != 2
        ) {
            return;
        }


        Inventory inventory =
                event.getView()
                        .getTopInventory();


        ItemStack weapon =
                inventory.getItem(
                        0
                );

        ItemStack stone =
                inventory.getItem(
                        1
                );


        boolean normalStone =
                enhancementService.isEnhancementStone(stone);

        boolean npcBetterStone =
                event.getWhoClicked() instanceof Player npcPlayer
                && isNpcAnvil(npcPlayer, event.getView())
                && enhancementService.isBetterEnhancementStone(stone);

        if (!normalStone && !npcBetterStone) {
            return;
        }


        if (
                !enhancementService
                        .canEnhance(
                                weapon
                        )
        ) {
            return;
        }


        HumanEntity whoClicked =
                event.getWhoClicked();


        if (!(whoClicked instanceof Player player)) {
            return;
        }


        /*
         * 바닐라 모루 결과 처리를 차단하고
         * RPGCore가 직접 소비/결과 지급한다.
         */
        event.setCancelled(
                true
        );


        ItemStack enhancedWeapon =
                weapon.clone();


        WeaponEnhancementService.AttemptResult result =
                enhancementService.attempt(
                        enhancedWeapon,
                        npcBetterStone
                );


        if (result == null) {
            return;
        }


        /*
         * 왼쪽 무기는 강화된 무기로 교체되므로
         * 원래 입력 무기를 모루에서 제거한다.
         */
        inventory.setItem(
                0,
                null
        );


        /*
         * 강화석 정확히 1개 소비.
         */
        if (
                stone.getAmount()
                        <= 1
        ) {

            inventory.setItem(
                    1,
                    null
            );

        } else {

            ItemStack remainingStone =
                    stone.clone();

            remainingStone.setAmount(
                    stone.getAmount() - 1
            );

            inventory.setItem(
                    1,
                    remainingStone
            );
        }


        inventory.setItem(
                2,
                null
        );


        /*
         * 일반 클릭이면 커서에 지급.
         *
         * 커서가 이미 차 있거나 Shift 클릭이면
         * 인벤토리로 지급한다.
         */
        ItemStack cursor =
                player.getItemOnCursor();


        if (
                !event.isShiftClick()
                &&
                (
                        cursor == null
                        ||
                        cursor.getType()
                                == Material.AIR
                )
        ) {

            player.setItemOnCursor(
                    enhancedWeapon
            );

        } else {

            Map<Integer, ItemStack> leftovers =
                    player.getInventory()
                            .addItem(
                                    enhancedWeapon
                            );


            for (
                    ItemStack leftover
                    : leftovers.values()
            ) {

                player.getWorld()
                        .dropItemNaturally(
                                player.getLocation(),
                                leftover
                        );
            }
        }


        switch (
                result.outcome()
        ) {

            case SUCCESS ->

                    player.sendMessage(
                            "§a[강화] 강화에 성공했습니다! §f+"
                                    + result.oldLevel()
                                    + " §7→ §a+"
                                    + result.newLevel()
                    );


            case KEEP ->

                    player.sendMessage(
                            "§e[강화] 강화에 실패했습니다. "
                                    + "강화 단계가 유지됩니다. §f(+"
                                    + result.newLevel()
                                    + ")"
                    );


            case DOWN ->

                    player.sendMessage(
                            "§c[강화] 강화에 실패하여 단계가 하락했습니다. §f+"
                                    + result.oldLevel()
                                    + " §7→ §c+"
                                    + result.newLevel()
                    );
        }
    }
}
