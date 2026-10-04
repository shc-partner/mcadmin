package com.hcs.rpgcore.hud;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.level.LevelService;
import com.hcs.rpgcore.mana.ManaService;
import com.hcs.rpgcore.player.PlayerData;
import com.hcs.rpgcore.stat.PlayerStats;
import com.hcs.rpgcore.stat.StatService;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class HudService {

    /*
     * =========================================================
     * SERVICES
     * =========================================================
     */

    private final LevelService levelService;
    private final StatService statService;
    private final ManaService manaService;


    /*
     * =========================================================
     * CACHE
     * =========================================================
     */

    private final Map<UUID, HudSnapshot> snapshotMap =
            new ConcurrentHashMap<>();

    private final Map<UUID, TemporaryMessage> temporaryMessageMap =
            new ConcurrentHashMap<>();

    private final java.util.Set<UUID> dirtyPlayerSet =
            ConcurrentHashMap.newKeySet();

    private final Map<UUID, Component> lastDisplayMap =
            new ConcurrentHashMap<>();


    /*
     * =========================================================
     * HUD POSITION
     * =========================================================
     */

    private final NamespacedKey hudPositionKey;

    /*
     * 추가적인 왼쪽 이동 없음.
     *
     * /hud position reset
     * →
     * 0으로 복귀.
     */
    private static final int DEFAULT_HUD_POSITION =
            0;

    private static final int MIN_HUD_POSITION =
            0;

    private static final int MAX_HUD_POSITION =
            3000;


    /*
     * =========================================================
     * HUD2 FONT
     * =========================================================
     */

    private static final Key HUD_FONT =
            Key.key(
                    "rpgcore",
                    "hud2"
            );


    /*
     * =========================================================
     * HUD3 GAUGE FONT
     * =========================================================
     */

    private static final Key HUD3_BAR_FONT =
            Key.key(
                    "rpgcore",
                    "hud3_bars"
            );


    /*
     * HP / MP / SP 숫자 전용 소형 font.
     */
    private static final Key HUD3_NUMBER_FONT =
            Key.key(
                    "rpgcore",
                    "hud3_numbers"
            );


    /*
     * =========================================================
     * HUD3 LOCATOR BAR
     * =========================================================
     */
    private static final Key HUD3_LOCATOR_FONT =
            Key.key(
                    "rpgcore",
                    "hud3_locator"
            );

    private static final String HUD3_LOCATOR_BAR =
            "\uF300";

    private static final String HUD3_LOCATOR_PLAYER =
            "\uF600";

    /*
     * bitmap font:
     *
     * 185px texture + 1px advance
     */
    private static final int HUD3_LOCATOR_ADVANCE =
            186;

    private static final int HUD3_LOCATOR_MARKER_ADVANCE =
            8;

    private static final int HUD3_LOCATOR_TRACK_WIDTH =
            178;



    /*
     * 21단계 gauge:
     *
     * 00 ~ 20
     */

    private static final int HUD3_HP_START =
            0xEC00;

    private static final int HUD3_MP_START =
            0xED00;

    private static final int HUD3_SP_START =
            0xEE00;

    private static final int HUD3_O2_START =
            0xF200;

    private static final int HUD3_O2_STEPS =
            20;

    private static final int HUD3_O2_WIDTH =
            250;

/*
 * 화면 중앙 기준에서 오른쪽 이동량.
 * 처음엔 110 정도로 두고 필요하면 미세조정.
 */
private static final int HUD3_O2_OFFSET_X =
        110;


    /*
     * EXP는 725px 단일 bitmap 대신
     * 3개 bitmap glyph로 분할한다.
     *
     * LEFT  : 242px
     * MID   : 241px
     * RIGHT : 242px
     *
     * total : 725px
     */

    private static final int HUD3_EXP_START =
            0xEF00;

    private static final int HUD3_EXP_MID_START =
            0xF000;

    private static final int HUD3_EXP_RIGHT_START =
            0xF100;


    private static final int HUD3_BAR_STEPS =
            20;


    /*
     * HP / MP / SP
     *
     * visual:
     *
     * 230 + 18 + 230 + 18 + 230
     * = 726px
     *
     * Minecraft bitmap glyph의 advance 보정을 고려해
     * 첫 테스트용 spacing은 17.
     */

    private static final int HUD3_TOP_GAP_SPACE =
            4;


    /*
     * 상단 3개 gauge 출력 후
     * EXP 시작 위치로 되돌리는 값.
     *
     * 첫 인게임 테스트 후
     * 필요하면 1~2px 단위 보정한다.
     */

    private static final int HUD3_EXP_REWIND =
            -185;



    /*
     * =========================================================
     * PANEL
     * =========================================================
     *
     * panel.png:
     *
     * 840 x 92
     */

    private static final String PANEL_GLYPH =
            "\uE700";

    /*
     * Bitmap glyph는 실제 폭 뒤에
     * 약 1px advance가 붙는 구조로 계산.
     */
    private static final int PANEL_ADVANCE =
            421;


    /*
     * 패널 내부 왼쪽 여백.
     */
    private static final int CONTENT_LEFT =
            12;




    /*
     * 16px bitmap + 1
     */
    private static final int ICON_ADVANCE =
            9;


    /*
     * =========================================================
     * TEXT
     * =========================================================
     *
     * HUD2 atlas:
     *
     * 10px 실제 문자
     * + 2px cell 여백
     *
     * Minecraft bitmap advance 보정까지 고려하여
     * 우선 13px로 계산.
     *
     * 실제 렌더링 후 1px 오차가 있으면
     * 이 값만 12/13으로 미세조정하면 된다.
     */

    private static final int HUD_TEXT_ADVANCE =
            7;


    /*
     * =========================================================
     * SPACE FONT
     * =========================================================
     */

    private static final int[] NEGATIVE_SPACE_CODEPOINTS = {
            0xE507,
            0xE506,
            0xE505,
            0xE504,
            0xE503,
            0xE502,
            0xE501,
            0xE500
    };

    private static final int[] NEGATIVE_SPACE_VALUES = {
            128,
            64,
            32,
            16,
            8,
            4,
            2,
            1
    };


    private static final int[] POSITIVE_SPACE_CODEPOINTS = {
            0xE527,
            0xE526,
            0xE525,
            0xE524,
            0xE523,
            0xE522,
            0xE521,
            0xE520
    };

    private static final int[] POSITIVE_SPACE_VALUES = {
            128,
            64,
            32,
            16,
            8,
            4,
            2,
            1
    };


    /*
     * =========================================================
     * CONSTRUCTOR
     * =========================================================
     */

    public HudService(
            RPGCorePlugin plugin,
            LevelService levelService,
            StatService statService,
            ManaService manaService
    ) {

        this.levelService =
                levelService;

        this.statService =
                statService;

        this.manaService =
                manaService;

        this.hudPositionKey =
                new NamespacedKey(
                        plugin,
                        "hud_position"
                );
    }


    /*
     * =========================================================
     * HUD POSITION
     * =========================================================
     */

    public int getHudPosition(
            Player player
    ) {

        PersistentDataContainer container =
                player.getPersistentDataContainer();

        Integer value =
                container.get(
                        hudPositionKey,
                        PersistentDataType.INTEGER
                );

        if (value == null) {

            return DEFAULT_HUD_POSITION;
        }

        return clampHudPosition(
                value
        );
    }


    public int setHudPosition(
            Player player,
            int position
    ) {

        int safePosition =
                clampHudPosition(
                        position
                );

        player
                .getPersistentDataContainer()
                .set(
                        hudPositionKey,
                        PersistentDataType.INTEGER,
                        safePosition
                );

        return safePosition;
    }


    public void resetHudPosition(
            Player player
    ) {

        player
                .getPersistentDataContainer()
                .remove(
                        hudPositionKey
                );
    }


    public int getDefaultHudPosition() {

        return DEFAULT_HUD_POSITION;
    }


    public int getMinimumHudPosition() {

        return MIN_HUD_POSITION;
    }


    public int getMaximumHudPosition() {

        return MAX_HUD_POSITION;
    }


    private int clampHudPosition(
            int position
    ) {

        return Math.max(
                MIN_HUD_POSITION,
                Math.min(
                        MAX_HUD_POSITION,
                        position
                )
        );
    }


    /*
     * =========================================================
     * PROFILE CACHE
     * =========================================================
     */

    public void updateProfile(
            PlayerData playerData
    ) {

        PlayerStats baseStats =
                statService.calculate(
                        playerData
                );


        UUID uuid =
                playerData.getUuid();


        double attack =
                baseStats.attack();

        double defense =
                baseStats.defense();


        /*
         * 이 메서드는 Player 객체 없이 호출될 수 있다.
         *
         * 이미 HUD snapshot이 존재한다면 기존 장비 보너스를
         * 보존해서 EXP/레벨/직업 갱신 때문에 장비 공격력과
         * 방어력이 사라지지 않게 한다.
         */
        HudSnapshot previous =
                snapshotMap.get(
                        uuid
                );


        if (previous != null) {

            PlayerStats previousBaseStats =
                statService.calculateBase(
                        previous.level(),
                        previous.playerClass()
                );


        double equipmentAttack =
                previous.attack()
                        - previousBaseStats.attack();

        double equipmentDefense =
                previous.defense()
                        - previousBaseStats.defense();


            attack +=
                    equipmentAttack;

            defense +=
                    equipmentDefense;
        }


        String playerClass =
                playerData.getPlayerClass();


        if (
                playerClass == null
                || playerClass.isBlank()
        ) {

            playerClass =
                    "NONE";
        }


        double damageReductionFlat =
                previous == null
                        ? 0.0
                        : previous.damageReductionFlat();


        HudSnapshot snapshot =
                new HudSnapshot(
                        uuid,
                        playerData.getLevel(),
                        playerData.getExperience(),
                        playerClass,
                        baseStats.maxHealth(),
                        attack,
                        defense,
                        baseStats.maxMana(),
                        damageReductionFlat
                );


        snapshotMap.put(
                uuid,
                snapshot
        );


        requestUpdate(
                uuid
        );
    }


    public void updateProfile(
            PlayerData playerData,
            Player player
    ) {

        PlayerStats stats =
                statService.calculate(
                        playerData,
                        player
                );


        String playerClass =
                playerData.getPlayerClass();


        if (
                playerClass == null
                || playerClass.isBlank()
        ) {

            playerClass =
                    "NONE";
        }


        HudSnapshot snapshot =
                new HudSnapshot(
                        playerData.getUuid(),
                        playerData.getLevel(),
                        playerData.getExperience(),
                        playerClass,
                        stats.maxHealth(),
                        stats.attack(),
                        stats.defense(),
                        stats.maxMana(),
                        stats.damageReductionFlat()
                );


        snapshotMap.put(
                playerData.getUuid(),
                snapshot
        );


        requestUpdate(
                playerData.getUuid()
        );
    }


    /*
     * =========================================================
     * EQUIPMENT STAT REFRESH
     * =========================================================
     *
     * 장비 변경 시 DB를 다시 조회하지 않는다.
     *
     * 기존 HudSnapshot의:
     *
     * - level
     * - experience
     * - class
     *
     * 를 유지하고 현재 장비만 다시 읽어
     * 공격력 / 방어력을 갱신한다.
     */
    public void refreshEquipmentStats(
            Player player
    ) {

        if (
                player == null
                || !player.isOnline()
        ) {
            return;
        }


        UUID uuid =
                player.getUniqueId();


        HudSnapshot current =
                snapshotMap.get(
                        uuid
                );


        if (current == null) {
            return;
        }


        PlayerStats stats =
                statService.calculate(
                        current.level(),
                        current.playerClass(),
                        player
                );


        HudSnapshot updated =
                new HudSnapshot(
                        current.uuid(),
                        current.level(),
                        current.experience(),
                        current.playerClass(),
                        stats.maxHealth(),
                        stats.attack(),
                        stats.defense(),
                        stats.maxMana(),
                        stats.damageReductionFlat()
                );


        snapshotMap.put(
                uuid,
                updated
        );


        requestUpdate(
                uuid
        );
    }


    public boolean hasProfile(
            UUID uuid
    ) {

        return snapshotMap.containsKey(
                uuid
        );
    }


    public HudSnapshot getProfile(
            UUID uuid
    ) {

        return snapshotMap.get(
                uuid
        );
    }


    public void remove(
            UUID uuid
    ) {

        snapshotMap.remove(
                uuid
        );

        temporaryMessageMap.remove(
                uuid
        );

        dirtyPlayerSet.remove(
                uuid
        );

        lastDisplayMap.remove(
                uuid
        );
    }


    public void clear() {

        snapshotMap.clear();

        temporaryMessageMap.clear();

        dirtyPlayerSet.clear();

        lastDisplayMap.clear();
    }


    /*
     * =========================================================
     * REALTIME HUD UPDATE
     * =========================================================
     */

    public void requestUpdate(
            Player player
    ) {

        if (player == null) {
            return;
        }

        requestUpdate(
                player.getUniqueId()
        );
    }


    public void requestUpdate(
            UUID uuid
    ) {

        if (uuid == null) {
            return;
        }

        dirtyPlayerSet.add(
                uuid
        );
    }


    public boolean consumeUpdateRequest(
            UUID uuid
    ) {

        if (uuid == null) {
            return false;
        }

        return dirtyPlayerSet.remove(
                uuid
        );
    }


    public void sendDisplay(
            Player player,
            boolean force
    ) {

        if (
                player == null
                || !player.isOnline()
        ) {
            return;
        }

        UUID uuid =
                player.getUniqueId();

        Component display =
                buildDisplay(
                        player
                );

        Component previous =
                lastDisplayMap.get(
                        uuid
                );

        if (
                !force
                && display.equals(
                        previous
                )
        ) {
            return;
        }

        player.sendActionBar(
                display
        );

        lastDisplayMap.put(
                uuid,
                display
        );
    }


    /*
     * =========================================================
     * TEMPORARY MESSAGE
     * =========================================================
     */

    public void showTemporary(
            UUID uuid,
            Component component,
            long durationMillis
    ) {

        long safeDuration =
                Math.max(
                        0L,
                        durationMillis
                );

        long expiresAt =
                System.currentTimeMillis()
                        + safeDuration;

        temporaryMessageMap.put(
                uuid,
                new TemporaryMessage(
                        component,
                        expiresAt
                )
        );

        requestUpdate(
                uuid
        );
    }


    /*
     * =========================================================
     * BUILD DISPLAY
     * =========================================================
     */

    public Component buildDisplay(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();

        TemporaryMessage temporaryMessage =
                temporaryMessageMap.get(
                        uuid
                );

        if (temporaryMessage != null) {

            if (
                    System.currentTimeMillis()
                    < temporaryMessage.expiresAt()
            ) {

                return temporaryMessage.component();
            }

            temporaryMessageMap.remove(
                    uuid,
                    temporaryMessage
            );
        }

        return buildHud(
                player
        );
    }


    /*
     * =========================================================
     * BUILD HUD2
     * =========================================================
     */

    private Component buildHud(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();

        HudSnapshot snapshot =
                snapshotMap.get(
                        uuid
                );

        if (snapshot == null) {

            return Component.empty();
        }


        /*
         * =====================================================
         * HP
         * =====================================================
         */

        /*
         * 현재 HP는 Minecraft 실제 Health 값을
         * 그대로 HUD에 표시한다.
         */
        double currentHealthRaw =
                player.getHealth();

        double maximumHealthRaw =
                snapshot.maxHealth();


        currentHealthRaw =
                Math.max(
                        0.0,
                        Math.min(
                                maximumHealthRaw,
                                currentHealthRaw
                        )
                );


        /*
         * 소수점 버림.
         */
        int currentHp =
                floorToInt(
                        currentHealthRaw
                );

        int maximumHp =
                floorToInt(
                        maximumHealthRaw
                );


        /*
         * =====================================================
         * MP
         * =====================================================
         */

        double currentManaRaw;
        double maximumManaRaw;

        if (
                manaService.isInitialized(
                        uuid
                )
        ) {

            currentManaRaw =
                    manaService.getCurrentMana(
                            uuid
                    );

            maximumManaRaw =
                    manaService.getMaximumMana(
                            uuid
                    );

        } else {

            currentManaRaw =
                    snapshot.maxMana();

            maximumManaRaw =
                    snapshot.maxMana();
        }

        currentManaRaw =
                Math.max(
                        0.0,
                        Math.min(
                                maximumManaRaw,
                                currentManaRaw
                        )
                );

        int currentMp =
                floorToInt(
                        currentManaRaw
                );

        int maximumMp =
                floorToInt(
                        maximumManaRaw
                );


        /*
         * =====================================================
         * SP
         * =====================================================
         *
         * Minecraft food level:
         *
         * 0 ~ 20
         *
         * RPG HUD에서는:
         *
         * 0 ~ 100
         *
         * 로 환산.
         */

        int foodLevel =
                player.getFoodLevel();

        foodLevel =
                Math.max(
                        0,
                        Math.min(
                                20,
                                foodLevel
                        )
                );

        int currentSp =
                foodLevel * 5;

        int maximumSp =
                100;


        /*
         * =====================================================
         * EXP
         * =====================================================
         */

        boolean maximumLevel =
                snapshot.level()
                >= levelService.getMaximumLevel();

        long currentExp =
                Math.max(
                        0L,
                        snapshot.experience()
                );

        long requiredExp =
                0L;

        if (!maximumLevel) {

            requiredExp =
                    Math.max(
                            0L,
                            levelService.getRequiredExperience(
                                    snapshot.level()
                            )
                    );
        }


        /*
         * =====================================================
         * CLASS
         * =====================================================
         */

        String playerClass =
                displayClassName(
                        snapshot.playerClass(),
                        snapshot.level()
                );


        /*
         * =====================================================
         * TOP VALUES
         * =====================================================
         */

        int attack =
                floorToInt(
                        snapshot.attack()
                );

        int defense =
                floorToInt(
                        snapshot.defense()
                );


        /*
         * =====================================================
         * HUD POSITION
         * =====================================================
         */

        int hudPosition =
                getHudPosition(
                        player
                );


        /*
         * =====================================================
         * PANEL
         * =====================================================
         */

        Component hud =
                Component.empty();


        /*
         * =====================================================
         * TOP ROW
         *
         * LV.3    NONE               ⚔14      🛡7
         * =====================================================
         */

        Component topRow =
                Component.empty();

        int topAdvance =
                0;


        String levelText =
                "LV."
                        + snapshot.level();


        topRow =
                topRow.append(
                        topText(
                                levelText,
                                NamedTextColor.YELLOW
                        )
                );

        topAdvance +=
                textAdvance(
                        levelText
                );


        /*
         * LEVEL → CLASS
         */
        topRow =
                topRow.append(
                        pixelSpace(
                                12
                        )
                );

        topAdvance +=
                12;


        topRow =
                topRow.append(
                        topText(
                                playerClass,
                                NamedTextColor.AQUA
                        )
                );

        topAdvance +=
                textAdvance(
                        playerClass
                );


        /*
         * CLASS → ATTACK
         *
         * 넓은 패널이므로 충분히 간격 확보.
         */
        topRow =
                topRow.append(
                        pixelSpace(
                                110
                        )
                );

        topAdvance +=
                110;



        topAdvance +=
                ICON_ADVANCE;


        topRow =
                topRow.append(
                        pixelSpace(
                                3
                        )
                );

        topAdvance +=
                3;


        String attackText =
                Integer.toString(
                        attack
                );


        topRow =
                topRow.append(
                        topText(
                                attackText,
                                NamedTextColor.YELLOW
                        )
                );

        topAdvance +=
                textAdvance(
                        attackText
                );


        /*
         * ATTACK → DEFENSE
         */
        topRow =
                topRow.append(
                        pixelSpace(
                                40
                        )
                );

        topAdvance +=
                40;



        topAdvance +=
                ICON_ADVANCE;


        topRow =
                topRow.append(
                        pixelSpace(
                                3
                        )
                );

        topAdvance +=
                3;


        String defenseText =
                Integer.toString(
                        defense
                );


        topRow =
                topRow.append(
                        topText(
                                defenseText,
                                NamedTextColor.AQUA
                        )
                );

        topAdvance +=
                textAdvance(
                        defenseText
                );


        /*
         * TOP 출력.
         */
        hud =
                hud.append(
                        topRow
                );


        /*
         * BAR 행 시작 위치로 X 좌표 복귀.
         */
        hud =
                hud.append(
                        pixelSpace(
                                -topAdvance
                        )
                );


        /*
         * =====================================================
         * PLAYER-SPECIFIC POSITION
         * =====================================================
         *
         * 0:
         * 기본 중앙 기준.
         *
         * 값 증가:
         * 더 왼쪽.
         */

        hud =
                hud.append(
                        pixelSpace(
                                hudPosition
                        )
                );


        /*
         * =====================================================
         * HUD3 GAUGES
         * =====================================================
         */

        double hud3ExpCurrent =
                maximumLevel
                        ? 1.0
                        : currentExp;

        double hud3ExpMaximum =
                maximumLevel
                        ? 1.0
                        : requiredExp;


        return buildHud3Display(
                player,
                snapshot.level(),
                playerClass,
                attack,
                defense,
                currentHp,
                maximumHp,
                currentMp,
                maximumMp,
                currentSp,
                maximumSp,
                hud3ExpCurrent,
                hud3ExpMaximum
        );
    }


    

    /*
     * =========================================================
     * TOP TEXT
     * =========================================================
     */

    private Component topText(
            String text,
            NamedTextColor color
    ) {

        return Component.text(
                        normalizeHudText(
                                text
                        )
                )
                .color(
                        color
                );
    }


    /*
     * =========================================================
     * BAR TEXT
     * =========================================================
     */

    private Component barText(
            String text
    ) {

        return Component.text(
                        normalizeHudTextAllowSpace(
                                text
                        )
                )
                .color(
                        NamedTextColor.WHITE
                );
    }


    /*
     * =========================================================
     * NORMALIZE HUD TEXT
     * =========================================================
     */

    private String normalizeHudText(
            String text
    ) {

        if (
                text == null
                || text.isBlank()
        ) {

            return "NONE";
        }

        String upper =
                text.toUpperCase(
                        Locale.ROOT
                );

        StringBuilder result =
                new StringBuilder();

        for (
                int i = 0;
                i < upper.length();
                i++
        ) {

            char ch =
                    upper.charAt(
                            i
                    );

            boolean supported =
                    (
                            ch >= 'A'
                            && ch <= 'Z'
                    )
                    ||
                    (
                            ch >= '0'
                            && ch <= '9'
                    )
                    ||
                    ch == '.'
                    ||
                    ch == '/'
                    ||
                    ch == '-';

            if (supported) {

                result.append(
                        ch
                );

            } else {

                result.append(
                        '-'
                );
            }
        }

        return result.toString();
    }


    /*
     * BAR 텍스트는 공백을 허용한다.
     *
     * HP 100/100
     */
    private String normalizeHudTextAllowSpace(
            String text
    ) {

        if (
                text == null
                || text.isBlank()
        ) {

            return "";
        }

        String upper =
                text.toUpperCase(
                        Locale.ROOT
                );

        StringBuilder result =
                new StringBuilder();

        for (
                int i = 0;
                i < upper.length();
                i++
        ) {

            char ch =
                    upper.charAt(
                            i
                    );

            boolean supported =
                    (
                            ch >= 'A'
                            && ch <= 'Z'
                    )
                    ||
                    (
                            ch >= '0'
                            && ch <= '9'
                    )
                    ||
                    ch == '.'
                    ||
                    ch == '/'
                    ||
                    ch == '-'
                    ||
                    ch == ' ';

            if (supported) {

                result.append(
                        ch
                );

            } else {

                result.append(
                        '-'
                );
            }
        }

        return result.toString();
    }


    /*
     * =========================================================
     * TEXT ADVANCE
     * =========================================================
     */

    private int textAdvance(
            String text
    ) {

        String normalized =
                normalizeHudTextAllowSpace(
                        text
                );

        int width =
                0;

        for (
                int i = 0;
                i < normalized.length();
                i++
        ) {

            char ch =
                    normalized.charAt(
                            i
                    );

            /*
             * 공백 문자 폭.
             */
            if (ch == ' ') {

                width +=
                        8;

            } else {

                width +=
                        HUD_TEXT_ADVANCE;
            }
        }

        return width;
    }


    /*
     * =========================================================
     * HUD GLYPH
     * =========================================================
     */

    private Component hudGlyph(
            String glyph
    ) {

        return Component.text(
                        glyph
                )
                .font(
                        HUD_FONT
                )
                .color(
                        NamedTextColor.WHITE
                );
    }


    /*
     * =========================================================
     * PIXEL SPACE
     * =========================================================
     */


    /*
     * =========================================================
     * HUD3 GAUGE STEP
     * =========================================================
     */

    private int hud3GaugeStep(
            double current,
            double maximum
    ) {

        if (maximum <= 0.0) {
            return 0;
        }

        double ratio =
                current
                        / maximum;

        ratio =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                ratio
                        )
                );

        return (int) Math.round(
                ratio
                        * HUD3_BAR_STEPS
        );
    }

    private boolean shouldShowO2(
            Player player
    ) {

        if (player == null) {
            return false;
        }

        return player.isUnderWater()
                || player.getRemainingAir()
                        < player.getMaximumAir();
    }


    private int hud3O2Step(
            Player player
    ) {

        if (player == null) {
            return 0;
        }

        int maxAir =
                Math.max(
                    1,
                        player.getMaximumAir()
                );

        int remainingAir =
                Math.max(
                        0,
                        Math.min(
                                maxAir,
                                player.getRemainingAir()
                        )
                );

        double ratio =
                (double) remainingAir
                        / (double) maxAir;

        ratio =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                ratio
                        )
                );

            return (int) Math.round(
                    ratio * HUD3_O2_STEPS
            );
        }


        private Component hud3O2Glyph(
                Player player
        ) {

        int step =
                hud3O2Step(
                        player
                );

        int codePoint =
                HUD3_O2_START
                        + step;

        String glyph =
                new String(
                        Character.toChars(
                                codePoint
                        )
                );

        return Component.text(
                        glyph
                )
                .font(
                        HUD3_BAR_FONT
                )
                .color(
                        NamedTextColor.WHITE
                );
    }


    private Component buildHud3O2Row(
            Player player,
            int renderWidth
    ) {

        /*
         * =====================================================
         * O2 ZERO-WIDTH OVERLAY
         * =====================================================
         *
         * O2 texture:
         *   source canvas = 250 x 72
         *   font height   = 18
         *
         * 따라서 실제 font advance는 약:
         *
         *   250 / 72 * 18 ~= 62.5
         *
         * Minecraft bitmap advance 보정을 고려해 63 사용.
         *
         * renderWidth가 185이므로:
         *
         *   185 - 63 = 122
         *
         * O2를 X=122에서 시작하면
         * 기존 HUD의 오른쪽 끝과 정렬된다.
         *
         * 마지막에 -(baseX + advance)만큼 되감아
         * 이 Component의 총 advance를 반드시 0으로 만든다.
         *
         * 따라서 O2 표시 여부가 전체 ActionBar 중앙 정렬에
         * 절대로 영향을 주지 않는다.
         */

        final int o2Advance =
                63;

        int o2BaseX =
                Math.max(
                        0,
                        renderWidth
                                - o2Advance
                );


        return Component.empty()
                .append(
                        pixelSpace(
                                o2BaseX
                        )
                )
                .append(
                        hud3O2Glyph(
                                player
                        )
                )
                .append(
                        pixelSpace(
                                -o2BaseX
                                        - o2Advance
                        )
                );
    }


    /*
     * =========================================================
     * HUD3 GAUGE GLYPH
     * =========================================================
     */

    private Component hud3GaugeGlyph(
            int baseCodePoint,
            double current,
            double maximum
    ) {

        int step =
                hud3GaugeStep(
                        current,
                        maximum
                );

        int codePoint =
                baseCodePoint
                        + step;

        String glyph =
                new String(
                        Character.toChars(
                                codePoint
                        )
                );

        return Component.text(
                        glyph
                )
                .font(
                        HUD3_BAR_FONT
                )
                .color(
                        NamedTextColor.WHITE
                );
    }


    /*
     * =========================================================
     * HUD3 GAUGE LAYOUT
     * =========================================================
     *
     * TOP:
     *
     * HP 230x36
     * gap 18
     * MP 230x36
     * gap 18
     * SP 230x36
     *
     * BOTTOM:
     *
     * EXP 725x16
     */

    /*
     * =========================================================
     * HUD3 EXP GAUGE
     * =========================================================
     *
     * 하나의 725px bitmap은 사용하지 않는다.
     *
     * LEFT + MID + RIGHT
     *
     * 242 + 241 + 242
     * = 725px
     */

    /*
     * =========================================================
     * HUD3 GAUGE NUMBER TEXT
     * =========================================================
     */

    private String hud3GaugeValueText(
            String label,
            double current,
            double maximum
    ) {

        long currentValue =
                Math.max(
                        0L,
                        Math.round(
                                current
                        )
                );

        long maximumValue =
                Math.max(
                        0L,
                        Math.round(
                                maximum
                        )
                );

        if (
                label == null
                || label.isEmpty()
        ) {

            return currentValue
                    + " / "
                    + maximumValue;
        }


        return label
                + "  "
                + currentValue
                + " / "
                + maximumValue;
    }


    /*
     * Minecraft 기본 숫자 font 폭 계산.
     */
    private int hud3GaugeTextWidth(
            String text
    ) {

        if (
                text == null
                || text.isEmpty()
        ) {
            return 0;
        }

        int width = 0;

        for (
                int i = 0;
                i < text.length();
                i++
        ) {

            char ch =
                    text.charAt(i);

            if (ch == ' ') {
                width += 2;
            } else {
                width += 4;
            }
        }

        return width;
    }


    /*
     * =========================================================
     * HP / MP / SP BAR + TEXT
     * =========================================================
     */

    private Component hud3GaugeWithText(
            int baseCodePoint,
            String label,
            double current,
            double maximum
    ) {

        final int barWidth =
                58;


        String valueText =
                hud3GaugeValueText(
                        label,
                        current,
                        maximum
                );


        int textWidth =
                hud3GaugeTextWidth(
                        valueText
                );


        /*
         * bitmap 숫자의 시각적 중심 보정.
         *
         * 실제 cell 폭은 4px이지만 glyph 자체가
         * 약간 왼쪽으로 보여 1px 오른쪽 보정한다.
         */
        int textOffset =
                Math.max(
                        0,
                        (
                                barWidth
                                - textWidth
                        ) / 2
                                + 1
                );


        Component gauge =
                Component.empty();


        gauge =
                gauge.append(
                        hud3GaugeGlyph(
                                baseCodePoint,
                                current,
                                maximum
                        )
                );


        gauge =
                gauge.append(
                        pixelSpace(
                                -barWidth
                        )
                );


        gauge =
                gauge.append(
                        pixelSpace(
                                textOffset
                        )
                );


        gauge =
                gauge.append(
                        Component.text(
                                        valueText
                                )
                                .font(
                                        HUD3_NUMBER_FONT
                                )
                                .color(
                                        NamedTextColor.WHITE
                                )
                );


        int remaining =
                barWidth
                        - textOffset
                        - textWidth;


        gauge =
                gauge.append(
                        pixelSpace(
                                remaining
                        )
                );


        return gauge;
    }


    private Component hud3ExpGauge(
            double current,
            double maximum
    ) {

        int step =
                hud3GaugeStep(
                        current,
                        maximum
                );


        String leftGlyph =
                new String(
                        Character.toChars(
                                HUD3_EXP_START
                                        + step
                        )
                );


        String midGlyph =
                new String(
                        Character.toChars(
                                HUD3_EXP_MID_START
                                        + step
                        )
                );


        String rightGlyph =
                new String(
                        Character.toChars(
                                HUD3_EXP_RIGHT_START
                                        + step
                        )
                );


        Component exp =
                Component.empty();


        exp =
                exp.append(
                        Component.text(
                                        leftGlyph
                                )
                                .font(
                                        HUD3_BAR_FONT
                                )
                                .color(
                                        NamedTextColor.WHITE
                                )
                );


        /*
         * bitmap glyph 사이의 기본 advance 보정으로
         * 시각적 틈이 생기는 것을 제거한다.
         *
         * 첫 테스트값: -1 logical px.
         */
        exp =
                exp.append(
                        pixelSpace(
                                -1
                        )
                );


        exp =
                exp.append(
                        Component.text(
                                        midGlyph
                                )
                                .font(
                                        HUD3_BAR_FONT
                                )
                                .color(
                                        NamedTextColor.WHITE
                                )
                );


        exp =
                exp.append(
                        pixelSpace(
                                -1
                        )
                );


        exp =
                exp.append(
                        Component.text(
                                        rightGlyph
                                )
                                .font(
                                        HUD3_BAR_FONT
                                )
                                .color(
                                        NamedTextColor.WHITE
                                )
                );


        /*
         * EXP 전체 폭 미세 보정.
         *
         * GUI Scale 4 기준
         * +1 logical px ~= +4 screen px.
         */
        exp =
                exp.append(
                        pixelSpace(
                                1
                        )
                );


        return exp;
    }


    /*
     * =========================================================
     * EXP BAR + TEXT
     * =========================================================
     */

    private Component hud3ExpGaugeWithText(
            double current,
            double maximum
    ) {

        final int barWidth =
                183;


        String valueText =
                hud3GaugeValueText(
                        "",
                        current,
                        maximum
                );


        int textWidth =
                hud3GaugeTextWidth(
                        valueText
                );


        int textOffset =
                Math.max(
                        0,
                        (
                                barWidth
                                - textWidth
                        ) / 2
                );


        Component gauge =
                Component.empty();


        gauge =
                gauge.append(
                        hud3ExpGauge(
                                current,
                                maximum
                        )
                );


        gauge =
                gauge.append(
                        pixelSpace(
                                -barWidth
                        )
                );


        gauge =
                gauge.append(
                        pixelSpace(
                                textOffset
                        )
                );


        gauge =
                gauge.append(
                        Component.text(
                                        valueText
                                )
                                .color(
                                        NamedTextColor.WHITE
                                )
                );


        int remaining =
                barWidth
                        - textOffset
                        - textWidth;


        gauge =
                gauge.append(
                        pixelSpace(
                                remaining
                        )
                );


        return gauge;
    }


    /*
     * =========================================================
     * HUD3 PLAYER LOCATOR
     * =========================================================
     */
    private Component buildHud3Locator(
            Player viewer,
            int renderWidth
    ) {

        /*
         * Locator bar 자체를 먼저 그리고
         * 즉시 시작 위치로 되돌린다.
         *
         * 따라서 이 Component의 최종 advance는 0이다.
         */
        Component result =
                Component.empty()
                        .append(
                                Component.text(
                                        HUD3_LOCATOR_BAR
                                ).font(
                                        HUD3_LOCATOR_FONT
                                )
                        )
                        .append(
                                pixelSpace(
                                        -HUD3_LOCATOR_ADVANCE
                                )
                        );


        for (
                Player target
                : viewer.getWorld()
                        .getPlayers()
        ) {

            if (
                    target.equals(
                            viewer
                    )
                    ||
                    !target.isOnline()
            ) {
                continue;
            }


            double dx =
                    target.getLocation().getX()
                            - viewer.getLocation().getX();

            double dz =
                    target.getLocation().getZ()
                            - viewer.getLocation().getZ();


            double targetYaw =
                    Math.toDegrees(
                            Math.atan2(
                                    -dx,
                                    dz
                            )
                    );


            double relativeYaw =
                    targetYaw
                            - viewer.getLocation()
                                    .getYaw();


            while (relativeYaw <= -180.0D) {
                relativeYaw += 360.0D;
            }

            while (relativeYaw > 180.0D) {
                relativeYaw -= 360.0D;
            }


            double ratio =
                    (
                            relativeYaw
                                    + 180.0D
                    )
                    / 360.0D;


            int markerPosition =
                    (int)Math.round(
                            ratio
                                    * HUD3_LOCATOR_TRACK_WIDTH
                    );


            markerPosition =
                    Math.max(
                            0,
                            Math.min(
                                    HUD3_LOCATOR_TRACK_WIDTH,
                                    markerPosition
                            )
                    );


            /*
             * 각각의 marker를 독립적인 zero-width overlay로 그린다.
             */
            result =
                    result
                            .append(
                                    pixelSpace(
                                            markerPosition
                                    )
                            )
                            .append(
                                    Component.text(
                                            HUD3_LOCATOR_PLAYER
                                    ).font(
                                            HUD3_LOCATOR_FONT
                                    )
                            )
                            .append(
                                    pixelSpace(
                                            -markerPosition
                                                    - HUD3_LOCATOR_MARKER_ADVANCE
                                    )
                            );
        }


        return result;
    }


    /*
     * =========================================================
     * HUD3 COMPLETE DISPLAY
     * =========================================================
     *
     * 게이지는 기존 위치를 절대 변경하지 않는다.
     *
     * 게이지 전체를 먼저 그린 후 negative space로 시작점
     * 부근으로 돌아와 상단 텍스트를 overlay한다.
     *
     * 현재 HUD3 이미지에는 위쪽 transparent padding이
     * 있으므로 기본 Minecraft 문자는 게이지보다 위쪽에
     * 표시된다.
     */
    private Component buildHud3Display(
            Player player,
            int level,
            String playerClass,
            int attack,
            int defense,
            double currentHp,
            double maximumHp,
            double currentMp,
            double maximumMp,
            double currentSp,
            double maximumSp,
            double currentExp,
            double maximumExp
    ) {

        Component gauges =
                buildHud3Gauges(
                        currentHp,
                        maximumHp,
                        currentMp,
                        maximumMp,
                        currentSp,
                        maximumSp,
                        currentExp,
                        maximumExp
                );


        Component topRow =
                buildHud3TopRow(
                        level,
                        playerClass,
                        attack,
                        defense
                );


        /*
         * 현재 HP/MP/SP 상단 폭과 EXP 폭은 약 185 font px.
         *
         * HUD3_EXP_REWIND가 현재 -185이므로 동일한 폭을
         * 기준으로 사용해 기존 gauge 위치에는 손대지 않는다.
         */
        int renderWidth =
                Math.abs(
                        HUD3_EXP_REWIND
                );


        int topWidth =
                hud3TopRowWidth(
                        level,
                        playerClass,
                        attack,
                        defense
                );


        int topOffset =
                Math.max(
                        0,
                        (
                                renderWidth
                                - topWidth
                        ) / 2
                );


        
        Component display =
                gauges
                        .append(
                                pixelSpace(
                                        -renderWidth
                                )
                        )
                        .append(
                                pixelSpace(
                                        topOffset
                                )
                        )
                        .append(
                                topRow
                        );


        /*
         * =====================================================
         * PLAYER LOCATOR BAR
         * =====================================================
         *
         * RPG EXP 아래 / Hotbar 위에 표시한다.
         *
         * zero-width overlay로 기존 HUD의
         * 전체 위치에는 영향을 주지 않는다.
         */
        display =
                buildHud3Locator(
                        player,
                        renderWidth
                )
                        .append(
                                display
                        );


        /*
         * =====================================================
         * O2
         * =====================================================
         *
         * O2 bitmap의 ascent/padding으로
         * TOP ROW 위쪽에 표시한다.
         */
    if (
            shouldShowO2(
                    player
            )
    ) {

        /*
         * O2는 zero-width overlay.
         *
         * display 앞에 붙이므로 기존 HUD 시작 위치,
         * 끝 위치, 전체 advance는 그대로 유지된다.
         */
        display =
                buildHud3O2Row(
                        player,
                        renderWidth
                )
                        .append(
                                display
                        );
    }


        return display;

    }


    /*
     * =========================================================
     * TOP ROW
     * =========================================================
     *
     * LV.10   전사   공격력 34   방어력 22
     */
    private Component buildHud3TopRow(
            int level,
            String playerClass,
            int attack,
            int defense
    ) {

        String levelText =
                "LV." + level;

        String classText =
                playerClass;

        String attackText =
                "공격력 " + attack;

        String defenseText =
                "방어력 " + defense;


        /*
         * =====================================================
         * FIXED TOP COLUMNS
         * =====================================================
         *
         * 각 값은 행 시작점 기준 절대 X 좌표.
         */
        final int levelX =
                0;

        final int classX =
                44;

        final int attackX =
                91;

        final int defenseX =
                166;


        Component row =
                Component.empty();

        int currentX =
                levelX;


        /*
         * LEVEL
         */
        row =
                row.append(
                        Component.text(
                                levelText,
                                NamedTextColor.GOLD
                        )
                );

        currentX +=
                hud3DefaultTextWidth(
                        levelText
                );


        /*
         * CLASS
         */
        row =
                row.append(
                        pixelSpace(
                                classX
                                        - currentX
                        )
                );

        row =
                row.append(
                        Component.text(
                                classText,
                                NamedTextColor.AQUA
                        )
                );

        currentX =
                classX
                        + hud3DefaultTextWidth(
                                classText
                        );


        /*
         * ATTACK
         */
        row =
                row.append(
                        pixelSpace(
                                attackX
                                        - currentX
                        )
                );

        row =
                row.append(
                        Component.text(
                                attackText,
                                NamedTextColor.RED
                        )
                );

        currentX =
                attackX
                        + hud3DefaultTextWidth(
                                attackText
                        );


        /*
         * DEFENSE
         */
        row =
                row.append(
                        pixelSpace(
                                defenseX
                                        - currentX
                        )
                );

        row =
                row.append(
                        Component.text(
                                defenseText,
                                NamedTextColor.BLUE
                        )
                );


        return row;
    }



    /*
     * =========================================================
     * CLASS DISPLAY NAME
     * =========================================================
     */
    private String displayClassName(
            String rawClass,
            int level
    ) {

        /*
         * Lv.10 이전은 무조건 초보자.
         */
        if (level < 10) {
            return "초보자";
        }


        if (
                rawClass == null
                || rawClass.isBlank()
        ) {

            return "초보자";
        }


        String value =
                rawClass.trim()
                        .toUpperCase(
                                Locale.ROOT
                        );


        return switch (value) {

            case "WARRIOR",
                 "FIGHTER",
                 "전사" ->
                    "전사";


            case "MAGE",
                 "MAGICIAN",
                 "WIZARD",
                 "마법사" ->
                    "마법사";


            case "NONE",
                 "NOVICE",
                 "BEGINNER",
                 "초보자" ->
                    "초보자";


            default ->
                    rawClass;
        };
    }


    /*
     * =========================================================
     * TOP ROW APPROXIMATE WIDTH
     * =========================================================
     *
     * 이 값은 gauge 위치에는 영향을 주지 않는다.
     * 상단 문자를 중앙 정렬하는 데만 사용한다.
     */

    /*
     * =========================================================
     * HUD3 DEFAULT TEXT WIDTH
     * =========================================================
     *
     * 상단 고정 컬럼 계산 전용.
     *
     * 현재 HUD에서 사용 중인 기본 Minecraft 글꼴의
     * 근사 advance 값.
     */
    private int hud3DefaultTextWidth(
            String text
    ) {

        if (text == null) {
            return 0;
        }


        int width =
                0;


        for (
                int offset = 0;
                offset < text.length();
        ) {

            int codePoint =
                    text.codePointAt(
                            offset
                    );

            offset +=
                    Character.charCount(
                            codePoint
                    );


            if (codePoint == ' ') {

                width +=
                        4;

            } else if (
                    codePoint >= 0xAC00
                    && codePoint <= 0xD7A3
            ) {

                width +=
                        9;

            } else {

                width +=
                        6;
            }
        }


        return width;
    }

    private int hud3TopRowWidth(
            int level,
            String playerClass,
            int attack,
            int defense
    ) {

        /*
         * 상단 행의 기준 폭을 항상 동일하게 유지.
         *
         * 숫자 자릿수가 달라져도 buildHud3Display()가
         * 상단 row 전체를 다시 중앙 이동시키지 않는다.
         */
        return 220;
    }



    private Component buildHud3Gauges(
            double currentHp,
            double maximumHp,
            double currentMp,
            double maximumMp,
            double currentSp,
            double maximumSp,
            double currentExp,
            double maximumExp
    ) {

        Component hud =
                Component.empty();


        /*
         * HP
         */
        hud =
                hud.append(
                        hud3GaugeWithText(
                                HUD3_HP_START,
                                "",
                                currentHp,
                                maximumHp
                        )
                );


        /*
         * HP -> MP
         */
        hud =
                hud.append(
                        pixelSpace(
                                HUD3_TOP_GAP_SPACE
                        )
                );


        /*
         * MP
         */
        hud =
                hud.append(
                        hud3GaugeWithText(
                                HUD3_MP_START,
                                "",
                                currentMp,
                                maximumMp
                        )
                );


        /*
         * MP -> SP
         */
        hud =
                hud.append(
                        pixelSpace(
                                HUD3_TOP_GAP_SPACE
                        )
                );


        /*
         * SP
         */
        hud =
                hud.append(
                        hud3GaugeWithText(
                                HUD3_SP_START,
                                "",
                                currentSp,
                                maximumSp
                        )
                );


        /*
         * 상단 row 끝에서 EXP 시작점으로 rewind.
         */
        hud =
                hud.append(
                        pixelSpace(
                                HUD3_EXP_REWIND
                        )
                );


        /*
         * EXP
         */
        hud =
                hud.append(
                        hud3ExpGauge(
                                currentExp,
                                maximumExp
                        )
                );


        return hud;
    }


    private Component pixelSpace(
            int pixels
    ) {

        if (pixels == 0) {

            return Component.empty();
        }

        boolean negative =
                pixels < 0;

        int remaining =
                Math.abs(
                        pixels
                );

        int[] codePoints =
                negative
                        ? NEGATIVE_SPACE_CODEPOINTS
                        : POSITIVE_SPACE_CODEPOINTS;

        int[] values =
                negative
                        ? NEGATIVE_SPACE_VALUES
                        : POSITIVE_SPACE_VALUES;

        StringBuilder builder =
                new StringBuilder();

        for (
                int i = 0;
                i < values.length;
                i++
        ) {

            while (
                    remaining
                    >= values[i]
            ) {

                builder.appendCodePoint(
                        codePoints[i]
                );

                remaining -=
                        values[i];
            }
        }

        return Component.text(
                        builder.toString()
                )
                .font(
                        HUD_FONT
                )
                .color(
                        NamedTextColor.WHITE
                );
    }


    

    

    /*
     * =========================================================
     * FLOOR
     * =========================================================
     *
     * 모든 RPG 포인트 표시를
     * 소수점 내림하여 정수로 만든다.
     *
     * 113.9
     * →
     * 113
     */

    private int floorToInt(
            double value
    ) {

        if (value <= 0.0) {

            return 0;
        }

        return (int) Math.floor(
                value
        );
    }


    /*
     * =========================================================
     * TEMP MESSAGE
     * =========================================================
     */

    private record TemporaryMessage(
            Component component,
            long expiresAt
    ) {
    }
}
