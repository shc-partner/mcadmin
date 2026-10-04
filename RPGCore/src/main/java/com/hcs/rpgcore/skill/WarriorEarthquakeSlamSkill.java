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


public final class WarriorEarthquakeSlamSkill {

    /*
     * =========================================================
     * LEVEL 50 - EARTHQUAKE SLAM
     * =========================================================
     *
     * 전방 지면을 강하게 내려쳐
     * 넓은 범위의 적에게 피해를 주고 띄운다.
     */

    public static final double MANA_COST =
            20.0;

    public static final long COOLDOWN_MILLIS =
            16000L;


    /*
     * 현재 총 공격력의 170%.
     */
    public static final double DAMAGE_MULTIPLIER =
            1.70;


    /*
     * 전방 최대 거리.
     */
    private static final double RANGE =
            7.0;


    /*
     * 중심선 기준 좌우 폭.
     */
    private static final double HALF_WIDTH =
            2.5;


    /*
     * 적을 띄우는 힘.
     */
    private static final double KNOCK_UP_Y =
            0.65;


    private final HudService hudService;

    private final WarriorWeaponAttackService
            weaponAttackService;


    public WarriorEarthquakeSlamSkill(
            HudService hudService,
            WarriorWeaponAttackService weaponAttackService
    ) {

        this.hudService =
                hudService;

        this.weaponAttackService =
                weaponAttackService;
    }


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


        Location origin =
                player.getLocation()
                        .clone();


        Vector forward =
                origin.getDirection()
                        .clone();


        forward.setY(
                0.0
        );


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


        Vector right =
                new Vector(
                        -forward.getZ(),
                        0.0,
                        forward.getX()
                );


        spawnSlamEffect(
                player,
                origin,
                forward,
                right
        );


        for (
                Entity entity
                : player.getNearbyEntities(
                        RANGE,
                        4.0,
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


            Vector relative =
                    target.getLocation()
                            .toVector()
                            .subtract(
                                    origin.toVector()
                            );


            double verticalDifference =
                    relative.getY();


            if (
                    Math.abs(
                            verticalDifference
                    ) > 3.0
            ) {
                continue;
            }


            relative.setY(
                    0.0
            );


            /*
             * 전방 거리.
             */
            double forwardDistance =
                    relative.dot(
                            forward
                    );


            if (
                    forwardDistance < 0.0
                    || forwardDistance > RANGE
            ) {
                continue;
            }


            /*
             * 중심선 좌우 거리.
             */
            double sideDistance =
                    Math.abs(
                            relative.dot(
                                    right
                            )
                    );


            if (
                    sideDistance
                            > HALF_WIDTH
            ) {
                continue;
            }


            target.damage(
                    damage,
                    player
            );


            /*
             * 기존 이동 방향은 일부 보존하면서
             * 위로 강하게 띄운다.
             */
            Vector velocity =
                    target.getVelocity()
                            .clone();


            velocity.setY(
                    Math.max(
                            velocity.getY(),
                            KNOCK_UP_Y
                    )
            );


            target.setVelocity(
                    velocity
            );


            spawnHitEffect(
                    target
            );
        }


        player.getWorld()
                .playSound(
                        player.getLocation(),
                        Sound.ENTITY_GENERIC_EXPLODE,
                        1.2f,
                        0.65f
                );


        player.getWorld()
                .playSound(
                        player.getLocation(),
                        Sound.ENTITY_IRON_GOLEM_ATTACK,
                        1.0f,
                        0.55f
                );


        return true;
    }


    /*
     * =========================================================
     * SLAM VISUAL
     * =========================================================
     */
    private void spawnSlamEffect(
            Player player,
            Location origin,
            Vector forward,
            Vector right
    ) {

        World world =
                player.getWorld();


        /*
         * 지진 균열의 가장 밝은 중심.
         */
        Particle.DustOptions coreGold =
                new Particle.DustOptions(
                        Color.fromRGB(
                                255,
                                225,
                                80
                        ),
                        1.35f
                );


        /*
         * 메인 금빛 에너지.
         */
        Particle.DustOptions gold =
                new Particle.DustOptions(
                        Color.fromRGB(
                                255,
                                165,
                                20
                        ),
                        1.30f
                );


        /*
         * 뜨거운 지면 균열 외곽.
         */
        Particle.DustOptions orange =
                new Particle.DustOptions(
                        Color.fromRGB(
                                230,
                                85,
                                8
                        ),
                        1.20f
                );


        /*
         * 지면 깊은 곳의 어두운 균열.
         */
        Particle.DustOptions darkEarth =
                new Particle.DustOptions(
                        Color.fromRGB(
                                105,
                                50,
                                18
                        ),
                        1.05f
                );


        /*
         * =====================================================
         * 1. IMPACT POINT
         * =====================================================
         *
         * 플레이어 바로 앞의 지면을 내려찍는다.
         */
        Location impact =
                origin.clone()
                        .add(
                                forward.clone()
                                        .multiply(
                                                0.85
                                        )
                        )
                        .add(
                                0.0,
                                0.12,
                                0.0
                        );


        /*
         * 발동 순간 중앙 금빛 폭발.
         */
        world.spawnParticle(
                Particle.DUST,
                impact,
                26,
                0.35,
                0.18,
                0.35,
                0.0,
                coreGold
        );


        world.spawnParticle(
                Particle.CRIT,
                impact,
                20,
                0.45,
                0.25,
                0.45,
                0.16
        );


        world.spawnParticle(
                Particle.CLOUD,
                impact,
                14,
                0.55,
                0.12,
                0.55,
                0.035
        );


        world.spawnParticle(
                Particle.EXPLOSION,
                impact.clone()
                        .add(
                                0.0,
                                0.12,
                                0.0
                        ),
                1,
                0.0,
                0.0,
                0.0,
                0.0
        );


        /*
         * =====================================================
         * 2. IMPACT RINGS
         * =====================================================
         *
         * 시작 지점에서 3중 충격파가 퍼진 것처럼
         * 서로 다른 크기의 원을 만든다.
         */
        double[] impactRadii = {
                0.65,
                1.15,
                1.70
        };


        for (
                int ring = 0;
                ring < impactRadii.length;
                ring++
        ) {

            double radius =
                    impactRadii[ring];


            int points =
                    14
                            + ring * 6;


            for (
                    int i = 0;
                    i < points;
                    i++
            ) {

                double angle =
                        Math.PI
                                * 2.0
                                * i
                                / points;


                Location point =
                        impact.clone()
                                .add(
                                        Math.cos(
                                                angle
                                        ) * radius,
                                        0.015
                                                + ring * 0.015,
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
                        ring == 0
                                ? coreGold
                                : gold
                );
            }
        }


        /*
         * =====================================================
         * 3. MAIN GROUND FRACTURE
         * =====================================================
         *
         * 플레이어 앞에서 7블록까지
         * 중심 균열을 생성한다.
         */
        for (
                double distance = 0.75;
                distance <= RANGE;
                distance += 0.22
        ) {

            double progress =
                    distance
                            / RANGE;


            Location center =
                    origin.clone()
                            .add(
                                    forward.clone()
                                            .multiply(
                                                    distance
                                            )
                            )
                            .add(
                                    0.0,
                                    0.08,
                                    0.0
                            );


            /*
             * 균열을 완전한 직선으로 만들지 않고
             * 좌우로 흔들리게 한다.
             */
            double wobble =
                    Math.sin(
                            distance * 4.8
                    )
                            * (
                                    0.08
                                            + progress
                                            * 0.22
                            );


            center.add(
                    right.clone()
                            .multiply(
                                    wobble
                            )
            );


            /*
             * 가장 밝은 균열 중심.
             */
            world.spawnParticle(
                    Particle.DUST,
                    center,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    coreGold
            );


            /*
             * 중심 외곽 주황색.
             */
            if (
                    ((int) (distance * 10.0))
                            % 2
                            == 0
            ) {

                world.spawnParticle(
                        Particle.DUST,
                        center.clone()
                                .add(
                                        right.clone()
                                                .multiply(
                                                        0.11
                                                )
                                ),
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        orange
                );


                world.spawnParticle(
                        Particle.DUST,
                        center.clone()
                                .subtract(
                                        right.clone()
                                                .multiply(
                                                        0.11
                                                )
                                ),
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        darkEarth
                );
            }


            /*
             * 진행 방향으로 먼지와 파편 추가.
             */
            if (
                    ((int) (distance * 100.0))
                            % 55
                            < 22
            ) {

                world.spawnParticle(
                        Particle.CLOUD,
                        center,
                        2,
                        0.18
                                + progress * 0.20,
                        0.05,
                        0.18
                                + progress * 0.20,
                        0.012
                );


                world.spawnParticle(
                        Particle.CRIT,
                        center.clone()
                                .add(
                                        0.0,
                                        0.08,
                                        0.0
                                ),
                        1,
                        0.14,
                        0.08,
                        0.14,
                        0.025
                );
            }
        }


        /*
         * =====================================================
         * 4. SIDE FRACTURES
         * =====================================================
         *
         * 아이콘처럼 중심 균열에서
         * 좌우로 갈라지는 가지 균열을 만든다.
         */
        double[] branchDistances = {
                1.7,
                2.8,
                3.9,
                5.0,
                6.1
        };


        for (
                int branch = 0;
                branch < branchDistances.length;
                branch++
        ) {

            double baseDistance =
                    branchDistances[branch];


            double progress =
                    baseDistance
                            / RANGE;


            double branchLength =
                    0.65
                            + progress
                                    * 1.45;


            for (
                    int sideSign = -1;
                    sideSign <= 1;
                    sideSign += 2
            ) {

                /*
                 * 좌우 균열이 서로 완전히 대칭이면
                 * 인공적으로 보이므로 길이를 조금 다르게 한다.
                 */
                double sideLength =
                        branchLength
                                * (
                                        sideSign < 0
                                                ? 0.86
                                                : 1.0
                                );


                int segments =
                        7;


                for (
                        int i = 1;
                        i <= segments;
                        i++
                ) {

                    double segmentProgress =
                            (double) i
                                    / segments;


                    double forwardOffset =
                            Math.sin(
                                    segmentProgress
                                            * Math.PI
                            )
                                    * 0.18
                                    * (
                                            branch % 2 == 0
                                                    ? 1.0
                                                    : -1.0
                                    );


                    double sideOffset =
                            sideLength
                                    * segmentProgress
                                    * sideSign;


                    Location point =
                            origin.clone()
                                    .add(
                                            forward.clone()
                                                    .multiply(
                                                            baseDistance
                                                                    + forwardOffset
                                                    )
                                    )
                                    .add(
                                            right.clone()
                                                    .multiply(
                                                            sideOffset
                                                    )
                                    )
                                    .add(
                                            0.0,
                                            0.075,
                                            0.0
                                    );


                    world.spawnParticle(
                            Particle.DUST,
                            point,
                            1,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            i <= 2
                                    ? gold
                                    : orange
                    );


                    /*
                     * 가지 끝부분은 어두운 균열을 섞는다.
                     */
                    if (
                            i
                                    >= segments - 2
                    ) {

                        world.spawnParticle(
                                Particle.DUST,
                                point.clone()
                                        .add(
                                                0.0,
                                                -0.01,
                                                0.0
                                        ),
                                1,
                                0.0,
                                0.0,
                                0.0,
                                0.0,
                                darkEarth
                        );
                    }
                }
            }
        }


        /*
         * =====================================================
         * 5. FORWARD SHOCK WAVES
         * =====================================================
         *
         * 전방으로 갈수록 넓어지는
         * 반원형 충격파를 세 번 생성한다.
         */
        double[] waveDistances = {
                2.0,
                4.0,
                6.2
        };


        for (
                int wave = 0;
                wave < waveDistances.length;
                wave++
        ) {

            double distance =
                    waveDistances[wave];


            double width =
                    0.75
                            + (
                                    distance
                                            / RANGE
                            )
                                    * HALF_WIDTH;


            int points =
                    17;


            for (
                    int i = 0;
                    i < points;
                    i++
            ) {

                double t =
                        (
                                (double) i
                                        / (
                                                points - 1
                                        )
                        )
                                * 2.0
                                - 1.0;


                /*
                 * 가운데가 조금 더 앞쪽으로 나오도록
                 * 곡선 형태로 만든다.
                 */
                double curve =
                        (
                                1.0
                                        - t * t
                        )
                                * 0.38;


                Location point =
                        origin.clone()
                                .add(
                                        forward.clone()
                                                .multiply(
                                                        distance
                                                                + curve
                                                )
                                )
                                .add(
                                        right.clone()
                                                .multiply(
                                                        t * width
                                                )
                                )
                                .add(
                                        0.0,
                                        0.11
                                                + wave * 0.025,
                                        0.0
                                );


                world.spawnParticle(
                        Particle.DUST,
                        point,
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        wave == 0
                                ? coreGold
                                : gold
                );
            }
        }


        /*
         * 가장 강한 충격 지점을
         * 플레이어 앞쪽에 한 번 더 강조한다.
         */
        Location heavyImpact =
                origin.clone()
                        .add(
                                forward.clone()
                                        .multiply(
                                                2.5
                                        )
                        )
                        .add(
                                0.0,
                                0.22,
                                0.0
                        );


        world.spawnParticle(
                Particle.EXPLOSION,
                heavyImpact,
                1,
                0.0,
                0.0,
                0.0,
                0.0
        );


        world.spawnParticle(
                Particle.CLOUD,
                heavyImpact,
                10,
                0.65,
                0.10,
                0.65,
                0.04
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

        Location ground =
                target.getLocation()
                        .clone()
                        .add(
                                0.0,
                                0.12,
                                0.0
                        );


        Location body =
                target.getLocation()
                        .clone()
                        .add(
                                0.0,
                                Math.min(
                                        0.85,
                                        target.getHeight()
                                                * 0.42
                                ),
                                0.0
                        );


        World world =
                target.getWorld();


        Particle.DustOptions gold =
                new Particle.DustOptions(
                        Color.fromRGB(
                                255,
                                180,
                                28
                        ),
                        1.15f
                );


        Particle.DustOptions orange =
                new Particle.DustOptions(
                        Color.fromRGB(
                                235,
                                85,
                                10
                        ),
                        1.05f
                );


        /*
         * 적 발밑의 작은 충격 링.
         */
        int points =
                14;


        for (
                int i = 0;
                i < points;
                i++
        ) {

            double angle =
                    Math.PI
                            * 2.0
                            * i
                            / points;


            Location point =
                    ground.clone()
                            .add(
                                    Math.cos(
                                            angle
                                    ) * 0.55,
                                    0.0,
                                    Math.sin(
                                            angle
                                    ) * 0.55
                            );


            world.spawnParticle(
                    Particle.DUST,
                    point,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    i % 2 == 0
                            ? gold
                            : orange
            );
        }


        /*
         * 실제 강타 파편.
         */
        world.spawnParticle(
                Particle.CRIT,
                body,
                14,
                0.38,
                0.45,
                0.38,
                0.14
        );


        /*
         * 적이 위로 뜨는 순간
         * 발밑에서 먼지가 터진다.
         */
        world.spawnParticle(
                Particle.CLOUD,
                ground,
                10,
                0.42,
                0.10,
                0.42,
                0.035
        );


        world.spawnParticle(
                Particle.DUST,
                body,
                8,
                0.30,
                0.35,
                0.30,
                0.0,
                gold
        );
    }

}