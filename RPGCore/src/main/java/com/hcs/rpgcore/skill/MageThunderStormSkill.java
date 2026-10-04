package com.hcs.rpgcore.skill;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.mana.ManaService;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import org.bukkit.scheduler.BukkitRunnable;

import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;


public final class MageThunderStormSkill {

    /*
     * =========================================================
     * LEVEL 80 - THUNDER STORM
     * =========================================================
     *
     * 플레이어가 바라보는 지점에
     * 강력한 번개 폭풍을 생성한다.
     *
     * 일정 시간 동안 넓은 범위의 적에게
     * 반복적인 마법 피해를 준다.
     */

    public static final double MANA_COST =
            70.0;

    public static final long COOLDOWN_MILLIS =
            30000L;


    /*
     * 1회 피해:
     * 15 + ATK x 0.22 + 최대 MP x 0.015
     *
     * 총 6회 풀히트:
     * 90 + ATK x 1.32 + 최대 MP x 0.09
     */
    public static final double BASE_DAMAGE =
            15.0;

    public static final double ATTACK_RATIO =
            0.22;

    public static final double MAX_MANA_RATIO =
            0.015;


    /*
     * 최대 시전 거리.
     */
    private static final double CAST_RANGE =
            18.0;


    /*
     * 폭풍 공격 반경.
     */
    private static final double AREA_RADIUS =
            6.0;


    /*
     * 총 6회 공격.
     */
    private static final int HIT_COUNT =
            6;


    /*
     * 0.5초 = 10 tick.
     *
     * 0 / 10 / 20 / 30 / 40 / 50 tick
     */
    private static final long HIT_INTERVAL_TICKS =
            10L;


    private final RPGCorePlugin plugin;

    private final ManaService manaService;

    private final SkillOffenseBuffService
            offenseBuffService;


    public MageThunderStormSkill(
            RPGCorePlugin plugin,
            ManaService manaService,
            SkillOffenseBuffService offenseBuffService
    ) {

        this.plugin =
                plugin;

        this.manaService =
                manaService;

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


        Location center =
                resolveTargetLocation(
                        player
                );


        if (center == null) {
            return false;
        }


        double maximumMana =
                manaService.getMaximumMana(
                        player.getUniqueId()
                );


        AttributeInstance attackAttribute =
                player.getAttribute(
                        Attribute.ATTACK_DAMAGE
                );


        double attack =
                attackAttribute != null
                        ? Math.max(
                                0.0,
                                attackAttribute.getValue()
                        )
                        : 1.0;


        double damagePerHit =
                BASE_DAMAGE
                        + attack
                        * ATTACK_RATIO
                        + maximumMana
                        * MAX_MANA_RATIO;


        /*
         * =====================================================
         * MANA OVERLOAD
         * =====================================================
         *
         * 시전 순간 마나 오버로드가 활성화되어 있으면
         * 전체 썬더 스톰 피해를 +30% 증가시킨다.
         *
         * 이후 지속되는 6회 공격은
         * 모두 동일한 snapshot 피해를 사용한다.
         */
        if (offenseBuffService != null) {

            damagePerHit =
                    offenseBuffService
                            .applyMageDamageMultiplier(
                                    player.getUniqueId(),
                                    damagePerHit
                            );
        }


        final double finalDamagePerHit =
                damagePerHit;


        spawnCastEffect(
                player,
                center
        );


        /*
         * =====================================================
         * THUNDER STORM
         * =====================================================
         */
        new BukkitRunnable() {

            private int hitIndex =
                    0;


            @Override
            public void run() {

                if (
                        !player.isOnline()
                        || player.isDead()
                ) {

                    cancel();
                    return;
                }


                performThunderStormTick(
                        player,
                        center,
                        finalDamagePerHit,
                        hitIndex
                );


                hitIndex++;


                if (
                        hitIndex
                                >= HIT_COUNT
                ) {

                    spawnFinishEffect(
                            center
                    );

                    cancel();
                }
            }

        }.runTaskTimer(
                plugin,
                0L,
                HIT_INTERVAL_TICKS
        );


        return true;
    }


    /*
     * =========================================================
     * TARGET LOCATION
     * =========================================================
     */
    private Location resolveTargetLocation(
            Player player
    ) {

        Location eye =
                player.getEyeLocation();


        Vector direction =
                eye.getDirection()
                        .clone();


        if (
                direction.lengthSquared()
                        <= 0.0001
        ) {
            return null;
        }


        direction.normalize();


        /*
         * 바라보는 블록이 있다면
         * 해당 충돌 지점을 폭풍 중심으로 사용한다.
         */
        RayTraceResult result =
                player.getWorld()
                        .rayTraceBlocks(
                                eye,
                                direction,
                                CAST_RANGE
                        );


        if (
                result != null
                && result.getHitPosition() != null
        ) {

            return result.getHitPosition()
                    .toLocation(
                            player.getWorld()
                    );
        }


        /*
         * 블록에 닿지 않으면
         * 최대 사거리 지점에 생성.
         */
        return eye.clone()
                .add(
                        direction.multiply(
                                CAST_RANGE
                        )
                );
    }


    /*
     * =========================================================
     * THUNDER STORM TICK
     * =========================================================
     */
    private void performThunderStormTick(
            Player player,
            Location center,
            double damage,
            int hitIndex
    ) {

        World world =
                center.getWorld();


        if (world == null) {
            return;
        }


        spawnStormEffect(
                center,
                hitIndex
        );


        /*
         * =====================================================
         * VISUAL LIGHTNING
         * =====================================================
         *
         * 공격 tick마다 실제 마인크래프트 번개 모양을
         * 딱 1회 생성한다.
         *
         * 총 공격 횟수가 6회이므로
         * 전체 스킬 동안 정확히 6번 낙뢰한다.
         *
         * strikeLightningEffect()만 사용하므로
         * 번개 자체의 추가 전투 피해는 발생시키지 않는다.
         */
        spawnVisualLightning(
                center,
                hitIndex
        );


        for (
                Entity entity
                : world.getNearbyEntities(
                        center,
                        AREA_RADIUS,
                        5.0,
                        AREA_RADIUS
                )
        ) {

            /*
             * 적대 몬스터만 공격.
             *
             * 플레이어 / 동물 / NPC 피해 없음.
             */
            if (!(entity instanceof Enemy)) {
                continue;
            }


            if (
                    !(entity
                            instanceof LivingEntity target)
            ) {
                continue;
            }


            if (target.isDead()) {
                continue;
            }


            Location targetLocation =
                    target.getLocation();


            double dx =
                    targetLocation.getX()
                            - center.getX();

            double dz =
                    targetLocation.getZ()
                            - center.getZ();


            double horizontalDistanceSquared =
                    dx * dx
                            + dz * dz;


            /*
             * 실제 원형 반경 검사.
             */
            if (
                    horizontalDistanceSquared
                            > AREA_RADIUS
                            * AREA_RADIUS
            ) {
                continue;
            }


            /*
             * 지나치게 다른 높이에 있는 대상 제외.
             */
            if (
                    Math.abs(
                            targetLocation.getY()
                                    - center.getY()
                    ) > 5.0
            ) {
                continue;
            }


            /*
             * 실제 마법 피해.
             *
             * 마나 오버로드 피해 증가는
             * cast() 시점에 이미 snapshot 처리됨.
             */
            target.damage(
                    damage,
                    player
            );


            spawnTargetLightningEffect(
                    target,
                    hitIndex
            );
        }


        world.playSound(
                center,
                Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                0.75f,
                1.15f
                        + (
                                hitIndex
                                        * 0.025f
                        )
        );
    }


    /*
     * =========================================================
     * VISUAL LIGHTNING
     * =========================================================
     *
     * 전투 판정과 완전히 분리된
     * 마인크래프트 실제 번개 VFX.
     *
     * performThunderStormTick()이 총 6회 실행되므로
     * 이 메서드 역시 정확히 6번 호출된다.
     */
    private void spawnVisualLightning(
            Location center,
            int hitIndex
    ) {

        World world =
                center.getWorld();


        if (world == null) {
            return;
        }


        /*
         * 6번의 번개가 모두 같은 자리에 떨어지면
         * 썬더 스톰보다는 단일 낙뢰처럼 보이므로,
         * 각 hitIndex마다 미리 정해진 방향으로
         * 낙뢰 위치를 분산한다.
         *
         * 완전 랜덤을 사용하지 않아
         * 테스트할 때도 항상 같은 형태로 확인 가능하다.
         */
        double[] radii = {
                0.8,
                3.2,
                4.6,
                2.4,
                5.0,
                1.7
        };


        double[] angles = {
                15.0,
                82.0,
                148.0,
                218.0,
                292.0,
                345.0
        };


        int index =
                Math.max(
                        0,
                        Math.min(
                                hitIndex,
                                radii.length - 1
                        )
                );


        double angle =
                Math.toRadians(
                        angles[index]
                );


        double radius =
                radii[index];


        Location strikeLocation =
                center.clone()
                        .add(
                                Math.cos(angle)
                                        * radius,
                                0.0,
                                Math.sin(angle)
                                        * radius
                        );


        /*
         * 실제 Minecraft 번개 모양만 생성.
         *
         * strikeLightning()을 사용하지 않는다.
         */
        world.strikeLightningEffect(
                strikeLocation
        );


        /*
         * 착탄 지점을 조금 더 명확하게 보여주는
         * 보조 전기 파티클.
         *
         * 전투 판정과는 무관하다.
         */
        world.spawnParticle(
                Particle.ELECTRIC_SPARK,
                strikeLocation.clone()
                        .add(
                                0.0,
                                0.45,
                                0.0
                        ),
                22,
                0.50,
                0.55,
                0.50,
                0.16
        );


        world.spawnParticle(
                Particle.END_ROD,
                strikeLocation.clone()
                        .add(
                                0.0,
                                0.40,
                                0.0
                        ),
                6,
                0.24,
                0.30,
                0.24,
                0.05
        );
    }


    /*
     * =========================================================
     * CAST EFFECT
     * =========================================================
     */
    private void spawnCastEffect(
            Player player,
            Location center
    ) {

        World world =
                player.getWorld();


        /*
         * 폭풍 생성 지점 상공.
         */
        world.spawnParticle(
                Particle.ELECTRIC_SPARK,
                center.clone()
                        .add(
                                0.0,
                                4.0,
                                0.0
                        ),
                75,
                2.5,
                1.5,
                2.5,
                0.18
        );


        world.spawnParticle(
                Particle.CLOUD,
                center.clone()
                        .add(
                                0.0,
                                4.2,
                                0.0
                        ),
                30,
                2.2,
                0.45,
                2.2,
                0.04
        );


        world.playSound(
                center,
                Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                1.0f,
                0.72f
        );
    }


    /*
     * =========================================================
     * STORM EFFECT
     * =========================================================
     */
    private void spawnStormEffect(
            Location center,
            int hitIndex
    ) {

        World world =
                center.getWorld();


        if (world == null) {
            return;
        }


        /*
         * 폭풍 상공의 전기 입자.
         */
        world.spawnParticle(
                Particle.ELECTRIC_SPARK,
                center.clone()
                        .add(
                                0.0,
                                4.0,
                                0.0
                        ),
                85,
                AREA_RADIUS * 0.65,
                1.4,
                AREA_RADIUS * 0.65,
                0.20
        );


        /*
         * 지면의 폭풍 범위 표시.
         */
        int points =
                40;


        double rotation =
                hitIndex
                        * Math.toRadians(
                                19.0
                        );


        for (
                int i = 0;
                i < points;
                i++
        ) {

            double angle =
                    (
                            Math.PI
                                    * 2.0
                                    * i
                            / points
                    )
                            + rotation;


            double x =
                    Math.cos(
                            angle
                    ) * AREA_RADIUS;


            double z =
                    Math.sin(
                            angle
                    ) * AREA_RADIUS;


            Location point =
                    center.clone()
                            .add(
                                    x,
                                    0.15,
                                    z
                            );


            world.spawnParticle(
                    Particle.ELECTRIC_SPARK,
                    point,
                    1,
                    0.03,
                    0.05,
                    0.03,
                    0.01
            );
        }


        /*
         * 폭풍 내부에 여러 개의 전기 기둥을 표시.
         *
         * 실제 피해 판정과는 분리된 시각 효과다.
         */
        for (
                int i = 0;
                i < 8;
                i++
        ) {

            double angle =
                    (
                            Math.PI
                                    * 2.0
                                    * i
                            / 8.0
                    )
                            + rotation;


            double radius =
                    1.5
                            + (
                                    (i % 3)
                                            * 1.4
                            );


            double x =
                    Math.cos(angle)
                            * radius;

            double z =
                    Math.sin(angle)
                            * radius;


            Location ground =
                    center.clone()
                            .add(
                                    x,
                                    0.3,
                                    z
                            );


            for (
                    int y = 0;
                    y <= 8;
                    y++
            ) {

                world.spawnParticle(
                        Particle.ELECTRIC_SPARK,
                        ground.clone()
                                .add(
                                        0.0,
                                        y * 0.5,
                                        0.0
                                ),
                        1,
                        0.08,
                        0.04,
                        0.08,
                        0.02
                );
            }
        }
    }


    /*
     * =========================================================
     * TARGET LIGHTNING EFFECT
     * =========================================================
     */
    private void spawnTargetLightningEffect(
            LivingEntity target,
            int hitIndex
    ) {

        World world =
                target.getWorld();


        Location base =
                target.getLocation()
                        .clone();


        double phase =
                hitIndex
                        * 0.55;


        /*
         * 대상 머리 위에서 지면까지
         * 번개처럼 보이는 전기 입자 기둥.
         */
        for (
                int i = 0;
                i <= 12;
                i++
        ) {

            double height =
                    6.0
                            - (
                                    i * 0.5
                            );


            double offsetX =
                    Math.sin(
                            phase
                                    + i * 1.7
                    ) * 0.14;


            double offsetZ =
                    Math.cos(
                            phase
                                    + i * 1.35
                    ) * 0.14;


            world.spawnParticle(
                    Particle.ELECTRIC_SPARK,
                    base.clone()
                            .add(
                                    offsetX,
                                    height,
                                    offsetZ
                            ),
                    2,
                    0.04,
                    0.04,
                    0.04,
                    0.03
            );
        }


        Location hitLocation =
                base.clone()
                        .add(
                                0.0,
                                Math.min(
                                        1.0,
                                        target.getHeight()
                                                * 0.5
                                ),
                                0.0
                        );


        world.spawnParticle(
                Particle.END_ROD,
                hitLocation,
                8,
                0.35,
                0.45,
                0.35,
                0.10
        );


        world.spawnParticle(
                Particle.ELECTRIC_SPARK,
                hitLocation,
                18,
                0.45,
                0.55,
                0.45,
                0.16
        );


        world.playSound(
                hitLocation,
                Sound.ENTITY_LIGHTNING_BOLT_IMPACT,
                0.60f,
                1.35f
        );
    }


    /*
     * =========================================================
     * FINISH EFFECT
     * =========================================================
     */
    private void spawnFinishEffect(
            Location center
    ) {

        World world =
                center.getWorld();


        if (world == null) {
            return;
        }


        world.spawnParticle(
                Particle.ELECTRIC_SPARK,
                center.clone()
                        .add(
                                0.0,
                                1.2,
                                0.0
                        ),
                65,
                2.8,
                1.3,
                2.8,
                0.22
        );


        world.spawnParticle(
                Particle.END_ROD,
                center.clone()
                        .add(
                                0.0,
                                1.0,
                                0.0
                        ),
                28,
                2.0,
                1.0,
                2.0,
                0.10
        );


        world.playSound(
                center,
                Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                0.9f,
                0.88f
        );
    }
}
