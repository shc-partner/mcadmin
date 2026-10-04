package com.hcs.rpgcore.stat;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public final class PlayerStatApplier {

    public void apply(
            Player player,
            PlayerStats stats
    ) {

        /*
         * 실제 공격력 적용.
         *
         * MAX_HEALTH 처리와 독립적으로 먼저 실행한다.
         */
        applyAttack(
                player,
                stats
        );


        /*
         * =========================================================
         * MAX HEALTH
         * =========================================================
         */
        AttributeInstance maxHealthAttribute =
                player.getAttribute(
                        Attribute.MAX_HEALTH
                );

        if (maxHealthAttribute == null) {
            return;
        }

        double oldMaximumHealth =
                maxHealthAttribute.getValue();

        double oldHealth =
                player.getHealth();

        double healthRatio;

        if (oldMaximumHealth > 0.0) {

            healthRatio =
                    oldHealth
                            / oldMaximumHealth;

        } else {

            healthRatio =
                    1.0;
        }

        double maximumHealth =
                Math.max(
                        1.0,
                        stats.maxHealth()
                );


        maxHealthAttribute.setBaseValue(
                maximumHealth
        );


        /*
         * 화면의 바닐라 하트 개수는
         * 항상 기본 10칸(20 health scale)으로 유지한다.
         *
         * 실제 RPG HP 숫자는 HUD에서 별도로 표시한다.
         */
        player.setHealthScaled(
                true
        );

        player.setHealthScale(
                20.0
        );


        /*
         * 장비 교체 전 체력 비율을 보존한다.
         *
         * 예:
         *
         * 기존 HP 50%
         * → 최대 HP가 변경되어도 50% 유지
         */
        double newHealth =
                maximumHealth
                        * healthRatio;


        newHealth =
                Math.max(
                        0.1,
                        Math.min(
                                maximumHealth,
                                newHealth
                        )
                );


        player.setHealth(
                newHealth
        );
    }


    /*
     * =============================================================
     * REAL ATTACK DAMAGE
     * =============================================================
     *
     * RPGCore의 최종 공격력과 Minecraft 실제 ATTACK_DAMAGE의
     * 최종값을 일치시킨다.
     *
     * 장비 modifier는 제거하거나 재작성하지 않는다.
     *
     * 기존 base에:
     *
     *   targetFinal - currentFinal
     *
     * 만큼만 더한다.
     */
    public void applyAttack(
            Player player,
            PlayerStats stats
    ) {

        AttributeInstance attackAttribute =
                player.getAttribute(
                        Attribute.ATTACK_DAMAGE
                );

        if (attackAttribute == null) {
            return;
        }

        double targetAttack =
                Math.max(
                        0.0,
                        stats.attack()
                );

        double currentFinalAttack =
                attackAttribute.getValue();

        double currentBaseAttack =
                attackAttribute.getBaseValue();

        double difference =
                targetAttack
                        - currentFinalAttack;

        double newBaseAttack =
                currentBaseAttack
                        + difference;

        attackAttribute.setBaseValue(
                newBaseAttack
        );
    }
}
