package com.hcs.rpgcore.hud;

import java.util.UUID;

public record HudSnapshot(
        UUID uuid,
        int level,
        long experience,
        String playerClass,
        double maxHealth,
        double attack,
        double defense,
        double maxMana,
        double damageReductionFlat
) {
}
