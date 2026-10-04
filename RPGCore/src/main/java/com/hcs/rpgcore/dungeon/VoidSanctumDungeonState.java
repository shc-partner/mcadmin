package com.hcs.rpgcore.dungeon;


/*
 * =========================================================
 * VOID SANCTUM DUNGEON STATE
 * =========================================================
 *
 * Lv.70~80 공허의 성전.
 */
public enum VoidSanctumDungeonState {

    IDLE,

    /*
     * 외부 입구 통과 후 내부 진입 완료.
     * 보스 전투 시작 직전 상태.
     */
    BOSS_INTRO,

    /*
     * ncr_Faceless 1페이즈.
     */
    PHASE_1,

    /*
     * ncr_Faceless_phase2 2페이즈.
     */
    PHASE_2,

    CLEARED,

    RESETTING
}
