package com.hcs.rpgcore.level;

public record LevelUpResult(
        int oldLevel,
        int newLevel,
        long remainingExperience,
        boolean leveledUp
) {
}
