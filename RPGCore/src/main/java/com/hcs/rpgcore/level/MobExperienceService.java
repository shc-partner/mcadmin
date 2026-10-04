package com.hcs.rpgcore.level;

import java.util.EnumMap;
import java.util.Map;

import org.bukkit.entity.EntityType;

public final class MobExperienceService {

    private final Map<EntityType, Long> experienceTable =
            new EnumMap<>(EntityType.class);

    public MobExperienceService() {
        registerDefaults();
    }

    private void registerDefaults() {

        // 일반 몬스터
        experienceTable.put(EntityType.ZOMBIE, 5L);
        experienceTable.put(EntityType.SKELETON, 5L);
        experienceTable.put(EntityType.SPIDER, 5L);
        experienceTable.put(EntityType.CREEPER, 5L);

        experienceTable.put(EntityType.DROWNED, 5L);
        experienceTable.put(EntityType.HUSK, 5L);
        experienceTable.put(EntityType.STRAY, 5L);
        experienceTable.put(EntityType.CAVE_SPIDER, 5L);

        // 네더
        experienceTable.put(EntityType.ZOMBIFIED_PIGLIN, 5L);
        experienceTable.put(EntityType.PIGLIN, 5L);
        experienceTable.put(EntityType.PIGLIN_BRUTE, 20L);
        experienceTable.put(EntityType.BLAZE, 10L);
        experienceTable.put(EntityType.WITHER_SKELETON, 5L);

        // 엔드
        experienceTable.put(EntityType.ENDERMAN, 5L);
        experienceTable.put(EntityType.SHULKER, 5L);

        // 약탈자
        experienceTable.put(EntityType.PILLAGER, 5L);
        experienceTable.put(EntityType.VINDICATOR, 5L);
        experienceTable.put(EntityType.EVOKER, 10L);
        experienceTable.put(EntityType.RAVAGER, 20L);

        // 강력한 몬스터
        experienceTable.put(EntityType.WARDEN, 5L);

        // 보스
        experienceTable.put(EntityType.WITHER, 50L);
        experienceTable.put(EntityType.ENDER_DRAGON, 12000L);
    }

    public long getExperience(EntityType entityType) {
        return experienceTable.getOrDefault(
                entityType,
                0L
        );
    }
}
