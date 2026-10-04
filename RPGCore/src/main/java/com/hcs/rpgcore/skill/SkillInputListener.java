package com.hcs.rpgcore.skill;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;


public final class SkillInputListener
        implements Listener {

    private final SkillItemFactory skillItemFactory;

    private final SkillInputWindowService
            inputWindowService;


    public SkillInputListener(
            SkillItemFactory skillItemFactory,
            SkillInputWindowService inputWindowService
    ) {

        this.skillItemFactory =
                skillItemFactory;

        this.inputWindowService =
                inputWindowService;
    }


    /*
     * =========================================================
     * RIGHT CLICK -> SKILL INPUT WINDOW
     * =========================================================
     */
    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = false
    )
    public void onRightClick(
            PlayerInteractEvent event
    ) {

        if (
                event.getHand()
                        != EquipmentSlot.HAND
        ) {
            return;
        }


        Action action =
                event.getAction();

        /*
         * =====================================================
         * GLIDE BOOST INPUT
         * =====================================================
         *
         * 활공 중 우클릭은 신화 활공 장비의
         * 추진 입력으로 사용한다.
         *
         * Dash / Teleport 입력창은 열지 않는다.
         */
        if (
                event.getPlayer()
                        .isGliding()
        ) {
            return;
        }


        if (
                action != Action.RIGHT_CLICK_AIR
                && action != Action.RIGHT_CLICK_BLOCK
        ) {
            return;
        }


        /*
         * =====================================================
         * INTERACTABLE BLOCK
         * =====================================================
         *
         * 문 / 상자 / 버튼 / 레버 / 작업대 등
         * 실제 상호작용 가능한 블록을 우클릭한 경우에는
         * 이동 스킬 입력창을 열지 않는다.
         *
         * 따라서 문을 열고 W로 통과할 때
         * 대쉬 / 텔레포트가 오발동하지 않는다.
         */
        if (
                action == Action.RIGHT_CLICK_BLOCK
                && event.getClickedBlock() != null
                && event.getClickedBlock()
                        .getType()
                        .isInteractable()
        ) {
            return;
        }


        ItemStack item =
                event.getItem();

        /*
         * 실제 스킬 아이템 및 직업 통합 스킬북의
         * 우클릭은 입력 시작 신호로 사용하지 않는다.
         */
        if (
                skillItemFactory.isSkillItem(
                        item
                )
        ) {
            return;
        }


        inputWindowService.open(
                event.getPlayer()
                        .getUniqueId()
        );
    }


}
