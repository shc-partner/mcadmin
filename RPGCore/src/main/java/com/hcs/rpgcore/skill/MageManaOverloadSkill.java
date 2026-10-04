package com.hcs.rpgcore.skill;

import org.bukkit.Particle;
import org.bukkit.Sound;

import org.bukkit.entity.Player;


public final class MageManaOverloadSkill {

    /*
     * =========================================================
     * LEVEL 70 - MANA OVERLOAD
     * =========================================================
     *
     * 일정 시간 마력 출력을 극대화한다.
     *
     * 효과:
     *
     * - 마법 스킬 피해 +30%
     * - 마법 스킬 MP 소모 +25%
     *
     * 실제 피해 / MP 비용 배율은
     * SkillOffenseBuffService를 통해
     * 스킬 실행 경로에서 적용한다.
     */

    public static final double MANA_COST =
            60.0;

    public static final long COOLDOWN_MILLIS =
            45000L;


    /*
     * 10초.
     */
    public static final long DURATION_MILLIS =
            10000L;


    private final SkillOffenseBuffService
            offenseBuffService;


    public MageManaOverloadSkill(
            SkillOffenseBuffService offenseBuffService
    ) {

        this.offenseBuffService =
                offenseBuffService;
    }


    /*
     * =========================================================
     * CAST
     * =========================================================
     */
    public boolean cast(
            Player player
    ) {

        if (
                player == null
                || !player.isOnline()
                || player.isDead()
        ) {
            return false;
        }


        offenseBuffService.activateManaOverload(
                player.getUniqueId(),
                DURATION_MILLIS
        );


        spawnActivationEffect(
                player
        );


        return true;
    }


    /*
     * =========================================================
     * ACTIVATION EFFECT
     * =========================================================
     */
    private void spawnActivationEffect(
            Player player
    ) {

        /*
         * 플레이어 주변으로 마력이 폭발적으로
         * 퍼지는 느낌의 시각 효과.
         */
        player.getWorld()
                .spawnParticle(
                        Particle.ENCHANT,
                        player.getLocation()
                                .clone()
                                .add(
                                        0.0,
                                        1.0,
                                        0.0
                                ),
                        70,
                        0.95,
                        1.15,
                        0.95,
                        0.22
                );


        player.getWorld()
                .spawnParticle(
                        Particle.END_ROD,
                        player.getLocation()
                                .clone()
                                .add(
                                        0.0,
                                        1.1,
                                        0.0
                                ),
                        30,
                        0.75,
                        1.0,
                        0.75,
                        0.08
                );


        player.getWorld()
                .spawnParticle(
                        Particle.REVERSE_PORTAL,
                        player.getLocation()
                                .clone()
                                .add(
                                        0.0,
                                        1.0,
                                        0.0
                                ),
                        35,
                        0.70,
                        0.90,
                        0.70,
                        0.12
                );


        player.getWorld()
                .playSound(
                        player.getLocation(),
                        Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,
                        0.9f,
                        1.25f
                );


        player.getWorld()
                .playSound(
                        player.getLocation(),
                        Sound.BLOCK_ENCHANTMENT_TABLE_USE,
                        1.0f,
                        0.65f
                );
    }
}
