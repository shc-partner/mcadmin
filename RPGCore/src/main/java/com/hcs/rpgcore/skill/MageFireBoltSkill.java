package com.hcs.rpgcore.skill;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.mana.ManaService;

import org.bukkit.NamespacedKey;
import org.bukkit.Sound;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;

import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import org.bukkit.util.Vector;


public final class MageFireBoltSkill {

    public static final double MANA_COST =
            4.0;

    public static final long COOLDOWN_MILLIS =
            4000L;

    public static final double BASE_DAMAGE =
            10.0;

    public static final double ATTACK_RATIO =
            0.30;

    public static final double MAX_MANA_RATIO =
            0.03;

    public static final double EXPLOSION_RANGE =
            3.0;


    private final ManaService manaService;

    private final NamespacedKey fireBoltKey;
    private final NamespacedKey fireBoltDamageKey;


    public MageFireBoltSkill(
            RPGCorePlugin plugin,
            ManaService manaService
    ) {

        this.manaService =
                manaService;


        this.fireBoltKey =
                new NamespacedKey(
                        plugin,
                        "fire_bolt"
                );


        this.fireBoltDamageKey =
                new NamespacedKey(
                        plugin,
                        "fire_bolt_damage"
                );
    }


    public boolean cast(
            Player player
    ) {

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


        double damage =
                BASE_DAMAGE
                        + attack
                        * ATTACK_RATIO
                        + maximumMana
                        * MAX_MANA_RATIO;


        LargeFireball fireball =
                player.launchProjectile(
                        LargeFireball.class
                );


        Vector direction =
                player.getEyeLocation()
                        .getDirection()
                        .normalize();


        fireball.setDirection(
                direction
        );


        fireball.setVelocity(
                direction.clone()
                        .multiply(
                                1.35
                        )
        );


        /*
         * 바닐라 폭발은 RPG 스킬 피해와 분리한다.
         *
         * 지형 파괴 및 화재는 별도 리스너에서도
         * 차단한다.
         */
        fireball.setYield(
                0.0f
        );


        fireball.setIsIncendiary(
                false
        );


        PersistentDataContainer pdc =
                fireball.getPersistentDataContainer();


        pdc.set(
                fireBoltKey,
                PersistentDataType.BYTE,
                (byte) 1
        );


        pdc.set(
                fireBoltDamageKey,
                PersistentDataType.DOUBLE,
                damage
        );


        player.getWorld()
                .playSound(
                        player.getEyeLocation(),
                        Sound.ENTITY_GHAST_SHOOT,
                        0.8f,
                        1.25f
                );


        return true;
    }


    public boolean isFireBolt(
            LargeFireball fireball
    ) {

        Byte value =
                fireball.getPersistentDataContainer()
                        .get(
                                fireBoltKey,
                                PersistentDataType.BYTE
                        );


        return value != null
                && value == (byte) 1;
    }


    public double getStoredDamage(
            LargeFireball fireball
    ) {

        Double damage =
                fireball.getPersistentDataContainer()
                        .get(
                                fireBoltDamageKey,
                                PersistentDataType.DOUBLE
                        );


        if (damage == null) {
            return 0.0;
        }


        return Math.max(
                0.0,
                damage
        );
    }
}
