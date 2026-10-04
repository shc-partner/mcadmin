package com.hcs.rpgcore.level;

import java.util.EnumMap;
import java.util.Map;

import org.bukkit.entity.EntityType;

public final class DungeonMobExperienceService {

    private final Map<EntityType, Long> experienceTable =
            new EnumMap<>(EntityType.class);

    public DungeonMobExperienceService() {
        registerDefaults();
    }

    private void registerDefaults() {

        experienceTable.put(EntityType.ZOMBIE, 25L);
        experienceTable.put(EntityType.SKELETON, 30L);
        experienceTable.put(EntityType.CREEPER, 35L);

        experienceTable.put(EntityType.DROWNED, 30L);
        experienceTable.put(EntityType.HUSK, 30L);
        experienceTable.put(EntityType.STRAY, 35L);
        experienceTable.put(EntityType.CAVE_SPIDER, 35L);

        experienceTable.put(EntityType.ZOMBIFIED_PIGLIN, 35L);
        experienceTable.put(EntityType.PIGLIN, 40L);
        experienceTable.put(EntityType.PIGLIN_BRUTE, 100L);
        experienceTable.put(EntityType.BLAZE, 70L);
        experienceTable.put(EntityType.WITHER_SKELETON, 90L);

        experienceTable.put(EntityType.ENDERMAN, 60L);
        experienceTable.put(EntityType.SHULKER, 100L);

        experienceTable.put(EntityType.PILLAGER, 40L);
        experienceTable.put(EntityType.VINDICATOR, 60L);
        experienceTable.put(EntityType.EVOKER, 100L);
        experienceTable.put(EntityType.RAVAGER, 150L);

        experienceTable.put(EntityType.WARDEN, 1000L);

        experienceTable.put(EntityType.WITHER, 4000L);
        experienceTable.put(EntityType.ENDER_DRAGON, 5000L);
    }

    public long getExperience(
            EntityType entityType
    ) {

        return experienceTable.getOrDefault(
                entityType,
                0L
        );
    }
}
