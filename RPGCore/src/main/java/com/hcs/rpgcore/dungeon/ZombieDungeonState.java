package com.hcs.rpgcore.dungeon;

public enum ZombieDungeonState {

    IDLE,

    WAVE_1,

    /*
     * 현재 메인 진행에서는 사용하지 않는 잔여 상태값.
     * 호환성 유지를 위해 enum 값은 그대로 둔다.
     */
    WAVE_1_CLEARED,

    WAVE_2,
    WAVE_3,
    BOSS,
    CLEARED,
    RESETTING
}
