package com.hcs.rpgcore.skill;

import com.hcs.rpgcore.hud.HudService;
import com.hcs.rpgcore.hud.HudSnapshot;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import org.bukkit.util.Vector;


public final class WarriorExecutionSlashSkill {

    /*
     * =========================================================
     * LEVEL 30 - EXECUTION SLASH
     * =========================================================
     *
     * 전방의 단일 적에게 강력한 일격을 가한다.
     *
     * 잡몹 광역 처리보다는
     * 보스 / 엘리트 단일 대상 전투에 특화한다.
     */

    public static final double MANA_COST =
            12.0;

    public static final long COOLDOWN_MILLIS =
            10000L;


    /*
     * 현재 총 공격력의 250%.
     */
    public static final double DAMAGE_MULTIPLIER =
            2.50;


    /*
     * 최대 탐색 거리.
     */
    public static final double RANGE =
            5.0;


    /*
     * 정면 대상 판정.
     *
     * 값이 높을수록
     * 플레이어가 실제 바라보는 적만 선택된다.
     */
    private static final double MIN_DOT =
            0.70;


    private final HudService hudService;

    private final WarriorWeaponAttackService
            weaponAttackService;


    public WarriorExecutionSlashSkill(
            HudService hudService,
            WarriorWeaponAttackService weaponAttackService
    ) {

        this.hudService =
                hudService;

        this.weaponAttackService =
                weaponAttackService;
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


        HudSnapshot snapshot =
                hudService.getProfile(
                        player.getUniqueId()
                );


        if (snapshot == null) {
            return false;
        }


        double totalAttack =
                weaponAttackService
                        .resolveBashAttack(
                                player,
                                snapshot
                        );


        double damage =
                totalAttack
                        * DAMAGE_MULTIPLIER;


        LivingEntity target =
                findTarget(
                        player
                );


        /*
         * =====================================================
         * SLASH END POSITION
         * =====================================================
         *
         * 대상이 있으면 대상 중심을 향해 베고,
         * 대상이 없으면 플레이어가 바라보는 방향으로
         * 최대 RANGE 거리까지 그대로 검기를 발사한다.
         *
         * 따라서 Execution Slash는
         * 적이 없어도 항상 정상 발동한다.
         */
        Location slashEnd;


        if (target != null) {

            slashEnd =
                    target.getLocation()
                            .clone()
                            .add(
                                    0.0,
                                    Math.min(
                                            1.15,
                                            target.getHeight()
                                                    * 0.55
                                    ),
                                    0.0
                            );

        } else {

            Vector forward =
                    player.getEyeLocation()
                            .getDirection()
                            .clone();


            if (
                    forward.lengthSquared()
                            <= 0.0001
            ) {

                forward =
                        new Vector(
                                0.0,
                                0.0,
                                1.0
                        );

            } else {

                forward.normalize();
            }


            slashEnd =
                    player.getEyeLocation()
                            .clone()
                            .add(
                                    forward.multiply(
                                            RANGE
                                    )
                            );
        }


        /*
         * 타겟 유무와 관계없이
         * 참격 VFX는 반드시 발생한다.
         */
        spawnSlashEffect(
                player,
                slashEnd
        );


        /*
         * 실제 대상이 존재하는 경우에만
         * 피해와 적중 효과를 적용한다.
         */
        if (target != null) {

            target.damage(
                    damage,
                    player
            );


            spawnHitEffect(
                    target
            );


            player.getWorld()
                    .playSound(
                            target.getLocation(),
                            Sound.ENTITY_PLAYER_ATTACK_CRIT,
                            1.4f,
                            0.65f
                    );


            player.getWorld()
                    .playSound(
                            target.getLocation(),
                            Sound.ENTITY_IRON_GOLEM_ATTACK,
                            0.9f,
                            0.65f
                    );
        }


        return true;
    }


    /*
     * =========================================================
     * TARGET SEARCH
     * =========================================================
     */
    private LivingEntity findTarget(
            Player player
    ) {

        Location origin =
                player.getEyeLocation();


        Vector forward =
                origin.getDirection()
                        .clone();


        if (
                forward.lengthSquared()
                        <= 0.0001
        ) {
            return null;
        }


        forward.normalize();


        LivingEntity bestTarget =
                null;


        double bestDistanceSquared =
                Double.MAX_VALUE;


        for (
                Entity entity
                : player.getNearbyEntities(
                        RANGE,
                        RANGE,
                        RANGE
                )
        ) {

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
                    target.getLocation()
                            .clone()
                            .add(
                                    0.0,
                                    Math.min(
                                            1.0,
                                            target.getHeight()
                                                    * 0.5
                                    ),
                                    0.0
                            );


            Vector toTarget =
                    targetLocation
                            .toVector()
                            .subtract(
                                    origin.toVector()
                            );


            double distanceSquared =
                    toTarget.lengthSquared();


            if (
                    distanceSquared <= 0.0001
                    || distanceSquared
                    > RANGE * RANGE
            ) {
                continue;
            }


            toTarget.normalize();


            double dot =
                    forward.dot(
                            toTarget
                    );


            if (dot < MIN_DOT) {
                continue;
            }


            if (
                    distanceSquared
                            < bestDistanceSquared
            ) {

                bestDistanceSquared =
                        distanceSquared;

                bestTarget =
                        target;
            }
        }


        return bestTarget;
    }


    /*
     * =========================================================
     * SLASH EFFECT
     * =========================================================
     */
    private void spawnSlashEffect(
            Player player,
            Location end
    ) {

        World world =
                player.getWorld();


        Location start =
                player.getEyeLocation()
                        .clone()
                        .add(
                                0.0,
                                -0.50,
                                0.0
                        );


        Vector forward =
                end.toVector()
                        .subtract(
                                start.toVector()
                        );


        double distance =
                forward.length();


        if (
                distance
                        <= 0.0001
        ) {
            return;
        }


        forward.normalize();


        /*
         * 검기 폭을 만들기 위한
         * 수평 좌우 벡터.
         */
        Vector side =
                forward.clone()
                        .crossProduct(
                                new Vector(
                                        0.0,
                                        1.0,
                                        0.0
                                )
                        );


        if (
                side.lengthSquared()
                        <= 0.0001
        ) {

            side =
                    new Vector(
                            1.0,
                            0.0,
                            0.0
                    );

        } else {

            side.normalize();
        }


        /*
         * 수직 방향 벡터.
         *
         * forward와 side에 수직인 방향을 사용해서
         * 단순한 직선이 아니라
         * 칼날 면처럼 보이게 한다.
         */
        Vector up =
                side.clone()
                        .crossProduct(
                                forward
                        );


        if (
                up.lengthSquared()
                        <= 0.0001
        ) {

            up =
                    new Vector(
                            0.0,
                            1.0,
                            0.0
                    );

        } else {

            up.normalize();
        }


        Particle.DustOptions redEnergy =
                new Particle.DustOptions(
                        Color.fromRGB(
                                210,
                                20,
                                12
                        ),
                        1.45f
                );


        Particle.DustOptions orangeEnergy =
                new Particle.DustOptions(
                        Color.fromRGB(
                                255,
                                92,
                                12
                        ),
                        1.20f
                );


        Particle.DustOptions whiteBlade =
                new Particle.DustOptions(
                        Color.fromRGB(
                                255,
                                245,
                                225
                        ),
                        1.15f
                );


        /*
         * -----------------------------------------------------
         * MAIN BLADE
         * -----------------------------------------------------
         *
         * 플레이어에서 대상 방향으로 갈수록
         * 약간 넓어지는 쐐기형 검기를 생성한다.
         */
        int segments =
                Math.max(
                        14,
                        (int) Math.ceil(
                                distance
                                        / 0.18
                        )
                );


        for (
                int i = 0;
                i <= segments;
                i++
        ) {

            double progress =
                    (double) i
                            / segments;


            double step =
                    distance
                            * progress;


            Location center =
                    start.clone()
                            .add(
                                    forward.clone()
                                            .multiply(
                                                    step
                                            )
                            );


            /*
             * 앞부분으로 갈수록
             * 검기 폭이 조금 넓어진다.
             */
            double width =
                    0.10
                            + (
                                    progress
                                            * 0.72
                            );


            /*
             * 사선으로 베어낸 모양을 만들기 위해
             * 진행률에 따라 높이를 변화시킨다.
             */
            double diagonal =
                    (
                            progress
                                    - 0.5
                    )
                            * 1.15;


            center.add(
                    up.clone()
                            .multiply(
                                    diagonal
                            )
            );


            /*
             * 중앙의 밝은 백색 칼날.
             */
            world.spawnParticle(
                    Particle.DUST,
                    center,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    whiteBlade
            );


            /*
             * 검기 좌우의 주황색 에너지.
             */
            Location orangeLeft =
                    center.clone()
                            .add(
                                    side.clone()
                                            .multiply(
                                                    width
                                                            * 0.48
                                            )
                            );


            Location orangeRight =
                    center.clone()
                            .subtract(
                                    side.clone()
                                            .multiply(
                                                    width
                                                            * 0.48
                                            )
                            );


            world.spawnParticle(
                    Particle.DUST,
                    orangeLeft,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    orangeEnergy
            );


            world.spawnParticle(
                    Particle.DUST,
                    orangeRight,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    orangeEnergy
            );


            /*
             * 검기 가장 바깥쪽은
             * 진한 붉은색으로 감싼다.
             */
            if (
                    i % 2
                            == 0
            ) {

                Location redLeft =
                        center.clone()
                                .add(
                                        side.clone()
                                                .multiply(
                                                        width
                                                )
                                );


                Location redRight =
                        center.clone()
                                .subtract(
                                        side.clone()
                                                .multiply(
                                                        width
                                                )
                                );


                world.spawnParticle(
                        Particle.DUST,
                        redLeft,
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        redEnergy
                );


                world.spawnParticle(
                        Particle.DUST,
                        redRight,
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        redEnergy
                );
            }


            /*
             * 칼날 진행 방향의
             * 날카로운 흰색 파편.
             */
            if (
                    i % 4
                            == 0
            ) {

                world.spawnParticle(
                        Particle.CRIT,
                        center,
                        1,
                        0.02,
                        0.02,
                        0.02,
                        0.01
                );
            }
        }


        /*
         * -----------------------------------------------------
         * TARGET-SIDE BLADE ARC
         * -----------------------------------------------------
         *
         * 대상 직전에 큰 검기 파편을 추가한다.
         */
        for (
                int i = -5;
                i <= 5;
                i++
        ) {

            double offset =
                    i * 0.16;


            Location point =
                    end.clone()
                            .add(
                                    side.clone()
                                            .multiply(
                                                    offset
                                            )
                            )
                            .add(
                                    up.clone()
                                            .multiply(
                                                    -offset
                                                            * 0.75
                                            )
                            );


            world.spawnParticle(
                    Particle.DUST,
                    point,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    whiteBlade
            );


            if (
                    i % 2
                            == 0
            ) {

                world.spawnParticle(
                        Particle.DUST,
                        point.clone()
                                .add(
                                        side.clone()
                                                .multiply(
                                                        0.12
                                                )
                                ),
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        orangeEnergy
                );
            }
        }


        /*
         * 바닐라 칼날 파티클은
         * 대상 위치에 소량만 사용한다.
         */
        world.spawnParticle(
                Particle.SWEEP_ATTACK,
                end,
                2,
                0.18,
                0.22,
                0.18,
                0.0
        );


        /*
         * 시전 순간 사운드.
         */
        world.playSound(
                player.getLocation(),
                Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                1.25f,
                0.62f
        );


        world.playSound(
                end,
                Sound.ENTITY_PLAYER_ATTACK_STRONG,
                1.15f,
                0.72f
        );
    }


    /*
     * =========================================================
     * HIT EFFECT
     * =========================================================
     */
    private void spawnHitEffect(
            LivingEntity target
    ) {

        Location location =
                target.getLocation()
                        .clone()
                        .add(
                                0.0,
                                Math.min(
                                        1.2,
                                        target.getHeight()
                                                * 0.55
                                ),
                                0.0
                        );


        World world =
                target.getWorld();


        Particle.DustOptions darkRed =
                new Particle.DustOptions(
                        Color.fromRGB(
                                150,
                                8,
                                8
                        ),
                        1.45f
                );


        Particle.DustOptions brightRed =
                new Particle.DustOptions(
                        Color.fromRGB(
                                255,
                                35,
                                18
                        ),
                        1.25f
                );


        Particle.DustOptions orange =
                new Particle.DustOptions(
                        Color.fromRGB(
                                255,
                                115,
                                20
                        ),
                        1.15f
                );


        /*
         * 중앙 타격 폭발.
         */
        world.spawnParticle(
                Particle.CRIT,
                location,
                22,
                0.42,
                0.48,
                0.42,
                0.20
        );


        /*
         * 피격 위치를 중심으로
         * 붉은 에너지 파편을 구형으로 뿌린다.
         */
        for (
                int i = 0;
                i < 18;
                i++
        ) {

            double angle =
                    (
                            Math.PI
                                    * 2.0
                                    * i
                            / 18.0
                    );


            double radius =
                    0.35
                            + (
                                    i % 3
                            )
                                    * 0.10;


            Location point =
                    location.clone()
                            .add(
                                    Math.cos(
                                            angle
                                    ) * radius,
                                    (
                                            (
                                                    i % 5
                                            )
                                                    - 2
                                    ) * 0.10,
                                    Math.sin(
                                            angle
                                    ) * radius
                            );


            world.spawnParticle(
                    Particle.DUST,
                    point,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    (
                            i % 3
                                    == 0
                    )
                            ? orange
                            : (
                                    i % 2
                                            == 0
                            )
                                    ? brightRed
                                    : darkRed
            );
        }


        /*
         * 강한 단일 참격의
         * 데미지 피드백.
         */
        world.spawnParticle(
                Particle.DAMAGE_INDICATOR,
                location,
                10,
                0.28,
                0.34,
                0.28,
                0.10
        );


        /*
         * 흰색 충격 파편.
         */
        world.spawnParticle(
                Particle.CRIT,
                location.clone()
                        .add(
                                0.0,
                                0.12,
                                0.0
                        ),
                10,
                0.22,
                0.26,
                0.22,
                0.16
        );


        /*
         * 연기 효과는 최소화해서
         * 붉은 참격이 가려지지 않게 한다.
         */
        world.spawnParticle(
                Particle.CLOUD,
                location,
                5,
                0.22,
                0.16,
                0.22,
                0.025
        );
    }

}