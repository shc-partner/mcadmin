package com.hcs.rpgcore.player;

import java.util.UUID;

public final class PlayerData {

    private final UUID uuid;
    private final String playerName;

    private int level;
    private long experience;
    private String playerClass;

    public PlayerData(
            UUID uuid,
            String playerName,
            int level,
            long experience,
            String playerClass
    ) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.level = level;
        this.experience = experience;
        this.playerClass = playerClass;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getExperience() {
        return experience;
    }

    public void setExperience(long experience) {
        this.experience = experience;
    }

    public String getPlayerClass() {
        return playerClass;
    }

    public void setPlayerClass(String playerClass) {
        this.playerClass = playerClass;
    }
}
