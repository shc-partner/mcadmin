package com.hcs.rpgcore.title;

import java.util.Arrays;

/**
 * RPGCore 아이템 등급과 동일한 칭호 등급 및 색상.
 *
 * 색상 기준:
 * CustomItemFactory.getRarityColor()
 */
public enum TitleRarity {

    ADVANCED("고급", "#55FF55"),
    RARE("희귀", "#55FFFF"),
    HEROIC("영웅", "#FF55FF"),
    LEGENDARY("전설", "#FFAA00"),
    MYTHIC("신화", "#FDE879");

    private final String displayName;
    private final String hexColor;

    TitleRarity(
            String displayName,
            String hexColor
    ) {
        this.displayName = displayName;
        this.hexColor = hexColor;
    }

    public String displayName() {
        return displayName;
    }

    public String hexColor() {
        return hexColor;
    }

    /**
     * DB rarity 또는 관리자 명령어 입력값을 등급으로 변환.
     */
    public static TitleRarity fromDisplayName(
            String value
    ) {
        if (value == null) {
            return null;
        }

        return Arrays.stream(values())
                .filter(rarity ->
                        rarity.displayName.equals(value.trim())
                )
                .findFirst()
                .orElse(null);
    }

    /**
     * 유효한 등급만 허용한다.
     */
    public static TitleRarity require(
            String value
    ) {
        TitleRarity rarity = fromDisplayName(value);

        if (rarity == null) {
            throw new IllegalArgumentException(
                    "올바르지 않은 칭호 등급: " + value
            );
        }

        return rarity;
    }
}
