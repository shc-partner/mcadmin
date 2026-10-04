package com.hcs.rpgcore.skill;

import com.hcs.rpgcore.RPGCorePlugin;

import org.bukkit.Particle;
import org.bukkit.Sound;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;

import org.bukkit.NamespacedKey;

import org.bukkit.entity.Player;


public final class WarriorBerserkerRageSkill {

    /*
     * =========================================================
     * LEVEL 70 - BERSERKER RAGE
     * =========================================================
     *
     * 일정 시간 전투 능력을 강화한다.
     *
     * 실제 공격 피해 증가 / 받는 피해 증가는
     * SkillOffenseBuffService를 통해
     * 전투 계산 경로에서 적용한다.
     */

    public static final double MANA_COST =
            25.0;

    public static final long COOLDOWN_MILLIS =
            45000L;


    /*
     * 10초.
     */
    public static final long DURATION_MILLIS =
            10000L;


    private final RPGCorePlugin plugin;

    private final SkillOffenseBuffService
            offenseBuffService;

    private static final NamespacedKey
            MOVEMENT_SPEED_KEY =
            new NamespacedKey(
                    "rpgcore",
                    "berserker_rage_move_speed"
            );


    public WarriorBerserkerRageSkill(
            RPGCorePlugin plugin,
            SkillOffenseBuffService offenseBuffService
    ) {

        this.plugin =
                plugin;

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


        offenseBuffService.activateBerserkerRage(
                player.getUniqueId(),
                DURATION_MILLIS
        );


        /*
         * 이동속도 +20%.
         *
         * base value를 직접 변경하지 않고
         * AttributeModifier를 사용한다.
         *
         * 동일한 key의 modifier가 이미 존재하면
         * 제거한 뒤 다시 추가한다.
         */
        AttributeInstance movement =
                player.getAttribute(
                        Attribute.MOVEMENT_SPEED
                );


        if (movement != null) {

            AttributeModifier oldModifier =
                    movement.getModifier(
                            MOVEMENT_SPEED_KEY
                    );


            if (oldModifier != null) {

                movement.removeModifier(
                        oldModifier
                );
            }


            AttributeModifier modifier =
                    new AttributeModifier(
                            MOVEMENT_SPEED_KEY,
                            SkillOffenseBuffService
                                    .BERSERKER_MOVE_SPEED_MULTIPLIER
                                    - 1.0,
                            AttributeModifier.Operation
                                    .MULTIPLY_SCALAR_1
                    );


            movement.addModifier(
                    modifier
            );
        }


        /*
         * 이동속도 modifier 만료 감시.
         *
         * 10초 경계에서 아직 몇 ms라도
         * 버프 시간이 남아 있으면 1 tick 후 다시 검사한다.
         *
         * 따라서 서버 tick 지연이나
         * 시간 경계 오차가 있어도 modifier가 남지 않는다.
         */
        scheduleMovementSpeedRemoval(
                player
        );


        spawnActivationEffect(
                player
        );


        return true;
    }


    /*
     * =========================================================
     * MOVEMENT SPEED EXPIRATION
     * =========================================================
     */
    private void scheduleMovementSpeedRemoval(
            Player player
    ) {

        if (player == null) {
            return;
        }


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            /*
                             * 아직 버프가 활성 상태라면
                             * 제거하지 않고 다음 tick에 다시 확인한다.
                             *
                             * 재시전된 경우에도 새 버프 시간이
                             * 끝날 때까지 계속 기다린다.
                             */
                            if (
                                    offenseBuffService
                                            .isBerserkerRageActive(
                                                    player.getUniqueId()
                                            )
                            ) {

                                scheduleMovementSpeedRemovalCheck(
                                        player
                                );

                                return;
                            }


                            removeMovementSpeedModifier(
                                    player
                            );
                        },
                        DURATION_MILLIS / 50L
                );
    }


    private void scheduleMovementSpeedRemovalCheck(
            Player player
    ) {

        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (
                                    offenseBuffService
                                            .isBerserkerRageActive(
                                                    player.getUniqueId()
                                            )
                            ) {

                                scheduleMovementSpeedRemovalCheck(
                                        player
                                );

                                return;
                            }


                            removeMovementSpeedModifier(
                                    player
                            );
                        },
                        1L
                );
    }


    /*
     * =========================================================
     * REMOVE MOVEMENT SPEED
     * =========================================================
     */
    public void removeMovementSpeedModifier(
            Player player
    ) {

        if (player == null) {
            return;
        }


        AttributeInstance movement =
                player.getAttribute(
                        Attribute.MOVEMENT_SPEED
                );


        if (movement == null) {
            return;
        }


        AttributeModifier modifier =
                movement.getModifier(
                        MOVEMENT_SPEED_KEY
                );


        if (modifier == null) {
            return;
        }


        movement.removeModifier(
                modifier
        );
    }


    /*
     * =========================================================
     * ACTIVATION EFFECT
     * =========================================================
     */
    private void spawnActivationEffect(
            Player player
    ) {

        player.getWorld()
                .spawnParticle(
                        Particle.CRIT,
                        player.getLocation()
                                .clone()
                                .add(
                                        0.0,
                                        1.0,
                                        0.0
                                ),
                        45,
                        0.85,
                        1.0,
                        0.85,
                        0.18
                );


        player.getWorld()
                .spawnParticle(
                        Particle.DAMAGE_INDICATOR,
                        player.getLocation()
                                .clone()
                                .add(
                                        0.0,
                                        1.0,
                                        0.0
                                ),
                        18,
                        0.70,
                        0.9,
                        0.70,
                        0.10
                );


        player.getWorld()
                .playSound(
                        player.getLocation(),
                        Sound.ENTITY_RAVAGER_ROAR,
                        0.9f,
                        0.70f
                );


        player.getWorld()
                .playSound(
                        player.getLocation(),
                        Sound.ENTITY_ENDER_DRAGON_GROWL,
                        0.45f,
                        1.15f
                );
    }
}
