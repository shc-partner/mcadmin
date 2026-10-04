package com.hcs.rpgcore.skill;

import com.hcs.rpgcore.combat.CombatService;
import com.hcs.rpgcore.mana.ManaService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Particle;
import org.bukkit.Sound;

import org.bukkit.entity.Player;

import org.bukkit.event.entity.EntityDamageEvent;


public final class SkillDefenseBuffService {

    /*
     * =========================================================
     * IRON WALL
     * =========================================================
     *
     * 받는 최종 피해 40% 감소.
     *
     * 즉:
     *
     * finalDamage x 0.60
     */
    public static final double IRON_WALL_DAMAGE_MULTIPLIER =
            0.60;


    /*
     * =========================================================
     * MANA SHIELD
     * =========================================================
     *
     * 받는 최종 피해의 50%를
     * MP로 대신 흡수한다.
     *
     * 피해 1 = MP 1
     */
    public static final double MANA_SHIELD_ABSORB_RATIO =
            0.50;


    private final CombatService combatService;

    private final ManaService manaService;


    /*
     * UUID -> 종료 시간 millis
     */
    private final Map<UUID, Long> ironWallExpireMap =
            new ConcurrentHashMap<>();

    private final Map<UUID, Long> manaShieldExpireMap =
            new ConcurrentHashMap<>();


    public SkillDefenseBuffService(
            CombatService combatService,
            ManaService manaService
    ) {

        this.combatService =
                combatService;

        this.manaService =
                manaService;
    }


    /*
     * =========================================================
     * ACTIVATE IRON WALL
     * =========================================================
     */
    public void activateIronWall(
            Player player,
            long durationMillis
    ) {

        if (player == null) {
            return;
        }


        long safeDuration =
                Math.max(
                        0L,
                        durationMillis
                );


        ironWallExpireMap.put(
                player.getUniqueId(),
                System.currentTimeMillis()
                        + safeDuration
        );
    }


    /*
     * =========================================================
     * ACTIVATE MANA SHIELD
     * =========================================================
     */
    public void activateManaShield(
            Player player,
            long durationMillis
    ) {

        if (player == null) {
            return;
        }


        long safeDuration =
                Math.max(
                        0L,
                        durationMillis
                );


        manaShieldExpireMap.put(
                player.getUniqueId(),
                System.currentTimeMillis()
                        + safeDuration
        );
    }


    /*
     * =========================================================
     * ACTIVE CHECK
     * =========================================================
     */
    public boolean isIronWallActive(
            UUID uuid
    ) {

        return isActive(
                ironWallExpireMap,
                uuid
        );
    }


    public boolean isManaShieldActive(
            UUID uuid
    ) {

        return isActive(
                manaShieldExpireMap,
                uuid
        );
    }


    private boolean isActive(
            Map<UUID, Long> map,
            UUID uuid
    ) {

        if (uuid == null) {
            return false;
        }


        Long expireTime =
                map.get(
                        uuid
                );


        if (expireTime == null) {
            return false;
        }


        if (
                expireTime
                        <= System.currentTimeMillis()
        ) {

            map.remove(
                    uuid
            );

            return false;
        }


        return true;
    }


    /*
     * =========================================================
     * APPLY DEFENSIVE SKILLS
     * =========================================================
     *
     * 호출 시점:
     *
     * vanilla armor
     * -> RPG DEF
     * -> flat damage reduction
     * -> 여기
     *
     * 순서:
     *
     * 1. Iron Wall
     * 2. Mana Shield
     */
    public void applyDefensiveSkills(
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


        double finalDamage =
                event.getFinalDamage();


        if (finalDamage <= 0.0) {
            return;
        }


        UUID uuid =
                player.getUniqueId();


        /*
         * =====================================================
         * 1. IRON WALL
         * =====================================================
         */
        if (
                isIronWallActive(
                        uuid
                )
        ) {

            finalDamage *=
                    IRON_WALL_DAMAGE_MULTIPLIER;


            spawnIronWallHitEffect(
                    player
            );
        }


        /*
         * =====================================================
         * 2. MANA SHIELD
         * =====================================================
         */
        if (
                isManaShieldActive(
                        uuid
                )
        ) {

            finalDamage =
                    applyManaShield(
                            player,
                            finalDamage
                    );
        }


        /*
         * CombatService가 사용하는 동일한
         * final damage 보정 경로를 재사용한다.
         */
        combatService.applyFinalDamage(
                event,
                finalDamage
        );
    }


    /*
     * =========================================================
     * MANA SHIELD ABSORB
     * =========================================================
     */
    private double applyManaShield(
            Player player,
            double incomingDamage
    ) {

        if (incomingDamage <= 0.0) {
            return 0.0;
        }


        UUID uuid =
                player.getUniqueId();


        double currentMana =
                Math.max(
                        0.0,
                        manaService.getCurrentMana(
                                uuid
                        )
                );


        if (currentMana <= 0.0) {

            /*
             * MP가 완전히 소진되면
             * 이번 공격부터 흡수하지 않는다.
             */
            return incomingDamage;
        }


        double requestedAbsorb =
                incomingDamage
                        * MANA_SHIELD_ABSORB_RATIO;


        double actualAbsorb =
                Math.min(
                        requestedAbsorb,
                        currentMana
                );


        if (actualAbsorb <= 0.0) {
            return incomingDamage;
        }


        /*
         * consume()은 충분한 MP가 있을 때만
         * 성공하므로 actualAbsorb는 반드시
         * currentMana 이하로 제한한다.
         */
        boolean consumed =
                manaService.consume(
                        uuid,
                        actualAbsorb
                );


        if (!consumed) {
            return incomingDamage;
        }


        double remainingDamage =
                Math.max(
                        0.0,
                        incomingDamage
                                - actualAbsorb
                );


        spawnManaShieldHitEffect(
                player,
                actualAbsorb
        );


        return remainingDamage;
    }


    /*
     * =========================================================
     * CLEAR
     * =========================================================
     */
    public void remove(
            UUID uuid
    ) {

        if (uuid == null) {
            return;
        }


        ironWallExpireMap.remove(
                uuid
        );

        manaShieldExpireMap.remove(
                uuid
        );
    }


    public void clear() {

        ironWallExpireMap.clear();

        manaShieldExpireMap.clear();
    }


    /*
     * =========================================================
     * IRON WALL HIT EFFECT
     * =========================================================
     */
    private void spawnIronWallHitEffect(
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
                        8,
                        0.45,
                        0.65,
                        0.45,
                        0.04
                );


        player.getWorld()
                .playSound(
                        player.getLocation(),
                        Sound.BLOCK_ANVIL_LAND,
                        0.18f,
                        1.65f
                );
    }


    /*
     * =========================================================
     * MANA SHIELD HIT EFFECT
     * =========================================================
     */
    private void spawnManaShieldHitEffect(
            Player player,
            double absorbedDamage
    ) {

        int particleCount =
                Math.max(
                        4,
                        Math.min(
                                18,
                                (int) Math.ceil(
                                        absorbedDamage
                                )
                        )
                );


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
                        particleCount,
                        0.55,
                        0.75,
                        0.55,
                        0.12
                );


        player.getWorld()
                .playSound(
                        player.getLocation(),
                        Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                        0.45f,
                        1.65f
                );
    }
}
