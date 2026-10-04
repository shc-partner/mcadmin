package com.hcs.rpgcore.stat;

public record PlayerStats(
        double maxHealth,
        double attack,
        double defense,
        double maxMana,
        double damageReductionFlat
) {
}
