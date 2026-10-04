package com.hcs.rpgcore.combat;

import com.hcs.rpgcore.hud.HudService;
import com.hcs.rpgcore.hud.HudSnapshot;

import org.bukkit.entity.Player;

import org.bukkit.event.entity.EntityDamageEvent;


public final class CombatService {

    /*
     * =========================================================
     * RPG DEFENSE CONFIG
     * =========================================================
     *
     * reduction =
     *
     * DEF / (DEF + 200)
     *
     * Lv.99 기본 DEF 50
     * → 20% 추가 피해 감소
     */
    private static final double DEFENSE_CONSTANT =
            200.0;


    /*
     * RPG 방어 계층 최대 감소율.
     *
     * 향후 직업 / 장비 / 버프로 DEF가 크게 올라가도
     * RPG DEF 하나만으로 완전 무적이 되는 것을 방지.
     */
    private static final double MAX_RPG_REDUCTION =
            0.80;


    private final HudService hudService;


    public CombatService(
            HudService hudService
    ) {

        this.hudService =
                hudService;
    }


    /*
     * =========================================================
     * APPLY RPG DEFENSE
     * =========================================================
     */
    public void applyRpgDefense(
            Player player,
            EntityDamageEvent event
    ) {

        if (
                player == null
                || event == null
                || event.isCancelled()
        ) {
            return;
        }


        HudSnapshot snapshot =
                hudService.getProfile(
                        player.getUniqueId()
                );


        if (snapshot == null) {
            return;
        }


        double defense =
                Math.max(
                        0.0,
                        snapshot.defense()
                );


        double damageReductionFlat =
                Math.max(
                        0.0,
                        snapshot.damageReductionFlat()
                );


        /*
         * RPG 방어력과 고정 피해 감소가 둘 다 없으면
         * 추가 계산이 필요 없다.
         */
        if (
                defense <= 0.0
                && damageReductionFlat <= 0.0
        ) {
            return;
        }


        /*
         * 현재 이벤트에서 Minecraft가 계산한
         * 최종 피해.
         *
         * 여기에는 바닐라 갑옷 / toughness /
         * Protection 등의 효과가 이미 반영된다.
         */
        double vanillaFinalDamage =
                event.getFinalDamage();


        if (vanillaFinalDamage <= 0.0) {
            return;
        }


        /*
         * -----------------------------------------------------
         * 1. RPG DEFENSE
         * -----------------------------------------------------
         *
         * 바닐라 최종 피해에 RPG DEF 감소율을 적용한다.
         */
        double targetFinalDamage =
                vanillaFinalDamage;


        if (defense > 0.0) {

            double reduction =
                    calculateReduction(
                            defense
                    );


            targetFinalDamage *=
                    (
                            1.0
                            - reduction
                    );
        }


        /*
         * -----------------------------------------------------
         * 2. FLAT DAMAGE REDUCTION
         * -----------------------------------------------------
         *
         * RPG DEF 계산까지 끝난 피해에서
         * 고정 피해 감소값을 마지막으로 차감한다.
         *
         * 예:
         *
         * RPG DEF 적용 후 피해 1000
         * damageReductionFlat 220
         *
         * → 최종 피해 780
         */
        targetFinalDamage =
                Math.max(
                        0.0,
                        targetFinalDamage
                                - damageReductionFlat
                );


        /*
         * Final Damage를 직접 set하는 API가 없으므로,
         *
         * raw damage를 변경하면서 getFinalDamage()를 확인해
         * 목표 final damage에 가장 가까운 raw 값을 찾는다.
         */
        applyTargetFinalDamage(
                event,
                targetFinalDamage
        );
    }


    /*
     * =========================================================
     * CALCULATE REDUCTION
     * =========================================================
     */
    public double calculateReduction(
            double defense
    ) {

        defense = Math.max(
                        0.0,
                        defense
                );


        double reduction =
                defense
                        / (
                                defense
                                + DEFENSE_CONSTANT
                        );


        return Math.max(
                0.0,
                Math.min(
                        MAX_RPG_REDUCTION,
                        reduction
                )
        );
    }


    /*
     * =========================================================
     * APPLY TARGET FINAL DAMAGE
     * =========================================================
     *
     * RPG DEF 계산 이후 추가 방어 계층에서
     * 최종 피해를 다시 조정할 때 사용하는 공개 API.
     *
     * 예:
     *
     * - 아이언 월
     * - 마나 실드
     *
     * 바닐라 armor / toughness / Protection 계산을
     * 임의로 우회하지 않고 기존 binary search 경로를
     * 그대로 재사용한다.
     */
    public void applyFinalDamage(
            EntityDamageEvent event,
            double targetFinalDamage
    ) {

        if (
                event == null
                || event.isCancelled()
        ) {
            return;
        }


        applyTargetFinalDamage(
                event,
                targetFinalDamage
        );
    }


    /*
     * =========================================================
     * TARGET FINAL DAMAGE
     * =========================================================
     */
    private void applyTargetFinalDamage(
            EntityDamageEvent event,
            double targetFinalDamage
    ) {

        double originalRawDamage =
                event.getDamage();


        double originalFinalDamage =
                event.getFinalDamage();


        if (
                originalRawDamage <= 0.0
                || originalFinalDamage <= 0.0
        ) {
            return;
        }


        targetFinalDamage = Math.max(
                        0.0,
                        Math.min(
                                originalFinalDamage,
                                targetFinalDamage
                        )
                );


        double low =
                0.0;

        double high =
                originalRawDamage;


        /*
         * Binary search.
         *
         * 24번이면 Minecraft 피해 수치에는
         * 충분한 정밀도.
         */
        for (
                int i = 0;
                i < 24;
                i++
        ) {

            double middle =
                    (
                            low
                            + high
                    )
                            / 2.0;


            event.setDamage(
                    middle
            );


            double resolvedFinalDamage =
                    event.getFinalDamage();


            if (
                    resolvedFinalDamage
                    > targetFinalDamage
            ) {

                high = middle;

            } else {

                low = middle;
            }
        }


        event.setDamage(
                (
                        low
                        + high
                )
                        / 2.0
        );
    }


    public double getDefenseConstant() {

        return DEFENSE_CONSTANT;
    }
}
