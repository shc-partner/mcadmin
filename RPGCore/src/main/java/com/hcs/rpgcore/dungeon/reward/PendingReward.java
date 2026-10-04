package com.hcs.rpgcore.dungeon.reward;

public record PendingReward(
        long id,
        String rewardType,
        int amount
) {
}
