package com.hcs.rpgcore.skill;

import java.util.Set;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;

import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.block.Action;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import org.bukkit.event.player.PlayerInteractEvent;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;


public final class SkillCollectionListener
        implements Listener {

    private final SkillItemFactory skillItemFactory;

    private final SkillCollectionMenu skillCollectionMenu;

    public SkillCollectionListener(
            SkillItemFactory skillItemFactory,
            SkillCollectionMenu skillCollectionMenu
    ) {

        this.skillItemFactory =
                skillItemFactory;

        this.skillCollectionMenu =
                skillCollectionMenu;
    }


    /*
     * =========================================================
     * CLASS SKILL TOME RIGHT CLICK
     * =========================================================
     */
    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = false
    )
    public void onClassSkillTomeUse(
            PlayerInteractEvent event
    ) {

        /*
         * PlayerInteractEvent는 양손 각각 발생할 수 있으므로
         * MAIN HAND만 처리한다.
         */
        if (
                event.getHand()
                        != EquipmentSlot.HAND
        ) {
            return;
        }


        Action action =
                event.getAction();


        if (
                action != Action.RIGHT_CLICK_AIR
                && action != Action.RIGHT_CLICK_BLOCK
        ) {
            return;
        }


        ItemStack item =
                event.getItem();


        if (
                !skillItemFactory.isClassSkillTome(
                        item
                )
        ) {
            return;
        }


        /*
         * 직업 스킬북 우클릭은
         * 블록 상호작용보다 도감 오픈을 우선한다.
         */
        event.setCancelled(
                true
        );


        Player player =
                event.getPlayer();


        /*
         * 귀속 소유자 확인.
         */
        if (
                !skillItemFactory.isOwnedBy(
                        item,
                        player.getUniqueId()
                )
        ) {

            player.sendActionBar(
                    Component.text(
                            "다른 플레이어에게 귀속된 스킬북입니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        String tomeClass =
                skillItemFactory.getSkillTomeClass(
                        item
                );


        if (
                tomeClass == null
                || tomeClass.isBlank()
        ) {

            player.sendActionBar(
                    Component.text(
                            "스킬북 직업 정보를 확인할 수 없습니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        skillCollectionMenu.open(
                player,
                tomeClass
        );
    }


    /*
     * =========================================================
     * SKILL COLLECTION CLICK
     * =========================================================
     *
     * 도감의 스킬은 실제 아이템처럼 클릭해서 들 수 있지만,
     * 이동 가능 위치는 플레이어 핫바 1~5로 제한한다.
     *
     * 도감 원본 아이콘 자체는 제거하지 않는다.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = false
    )
    public void onSkillCollectionClick(
            InventoryClickEvent event
    ) {

        String menuClass =
                skillCollectionMenu
                        .resolveMenuClass(
                                event.getView()
                                        .title()
                        );


        if (menuClass == null) {
            return;
        }


        if (
                !(event.getWhoClicked()
                        instanceof Player player)
        ) {
            return;
        }


        Inventory topInventory =
                event.getView()
                        .getTopInventory();


        Inventory clickedInventory =
                event.getClickedInventory();


        ItemStack cursor =
                event.getCursor();


        ItemStack current =
                event.getCurrentItem();


        /*
         * =====================================================
         * NUMBER KEY SWAP BLOCK
         * =====================================================
         *
         * 도감 위에서 숫자키를 눌러
         * GUI 아이콘과 실제 핫바 아이템을 교환하는 동작은
         * 완전히 금지한다.
         *
         * 취소 + DENY + 강제 동기화로
         * 클라이언트의 순간적인 핫바 교환 표시도 되돌린다.
         */
        if (
                clickedInventory == topInventory
                && event.getHotbarButton() >= 0
        ) {

            event.setCancelled(
                    true
            );

            event.setResult(
                    org.bukkit.event.Event.Result.DENY
            );

            player.updateInventory();

            return;
        }


        /*
         * Shift 클릭을 이용한 이동도 차단.
         */
        if (event.isShiftClick()) {

            if (
                    clickedInventory == topInventory
                    || isIndividualSkillItem(
                            current
                    )
            ) {

                event.setCancelled(
                        true
                );

                return;
            }
        }


        /*
         * =====================================================
         * OUTSIDE INVENTORY
         * =====================================================
         *
         * 가상 스킬 아이콘을 인벤토리 밖으로 버리지 못한다.
         */
        if (clickedInventory == null) {

            if (
                    isIndividualSkillItem(
                            cursor
                    )
            ) {

                event.setCancelled(
                        true
                );

                event.setCursor(
                        new ItemStack(
                                Material.AIR
                        )
                );

                player.sendActionBar(
                        Component.text(
                                "스킬을 내려놓았습니다.",
                                NamedTextColor.YELLOW
                        )
                );
            }

            return;
        }


        /*
         * =====================================================
         * PLAYER INVENTORY
         * =====================================================
         *
         * 개별 스킬 아이템은 플레이어 자신의
         * 일반 인벤토리 / 핫바 어디에나 배치 가능.
         */
        if (
                clickedInventory
                        == player.getInventory()
        ) {

            return;
        }


        /*
         * 도감 이외의 다른 인벤토리에는 개입하지 않는다.
         */
        if (
                clickedInventory
                        != topInventory
        ) {

            if (
                    isIndividualSkillItem(
                            cursor
                    )
            ) {

                event.setCancelled(
                        true
                );
            }

            return;
        }


        /*
         * 도감 GUI 아이템 자체는 Bukkit 기본 이동 금지.
         */
        event.setCancelled(
                true
        );


        int rawSlot =
                event.getRawSlot();


        /*
         * =====================================================
         * SKILL COLLECTION ICON
         * =====================================================
         */
        int skillIndex =
                skillCollectionMenu
                        .getSkillIndex(
                                rawSlot
                        );


        if (skillIndex < 0) {

            /*
             * 커서에 스킬을 들고
             * 장식 영역을 클릭해도 아이템은 유지한다.
             */
            return;
        }


        SkillDefinition definition =
                skillCollectionMenu
                        .getSkillDefinition(
                                menuClass,
                                skillIndex
                        );


        if (definition == null) {
            return;
        }


        /*
         * =====================================================
         * PASSIVE MOVEMENT SKILL
         * =====================================================
         *
         * 대쉬 / 텔레포트는 방향키 더블탭 전용이므로
         * 스킬 창에서 들거나 인벤토리/슬롯으로
         * 이동시킬 수 없다.
         */
                /*
         * 미해금 / 미래 스킬은 집을 수 없다.
         */
        if (
                !isIndividualSkillItem(
                        current
                )
        ) {

            player.sendActionBar(
                    Component.text(
                            "아직 사용할 수 없는 스킬입니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        if (
                !skillItemFactory.isOwnedBy(
                        current,
                        player.getUniqueId()
                )
        ) {

            player.sendActionBar(
                    Component.text(
                            "이 스킬의 소유자가 아닙니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        /*
         * 도감 원본은 그대로 두고
         * 복제 아이콘만 실제 커서에 올린다.
         */
        event.setCursor(
                current.clone()
        );


        player.sendActionBar(
                Component.text(
                        definition.displayName()
                                + " - 인벤토리 또는 핫바에 놓으세요.",
                        NamedTextColor.AQUA
                )
        );
    }




    /*
     * =========================================================
     * INVENTORY DRAG
     * =========================================================
     *
     * 스킬 아이템은 클릭 방식으로만 관리한다.
     *
     * 드래그를 허용하면 도감/일반 인벤토리로
     * 복수 슬롯 이동 우회가 가능하므로 차단한다.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = false
    )
    public void onSkillCollectionDrag(
            InventoryDragEvent event
    ) {

        String menuClass =
                skillCollectionMenu
                        .resolveMenuClass(
                                event.getView()
                                        .title()
                        );


        if (menuClass == null) {
            return;
        }


        int topSize =
                event.getView()
                        .getTopInventory()
                        .getSize();


        Set<Integer> rawSlots =
                event.getRawSlots();


        boolean touchesTop =
                rawSlots.stream()
                        .anyMatch(
                                rawSlot ->
                                        rawSlot < topSize
                        );


        if (touchesTop) {

            event.setCancelled(
                    true
            );
        }
    }


    /*
     * =========================================================
     * INVENTORY CLOSE
     * =========================================================
     *
     * 도감에서 복제한 스킬이 커서에 남은 상태로
     * GUI를 닫아서 실제 아이템으로 유출되는 것을 방지.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onSkillCollectionClose(
            InventoryCloseEvent event
    ) {

        String menuClass =
                skillCollectionMenu
                        .resolveMenuClass(
                                event.getView()
                                        .title()
                        );


        if (menuClass == null) {
            return;
        }


        if (
                !(event.getPlayer()
                        instanceof Player player)
        ) {
            return;
        }


        ItemStack cursor =
                player.getItemOnCursor();


        if (
                isIndividualSkillItem(
                        cursor
                )
        ) {

            player.setItemOnCursor(
                    new ItemStack(
                            Material.AIR
                    )
            );
        }
    }


    /*
     * =========================================================
     * INDIVIDUAL SKILL ITEM
     * =========================================================
     *
     * 직업 통합 스킬북(Tome)은 제외한다.
     *
     * SkillRegistry에 존재하는 실제 전투 스킬만
     * 장착 가능한 스킬 아이템으로 인정한다.
     */
    private boolean isIndividualSkillItem(
            ItemStack item
    ) {

        if (
                !skillItemFactory.isSkillItem(
                        item
                )
        ) {
            return false;
        }


        if (
                skillItemFactory.isClassSkillTome(
                        item
                )
        ) {
            return false;
        }


        String skillId =
                skillItemFactory.getSkillId(
                        item
                );


        if (
                skillId == null
                || skillId.isBlank()
        ) {
            return false;
        }


        return SkillRegistry.find(
                skillId
        ) != null;
    }
}
