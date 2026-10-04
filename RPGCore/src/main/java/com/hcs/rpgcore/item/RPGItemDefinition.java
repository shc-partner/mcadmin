package com.hcs.rpgcore.item;


/*
 * =========================================================
 * RPG ITEM DEFINITION
 * =========================================================
 *
 * MariaDB rpg_items의 한 행.
 *
 * MariaDB 구조화 컬럼을 직접 표현한다.
 *
 * 강화 단계 자체는 여기 저장하지 않는다.
 * 개별 ItemStack PDC에 저장한다.
 */
public record RPGItemDefinition(

        String itemId,

        String displayName,

        String rarity,

        String description,

        String description2,

        String description3,

        String displayType,

        String specialEffect1,

        String specialEffect2,

        String material,

        String itemModel,

        String equipmentModel,

        String equipmentSlot,

        boolean unbreakable,

        Integer maxDurability,

        Double weaponAttack,

        Double weaponAttackSpeed,

        Double armorBonus,

        Double displayArmor,

        Double toughnessBonus,

        Double displayToughness,

        Double displayKnockbackResistance,

        Double damageReductionFlat,

        Double maxMana,

        boolean glide,

        boolean consumableEnabled,

        Double consumableSeconds,

        Double restoreHpPercent,

        Double restoreMpPercent,

        boolean bound,

        boolean enabled,

        boolean enhanceable,

        String enhancementType,

        int maxEnhancementLevel

) {
}
