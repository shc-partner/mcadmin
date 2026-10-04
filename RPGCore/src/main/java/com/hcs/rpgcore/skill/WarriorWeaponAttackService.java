package com.hcs.rpgcore.skill;

import com.hcs.rpgcore.hud.HudSnapshot;
import com.hcs.rpgcore.stat.EquipmentStatReader;
import com.hcs.rpgcore.stat.PlayerStats;
import com.hcs.rpgcore.stat.StatService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import org.bukkit.inventory.ItemStack;


public final class WarriorWeaponAttackService {

    private final EquipmentStatReader equipmentStatReader;
    private final StatService statService;
    private final SkillItemFactory skillItemFactory;


    /*
     * 마지막으로 실제 주손에 장착했던
     * 공격 가능한 아이템의 공격력만 저장한다.
     *
     * 캐릭터 기본 공격력이나 방어구 공격력은
     * 저장하지 않고 배쉬 사용 순간 다시 계산한다.
     */
    private final Map<UUID, Double> weaponAttackMap =
            new ConcurrentHashMap<>();


    public WarriorWeaponAttackService(
            EquipmentStatReader equipmentStatReader,
            StatService statService,
            SkillItemFactory skillItemFactory
    ) {

        this.equipmentStatReader =
                equipmentStatReader;

        this.statService =
                statService;

        this.skillItemFactory =
                skillItemFactory;
    }


    /*
     * =========================================================
     * CAPTURE MAIN WEAPON
     * =========================================================
     */
    public void capture(
            Player player
    ) {

        if (player == null) {
            return;
        }


        ItemStack mainHand =
                player.getInventory()
                        .getItemInMainHand();


        /*
         * 스킬북을 무기로 기억하면 안 된다.
         */
        if (
                skillItemFactory.isSkillItem(
                        mainHand
                )
        ) {
            return;
        }


        double weaponAttack =
                equipmentStatReader
                        .readMainHandAttack(
                                mainHand
                        );


        /*
         * 공격력이 없는 음식 / 블록 / 빈손 등은
         * 마지막 주 무기 기록을 덮어쓰지 않는다.
         */
        if (weaponAttack <= 0.0) {
            return;
        }


        weaponAttackMap.put(
                player.getUniqueId(),
                weaponAttack
        );
    }


    /*
     * =========================================================
     * BASH TOTAL ATTACK
     * =========================================================
     */
    public double resolveBashAttack(
            Player player,
            HudSnapshot snapshot
    ) {

        if (
                player == null
                || snapshot == null
        ) {
            return 0.0;
        }


        /*
         * 현재 상태의 최종 공격력.
         *
         * 배쉬 사용 시에는 주손이 스킬북이므로
         * 일반적으로 currentMainHandAttack = 0.
         */
        PlayerStats currentStats =
                statService.calculate(
                        snapshot.level(),
                        snapshot.playerClass(),
                        player
                );


        ItemStack currentMainHand =
                player.getInventory()
                        .getItemInMainHand();


        double currentMainHandAttack =
                equipmentStatReader
                        .readMainHandAttack(
                                currentMainHand
                        );


        Double rememberedWeaponAttack =
                weaponAttackMap.get(
                        player.getUniqueId()
                );


        /*
         * 아직 기록된 무기가 없고
         * 현재 손에 실제 공격 가능한 아이템이 있다면
         * 그것을 즉시 기준으로 사용한다.
         */
        if (
                rememberedWeaponAttack == null
                && !skillItemFactory.isSkillItem(
                        currentMainHand
                )
                && currentMainHandAttack > 0.0
        ) {

            rememberedWeaponAttack =
                    currentMainHandAttack;


            weaponAttackMap.put(
                    player.getUniqueId(),
                    rememberedWeaponAttack
            );
        }


        /*
         * 서버 접속 직후부터 스킬북만 들고 있었던 등
         * 아직 한 번도 무기가 등록되지 않았다면
         * 무기 보너스 없이 현재 공격력을 사용한다.
         */
        if (rememberedWeaponAttack == null) {

            return Math.max(
                    0.0,
                    currentStats.attack()
            );
        }


        /*
         * 현재 총 공격력에서 현재 주손 공격력을 제거하고
         * 마지막 주 무기 공격력을 대신 넣는다.
         *
         * 따라서:
         *
         * - 레벨 기본 공격력
         * - 전사 성장 공격력
         * - 방어구 PDC attack
         * - 보조손 PDC attack
         *
         * 은 모두 현재 상태를 실시간 반영한다.
         */
        double resolvedAttack =
                currentStats.attack()
                        - currentMainHandAttack
                        + rememberedWeaponAttack;


        return Math.max(
                0.0,
                resolvedAttack
        );
    }


    public void remove(
            UUID uuid
    ) {

        weaponAttackMap.remove(
                uuid
        );
    }


    public void clear() {

        weaponAttackMap.clear();
    }
}
