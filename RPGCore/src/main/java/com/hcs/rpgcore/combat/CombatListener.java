package com.hcs.rpgcore.combat;

import com.hcs.rpgcore.skill.SkillDefenseBuffService;
import com.hcs.rpgcore.skill.SkillOffenseBuffService;

import org.bukkit.NamespacedKey;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.persistence.PersistentDataType;

import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.EntityDamageByEntityEvent;


public final class CombatListener
        implements Listener {

    private final CombatService combatService;

    private final SkillDefenseBuffService
            skillDefenseBuffService;

    private final SkillOffenseBuffService
            skillOffenseBuffService;


    /*
     * =========================================================
     * MAL_NYUN SWORD
     * =========================================================
     */
    private static final String MAL_NYUN_SWORD_ID =
            "mal_nyun_sword";

    private static final String MAL_NYUN_IRON_SWORD_ID =
            "mal_nyun_iron_sword";

    private static final double MAL_NYUN_SWORD_RANGE =
            10.0D;

    private static final double MAL_NYUN_SWORD_RANGE_SQUARED =
            MAL_NYUN_SWORD_RANGE
                    * MAL_NYUN_SWORD_RANGE;


    private final NamespacedKey
            customItemIdKey;


    /*
     * 광역 피해로 발생한 EntityDamageByEntityEvent가
     * 다시 광역 공격을 생성하는 것을 막는다.
     */
    private final Set<UUID>
            malNyunSwordAreaAttackers =
            new HashSet<>();


    /*
     * 기존 생성자.
     *
     * RPGCorePlugin 연결을 다음 단계에서 변경하기 전에도
     * 현재 프로젝트가 계속 빌드되도록 유지한다.
     */
    public CombatListener(
            CombatService combatService,
            SkillDefenseBuffService skillDefenseBuffService
    ) {

        this(
                combatService,
                skillDefenseBuffService,
                null
        );
    }


    /*
     * Lv70 공격 버프까지 사용하는 생성자.
     */
    public CombatListener(
            CombatService combatService,
            SkillDefenseBuffService skillDefenseBuffService,
            SkillOffenseBuffService skillOffenseBuffService
    ) {

        this(
                combatService,
                skillDefenseBuffService,
                skillOffenseBuffService,
                null
        );
    }


    /*
     * RPGCorePlugin에서 사용하는 생성자.
     *
     * custom_item_id NamespacedKey를 생성하기 위해
     * Plugin 인스턴스를 함께 받는다.
     */
    public CombatListener(
            CombatService combatService,
            SkillDefenseBuffService skillDefenseBuffService,
            SkillOffenseBuffService skillOffenseBuffService,
            Plugin plugin
    ) {

        this.combatService =
                combatService;

        this.skillDefenseBuffService =
                skillDefenseBuffService;

        this.skillOffenseBuffService =
                skillOffenseBuffService;

        this.customItemIdKey =
                plugin == null
                        ? null
                        : new NamespacedKey(
                                plugin,
                                "custom_item_id"
                        );
    }


    /*
     * =========================================================
     * RPG PHYSICAL DEFENSE
     * =========================================================
     *
     * 처리 순서:
     *
     * 1. Minecraft vanilla armor / toughness / Protection
     * 2. RPG DEF
     * 3. flat damage reduction
     * 4. Iron Wall / Mana Shield
     *
     * Combat v1:
     *
     * EntityDamageByEntityEvent에만 적용.
     *
     * 즉:
     *
     * 몬스터 공격
     * 플레이어 공격
     * 화살 등 엔티티 기반 피해
     *
     * 에 RPG 방어 계층을 적용한다.
     *
     * 낙하 / 용암 / 익사 / 굶주림 등은
     * 현재 대상에서 제외한다.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onCombatDamage(
            EntityDamageByEntityEvent event
    ) {

        /*
         * =====================================================
         * MAL_NYUN SWORD - 10 BLOCK AREA BASIC ATTACK
         * =====================================================
         *
         * 반드시 공격 버프 적용 전에 실행한다.
         *
         * 주변 대상에게 전달한 기본 피해가
         * 새로운 EntityDamageByEntityEvent를 발생시키고,
         * 그 이벤트에서 버서커 등의 기존 공격 보정이
         * 정상적으로 한 번 적용된다.
         */
        applyMalNyunSwordAreaAttack(
                event
        );


        /*
         * =====================================================
         * Lv70 - BERSERKER RAGE
         * ATTACK DAMAGE
         * =====================================================
         *
         * 공격 피해 +25%는 피해 대상이
         * 플레이어인지 몬스터인지와 관계없이
         * 가장 먼저 적용한다.
         */
        applyBerserkerOutgoingDamage(
                event
        );


        /*
         * 이하 방어 계층은
         * 피해 대상이 플레이어일 때만 적용한다.
         */
        if (!(
                event.getEntity()
                instanceof Player player
        )) {
            return;
        }


        /*
         * 버서커 레이지 활성 중
         * 받는 피해 +10%.
         */
        applyBerserkerIncomingDamage(
                player,
                event
        );


        /*
         * 1. RPG DEF + flat reduction
         */
        combatService.applyRpgDefense(
                player,
                event
        );


        /*
         * 2. Lv40 defensive skills
         *
         * RPG DEF 계산으로 조정된
         * 현재 final damage를 기준으로 처리한다.
         */
        skillDefenseBuffService
                .applyDefensiveSkills(
                        player,
                        event
                );
    }


    /*
     * =========================================================
     * BERSERKER RAGE - OUTGOING DAMAGE
     * =========================================================
     *
     * 버서커 레이지 활성 중:
     *
     * 플레이어가 가하는 피해 +25%.
     *
     * 피해 대상이 몬스터여도 적용된다.
     */
    private void applyBerserkerOutgoingDamage(
            EntityDamageByEntityEvent event
    ) {

        if (
                skillOffenseBuffService == null
                || event == null
                || event.isCancelled()
        ) {
            return;
        }


        Player attacker =
                resolveAttackingPlayer(
                        event.getDamager()
                );


        if (attacker == null) {
            return;
        }


        /*
         * 자기 자신에게 발생시킨 피해에는
         * 공격 증가를 적용하지 않는다.
         */
        if (event.getEntity() == attacker) {
            return;
        }


        double damage =
                Math.max(
                        0.0,
                        event.getDamage()
                );


        damage =
                skillOffenseBuffService
                        .applyWarriorAttackMultiplier(
                                attacker.getUniqueId(),
                                damage
                        );


        event.setDamage(
                Math.max(
                        0.0,
                        damage
                )
        );
    }


    /*
     * =========================================================
     * BERSERKER RAGE - INCOMING DAMAGE
     * =========================================================
     *
     * 버서커 레이지 활성 중:
     *
     * 받는 피해 +10%.
     *
     * 플레이어 자신이 피해 대상일 때만 적용한다.
     */
    private void applyBerserkerIncomingDamage(
            Player victim,
            EntityDamageByEntityEvent event
    ) {

        if (
                skillOffenseBuffService == null
                || victim == null
                || event == null
                || event.isCancelled()
        ) {
            return;
        }


        double damage =
                Math.max(
                        0.0,
                        event.getDamage()
                );


        damage =
                skillOffenseBuffService
                        .applyIncomingDamageMultiplier(
                                victim.getUniqueId(),
                                damage
                        );


        event.setDamage(
                Math.max(
                        0.0,
                        damage
                )
        );
    }


    /*
     * =========================================================
     * MAL_NYUN SWORD - AREA BASIC ATTACK
     * =========================================================
     *
     * 직접 근접 공격이 발생했을 때:
     *
     * - 플레이어 중심 반경 10블록
     * - 360도
     * - 대상 수 제한 없음
     * - 다른 플레이어 제외
     * - ArmorStand 제외
     * - 최초 직접 타격 대상 중복 제외
     * - 일반 몹 / 던전 몹 / 엘리트 / 보스 포함
     * - 벽 / 시야 제한 없음
     *
     * 공격력은 하드코딩하지 않는다.
     * 최초 공격 이벤트의 기본 피해를 그대로 사용한다.
     */
    private void applyMalNyunSwordAreaAttack(
            EntityDamageByEntityEvent event
    ) {

        if (
                event == null
                || event.isCancelled()
                || customItemIdKey == null
        ) {
            return;
        }


        /*
         * 화살 / 스킬 / 투사체가 아니라
         * 플레이어의 직접 근접 공격만 인정한다.
         */
        if (!(
                event.getDamager()
                        instanceof Player attacker
        )) {
            return;
        }


        if (!(
                event.getEntity()
                        instanceof LivingEntity primaryTarget
        )) {
            return;
        }


        ItemStack weapon =
                attacker.getInventory()
                        .getItemInMainHand();

        if (
                weapon == null
                || weapon.getType().isAir()
        ) {
            return;
        }


        ItemMeta meta =
                weapon.getItemMeta();

        if (meta == null) {
            return;
        }


        String customItemId =
                meta.getPersistentDataContainer()
                        .get(
                                customItemIdKey,
                                PersistentDataType.STRING
                        );

        if (
                !MAL_NYUN_SWORD_ID.equals(
                        customItemId
                )
                && !MAL_NYUN_IRON_SWORD_ID.equals(
                        customItemId
                )
        ) {
            return;
        }


        UUID attackerUuid =
                attacker.getUniqueId();


        /*
         * target.damage(...)가 다시
         * EntityDamageByEntityEvent를 발생시키므로
         * 재귀 진입을 차단한다.
         */
        if (!malNyunSwordAreaAttackers.add(
                attackerUuid
        )) {
            return;
        }


        try {

            double damage =
                    Math.max(
                            0.0D,
                            event.getDamage()
                    );

            if (damage <= 0.0D) {
                return;
            }


            for (
                    Entity nearby
                    : attacker.getNearbyEntities(
                            MAL_NYUN_SWORD_RANGE,
                            MAL_NYUN_SWORD_RANGE,
                            MAL_NYUN_SWORD_RANGE
                    )
            ) {

                if (!(
                        nearby
                                instanceof LivingEntity target
                )) {
                    continue;
                }


                /*
                 * 최초 직접 타격 대상은
                 * 원래 이벤트가 처리하므로 제외.
                 */
                if (target == primaryTarget) {
                    continue;
                }


                /*
                 * PvP 광역 학살 방지.
                 * 관리자 자신도 LivingEntity이지만
                 * getNearbyEntities에는 보통 자신이 포함되지 않는다.
                 */
                if (
                        target == attacker
                        || target instanceof Player
                        || target instanceof ArmorStand
                ) {
                    continue;
                }


                if (
                        !target.isValid()
                        || target.isDead()
                ) {
                    continue;
                }


                /*
                 * getNearbyEntities는 직육면체 검색이므로
                 * 실제 반경 10블록의 구형 거리로 다시 제한한다.
                 */
                if (
                        target.getLocation()
                                .distanceSquared(
                                        attacker.getLocation()
                                )
                        > MAL_NYUN_SWORD_RANGE_SQUARED
                ) {
                    continue;
                }


                /*
                 * 새 피해 이벤트를 정상 발생시킨다.
                 *
                 * 따라서:
                 *
                 * - 던전 몹 처리
                 * - 보스 처리
                 * - 엘리트 처리
                 * - 기존 전투 리스너
                 *
                 * 를 우회하지 않는다.
                 */
                target.damage(
                        damage,
                        attacker
                );
            }

        } finally {

            malNyunSwordAreaAttackers.remove(
                    attackerUuid
            );
        }
    }


    /*
     * =========================================================
     * ATTACKER RESOLUTION
     * =========================================================
     */
    private Player resolveAttackingPlayer(
            Entity damager
    ) {

        if (damager instanceof Player player) {

            return player;
        }


        if (
                damager
                        instanceof Projectile projectile
        ) {

            Object shooter =
                    projectile.getShooter();


            if (shooter instanceof Player player) {

                return player;
            }
        }


        return null;
    }
}
