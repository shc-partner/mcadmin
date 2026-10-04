package com.hcs.rpgcore.skill;

import org.bukkit.Particle;
import org.bukkit.Sound;

import org.bukkit.entity.Player;


public final class MageManaShieldSkill {

    /*
     * =========================================================
     * LEVEL 40 - MANA SHIELD
     * =========================================================
     *
     * 일정 시간 받는 피해의 50%를
     * MP로 대신 흡수한다.
     *
     * 실제 피해 / MP 흡수 계산은
     * SkillDefenseBuffService에서 처리한다.
     */

    public static final double MANA_COST =
            20.0;

    public static final long COOLDOWN_MILLIS =
            30000L;


    /*
     * 8초.
     */
    public static final long DURATION_MILLIS =
            8000L;


    private final SkillDefenseBuffService
            defenseBuffService;


    public MageManaShieldSkill(
            SkillDefenseBuffService defenseBuffService
    ) {

        this.defenseBuffService =
                defenseBuffService;
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


        defenseBuffService.activateManaShield(
                player,
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
         * 몸 주변의 마력 보호막 느낌.
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
                        45,
                        0.8,
                        1.0,
                        0.8,
                        0.18
                );


        player.getWorld()
                .spawnParticle(
                        Particle.END_ROD,
                        player.getLocation()
                                .clone()
                                .add(
                                        0.0,
                                        1.0,
                                        0.0
                                ),
                        18,
                        0.65,
                        0.9,
                        0.65,
                        0.04
                );


        player.getWorld()
                .playSound(
                        player.getLocation(),
                        Sound.BLOCK_ENCHANTMENT_TABLE_USE,
                        1.0f,
                        1.35f
                );


        player.getWorld()
                .playSound(
                        player.getLocation(),
                        Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                        0.8f,
                        1.55f
                );
    }
}
