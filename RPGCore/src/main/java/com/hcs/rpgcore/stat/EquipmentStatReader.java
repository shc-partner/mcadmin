package com.hcs.rpgcore.stat;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

import org.bukkit.entity.Player;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.PlayerInventory;

import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;


public final class EquipmentStatReader {

    private final ItemStatKeys keys;


    public EquipmentStatReader(
            ItemStatKeys keys
    ) {

        this.keys =
                keys;
    }


    /*
     * =========================================================
     * PLAYER EQUIPMENT
     * =========================================================
     *
     * 기본 원칙:
     *
     * 공격력
     * = 주손 아이템의 바닐라 ATTACK_DAMAGE modifier
     * + 모든 장비의 PDC attack
     *
     * RPG 방어력
     * = 모든 장비의 PDC defense
     *
     * 바닐라 ARMOR / ARMOR_TOUGHNESS는 HUD 방어력에
     * 합산하지 않는다.
     *
     * 인챈트 효과 역시 여기에서 계산하지 않는다.
     */
    public EquipmentStats read(
            Player player
    ) {

        if (player == null) {
            return EquipmentStats.empty();
        }


        PlayerInventory inventory =
                player.getInventory();


        double attack =
                0.0;

        double defense =
                0.0;

        double maxMana =
                0.0;

        double damageReductionFlat =
                0.0;


        /*
         * =====================================================
         * MAIN HAND
         * =====================================================
         *
         * 바닐라 무기의 ATTACK_DAMAGE는
         * 주손 장비에서만 기본 공격력으로 합산한다.
         */
        ItemStack mainHand =
                inventory.getItemInMainHand();


        attack +=
                readVanillaAttack(
                        mainHand,
                        EquipmentSlot.HAND
                );


        EquipmentStats mainHandPdc =
                readPdcStats(
                        mainHand
                );


        attack +=
                mainHandPdc.attack();


        defense +=
                mainHandPdc.defense();

        /*
         * 최대 MP 장비 옵션은 주손에서만 적용한다.
         *
         * 지팡이를 보조손에 두거나
         * 다른 슬롯으로 이동해도 최대 MP가 증가하지 않는다.
         */
        maxMana +=
                mainHandPdc.maxMana();

        damageReductionFlat +=
                mainHandPdc.damageReductionFlat();


        /*
         * =====================================================
         * OFF HAND
         * =====================================================
         *
         * 보조손 아이템의 바닐라 ATTACK_DAMAGE는
         * 공격력에 자동 합산하지 않는다.
         *
         * PDC로 의도적으로 부여한 추가 능력치는 허용.
         */
        EquipmentStats offHandPdc =
                readPdcStats(
                        inventory.getItemInOffHand()
                );


        attack +=
                offHandPdc.attack();

        defense +=
                offHandPdc.defense();

        damageReductionFlat +=
                offHandPdc.damageReductionFlat();


        /*
         * =====================================================
         * ARMOR
         * =====================================================
         */
        EquipmentStats helmet =
                readArmorSlot(
                        inventory.getHelmet(),
                        EquipmentSlot.HEAD
                );


        EquipmentStats chestplate =
                readArmorSlot(
                        inventory.getChestplate(),
                        EquipmentSlot.CHEST
                );


        EquipmentStats leggings =
                readArmorSlot(
                        inventory.getLeggings(),
                        EquipmentSlot.LEGS
                );


        EquipmentStats boots =
                readArmorSlot(
                        inventory.getBoots(),
                        EquipmentSlot.FEET
                );


        attack +=
                helmet.attack()
                        + chestplate.attack()
                        + leggings.attack()
                        + boots.attack();


        defense +=
                helmet.defense()
                        + chestplate.defense()
                        + leggings.defense()
                        + boots.defense();

        damageReductionFlat +=
                helmet.damageReductionFlat()
                        + chestplate.damageReductionFlat()
                        + leggings.damageReductionFlat()
                        + boots.damageReductionFlat();


        return new EquipmentStats(
                attack,
                defense,
                maxMana,
                damageReductionFlat
        );
    }


    /*
     * =========================================================
     * ARMOR SLOT
     * =========================================================
     */
    private EquipmentStats readArmorSlot(
            ItemStack item,
            EquipmentSlot slot
    ) {

        /*
         * 바닐라 ARMOR는 RPGCore HUD 방어력에 합산하지 않는다.
         *
         * 이 슬롯에서는 RPGCore PDC 능력치만 읽는다.
         *
         * Minecraft의 ARMOR / ARMOR_TOUGHNESS /
         * 보호 인챈트는 실제 전투에서 바닐라 규칙으로 처리된다.
         */
        return readPdcStats(
                item
        );
    }


    /*
     * =========================================================
     * VANILLA ATTACK
     * =========================================================
     */
    private double readVanillaAttack(
            ItemStack item,
            EquipmentSlot slot
    ) {

        if (!isValidItem(item)) {
            return 0.0;
        }

        ItemType itemType =
                item.getType()
                        .asItemType();

        if (itemType == null) {
            return 0.0;
        }

        double total =
                0.0;

        boolean hasAttackDamageModifier =
                false;

        for (
                AttributeModifier modifier
                : itemType
                        .getDefaultAttributeModifiers(
                                slot
                        )
                        .get(
                                Attribute.ATTACK_DAMAGE
                        )
        ) {

            if (
                    modifier.getOperation()
                            == AttributeModifier.Operation.ADD_NUMBER
            ) {

                total +=
                        modifier.getAmount();

                hasAttackDamageModifier =
                        true;
            }
        }

        /*
         * 바닐라 무기의 표시 공격력:
         *
         * 플레이어 기본 공격 피해 1
         * + ATTACK_DAMAGE modifier
         *
         * RPGCore에서는 레벨 기본 공격력과 별도로
         * 무기의 완전한 바닐라 표시 공격력을 장비 공격력으로 사용한다.
         *
         * 예:
         * STONE_SWORD modifier +4
         * + Minecraft 기본 공격 1
         * = 무기 공격력 5
         *
         * Lv.1 RPG 기본 공격력 1
         * + 무기 공격력 5
         * = HUD / RPG 공격력 6
         */
        if (hasAttackDamageModifier) {
            total +=
                    1.0;
        }

        return total;
    }


    /*
     * =========================================================
     * VANILLA DEFENSE
     * =========================================================
     */
    private double readVanillaDefense(
            ItemStack item,
            EquipmentSlot slot
    ) {

        if (!isValidItem(item)) {
            return 0.0;
        }


        ItemType itemType =
                item.getType()
                        .asItemType();


        if (itemType == null) {
            return 0.0;
        }


        double total =
                0.0;


        for (
                AttributeModifier modifier
                : itemType
                        .getDefaultAttributeModifiers(
                                slot
                        )
                        .get(
                                Attribute.ARMOR
                        )
        ) {

            if (
                    modifier.getOperation()
                            == AttributeModifier.Operation.ADD_NUMBER
            ) {

                total +=
                        modifier.getAmount();
            }
        }


        return total;
    }


    /*
     * =========================================================
     * PDC
     * =========================================================
     */

    /*
     * =========================================================
     * MAIN HAND ATTACK
     * =========================================================
     *
     * 주손 아이템 하나가 제공하는 RPG 공격력.
     *
     * vanilla ATTACK_DAMAGE
     * + RPGCore PDC attack
     *
     * 배쉬의 마지막 주 무기 공격력을 기억할 때 사용한다.
     */
    public double readMainHandAttack(
            ItemStack item
    ) {

        double attack =
                readVanillaAttack(
                        item,
                        EquipmentSlot.HAND
                );


        EquipmentStats pdcStats =
                readPdcStats(
                        item
                );


        attack +=
                pdcStats.attack();


        return Math.max(
                0.0,
                attack
        );
    }


    public EquipmentStats readItem(
            ItemStack item
    ) {

        return readPdcStats(
                item
        );
    }


    private EquipmentStats readPdcStats(
            ItemStack item
    ) {

        if (!isValidItem(item)) {
            return EquipmentStats.empty();
        }


        ItemMeta meta =
                item.getItemMeta();


        if (meta == null) {
            return EquipmentStats.empty();
        }


        PersistentDataContainer container =
                meta.getPersistentDataContainer();


        Double attack =
                container.get(
                        keys.attackKey(),
                        PersistentDataType.DOUBLE
                );


        Double defense =
                container.get(
                        keys.defenseKey(),
                        PersistentDataType.DOUBLE
                );

        Double maxMana =
                container.get(
                        keys.maxManaKey(),
                        PersistentDataType.DOUBLE
                );

        Double damageReductionFlat =
                container.get(
                        keys.damageReductionFlatKey(),
                        PersistentDataType.DOUBLE
                );


        return new EquipmentStats(
                attack == null
                        ? 0.0
                        : attack,

                defense == null
                        ? 0.0
                        : defense,

                maxMana == null
                        ? 0.0
                        : maxMana,

                damageReductionFlat == null
                        ? 0.0
                        : damageReductionFlat
        );
    }


    /*
     * =========================================================
     * UTIL
     * =========================================================
     */
    private boolean isValidItem(
            ItemStack item
    ) {

        return item != null
                && !item.getType().isAir();
    }
}
