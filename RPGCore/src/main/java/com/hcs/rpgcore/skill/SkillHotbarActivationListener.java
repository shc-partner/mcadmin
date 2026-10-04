package com.hcs.rpgcore.skill;

import com.hcs.rpgcore.RPGCorePlugin;

import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.player.PlayerItemHeldEvent;

import org.bukkit.inventory.ItemStack;


public final class SkillHotbarActivationListener
        implements Listener {

    /*
     * 우클릭 후 발동 가능한 단축 슬롯.
     *
     * Bukkit:
     * 0 = 핫바 1
     * 1 = 핫바 2
     * 2 = 핫바 3
     * 3 = 핫바 4
     * 4 = 핫바 5
     */
    private static final int FIRST_ACTIVATION_SLOT = 0;
    private static final int LAST_ACTIVATION_SLOT = 4;


    private final RPGCorePlugin plugin;

    private final SkillItemFactory skillItemFactory;

    private final SkillUseListener skillUseListener;

    private final SkillInputWindowService
            inputWindowService;


    public SkillHotbarActivationListener(
            RPGCorePlugin plugin,
            SkillItemFactory skillItemFactory,
            SkillUseListener skillUseListener,
            SkillInputWindowService inputWindowService
    ) {

        this.plugin =
                plugin;

        this.skillItemFactory =
                skillItemFactory;

        this.skillUseListener =
                skillUseListener;

        this.inputWindowService =
                inputWindowService;
    }


    /*
     * =========================================================
     * HOTBAR SELECTION
     * =========================================================
     *
     * 개별 스킬 아이템이 들어 있는 핫바 슬롯은
     * 숫자키 / 마우스 휠 모두 실제 주손으로 선택할 수 없다.
     *
     * 우클릭 입력 상태이고 대상이 1~5번이면
     * 해당 스킬만 발동한 뒤 이전 주손을 유지한다.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = false
    )
    public void onHotbarChange(
            PlayerItemHeldEvent event
    ) {

        Player player =
                event.getPlayer();


        int previousSlot =
                event.getPreviousSlot();

        int newSlot =
                event.getNewSlot();


        ItemStack targetItem =
                player.getInventory()
                        .getItem(
                                newSlot
                        );


        /*
         * 일반 아이템이면
         * 정상 핫바 변경 허용.
         */
        if (
                !skillItemFactory.isSkillItem(
                        targetItem
                )
        ) {
            return;
        }


        /*
         * 직업 통합 스킬북(Tome)은
         * 일반 아이템처럼 선택 가능.
         */
        if (
                skillItemFactory
                        .isClassSkillTome(
                                targetItem
                        )
        ) {
            return;
        }


        /*
         * =====================================================
         * BLOCK SKILL ITEM FROM MAIN HAND
         * =====================================================
         *
         * 개별 스킬 아이템은 절대로
         * 주손으로 선택되지 않는다.
         */
        event.setCancelled(
                true
        );


        /*
         * Paper / 클라이언트 동기화까지 확실하게 원복.
         *
         * 이벤트 직후 다음 서버 틱에
         * 기존 선택 슬롯을 강제로 복구한다.
         */
        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        () -> {

                            if (!player.isOnline()) {
                                return;
                            }

                            player.getInventory()
                                    .setHeldItemSlot(
                                            previousSlot
                                    );
                        }
                );


        /*
         * =====================================================
         * NORMAL HOTBAR ATTEMPT
         * =====================================================
         *
         * 우클릭 입력 상태가 아니면
         * 선택만 차단하고 끝.
         *
         * 스킬 발동 X
         * 주손 변경 X
         */
        if (
                !inputWindowService.isActive(
                        player.getUniqueId()
                )
        ) {
            return;
        }


        /*
         * =====================================================
         * ACTIVATION SLOT 1~5
         * =====================================================
         *
         * 발동 단축 슬롯은 실제 핫바 1~5만.
         *
         * 6~9에 스킬 아이템이 있어도:
         *
         * - 주손 선택 X
         * - 스킬 발동 X
         */
        if (
                newSlot < FIRST_ACTIVATION_SLOT
                || newSlot > LAST_ACTIVATION_SLOT
        ) {
            return;
        }


        /*
         * 우클릭 -> 1~5 입력을 소비.
         */
        if (
                !inputWindowService.consume(
                        player.getUniqueId()
                )
        ) {
            return;
        }


        /*
         * 해당 실제 핫바 슬롯의
         * ItemStack으로 기존 공통 실행 경로 호출.
         */
        skillUseListener.executeSkill(
                player,
                targetItem
        );
    }
}
