package com.hcs.rpgcore.skill;

import org.bukkit.entity.Player;


public final class SkillAccessService {

    /*
     * 과거 전 직업 스킬 테스트 계정 호환 메서드.
     *
     * 전 직업 사용 예외 기능은 제거되었으므로
     * 모든 플레이어에 대해 false를 반환한다.
     */
    public boolean isAllClassSkillPlayer(
            Player player
    ) {

        return false;
    }


    public boolean canUseClass(
            Player player,
            String actualClass,
            String requiredClass
    ) {

        if (
                player == null
                || actualClass == null
                || requiredClass == null
        ) {
            return false;
        }


        return requiredClass.equalsIgnoreCase(
                actualClass
        );
    }
}
