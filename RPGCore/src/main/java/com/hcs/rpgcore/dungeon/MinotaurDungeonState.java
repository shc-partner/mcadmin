package com.hcs.rpgcore.dungeon;

public enum MinotaurDungeonState {

    IDLE,

    ENTRY_INTRO,

    WAITING_FOR_WAVE_1_TRIGGER,

    WAVE_1_INTRO,
    WAVE_1,

    WAVE_2_INTRO,
    WAVE_2,

    WAVE_3_INTRO,
    WAVE_3,

    WAVES_CLEARED_INTRO,

    WAITING_FOR_BOSS_TRIGGER,

    BOSS_INTRO,
    BOSS_FIGHT,

    CLEARED,
    RESETTING
}
