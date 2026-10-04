package com.hcs.rpgcore.skill;


public record SkillDefinition(
        String skillId,
        String playerClass,
        String displayName,
        int requiredLevel
) {

    public boolean isUnlocked(
            int playerLevel
    ) {

        return playerLevel
                >= requiredLevel;
    }


    public boolean isClass(
            String playerClass
    ) {

        return playerClass != null
                && this.playerClass
                        .equalsIgnoreCase(
                                playerClass
                        );
    }
}
