package com.hcs.rpgcore.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import io.lumine.mythic.bukkit.MythicBukkit;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import org.bukkit.util.Vector;


/*
 * ============================================================
 * RED DRAGON BOSS CONTROLLER
 * ============================================================
 *
 * Drako의 모델 / 애니메이션 / 공격 스킬은
 * MythicMobs + ModelEngine을 사용한다.
 *
 * 실제 전투 판단:
 * - 타겟 선정
 * - 지상 / 공중 전환
 * - 비행 이동
 * - 공격 패턴 선택
 * - 광폭화
 *
 * 위 동작은 RPGCore가 직접 제어한다.
 */
public final class RedDragonBossController {

    /*
     * =========================================================
     * MYTHICMOBS SKILLS
     * =========================================================
     */

    private static final String SKILL_BITE =
            "NocsyDragonAttack_Bite";

    private static final String SKILL_WING_SWING =
            "NocsyDragonAttack_WingSwing";

    private static final String SKILL_FIREBALL_GROUND =
            "NocsyDragonAttack_Fireball_Ground";

    private static final String SKILL_FIREBALL_AIR =
            "NocsyDragonAttack_Fireball_Fly";


    /*
     * Fireball 애니메이션 전체를 다시 실행하지 않고
     * 실제 투사체만 추가 발사할 때 사용한다.
     */
    private static final String SKILL_FIREBALL_PROJECTILE =
            "NocsyDragon_Fireball";

    private static final String SKILL_FIRE_BREATH_AIR =
            "NocsyDragonAttack_Fireflame_Fly";

    private static final String SKILL_GRAB =
            "NocsyDragonAttack_Grab";

    private static final String SKILL_START_FLYING =
            "NocsyDragonAnimating_Start_Flying";

    private static final String SKILL_STOP_FLYING =
            "NocsyDragonAnimating_Stop_Flying";


    /*
     * =========================================================
     * ARENA
     * =========================================================
     */

    private static final double ARENA_CENTER_X =
            -9999784.0D;

    private static final double ARENA_CENTER_Z =
            1000126.0D;

    /*
     * 실제 전투 이동 제한.
     *
     * 기존 원형 장판 반경 35보다 안쪽에서 제어한다.
     */
    private static final double MAX_ARENA_RADIUS =
            32.0D;

    private static final double SAFE_ARENA_RADIUS =
            29.0D;


    /*
     * =========================================================
     * FLIGHT
     * =========================================================
     *
     * 플레이어 시야에서 지나치게 높아지지 않도록
     * 절대 고도를 제한한다.
     */

    private static final double MIN_FLIGHT_Y =
            132.0D;

    private static final double MAX_FLIGHT_Y =
            159.0D;


    /*
     * 타겟 플레이어보다
     * 약 8 ~ 14블록 위를 목표로 한다.
     */
    /*
     * 일반 공중전은 전사도 참여할 수 있도록
     * 저공 비행을 기본으로 한다.
     *
     * 높은 +12 ~ +21 구간은
     * 광폭화 전용 위험 패턴에서만 사용한다.
     */
    private static final double MIN_TARGET_HEIGHT =
            4.0D;

    private static final double MAX_TARGET_HEIGHT =
            9.0D;


    private static final double RAGE_MIN_TARGET_HEIGHT =
            12.0D;

    private static final double RAGE_MAX_TARGET_HEIGHT =
            21.0D;


    /*
     * 플레이어와의 수평 비행 거리.
     */
    private static final double MIN_TARGET_DISTANCE =
            15.0D;

    private static final double MAX_TARGET_DISTANCE =
            25.0D;


    /*
     * 전투장 바닥 기준 위치.
     */
    private static final double GROUND_Y =
            127.0D;


    /*
     * =========================================================
     * TICK
     * =========================================================
     */

    private static final long TASK_PERIOD =
            2L;


    private final JavaPlugin plugin;

    private final LivingEntity boss;

    private final Set<UUID> participants;


    private BukkitTask task;


    /*
     * 콤보/연계 패턴에서 예약된 작업.
     *
     * 보스 처치, 실패, 리셋 시
     * 반드시 전부 취소한다.
     */
    private final List<BukkitTask> patternTasks =
            new ArrayList<>();


    /*
     * 연계 패턴 도중 기본 AI 공격 루프가
     * 다른 스킬을 실행하지 못하도록 잠근다.
     */
    private long patternLockedUntilTick =
            0L;


    private long combatTicks =
            0L;

    private long nextAttackTick =
            0L;

    private long flightStartedTick =
            0L;

    private long groundStartedTick =
            0L;


    private boolean firstFlightTriggered =
            false;

    private boolean flying =
            false;


    /*
     * 지상 -> 비행 전환 전용 상태.
     *
     * 이 구간에서는 수평 비행 목표를 사용하지 않고
     * 거의 수직으로만 상승한다.
     */
    private boolean takingOff =
            false;

    private long takeoffStartedTick =
            0L;

    private double takeoffTargetY =
            0.0D;


    private boolean landing =
            false;


    /*
     * 저공 급강하 공격 상태.
     */
    private boolean diveActive =
            false;

    private long diveStartedTick =
            0L;

    private Location diveTarget;


    /*
     * =========================================================
     * HIGH ALTITUDE RAGE
     * =========================================================
     *
     * HP 30% 이하에서만 사용하는
     * 전사 / 마법사 공통 회피용 고공 위험 패턴.
     */
    private boolean highAltitudeRageActive =
            false;

    private long highAltitudeRageStartedTick =
            0L;

    private long nextHighAltitudeRageTick =
            0L;

    private long nextHighAltitudeRageAttackTick =
            0L;

    private int highAltitudeRageAttackIndex =
            0;

    private int highAltitudeOrbitDirection =
            1;


    private Location flightTarget;

    private Location landingTarget;


    public RedDragonBossController(
            JavaPlugin plugin,
            LivingEntity boss,
            Set<UUID> participants
    ) {

        this.plugin =
                plugin;

        this.boss =
                boss;

        this.participants =
                participants;
    }


    /*
     * =========================================================
     * START
     * =========================================================
     */

    public void start() {

        stop();


        combatTicks =
                0L;

        patternLockedUntilTick =
                0L;

        patternTasks.clear();

        nextAttackTick =
                30L;

        flightStartedTick =
                0L;

        groundStartedTick =
                0L;

        firstFlightTriggered =
                false;

        flying =
                false;

        takingOff =
                false;

        takeoffStartedTick =
                0L;

        takeoffTargetY =
                0.0D;

        landing =
                false;

        diveActive =
                false;

        diveStartedTick =
                0L;

        diveTarget =
                null;

        highAltitudeRageActive =
                false;

        highAltitudeRageStartedTick =
                0L;

        nextHighAltitudeRageTick =
                0L;

        nextHighAltitudeRageAttackTick =
                0L;

        highAltitudeRageAttackIndex =
                0;

        highAltitudeOrbitDirection =
                1;

        flightTarget =
                null;

        landingTarget =
                null;


        if (boss != null) {

            boss.setGravity(
                    true
            );

            boss.setAI(
                    true
            );
        }


        task =
                plugin.getServer()
                        .getScheduler()
                        .runTaskTimer(
                                plugin,
                                this::tick,
                                1L,
                                TASK_PERIOD
                        );
    }


    /*
     * =========================================================
     * STOP
     * =========================================================
     */

    public void stop() {

        for (
                BukkitTask patternTask
                : new ArrayList<>(
                        patternTasks
                )
        ) {

            if (patternTask != null) {

                patternTask.cancel();
            }
        }

        patternTasks.clear();

        patternLockedUntilTick =
                0L;


        if (task != null) {

            task.cancel();

            task =
                    null;
        }


        if (
                boss != null
                        &&
                boss.isValid()
                        &&
                !boss.isDead()
        ) {

            boss.setGravity(
                    true
            );

            boss.setAI(
                    true
            );

            boss.setFallDistance(
                    0.0F
            );
        }


        flying =
                false;

        takingOff =
                false;

        takeoffStartedTick =
                0L;

        takeoffTargetY =
                0.0D;

        landing =
                false;

        diveActive =
                false;

        diveStartedTick =
                0L;

        diveTarget =
                null;

        highAltitudeRageActive =
                false;

        highAltitudeRageStartedTick =
                0L;

        nextHighAltitudeRageAttackTick =
                0L;

        highAltitudeRageAttackIndex =
                0;

        flightTarget =
                null;

        landingTarget =
                null;
    }


    /*
     * =========================================================
     * MAIN TICK
     * =========================================================
     */

    private void tick() {

        combatTicks +=
                TASK_PERIOD;


        if (
                boss == null
                        ||
                !boss.isValid()
                        ||
                boss.isDead()
        ) {

            stop();

            return;
        }


        Player target =
                findTarget();


        if (target == null) {

            return;
        }


        setCurrentTarget(
                target
        );


        double healthPercent =
                getHealthPercent();


        /*
         * =====================================================
         * PHASE 1
         *
         * HP 100% ~ 70%
         * 지상전
         * =====================================================
         */

        if (healthPercent > 0.70D) {

            if (flying || landing) {

                forceGroundState(
                        target
                );
            }


            keepGroundBossInsideArena();

            runGroundCombat(
                    target,
                    false
            );

            return;
        }


        /*
         * =====================================================
         * 최초 70% 진입
         *
         * 무조건 한 번 공중 페이즈로 전환한다.
         * =====================================================
         */

        if (!firstFlightTriggered) {

            firstFlightTriggered =
                    true;

            beginFlight(
                    target
            );

            return;
        }


        /*
         * =====================================================
         * LANDING
         * =====================================================
         */

        if (landing) {

            tickLanding(
                    target
            );

            return;
        }


        /*
         * =====================================================
         * FLIGHT
         * =====================================================
         */

        if (flying) {

            tickFlight(
                    target,
                    healthPercent
            );

            return;
        }


        /*
         * =====================================================
         * GROUND
         *
         * 70 ~ 30%
         * 또는
         * 30% 이하 광폭화
         * =====================================================
         */

        keepGroundBossInsideArena();


        boolean enraged =
                healthPercent <= 0.30D;


        runGroundCombat(
                target,
                enraged
        );


        long groundDuration =
                enraged
                        ? 100L
                        : 160L;


        if (
                combatTicks
                        - groundStartedTick
                        >= groundDuration
        ) {

            beginFlight(
                    target
            );
        }
    }


    /*
     * =========================================================
     * TARGET
     * =========================================================
     */

    private Player findTarget() {

        Player best =
                null;

        double bestDistance =
                Double.MAX_VALUE;


        for (UUID uuid : participants) {

            Player player =
                    plugin.getServer()
                            .getPlayer(
                                    uuid
                            );


            if (
                    player == null
                            ||
                    !player.isOnline()
                            ||
                    player.isDead()
            ) {

                continue;
            }


            if (
                    player.getWorld()
                            != boss.getWorld()
            ) {

                continue;
            }


            double distance =
                    player.getLocation()
                            .distanceSquared(
                                    boss.getLocation()
                            );


            if (distance < bestDistance) {

                bestDistance =
                        distance;

                best =
                        player;
            }
        }


        return best;
    }


    private void setCurrentTarget(
            Player target
    ) {

        if (boss instanceof Mob mob) {

            mob.setTarget(
                    target
            );
        }
    }


    /*
     * =========================================================
     * HEALTH
     * =========================================================
     */

    private double getHealthPercent() {

        double maxHealth =
                boss.getMaxHealth();


        if (maxHealth <= 0.0D) {

            return 0.0D;
        }


        return Math.max(
                0.0D,
                Math.min(
                        1.0D,
                        boss.getHealth()
                                / maxHealth
                )
        );
    }


    /*
     * =========================================================
     * GROUND COMBAT
     * =========================================================
     */

    private void runGroundCombat(
            Player target,
            boolean enraged
    ) {

        if (
                combatTicks
                        < patternLockedUntilTick
        ) {

            return;
        }


        if (combatTicks < nextAttackTick) {

            return;
        }


        double horizontalDistance =
                horizontalDistance(
                        boss.getLocation(),
                        target.getLocation()
                );


        ThreadLocalRandom random =
                ThreadLocalRandom.current();


        /*
         * =====================================================
         * PHASE 3
         *
         * HP 30% 이하
         *
         * Grab 활성화
         * =====================================================
         */

        if (
                enraged
                        &&
                horizontalDistance <= 8.0D
                        &&
                random.nextDouble()
                        < 0.25D
        ) {

            castSkill(
                    SKILL_GRAB
            );


            patternLockedUntilTick =
                    combatTicks
                            + 150L;


            nextAttackTick =
                    patternLockedUntilTick
                            + 20L;

            return;
        }


        /*
         * =====================================================
         * 근거리
         *
         * 단일 Bite
         * 단일 Wing Swing
         * Wing -> Bite 연계
         * =====================================================
         */

        if (horizontalDistance <= 8.0D) {

            double roll =
                    random.nextDouble();


            if (roll < 0.30D) {

                performWingBiteCombo(
                        target,
                        enraged
                );

                return;
            }


            if (roll < 0.65D) {

                castSkill(
                        SKILL_BITE
                );

            } else {

                castSkill(
                        SKILL_WING_SWING
                );
            }


            scheduleNextAttack(
                    enraged
            );

            return;
        }


        /*
         * =====================================================
         * 중 / 장거리
         *
         * 일반 Fireball
         * 또는
         * Fireball Barrage
         * =====================================================
         */

        if (
                random.nextDouble()
                        < 0.40D
        ) {

            performFireballBarrage(
                    target,
                    enraged
            );

            return;
        }


        castSkill(
                SKILL_FIREBALL_GROUND
        );


        scheduleNextAttack(
                enraged
        );
    }


    /*
     * =========================================================
     * WING -> BITE COMBO
     * =========================================================
     *
     * Wing Swing의 원본 애니메이션/attacking 태그가
     * 약 100틱 동안 유지되므로,
     * 해당 동작 종료 후 Bite를 연결한다.
     */

    private void performWingBiteCombo(
            Player target,
            boolean enraged
    ) {

        castSkill(
                SKILL_WING_SWING
        );


        long biteDelay =
                105L;


        patternLockedUntilTick =
                combatTicks
                        + biteDelay
                        + 45L;


        BukkitTask comboTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {

                                    if (
                                            boss == null
                                                    ||
                                            !boss.isValid()
                                                    ||
                                            boss.isDead()
                                    ) {

                                        return;
                                    }


                                    if (
                                            target == null
                                                    ||
                                            !target.isOnline()
                                                    ||
                                            target.isDead()
                                    ) {

                                        return;
                                    }


                                    setCurrentTarget(
                                            target
                                    );


                                    /*
                                     * Wing Swing으로 밀려난 플레이어가
                                     * 너무 멀리 있다면 Bite는 낭비하지 않는다.
                                     */
                                    if (
                                            horizontalDistance(
                                                    boss.getLocation(),
                                                    target.getLocation()
                                            ) > 10.0D
                                    ) {

                                        return;
                                    }


                                    castSkill(
                                            SKILL_BITE
                                    );
                                },
                                biteDelay
                        );


        patternTasks.add(
                comboTask
        );


        nextAttackTick =
                patternLockedUntilTick
                        + (
                        enraged
                                ? 15L
                                : 30L
                );
    }


    /*
     * =========================================================
     * FIREBALL BARRAGE
     * =========================================================
     *
     * 첫 번째 발사에서는 정상 Fireball Ground
     * 애니메이션을 사용한다.
     *
     * 이후 추가 2발은 동일한 긴 애니메이션을
     * 다시 시작하지 않고 실제 projectile skill만 호출한다.
     */

    private void performFireballBarrage(
            Player target,
            boolean enraged
    ) {

        castSkill(
                SKILL_FIREBALL_GROUND
        );


        /*
         * 첫 Fireball Ground는 약 30틱 후 발사된다.
         * 추가 탄은 그 뒤 간격을 두고 발사한다.
         */
        schedulePatternSkill(
                SKILL_FIREBALL_PROJECTILE,
                42L
        );

        schedulePatternSkill(
                SKILL_FIREBALL_PROJECTILE,
                56L
        );


        patternLockedUntilTick =
                combatTicks
                        + 75L;


        nextAttackTick =
                patternLockedUntilTick
                        + (
                        enraged
                                ? 15L
                                : 30L
                );
    }


    /*
     * =========================================================
     * PATTERN SCHEDULER
     * =========================================================
     */

    private void schedulePatternSkill(
            String skillName,
            long delay
    ) {

        BukkitTask scheduled =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {

                                    if (
                                            boss == null
                                                    ||
                                            !boss.isValid()
                                                    ||
                                            boss.isDead()
                                    ) {

                                        return;
                                    }


                                    castSkill(
                                            skillName
                                    );
                                },
                                delay
                        );


        patternTasks.add(
                scheduled
        );
    }


    /*
     * =========================================================
     * LOW FIRE BREATH PASS
     * =========================================================
     *
     * 플레이어보다 낮은 고도로 접근한 뒤
     * 플레이어 앞을 가로지르면서 Fire Breath를 사용한다.
     *
     * 전사는 회피 후 Dash / 근접 공격으로
     * 반격할 수 있는 저공 패턴이다.
     */

    private void performLowFireBreathPass(
            Player target,
            boolean enraged
    ) {

        Location playerLocation =
                target.getLocation();


        Vector horizontal =
                playerLocation.toVector()
                        .subtract(
                                boss.getLocation().toVector()
                        );

        horizontal.setY(
                0.0D
        );


        if (
                horizontal.lengthSquared()
                        <= 0.01D
        ) {

            horizontal =
                    new Vector(
                            1.0D,
                            0.0D,
                            0.0D
                    );

        } else {

            horizontal.normalize();
        }


        /*
         * 플레이어 위치를 약간 지나치는 지점을 목표로 한다.
         */
        double x =
                playerLocation.getX()
                        + horizontal.getX()
                        * 12.0D;

        double z =
                playerLocation.getZ()
                        + horizontal.getZ()
                        * 12.0D;


        /*
         * 전사가 접근 가능한 저공 높이.
         */
        double y =
                clamp(
                        playerLocation.getY()
                                + 15.0D,
                        MIN_FLIGHT_Y,
                        MAX_FLIGHT_Y
                );


        Location passTarget =
                clampInsideArena(
                        new Location(
                                boss.getWorld(),
                                x,
                                y,
                                z
                        )
                );


        flightTarget =
                passTarget;


        faceToward(
                target.getLocation()
        );


        castSkill(
                SKILL_FIRE_BREATH_AIR
        );


        patternLockedUntilTick =
                combatTicks
                        + 75L;


        nextAttackTick =
                patternLockedUntilTick
                        + (
                        enraged
                                ? 18L
                                : 32L
                );
    }


    /*
     * =========================================================
     * DIVE ATTACK
     * =========================================================
     *
     * 저공 비행 상태에서 플레이어 쪽으로 급강하한다.
     *
     * 플레이어 근처까지 도달하면 Bite를 실행하고
     * 바로 착지 단계로 넘어간다.
     */

    private void performDiveAttack(
            Player target,
            boolean enraged
    ) {

        diveActive =
                true;

        diveStartedTick =
                combatTicks;


        diveTarget =
                createDiveTarget(
                        target
                );


        patternLockedUntilTick =
                combatTicks
                        + 70L;


        nextAttackTick =
                patternLockedUntilTick
                        + (
                        enraged
                                ? 15L
                                : 30L
                );
    }


    private Location createDiveTarget(
            Player target
    ) {

        Location targetLocation =
                target.getLocation()
                        .clone();


        /*
         * Drako 모델 크기를 고려하여
         * 플레이어보다 약간 위쪽을 향한다.
         */
        targetLocation.setY(
                Math.max(
                        GROUND_Y + 2.0D,
                        targetLocation.getY()
                                + 2.0D
                )
        );


        return clampInsideArena(
                targetLocation
        );
    }


    private void tickDiveAttack(
            Player target
    ) {

        if (
                target == null
                        ||
                !target.isOnline()
                        ||
                target.isDead()
        ) {

            cancelDiveAttack();

            return;
        }


        /*
         * 플레이어 이동을 어느 정도 따라가도록
         * 목표 위치를 갱신한다.
         */
        diveTarget =
                createDiveTarget(
                        target
                );


        Location current =
                boss.getLocation();


        Vector direction =
                diveTarget.toVector()
                        .subtract(
                                current.toVector()
                        );


        double horizontal =
                horizontalDistance(
                        current,
                        target.getLocation()
                );


        /*
         * 충분히 근접하면 Bite 후 착지.
         */
        if (
                horizontal <= 5.5D
                        ||
                direction.lengthSquared()
                        <= 9.0D
        ) {

            diveActive =
                    false;

            diveTarget =
                    null;


            faceToward(
                    target.getLocation()
            );


            castSkill(
                    SKILL_BITE
            );


            beginLanding(
                    target
            );

            return;
        }


        /*
         * 너무 오래 추적해서 플레이어를 괴롭히지 않는다.
         */
        if (
                combatTicks
                        - diveStartedTick
                        >= 50L
        ) {

            diveActive =
                    false;

            diveTarget =
                    null;

            beginLanding(
                    target
            );

            return;
        }


        if (
                direction.lengthSquared()
                        <= 0.01D
        ) {

            return;
        }


        Vector velocity =
                direction.normalize()
                        .multiply(
                                0.82D
                        );


        /*
         * Dive이므로 위로 튀지 않도록 한다.
         */
        if (
                current.getY()
                        > diveTarget.getY()
                        &&
                velocity.getY() > -0.15D
        ) {

            velocity.setY(
                    -0.15D
            );
        }


        boss.setVelocity(
                velocity
        );


        faceDirection(
                velocity
        );
    }


    private void cancelDiveAttack() {

        diveActive =
                false;

        diveTarget =
                null;
    }


    /*
     * =========================================================
     * ARENA LOCATION
     * =========================================================
     */

    private Location clampInsideArena(
            Location location
    ) {

        double dx =
                location.getX()
                        - ARENA_CENTER_X;

        double dz =
                location.getZ()
                        - ARENA_CENTER_Z;


        double radius =
                Math.sqrt(
                        dx * dx
                                + dz * dz
                );


        if (radius <= SAFE_ARENA_RADIUS) {

            return location;
        }


        double scale =
                SAFE_ARENA_RADIUS
                        / radius;


        location.setX(
                ARENA_CENTER_X
                        + dx * scale
        );

        location.setZ(
                ARENA_CENTER_Z
                        + dz * scale
        );


        return location;
    }


    private void faceToward(
            Location location
    ) {

        if (location == null) {

            return;
        }


        Vector direction =
                location.toVector()
                        .subtract(
                                boss.getLocation()
                                        .toVector()
                        );


        faceDirection(
                direction
        );
    }


    /*
     * =========================================================
     * HIGH ALTITUDE RAGE
     * =========================================================
     *
     * HP 30% 이하에서만 발동한다.
     *
     * 이 패턴은 직업별 딜 타임이 아니라
     * 모든 참가자가 회피해야 하는 위험 구간이다.
     *
     * 약 7초 동안 고공에서 플레이어 주변을 선회하며
     * Fireball / Fire Breath를 사용한 뒤
     * 급강하 -> Bite -> 착지로 종료한다.
     */

    private void beginHighAltitudeRage(
            Player target
    ) {

        if (
                highAltitudeRageActive
                        ||
                landing
                        ||
                diveActive
        ) {

            return;
        }


        highAltitudeRageActive =
                true;

        highAltitudeRageStartedTick =
                combatTicks;

        nextHighAltitudeRageAttackTick =
                combatTicks
                        + 20L;

        highAltitudeRageAttackIndex =
                0;


        highAltitudeOrbitDirection =
                ThreadLocalRandom.current()
                        .nextBoolean()
                        ? 1
                        : -1;


        boss.setAI(
                false
        );

        boss.setGravity(
                false
        );

        boss.setFallDistance(
                0.0F
        );


        /*
         * 기존 일반 저공 목표점을 제거하고
         * 고공 선회 상태로 전환한다.
         */
        flightTarget =
                null;


        patternLockedUntilTick =
                combatTicks
                        + 160L;


        plugin.getLogger()
                .info(
                        "[RedDragonBoss] "
                                + "High Altitude Rage started."
                );
    }


    private void tickHighAltitudeRage(
            Player target
    ) {

        if (
                target == null
                        ||
                !target.isOnline()
                        ||
                target.isDead()
        ) {

            finishHighAltitudeRage(
                    target
            );

            return;
        }


        boss.setAI(
                false
        );

        boss.setGravity(
                false
        );

        boss.setFallDistance(
                0.0F
        );


        long elapsed =
                combatTicks
                        - highAltitudeRageStartedTick;


        /*
         * =====================================================
         * HIGH ALTITUDE ORBIT
         * =====================================================
         *
         * 플레이어 주위를 약 22블록 반경으로 선회한다.
         */

        double angle =
                elapsed
                        * 0.075D
                        * highAltitudeOrbitDirection;


        double orbitRadius =
                22.0D;


        Location playerLocation =
                target.getLocation();


        double x =
                playerLocation.getX()
                        + Math.cos(
                                angle
                        )
                        * orbitRadius;

        double z =
                playerLocation.getZ()
                        + Math.sin(
                                angle
                        )
                        * orbitRadius;


        /*
         * 일반 저공 +4 ~ +9와 구분되는
         * 광폭화 전용 +12 ~ +21 고도.
         */
        double rageHeight =
                16.0D;


        double y =
                clamp(
                        playerLocation.getY()
                                + rageHeight,
                        MIN_FLIGHT_Y,
                        MAX_FLIGHT_Y
                );


        Location orbitTarget =
                clampInsideArena(
                        new Location(
                                boss.getWorld(),
                                x,
                                y,
                                z
                        )
                );


        moveTowardRageTarget(
                orbitTarget
        );


        /*
         * =====================================================
         * HIGH ALTITUDE ATTACKS
         * =====================================================
         */

        if (
                combatTicks
                        >= nextHighAltitudeRageAttackTick
        ) {

            highAltitudeRageAttackIndex++;


            /*
             * 세 번째 공격마다 Fire Breath.
             *
             * 나머지는 직접 projectile을 발사해서
             * 긴 Fireball 애니메이션 때문에
             * 패턴 전체가 멈추는 것을 방지한다.
             */
            if (
                    highAltitudeRageAttackIndex
                            % 3
                            == 0
            ) {

                faceToward(
                        target.getLocation()
                );


                castSkill(
                        SKILL_FIRE_BREATH_AIR
                );


                nextHighAltitudeRageAttackTick =
                        combatTicks
                                + 38L;

            } else {

                faceToward(
                        target.getLocation()
                );


                castSkill(
                        SKILL_FIREBALL_PROJECTILE
                );


                nextHighAltitudeRageAttackTick =
                        combatTicks
                                + 24L;
            }
        }


        /*
         * 약 7초 후 급강하로 전환한다.
         */
        if (elapsed >= 140L) {

            finishHighAltitudeRage(
                    target
            );
        }
    }


    private void moveTowardRageTarget(
            Location target
    ) {

        if (target == null) {

            return;
        }


        Location current =
                boss.getLocation();


        Vector direction =
                target.toVector()
                        .subtract(
                                current.toVector()
                        );


        if (
                direction.lengthSquared()
                        <= 0.01D
        ) {

            return;
        }


        /*
         * 일반 비행 0.42보다 빠른 선회.
         */
        Vector velocity =
                direction.normalize()
                        .multiply(
                                0.68D
                        );


        boss.setVelocity(
                velocity
        );


        faceDirection(
                velocity
        );
    }


    private void finishHighAltitudeRage(
            Player target
    ) {

        if (!highAltitudeRageActive) {

            return;
        }


        highAltitudeRageActive =
                false;


        /*
         * 고공 위험 패턴 재사용 대기시간.
         *
         * 약 18초.
         */
        nextHighAltitudeRageTick =
                combatTicks
                        + 360L;


        nextHighAltitudeRageAttackTick =
                0L;


        /*
         * 마지막은 급강하 공격으로 연결한다.
         */
        if (
                target != null
                        &&
                target.isOnline()
                        &&
                !target.isDead()
        ) {

            performDiveAttack(
                    target,
                    true
            );

            return;
        }


        /*
         * 타겟이 사라졌다면 안전하게 착지한다.
         */
        flying =
                false;

        boss.setGravity(
                true
        );

        boss.setAI(
                true
        );


        plugin.getLogger()
                .info(
                        "[RedDragonBoss] "
                                + "High Altitude Rage finished."
                );
    }


    /*
     * =========================================================
     * BEGIN FLIGHT
     * =========================================================
     */

    private void beginFlight(
            Player target
    ) {

        if (
                flying
                        ||
                takingOff
                        ||
                landing
        ) {

            return;
        }


        /*
         * =====================================================
         * TAKEOFF START
         * =====================================================
         */

        flying =
                true;

        takingOff =
                true;

        landing =
                false;


        boss.setAI(
                false
        );

        boss.setGravity(
                false
        );

        boss.setFallDistance(
                0.0F
        );


        /*
         * ModelEngine / MythicMobs의 fly 상태 전환.
         */
        castSkill(
                SKILL_START_FLYING
        );


        flightStartedTick =
                combatTicks;

        takeoffStartedTick =
                combatTicks;


        /*
         * 일반 저공 순항에 진입하기 위한
         * 초기 상승 목표.
         *
         * 지상에서 약 5블록 정도 상승하되
         * 절대 비행 범위를 넘지 않는다.
         */
        takeoffTargetY =
                clamp(
                        boss.getLocation().getY()
                                + 5.0D,
                        MIN_FLIGHT_Y,
                        MAX_FLIGHT_Y
                );


        /*
         * TAKEOFF 동안에는 수평 목표를 만들지 않는다.
         */
        flightTarget =
                null;


        /*
         * 이륙 도중 공격 금지.
         */
        patternLockedUntilTick =
                combatTicks
                        + 22L;

        nextAttackTick =
                combatTicks
                        + 45L;
    }


    /*
     * =========================================================
     * TAKEOFF
     * =========================================================
     *
     * 약 0.8 ~ 1.0초 동안 거의 수직으로 상승한다.
     *
     * 이 구간이 끝난 뒤에야 일반 flightTarget을 생성해서
     * 수평 저공비행을 시작한다.
     */

    private void tickTakeoff(
            Player target
    ) {

        boss.setAI(
                false
        );

        boss.setGravity(
                false
        );

        boss.setFallDistance(
                0.0F
        );


        Location current =
                boss.getLocation();


        double remainingY =
                takeoffTargetY
                        - current.getY();


        long elapsed =
                combatTicks
                        - takeoffStartedTick;


        /*
         * 목표 고도 도달 또는 최대 약 1초 경과.
         */
        if (
                remainingY <= 0.35D
                        ||
                elapsed >= 20L
        ) {

            finishTakeoff(
                    target
            );

            return;
        }


        /*
         * 수직 상승 속도.
         *
         * 처음에는 조금 빠르게 뜨고
         * 목표에 가까워지면 부드럽게 감속한다.
         */
        double verticalSpeed =
                clamp(
                        remainingY * 0.18D,
                        0.16D,
                        0.34D
                );


        /*
         * 수평 속도는 거의 제거한다.
         *
         * 완전히 0으로 고정하면 모델이 뻣뻣하게 보일 수 있어
         * 기존 수평 속도를 매우 약하게만 남긴다.
         */
        Vector currentVelocity =
                boss.getVelocity()
                        .clone();


        Vector velocity =
                new Vector(
                        currentVelocity.getX()
                                * 0.12D,
                        verticalSpeed,
                        currentVelocity.getZ()
                                * 0.12D
                );


        boss.setVelocity(
                velocity
        );


        /*
         * 이륙 중에는 플레이어 방향을 유지한다.
         */
        if (
                target != null
                        &&
                target.isOnline()
                        &&
                !target.isDead()
        ) {

            faceToward(
                    target.getLocation()
            );
        }
    }


    private void finishTakeoff(
            Player target
    ) {

        takingOff =
                false;


        /*
         * 여기서 처음으로 일반 저공 비행 목표를 생성한다.
         */
        if (
                target != null
                        &&
                target.isOnline()
                        &&
                !target.isDead()
        ) {

            flightTarget =
                    createFlightTarget(
                            target
                    );

        } else {

            flightTarget =
                    null;
        }


        /*
         * TAKEOFF에서 남은 수직 속도가
         * 순항 시작 시 과하게 이어지지 않도록 제한.
         */
        Vector velocity =
                boss.getVelocity()
                        .clone();

        velocity.setY(
                clamp(
                        velocity.getY(),
                        -0.10D,
                        0.10D
                )
        );

        boss.setVelocity(
                velocity
        );
    }


    /*
     * =========================================================
     * FLIGHT
     * =========================================================
     */

    private void tickFlight(
            Player target,
            double healthPercent
    ) {

        boss.setFallDistance(
                0.0F
        );


        /*
         * 지상에서 바로 일반 순항으로 넘어가지 않고
         * TAKEOFF 단계를 먼저 처리한다.
         */
        if (takingOff) {

            tickTakeoff(
                    target
            );

            return;
        }


        if (highAltitudeRageActive) {

            tickHighAltitudeRage(
                    target
            );

            return;
        }


        if (diveActive) {

            tickDiveAttack(
                    target
            );

            return;
        }


        correctAbsoluteFlightHeight();


        if (
                flightTarget == null
                        ||
                boss.getLocation()
                        .distanceSquared(
                                flightTarget
                        ) <= 9.0D
                        ||
                combatTicks % 50L == 0L
        ) {

            flightTarget =
                    createFlightTarget(
                            target
                    );
        }


        moveTowardFlightTarget();


        boolean enraged =
                healthPercent <= 0.30D;


        /*
         * =====================================================
         * HIGH ALTITUDE RAGE
         * =====================================================
         *
         * HP 30% 이하에서만 사용.
         *
         * 일반 비행 시작 직후 즉시 올라가지 않도록
         * 최소 1.5초 정도의 저공 전투 후 진입한다.
         */
        if (
                enraged
                        &&
                combatTicks
                        >= nextHighAltitudeRageTick
                        &&
                combatTicks
                        - flightStartedTick
                        >= 30L
                        &&
                combatTicks
                        >= patternLockedUntilTick
        ) {

            beginHighAltitudeRage(
                    target
            );

            return;
        }


        /*
         * 공중 공격.
         */
        if (
                !diveActive
                        &&
                combatTicks >= nextAttackTick
                        &&
                combatTicks >= patternLockedUntilTick
        ) {

            ThreadLocalRandom random =
                    ThreadLocalRandom.current();


            double roll =
                    random.nextDouble();


            /*
             * 저공 급강하.
             */
            if (roll < 0.25D) {

                performDiveAttack(
                        target,
                        enraged
                );

                return;
            }


            /*
             * 플레이어 앞을 낮게 가로지르며
             * Fire Breath를 사용한다.
             */
            if (roll < 0.55D) {

                performLowFireBreathPass(
                        target,
                        enraged
                );

                return;
            }


            double breathChance =
                    enraged
                            ? 0.65D
                            : 0.40D;


            if (
                    random.nextDouble()
                            < breathChance
            ) {

                castSkill(
                        SKILL_FIRE_BREATH_AIR
                );

            } else {

                castSkill(
                        SKILL_FIREBALL_AIR
                );
            }


            scheduleNextAirAttack(
                    enraged
            );
        }


        /*
         * 공중 체류시간.
         *
         * 일반:
         * 약 10초
         *
         * 광폭화:
         * 약 8초
         */
        long flightDuration =
                enraged
                        ? 160L
                        : 200L;


        if (
                combatTicks
                        - flightStartedTick
                        >= flightDuration
        ) {

            beginLanding(
                    target
            );
        }
    }


    /*
     * =========================================================
     * FLIGHT TARGET
     * =========================================================
     */

    private Location createFlightTarget(
            Player target
    ) {

        ThreadLocalRandom random =
                ThreadLocalRandom.current();


        double angle =
                random.nextDouble(
                        0.0D,
                        Math.PI * 2.0D
                );


        double distance =
                random.nextDouble(
                        MIN_TARGET_DISTANCE,
                        MAX_TARGET_DISTANCE
                );


        double x =
                target.getLocation().getX()
                        + Math.cos(angle)
                        * distance;

        double z =
                target.getLocation().getZ()
                        + Math.sin(angle)
                        * distance;


        double targetY =
                target.getLocation().getY()
                        + random.nextDouble(
                                MIN_TARGET_HEIGHT,
                                MAX_TARGET_HEIGHT
                        );


        targetY =
                clamp(
                        targetY,
                        MIN_FLIGHT_Y,
                        MAX_FLIGHT_Y
                );


        /*
         * 전투장 원 바깥으로 목표점이 생성되면
         * 원 안쪽으로 투영한다.
         */
        double dx =
                x - ARENA_CENTER_X;

        double dz =
                z - ARENA_CENTER_Z;

        double radius =
                Math.sqrt(
                        dx * dx
                                + dz * dz
                );


        if (radius > SAFE_ARENA_RADIUS) {

            double scale =
                    SAFE_ARENA_RADIUS
                            / radius;


            x =
                    ARENA_CENTER_X
                            + dx * scale;

            z =
                    ARENA_CENTER_Z
                            + dz * scale;
        }


        return new Location(
                boss.getWorld(),
                x,
                targetY,
                z
        );
    }


    /*
     * =========================================================
     * FLIGHT MOVEMENT
     * =========================================================
     */

    private void moveTowardFlightTarget() {

        if (flightTarget == null) {

            return;
        }


        Location current =
                boss.getLocation();


        Vector direction =
                flightTarget.toVector()
                        .subtract(
                                current.toVector()
                        );


        if (
                direction.lengthSquared()
                        <= 0.01D
        ) {

            boss.setVelocity(
                    new Vector(
                            0.0D,
                            0.0D,
                            0.0D
                    )
            );

            return;
        }


        /*
         * 지나치게 빠르게 날지 않도록
         * 공중 이동 속도를 제한한다.
         */
        double speed =
                0.52D;


        Vector velocity =
                direction.normalize()
                        .multiply(
                                speed
                        );


        boss.setVelocity(
                velocity
        );


        faceDirection(
                velocity
        );
    }


    /*
     * =========================================================
     * LANDING
     * =========================================================
     */

    private void beginLanding(
            Player target
    ) {

        if (!flying) {

            return;
        }


        flying =
                false;

        landing =
                true;


        castSkill(
                SKILL_STOP_FLYING
        );


        boss.setAI(
                false
        );

        boss.setGravity(
                false
        );


        landingTarget =
                createLandingTarget(
                        target
                );
    }


    private Location createLandingTarget(
            Player target
    ) {

        double x =
                target.getLocation().getX();

        double z =
                target.getLocation().getZ();


        double dx =
                x - ARENA_CENTER_X;

        double dz =
                z - ARENA_CENTER_Z;

        double radius =
                Math.sqrt(
                        dx * dx
                                + dz * dz
                );


        /*
         * 플레이어 바로 머리 위에 착지하지 않고
         * 조금 떨어진 위치를 사용한다.
         */
        if (radius <= 0.01D) {

            x =
                    ARENA_CENTER_X + 8.0D;

            z =
                    ARENA_CENTER_Z;

        } else {

            double desiredRadius =
                    Math.min(
                            radius,
                            22.0D
                    );


            x =
                    ARENA_CENTER_X
                            + dx / radius
                            * desiredRadius;

            z =
                    ARENA_CENTER_Z
                            + dz / radius
                            * desiredRadius;
        }


        return new Location(
                boss.getWorld(),
                x,
                GROUND_Y,
                z
        );
    }


    private void tickLanding(
            Player target
    ) {

        boss.setFallDistance(
                0.0F
        );


        if (landingTarget == null) {

            landingTarget =
                    createLandingTarget(
                            target
                    );
        }


        Location current =
                boss.getLocation();


        Vector direction =
                landingTarget.toVector()
                        .subtract(
                                current.toVector()
                        );


        if (
                direction.lengthSquared()
                        <= 2.25D
                        ||
                current.getY()
                        <= GROUND_Y + 0.6D
        ) {

            finishLanding();

            return;
        }


        Vector velocity =
                direction.normalize()
                        .multiply(
                                0.48D
                        );


        /*
         * 반드시 하강하도록 한다.
         */
        if (velocity.getY() > -0.12D) {

            velocity.setY(
                    -0.12D
            );
        }


        boss.setVelocity(
                velocity
        );


        faceDirection(
                velocity
        );
    }


    private void finishLanding() {

        if (landingTarget != null) {

            Location current =
                    boss.getLocation();


            Location finalLocation =
                    landingTarget.clone();


            finalLocation.setYaw(
                    current.getYaw()
            );

            finalLocation.setPitch(
                    0.0F
            );


            boss.teleport(
                    finalLocation
            );
        }


        boss.setVelocity(
                new Vector(
                        0.0D,
                        0.0D,
                        0.0D
                )
        );


        boss.setFallDistance(
                0.0F
        );

        boss.setGravity(
                true
        );

        boss.setAI(
                true
        );


        landing =
                false;

        landingTarget =
                null;

        groundStartedTick =
                combatTicks;


        nextAttackTick =
                combatTicks
                        + 20L;
    }


    /*
     * =========================================================
     * FORCE GROUND
     * =========================================================
     */

    private void forceGroundState(
            Player target
    ) {

        flying =
                false;

        landing =
                false;


        flightTarget =
                null;

        landingTarget =
                null;


        boss.setGravity(
                true
        );

        boss.setAI(
                true
        );

        boss.setFallDistance(
                0.0F
        );


        setCurrentTarget(
                target
        );
    }


    /*
     * =========================================================
     * ARENA BOUNDARY
     * =========================================================
     */

    private void keepGroundBossInsideArena() {

        Location current =
                boss.getLocation();


        double dx =
                current.getX()
                        - ARENA_CENTER_X;

        double dz =
                current.getZ()
                        - ARENA_CENTER_Z;


        double radius =
                Math.sqrt(
                        dx * dx
                                + dz * dz
                );


        if (radius <= MAX_ARENA_RADIUS) {

            return;
        }


        double scale =
                SAFE_ARENA_RADIUS
                        / radius;


        Location corrected =
                current.clone();


        corrected.setX(
                ARENA_CENTER_X
                        + dx * scale
        );

        corrected.setZ(
                ARENA_CENTER_Z
                        + dz * scale
        );


        boss.teleport(
                corrected
        );
    }


    private void correctAbsoluteFlightHeight() {

        Location location =
                boss.getLocation();


        /*
         * 이륙 직후에는 지상 Y에서 비행 고도로
         * 자연스럽게 상승하도록 강제 보정을 하지 않는다.
         *
         * 20틱 = 약 1초.
         */
        if (takingOff) {

            return;
        }


        double currentY =
                location.getY();


        /*
         * 정상 비행 범위 안이면 아무 처리도 하지 않는다.
         */
        if (
                currentY >= MIN_FLIGHT_Y
                        &&
                currentY <= MAX_FLIGHT_Y
        ) {

            return;
        }


        /*
         * teleport()로 즉시 높이를 변경하지 않고
         * velocity를 이용해 천천히 정상 고도로 복귀시킨다.
         */
        Vector velocity =
                boss.getVelocity()
                        .clone();


        if (currentY < MIN_FLIGHT_Y) {

            velocity.setY(
                    Math.max(
                            velocity.getY(),
                            0.22D
                    )
            );

        } else if (currentY > MAX_FLIGHT_Y) {

            velocity.setY(
                    Math.min(
                            velocity.getY(),
                            -0.22D
                    )
            );
        }


        boss.setVelocity(
                velocity
        );
    }


    /*
     * =========================================================
     * ATTACK COOLDOWN
     * =========================================================
     */

    private void scheduleNextAttack(
            boolean enraged
    ) {

        ThreadLocalRandom random =
                ThreadLocalRandom.current();


        if (enraged) {

            nextAttackTick =
                    combatTicks
                            + random.nextLong(
                                    20L,
                                    33L
                            );

            return;
        }


        nextAttackTick =
                combatTicks
                        + random.nextLong(
                                40L,
                                56L
                        );
    }


    private void scheduleNextAirAttack(
            boolean enraged
    ) {

        ThreadLocalRandom random =
                ThreadLocalRandom.current();


        if (enraged) {

            nextAttackTick =
                    combatTicks
                            + random.nextLong(
                                    20L,
                                    33L
                            );

            return;
        }


        nextAttackTick =
                combatTicks
                        + random.nextLong(
                                36L,
                                51L
                        );
    }


    /*
     * =========================================================
     * MYTHIC SKILL
     * =========================================================
     */

    private void castSkill(
            String skillName
    ) {

        try {

            boolean success =
                    MythicBukkit.inst()
                            .getAPIHelper()
                            .castSkill(
                                    boss,
                                    skillName
                            );


            if (!success) {

                plugin.getLogger()
                        .warning(
                                "[RedDragonBoss] "
                                        + "Failed to cast skill: "
                                        + skillName
                        );
            }

        } catch (Exception exception) {

            plugin.getLogger()
                    .warning(
                            "[RedDragonBoss] "
                                    + "Skill exception: "
                                    + skillName
                                    + " / "
                                    + exception.getMessage()
                    );
        }
    }


    /*
     * =========================================================
     * ROTATION
     * =========================================================
     */

    private void faceDirection(
            Vector direction
    ) {

        if (
                direction == null
                        ||
                direction.lengthSquared()
                        <= 0.001D
        ) {

            return;
        }


        double x =
                direction.getX();

        double y =
                direction.getY();

        double z =
                direction.getZ();


        float yaw =
                (float) Math.toDegrees(
                        Math.atan2(
                                -x,
                                z
                        )
                );


        double horizontal =
                Math.sqrt(
                        x * x
                                + z * z
                );


        float pitch =
                (float) Math.toDegrees(
                        -Math.atan2(
                                y,
                                horizontal
                        )
                );


        boss.setRotation(
                yaw,
                pitch
        );
    }


    /*
     * =========================================================
     * UTILITY
     * =========================================================
     */

    private static double horizontalDistance(
            Location first,
            Location second
    ) {

        double dx =
                first.getX()
                        - second.getX();

        double dz =
                first.getZ()
                        - second.getZ();


        return Math.sqrt(
                dx * dx
                        + dz * dz
        );
    }


    private static double clamp(
            double value,
            double min,
            double max
    ) {

        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }
}
