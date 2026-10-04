package com.hcs.rpgcore.dungeon;

public enum RedDragonDungeonState {

    IDLE,

    /*
     * 성채 내부 이동 중.
     */
    WAITING_FOR_BATTLE_TRIGGER,

    /*
     * 드래곤 영역 이동 3초 카운트다운.
     */
    BATTLE_COUNTDOWN,

    /*
     * 전투장 이동 후 보스 등장 연출.
     */
    BOSS_INTRO,

    BOSS_FIGHT,

    CLEARED,

    RESETTING
}
