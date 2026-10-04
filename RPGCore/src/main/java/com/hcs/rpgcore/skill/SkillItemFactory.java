package com.hcs.rpgcore.skill;

import com.hcs.rpgcore.RPGCorePlugin;

import java.util.List;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;


public final class SkillItemFactory {

    /*
     * =========================================================
     * CLASS SKILL TOME IDS
     * =========================================================
     */
    public static final String WARRIOR_SKILL_TOME_ID =
            "WARRIOR_SKILL_TOME";

    public static final String MAGE_SKILL_TOME_ID =
            "MAGE_SKILL_TOME";


    private final NamespacedKey skillItemKey;
    private final NamespacedKey skillIdKey;
    private final NamespacedKey ownerUuidKey;


    public SkillItemFactory(
            RPGCorePlugin plugin
    ) {

        this.skillItemKey =
                new NamespacedKey(
                        plugin,
                        "skill_item"
                );

        this.skillIdKey =
                new NamespacedKey(
                        plugin,
                        "skill_id"
                );

        this.ownerUuidKey =
                new NamespacedKey(
                        plugin,
                        "owner_uuid"
                );
    }


    /*
     * =========================================================
     * CREATE BY CLASS
     * =========================================================
     */
    /*
     * =========================================================
     * CLASS SKILL TOME
     * =========================================================
     *
     * Lv.10 전직 완료 시 한 번 지급되는
     * 직업별 영구 스킬 도감 아이템.
     *
     * 개별 스킬을 담는 소비형 책이 아니다.
     * 우클릭하면 직업 스킬 도감을 열며,
     * 레벨에 따라 스킬이 자동 해금된다.
     */
    public ItemStack createClassSkillTome(
            String playerClass,
            UUID ownerUuid
    ) {

        if (
                playerClass != null
                && "WARRIOR".equalsIgnoreCase(
                        playerClass
                )
        ) {

            return createWarriorSkillTome(
                    ownerUuid
            );
        }


        if (
                playerClass != null
                && "MAGE".equalsIgnoreCase(
                        playerClass
                )
        ) {

            return createMageSkillTome(
                    ownerUuid
            );
        }


        return null;
    }


    /*
     * =========================================================
     * WARRIOR SKILL TOME
     * =========================================================
     */
    public ItemStack createWarriorSkillTome(
            UUID ownerUuid
    ) {

        return createSkillBook(
                WARRIOR_SKILL_TOME_ID,
                ownerUuid,

                Component.text(
                        "[전사] 스킬북",
                        NamedTextColor.GOLD
                ),

                List.of(
                        Component.empty(),

                        Component.text(
                                "[우클릭] 전사 스킬 도감 열기",
                                NamedTextColor.YELLOW
                        ),

                        Component.empty(),

                        Component.text(
                                "전사 스킬을 관리하는 영구 스킬북입니다.",
                                NamedTextColor.WHITE
                        ),

                        Component.text(
                                "레벨이 오르면 새로운 스킬이 자동으로 해금됩니다.",
                                NamedTextColor.GRAY
                        ),

                        Component.text(
                                "해금된 스킬을 1~5번 슬롯에 장착할 수 있습니다.",
                                NamedTextColor.GRAY
                        ),

                        Component.empty(),

                        Component.text(
                                "전직 보상: Lv.10",
                                NamedTextColor.GOLD
                        ),

                        Component.text(
                                "귀속 아이템",
                                NamedTextColor.DARK_GRAY
                        )
                )
        );
    }


    /*
     * =========================================================
     * MAGE SKILL TOME
     * =========================================================
     */
    public ItemStack createMageSkillTome(
            UUID ownerUuid
    ) {

        return createSkillBook(
                MAGE_SKILL_TOME_ID,
                ownerUuid,

                Component.text(
                        "[마법사] 스킬북",
                        NamedTextColor.LIGHT_PURPLE
                ),

                List.of(
                        Component.empty(),

                        Component.text(
                                "[우클릭] 마법사 스킬 도감 열기",
                                NamedTextColor.YELLOW
                        ),

                        Component.empty(),

                        Component.text(
                                "마법사 스킬을 관리하는 영구 스킬북입니다.",
                                NamedTextColor.WHITE
                        ),

                        Component.text(
                                "레벨이 오르면 새로운 스킬이 자동으로 해금됩니다.",
                                NamedTextColor.GRAY
                        ),

                        Component.text(
                                "해금된 스킬을 1~5번 슬롯에 장착할 수 있습니다.",
                                NamedTextColor.GRAY
                        ),

                        Component.empty(),

                        Component.text(
                                "전직 보상: Lv.10",
                                NamedTextColor.LIGHT_PURPLE
                        ),

                        Component.text(
                                "귀속 아이템",
                                NamedTextColor.DARK_GRAY
                        )
                )
        );
    }


    public ItemStack createStarterSkillBook(
            String playerClass,
            UUID ownerUuid
    ) {

        if (
                playerClass != null
                && "WARRIOR".equalsIgnoreCase(
                        playerClass
                )
        ) {

            return createBashBook(
                    ownerUuid
            );
        }


        if (
                playerClass != null
                && "MAGE".equalsIgnoreCase(
                        playerClass
                )
        ) {

            return createFireBoltBook(
                    ownerUuid
            );
        }


        return null;
    }


    /*
     * =========================================================
     * WARRIOR - BASH
     * =========================================================
     */
    public ItemStack createBashBook(
            UUID ownerUuid
    ) {

        ItemStack item =
                createSkillBook(
                        SkillIds.WARRIOR_BASH,
                        ownerUuid,

                        Component.text(
                                "[전사 스킬] 배쉬",
                                NamedTextColor.GOLD
                        ),

                        List.of(
                                Component.empty(),

                                Component.text(
                                        "[슬롯 1~5 등록] 숫자 키로 사용",
                                        NamedTextColor.YELLOW
                                ),

                                Component.empty(),

                                Component.text(
                                        "현재 총 공격력의 150% 피해",
                                        NamedTextColor.WHITE
                                ),

                                Component.text(
                                        "전방 넓은 범위의 적을 공격합니다.",
                                        NamedTextColor.GRAY
                                ),

                                Component.empty(),

                                Component.text(
                                        "MP 소모: 없음",
                                        NamedTextColor.AQUA
                                ),

                                Component.text(
                                        "쿨타임: 5초",
                                        NamedTextColor.GRAY
                                )
                        )
                );


        if (item == null) {
            return null;
        }


        return item;
    }


    /*
     * =========================================================
     * MAGE - FIRE BOLT
     * =========================================================
     */
    public ItemStack createFireBoltBook(
            UUID ownerUuid
    ) {

        return createSkillBook(
                SkillIds.MAGE_FIRE_BOLT,
                ownerUuid,

                Component.text(
                        "[마법사 스킬] 파이어 볼트",
                        NamedTextColor.LIGHT_PURPLE
                ),

                List.of(
                        Component.empty(),

                        Component.text(
                                "[슬롯 1~5 등록] 숫자 키로 사용",
                                NamedTextColor.YELLOW
                        ),

                        Component.empty(),

                        Component.text(
                                "최대 MP에 비례한 화염구를 발사합니다.",
                                NamedTextColor.WHITE
                        ),

                        Component.text(
                                "충돌 지점 주변의 적에게 광역 피해를 줍니다.",
                                NamedTextColor.GRAY
                        ),

                        Component.empty(),

                        Component.text(
                                "MP 소모: 4",
                                NamedTextColor.AQUA
                        ),

                        Component.text(
                                "쿨타임: 4초",
                                NamedTextColor.GRAY
                        )
                )
        );
    }


    /*
     * =========================================================
     * COMMON SKILL BOOK
     * =========================================================
     */
    private ItemStack createSkillBook(
            String skillId,
            UUID ownerUuid,
            Component displayName,
            List<Component> lore
    ) {

        if (ownerUuid == null) {
            throw new IllegalArgumentException(
                    "ownerUuid cannot be null"
            );
        }

        ItemStack item =
                new ItemStack(
                        Material.ENCHANTED_BOOK
                );


        ItemMeta meta =
                item.getItemMeta();


        meta.displayName(
                displayName.decoration(
                        TextDecoration.ITALIC,
                        false
                )
        );


        meta.lore(
                lore.stream()
                        .map(
                                line ->
                                        line.decoration(
                                                TextDecoration.ITALIC,
                                                false
                                        )
                        )
                        .toList()
        );


        PersistentDataContainer pdc =
                meta.getPersistentDataContainer();


        pdc.set(
                skillItemKey,
                PersistentDataType.BYTE,
                (byte) 1
        );


        pdc.set(
                skillIdKey,
                PersistentDataType.STRING,
                skillId
        );


        pdc.set(
                ownerUuidKey,
                PersistentDataType.STRING,
                ownerUuid.toString()
        );


        /*
         * =====================================================
         * SKILL ITEM MODEL
         * =====================================================
         *
         * skillId에 대응하는 전용 아이콘이 존재하면
         * resource pack item model을 적용한다.
         */
        String itemModel =
                resolveSkillItemModel(
                        skillId
                );


        if (itemModel != null) {

            meta.setItemModel(
                    new NamespacedKey(
                            "rpgcore",
                            itemModel
                    )
            );
        }


        item.setItemMeta(
                meta
        );


        return item;
    }


    /*
     * =========================================================
     * SKILL ITEM MODEL
     * =========================================================
     *
     * Resource Pack:
     *
     * assets/rpgcore/items/<model>.json
     * assets/rpgcore/models/item/<model>.json
     * assets/rpgcore/textures/item/<model>.png
     */
    private String resolveSkillItemModel(
            String skillId
    ) {

        if (SkillIds.WARRIOR_BASH.equals(skillId)) {
            return "skill_bash";
        }

                if (SkillIds.WARRIOR_WHIRLWIND.equals(skillId)) {
            return "skill_whirlwind";
        }

        if (SkillIds.WARRIOR_EXECUTION_SLASH.equals(skillId)) {
            return "skill_execution_slash";
        }

        if (SkillIds.WARRIOR_IRON_WALL.equals(skillId)) {
            return "skill_iron_wall";
        }

        if (SkillIds.WARRIOR_EARTHQUAKE_SLAM.equals(skillId)) {
            return "skill_earthquake_slam";
        }

        if (SkillIds.WARRIOR_BATTLE_CRY.equals(skillId)) {
            return "skill_battle_cry";
        }

        if (SkillIds.WARRIOR_BERSERKER_RAGE.equals(skillId)) {
            return "skill_berserker_rage";
        }

        if (SkillIds.WARRIOR_BLADE_STORM.equals(skillId)) {
            return "skill_blade_storm";
        }

        if (SkillIds.WARRIOR_GATE_OF_BABYLON.equals(skillId)) {
            return "skill_gate_of_babylon";
        }


        if (SkillIds.MAGE_FIRE_BOLT.equals(skillId)) {
            return "skill_fire_bolt";
        }

                if (SkillIds.MAGE_FROST_NOVA.equals(skillId)) {
            return "skill_frost_nova";
        }

        if (SkillIds.MAGE_CHAIN_LIGHTNING.equals(skillId)) {
            return "skill_chain_lightning";
        }

        if (SkillIds.MAGE_MANA_SHIELD.equals(skillId)) {
            return "skill_mana_shield";
        }

        if (SkillIds.MAGE_BLIZZARD.equals(skillId)) {
            return "skill_blizzard";
        }

        if (SkillIds.MAGE_BLACK_HOLE.equals(skillId)) {
            return "skill_black_hole";
        }

        if (SkillIds.MAGE_MANA_OVERLOAD.equals(skillId)) {
            return "skill_mana_overload";
        }

        if (SkillIds.MAGE_THUNDER_STORM.equals(skillId)) {
            return "skill_thunder_storm";
        }

        if (SkillIds.MAGE_METEOR.equals(skillId)) {
            return "skill_meteor";
        }


        return null;
    }


    /*
     * =========================================================
     * IDENTIFY SKILL ITEM
     * =========================================================
     */
    public boolean isSkillItem(
            ItemStack item
    ) {

        if (
                item == null
                || item.getType() != Material.ENCHANTED_BOOK
                || !item.hasItemMeta()
        ) {
            return false;
        }


        Byte value =
                item.getItemMeta()
                        .getPersistentDataContainer()
                        .get(
                                skillItemKey,
                                PersistentDataType.BYTE
                        );


        return value != null
                && value == (byte) 1;
    }


    /*
     * =========================================================
     * IDENTIFY CLASS SKILL TOME
     * =========================================================
     */
    public boolean isClassSkillTome(
            ItemStack item
    ) {

        String skillId =
                getSkillId(
                        item
                );


        return WARRIOR_SKILL_TOME_ID.equals(
                skillId
        )
                || MAGE_SKILL_TOME_ID.equals(
                        skillId
                );
    }


    public String getSkillTomeClass(
            ItemStack item
    ) {

        String skillId =
                getSkillId(
                        item
                );


        if (
                WARRIOR_SKILL_TOME_ID.equals(
                        skillId
                )
        ) {

            return "WARRIOR";
        }


        if (
                MAGE_SKILL_TOME_ID.equals(
                        skillId
                )
        ) {

            return "MAGE";
        }


        return null;
    }


    /*
     * =========================================================
     * GET SKILL ID
     * =========================================================
     */
    public String getSkillId(
            ItemStack item
    ) {

        if (!isSkillItem(item)) {
            return null;
        }


        return item.getItemMeta()
                .getPersistentDataContainer()
                .get(
                        skillIdKey,
                        PersistentDataType.STRING
                );
    }


    /*
     * =========================================================
     * OWNER UUID
     * =========================================================
     */
    public UUID getOwnerUuid(
            ItemStack item
    ) {

        if (!isSkillItem(item)) {
            return null;
        }


        String value =
                item.getItemMeta()
                        .getPersistentDataContainer()
                        .get(
                                ownerUuidKey,
                                PersistentDataType.STRING
                        );


        if (
                value == null
                || value.isBlank()
        ) {
            return null;
        }


        try {

            return UUID.fromString(
                    value
            );

        } catch (IllegalArgumentException exception) {

            return null;
        }
    }


    public boolean isOwnedBy(
            ItemStack item,
            UUID playerUuid
    ) {

        if (playerUuid == null) {
            return false;
        }


        UUID ownerUuid =
                getOwnerUuid(
                        item
                );


        return playerUuid.equals(
                ownerUuid
        );
    }


    /*
     * =========================================================
     * CREATE SKILL BOOK BY SKILL ID
     * =========================================================
     *
     * 스킬 도감에서 핫바 1~5에 장착할
     * 실제 RPGCore 스킬 아이템을 생성한다.
     *
     * 저장 기준:
     *
     *   skill_item = 1
     *   skill_id   = SkillIds.*
     *   owner_uuid = player UUID
     *
     * 기존 상세 스킬북 생성기가 존재하는 스킬은
     * 해당 생성기를 그대로 사용한다.
     *
     * 나머지 스킬은 SkillRegistry를 기준으로
     * 공통 장착용 스킬 아이템을 생성한다.
     */
    public ItemStack createSkillBookById(
            String skillId,
            UUID ownerUuid
    ) {

        if (
                skillId == null
                || skillId.isBlank()
                || ownerUuid == null
        ) {
            return null;
        }


        /*
         * =====================================================
         * EXISTING DETAILED SKILL BOOKS
         * =====================================================
         */

        if (
                SkillIds.WARRIOR_BASH.equals(
                        skillId
                )
        ) {
            return createBashBook(
                    ownerUuid
            );
        }


        if (
                SkillIds.MAGE_FIRE_BOLT.equals(
                        skillId
                )
        ) {
            return createFireBoltBook(
                    ownerUuid
            );
        }


                        if (
                SkillIds.WARRIOR_GATE_OF_BABYLON.equals(
                        skillId
                )
        ) {
            return createGateOfBabylonBook(
                    ownerUuid
            );
        }


        if (
                SkillIds.MAGE_METEOR.equals(
                        skillId
                )
        ) {
            return createMeteorBook(
                    ownerUuid
            );
        }


        /*
         * =====================================================
         * LEVEL 20 - WARRIOR WHIRLWIND
         * =====================================================
         */
        if (
                SkillIds.WARRIOR_WHIRLWIND.equals(
                        skillId
                )
        ) {

            return createSkillBook(
                    skillId,
                    ownerUuid,

                    Component.text(
                            "[전사 스킬] 휠윈드",
                            NamedTextColor.GOLD
                    ),

                    List.of(
                            Component.empty(),

                            Component.text(
                                    "[슬롯 1~5 등록] 숫자 키로 사용",
                                    NamedTextColor.YELLOW
                            ),

                            Component.empty(),

                            Component.text(
                                    "주변의 적을 공격하며 연속 회전합니다.",
                                    NamedTextColor.WHITE
                            ),

                            Component.text(
                                    "총 5회 공격하며, 1회당 총 공격력의 55% 피해를 줍니다.",
                                    NamedTextColor.GRAY
                            ),

                            Component.text(
                                    "회전 중에도 이동할 수 있습니다.",
                                    NamedTextColor.GRAY
                            ),

                            Component.empty(),

                            Component.text(
                                    "MP 소모: 8",
                                    NamedTextColor.AQUA
                            ),

                            Component.text(
                                    "쿨타임: 10초",
                                    NamedTextColor.GRAY
                            ),

                            Component.text(
                                    "습득 레벨: Lv.20",
                                    NamedTextColor.DARK_GRAY
                            )
                    )
            );
        }


        /*
         * =====================================================
         * LEVEL 20 - MAGE FROST NOVA
         * =====================================================
         */
        if (
                SkillIds.MAGE_FROST_NOVA.equals(
                        skillId
                )
        ) {

            return createSkillBook(
                    skillId,
                    ownerUuid,

                    Component.text(
                            "[마법사 스킬] 프로스트 노바",
                            NamedTextColor.LIGHT_PURPLE
                    ),

                    List.of(
                            Component.empty(),

                            Component.text(
                                    "[슬롯 1~5 등록] 숫자 키로 사용",
                                    NamedTextColor.YELLOW
                            ),

                            Component.empty(),

                            Component.text(
                                    "자신을 중심으로 냉기를 폭발시켜 주변의 적을 공격합니다.",
                                    NamedTextColor.WHITE
                            ),

                            Component.text(
                                    "피해: 30 + 최대 MP의 18%",
                                    NamedTextColor.AQUA
                            ),

                            Component.text(
                                    "적의 이동 속도를 4초 동안 크게 감소시킵니다.",
                                    NamedTextColor.GRAY
                            ),

                            Component.empty(),

                            Component.text(
                                    "MP 소모: 12",
                                    NamedTextColor.AQUA
                            ),

                            Component.text(
                                    "쿨타임: 12초",
                                    NamedTextColor.GRAY
                            ),

                            Component.text(
                                    "습득 레벨: Lv.20",
                                    NamedTextColor.DARK_GRAY
                            )
                    )
            );
        }


        /*
         * =====================================================
         * LEVEL 30 - WARRIOR EXECUTION SLASH
         * =====================================================
         */
        if (
                SkillIds.WARRIOR_EXECUTION_SLASH.equals(
                        skillId
                )
        ) {

            return createSkillBook(
                    skillId,
                    ownerUuid,

                    Component.text(
                            "[전사 스킬] 익스큐션 슬래시",
                            NamedTextColor.GOLD
                    ),

                    List.of(
                            Component.empty(),

                            Component.text(
                                    "[슬롯 1~5 등록] 숫자 키로 사용",
                                    NamedTextColor.YELLOW
                            ),

                            Component.empty(),

                            Component.text(
                                    "전방의 적을 강하게 베어냅니다.",
                                    NamedTextColor.WHITE
                            ),

                            Component.text(
                                    "현재 총 공격력의 275% 피해",
                                    NamedTextColor.GOLD
                            ),

                            Component.text(
                                    "전방 최대 5블록의 적을 공격합니다.",
                                    NamedTextColor.GRAY
                            ),

                            Component.empty(),

                            Component.text(
                                    "MP 소모: 12",
                                    NamedTextColor.AQUA
                            ),

                            Component.text(
                                    "쿨타임: 10초",
                                    NamedTextColor.GRAY
                            ),

                            Component.text(
                                    "습득 레벨: Lv.30",
                                    NamedTextColor.DARK_GRAY
                            )
                    )
            );
        }


        /*
         * =====================================================
         * LEVEL 30 - MAGE CHAIN LIGHTNING
         * =====================================================
         */
        if (
                SkillIds.MAGE_CHAIN_LIGHTNING.equals(
                        skillId
                )
        ) {

            return createSkillBook(
                    skillId,
                    ownerUuid,

                    Component.text(
                            "[마법사 스킬] 체인 라이트닝",
                            NamedTextColor.LIGHT_PURPLE
                    ),

                    List.of(
                            Component.empty(),

                            Component.text(
                                    "[슬롯 1~5 등록] 숫자 키로 사용",
                                    NamedTextColor.YELLOW
                            ),

                            Component.empty(),

                            Component.text(
                                    "첫 번째 적에게 번개를 발사한 뒤 주변 적에게 연쇄됩니다.",
                                    NamedTextColor.WHITE
                            ),

                            Component.text(
                                    "기본 피해: 40 + 최대 MP의 25%",
                                    NamedTextColor.AQUA
                            ),

                            Component.text(
                                    "최대 5명의 적을 공격하며 연쇄될수록 피해가 감소합니다.",
                                    NamedTextColor.GRAY
                            ),

                            Component.empty(),

                            Component.text(
                                    "MP 소모: 18",
                                    NamedTextColor.AQUA
                            ),

                            Component.text(
                                    "쿨타임: 10초",
                                    NamedTextColor.GRAY
                            ),

                            Component.text(
                                    "습득 레벨: Lv.30",
                                    NamedTextColor.DARK_GRAY
                            )
                    )
            );
        }


        /*
         * =====================================================
         * LEVEL 40 - WARRIOR IRON WALL
         * =====================================================
         */
        if (
                SkillIds.WARRIOR_IRON_WALL.equals(
                        skillId
                )
        ) {

            return createSkillBook(
                    skillId,
                    ownerUuid,

                    Component.text(
                            "[전사 스킬] 아이언 월",
                            NamedTextColor.GOLD
                    ),

                    List.of(
                            Component.empty(),

                            Component.text(
                                    "[슬롯 1~5 등록] 숫자 키로 사용",
                                    NamedTextColor.YELLOW
                            ),

                            Component.empty(),

                            Component.text(
                                    "6초 동안 강력한 방어 태세를 취합니다.",
                                    NamedTextColor.WHITE
                            ),

                            Component.text(
                                    "받는 최종 피해가 40% 감소합니다.",
                                    NamedTextColor.GRAY
                            ),

                            Component.empty(),

                            Component.text(
                                    "MP 소모: 15",
                                    NamedTextColor.AQUA
                            ),

                            Component.text(
                                    "쿨타임: 30초",
                                    NamedTextColor.GRAY
                            ),

                            Component.text(
                                    "습득 레벨: Lv.40",
                                    NamedTextColor.DARK_GRAY
                            )
                    )
            );
        }


        /*
         * =====================================================
         * LEVEL 40 - MAGE MANA SHIELD
         * =====================================================
         */
        if (
                SkillIds.MAGE_MANA_SHIELD.equals(
                        skillId
                )
        ) {

            return createSkillBook(
                    skillId,
                    ownerUuid,

                    Component.text(
                            "[마법사 스킬] 마나 실드",
                            NamedTextColor.LIGHT_PURPLE
                    ),

                    List.of(
                            Component.empty(),

                            Component.text(
                                    "[슬롯 1~5 등록] 숫자 키로 사용",
                                    NamedTextColor.YELLOW
                            ),

                            Component.empty(),

                            Component.text(
                                    "8초 동안 마나로 자신을 보호합니다.",
                                    NamedTextColor.WHITE
                            ),

                            Component.text(
                                    "받는 피해의 50%를 MP로 대신 흡수합니다.",
                                    NamedTextColor.AQUA
                            ),

                            Component.text(
                                    "피해 1당 MP 1을 소모하며 MP가 부족하면 일부만 흡수합니다.",
                                    NamedTextColor.GRAY
                            ),

                            Component.empty(),

                            Component.text(
                                    "MP 소모: 20",
                                    NamedTextColor.AQUA
                            ),

                            Component.text(
                                    "쿨타임: 30초",
                                    NamedTextColor.GRAY
                            ),

                            Component.text(
                                    "습득 레벨: Lv.40",
                                    NamedTextColor.DARK_GRAY
                            )
                    )
            );
        }


        /*
         * =====================================================
         * LEVEL 50 - WARRIOR EARTHQUAKE SLAM
         * =====================================================
         */
        if (
                SkillIds.WARRIOR_EARTHQUAKE_SLAM.equals(
                        skillId
                )
        ) {

            return createSkillBook(
                    skillId,
                    ownerUuid,

                    Component.text(
                            "[전사 스킬] 어스퀘이크 슬램",
                            NamedTextColor.GOLD
                    ),

                    List.of(
                            Component.empty(),

                            Component.text(
                                    "[슬롯 1~5 등록] 숫자 키로 사용",
                                    NamedTextColor.YELLOW
                            ),

                            Component.empty(),

                            Component.text(
                                    "전방의 지면을 강타하여 적들을 공격합니다.",
                                    NamedTextColor.WHITE
                            ),

                            Component.text(
                                    "현재 총 공격력의 180% 피해",
                                    NamedTextColor.GOLD
                            ),

                            Component.text(
                                    "전방 최대 7블록의 적을 공중으로 띄웁니다.",
                                    NamedTextColor.GRAY
                            ),

                            Component.empty(),

                            Component.text(
                                    "MP 소모: 20",
                                    NamedTextColor.AQUA
                            ),

                            Component.text(
                                    "쿨타임: 16초",
                                    NamedTextColor.GRAY
                            ),

                            Component.text(
                                    "습득 레벨: Lv.50",
                                    NamedTextColor.DARK_GRAY
                            )
                    )
            );
        }


        /*
         * =====================================================
         * LEVEL 50 - MAGE BLIZZARD
         * =====================================================
         */
        if (
                SkillIds.MAGE_BLIZZARD.equals(
                        skillId
                )
        ) {

            return createSkillBook(
                    skillId,
                    ownerUuid,

                    Component.text(
                            "[마법사 스킬] 블리자드",
                            NamedTextColor.LIGHT_PURPLE
                    ),

                    List.of(
                            Component.empty(),

                            Component.text(
                                    "[슬롯 1~5 등록] 숫자 키로 사용",
                                    NamedTextColor.YELLOW
                            ),

                            Component.empty(),

                            Component.text(
                                    "지정한 지역에 강력한 눈보라를 생성합니다.",
                                    NamedTextColor.WHITE
                            ),

                            Component.text(
                                    "총 6회 공격하며, 1회 피해: 35 + 최대 MP의 12%",
                                    NamedTextColor.AQUA
                            ),

                            Component.text(
                                    "범위 안의 적의 이동 속도를 지속적으로 감소시킵니다.",
                                    NamedTextColor.GRAY
                            ),

                            Component.empty(),

                            Component.text(
                                    "MP 소모: 35",
                                    NamedTextColor.AQUA
                            ),

                            Component.text(
                                    "쿨타임: 20초",
                                    NamedTextColor.GRAY
                            ),

                            Component.text(
                                    "습득 레벨: Lv.50",
                                    NamedTextColor.DARK_GRAY
                            )
                    )
            );
        }


        /*
         * =====================================================
         * LEVEL 60 - WARRIOR BATTLE CRY
         * =====================================================
         */
        if (
                SkillIds.WARRIOR_BATTLE_CRY.equals(
                        skillId
                )
        ) {

            return createSkillBook(
                    skillId,
                    ownerUuid,

                    Component.text(
                            "[전사 스킬] 배틀 크라이",
                            NamedTextColor.GOLD
                    ),

                    List.of(
                            Component.empty(),

                            Component.text(
                                    "[슬롯 1~5 등록] 숫자 키로 사용",
                                    NamedTextColor.YELLOW
                            ),

                            Component.empty(),

                            Component.text(
                                    "강력한 함성으로 주변의 적을 자신에게 끌어당깁니다.",
                                    NamedTextColor.WHITE
                            ),

                            Component.text(
                                    "반경 10블록의 적에게 4초 동안 강한 이동 속도 감소를 부여합니다.",
                                    NamedTextColor.GRAY
                            ),

                            Component.text(
                                    "직접적인 피해는 주지 않습니다.",
                                    NamedTextColor.DARK_GRAY
                            ),

                            Component.empty(),

                            Component.text(
                                    "MP 소모: 18",
                                    NamedTextColor.AQUA
                            ),

                            Component.text(
                                    "쿨타임: 20초",
                                    NamedTextColor.GRAY
                            ),

                            Component.text(
                                    "습득 레벨: Lv.60",
                                    NamedTextColor.DARK_GRAY
                            )
                    )
            );
        }


        /*
         * =====================================================
         * LEVEL 60 - MAGE BLACK HOLE
         * =====================================================
         */
        if (
                SkillIds.MAGE_BLACK_HOLE.equals(
                        skillId
                )
        ) {

            return createSkillBook(
                    skillId,
                    ownerUuid,

                    Component.text(
                            "[마법사 스킬] 블랙홀",
                            NamedTextColor.LIGHT_PURPLE
                    ),

                    List.of(
                            Component.empty(),

                            Component.text(
                                    "[슬롯 1~5 등록] 숫자 키로 사용",
                                    NamedTextColor.YELLOW
                            ),

                            Component.empty(),

                            Component.text(
                                    "지정한 위치에 블랙홀을 생성하여 주변의 적을 끌어당깁니다.",
                                    NamedTextColor.WHITE
                            ),

                            Component.text(
                                    "총 10회 공격하며, 1회 피해: 18 + 최대 MP의 5%",
                                    NamedTextColor.AQUA
                            ),

                            Component.text(
                                    "블랙홀 범위 안의 적을 중심부로 지속적으로 끌어당깁니다.",
                                    NamedTextColor.GRAY
                            ),

                            Component.empty(),

                            Component.text(
                                    "MP 소모: 45",
                                    NamedTextColor.AQUA
                            ),

                            Component.text(
                                    "쿨타임: 25초",
                                    NamedTextColor.GRAY
                            ),

                            Component.text(
                                    "습득 레벨: Lv.60",
                                    NamedTextColor.DARK_GRAY
                            )
                    )
            );
        }


        /*
         * =====================================================
         * LEVEL 70 - WARRIOR BERSERKER RAGE
         * =====================================================
         */
        if (
                SkillIds.WARRIOR_BERSERKER_RAGE.equals(
                        skillId
                )
        ) {

            return createSkillBook(
                    skillId,
                    ownerUuid,

                    Component.text(
                            "[전사 스킬] 버서커 레이지",
                            NamedTextColor.GOLD
                    ),

                    List.of(
                            Component.empty(),

                            Component.text(
                                    "[슬롯 1~5 등록] 숫자 키로 사용",
                                    NamedTextColor.YELLOW
                            ),

                            Component.empty(),

                            Component.text(
                                    "10초 동안 광전사 상태가 됩니다.",
                                    NamedTextColor.WHITE
                            ),

                            Component.text(
                                    "공격력이 25%, 이동 속도가 20% 증가합니다.",
                                    NamedTextColor.GOLD
                            ),

                            Component.text(
                                    "효과 중 받는 피해가 10% 증가합니다.",
                                    NamedTextColor.RED
                            ),

                            Component.empty(),

                            Component.text(
                                    "MP 소모: 25",
                                    NamedTextColor.AQUA
                            ),

                            Component.text(
                                    "쿨타임: 45초",
                                    NamedTextColor.GRAY
                            ),

                            Component.text(
                                    "습득 레벨: Lv.70",
                                    NamedTextColor.DARK_GRAY
                            )
                    )
            );
        }


        /*
         * =====================================================
         * LEVEL 70 - MAGE MANA OVERLOAD
         * =====================================================
         */
        if (
                SkillIds.MAGE_MANA_OVERLOAD.equals(
                        skillId
                )
        ) {

            return createSkillBook(
                    skillId,
                    ownerUuid,

                    Component.text(
                            "[마법사 스킬] 마나 오버로드",
                            NamedTextColor.LIGHT_PURPLE
                    ),

                    List.of(
                            Component.empty(),

                            Component.text(
                                    "[슬롯 1~5 등록] 숫자 키로 사용",
                                    NamedTextColor.YELLOW
                            ),

                            Component.empty(),

                            Component.text(
                                    "10초 동안 마나의 힘을 폭발적으로 증폭시킵니다.",
                                    NamedTextColor.WHITE
                            ),

                            Component.text(
                                    "마법 스킬의 피해량이 30% 증가합니다.",
                                    NamedTextColor.LIGHT_PURPLE
                            ),

                            Component.text(
                                    "효과 중 다른 마법 스킬의 MP 소모량이 25% 증가합니다.",
                                    NamedTextColor.AQUA
                            ),

                            Component.empty(),

                            Component.text(
                                    "MP 소모: 60",
                                    NamedTextColor.AQUA
                            ),

                            Component.text(
                                    "쿨타임: 45초",
                                    NamedTextColor.GRAY
                            ),

                            Component.text(
                                    "습득 레벨: Lv.70",
                                    NamedTextColor.DARK_GRAY
                            )
                    )
            );
        }


        /*
         * =====================================================
         * LEVEL 80 - WARRIOR BLADE STORM
         * =====================================================
         */
        if (
                SkillIds.WARRIOR_BLADE_STORM.equals(
                        skillId
                )
        ) {

            return createSkillBook(
                    skillId,
                    ownerUuid,

                    Component.text(
                            "[전사 스킬] 블레이드 스톰",
                            NamedTextColor.GOLD
                    ),

                    List.of(
                            Component.empty(),

                            Component.text(
                                    "[슬롯 1~5 등록] 숫자 키로 사용",
                                    NamedTextColor.YELLOW
                            ),

                            Component.empty(),

                            Component.text(
                                    "검의 폭풍을 일으키며 주변의 적을 연속 공격합니다.",
                                    NamedTextColor.WHITE
                            ),

                            Component.text(
                                    "총 8회 공격하며, 1회당 총 공격력의 50% 피해를 줍니다.",
                                    NamedTextColor.GOLD
                            ),

                            Component.text(
                                    "공격 중에도 이동할 수 있습니다.",
                                    NamedTextColor.GRAY
                            ),

                            Component.empty(),

                            Component.text(
                                    "MP 소모: 35",
                                    NamedTextColor.AQUA
                            ),

                            Component.text(
                                    "쿨타임: 30초",
                                    NamedTextColor.GRAY
                            ),

                            Component.text(
                                    "습득 레벨: Lv.80",
                                    NamedTextColor.DARK_GRAY
                            )
                    )
            );
        }


        /*
         * =====================================================
         * LEVEL 80 - MAGE THUNDER STORM
         * =====================================================
         */
        if (
                SkillIds.MAGE_THUNDER_STORM.equals(
                        skillId
                )
        ) {

            return createSkillBook(
                    skillId,
                    ownerUuid,

                    Component.text(
                            "[마법사 스킬] 썬더 스톰",
                            NamedTextColor.LIGHT_PURPLE
                    ),

                    List.of(
                            Component.empty(),

                            Component.text(
                                    "[슬롯 1~5 등록] 숫자 키로 사용",
                                    NamedTextColor.YELLOW
                            ),

                            Component.empty(),

                            Component.text(
                                    "지정한 지역에 강력한 번개 폭풍을 생성합니다.",
                                    NamedTextColor.WHITE
                            ),

                            Component.text(
                                    "총 6회 공격하며, 1회 피해: 45 + 최대 MP의 10%",
                                    NamedTextColor.AQUA
                            ),

                            Component.text(
                                    "범위 안의 적에게 연속적인 번개 피해를 줍니다.",
                                    NamedTextColor.GRAY
                            ),

                            Component.empty(),

                            Component.text(
                                    "MP 소모: 70",
                                    NamedTextColor.AQUA
                            ),

                            Component.text(
                                    "쿨타임: 30초",
                                    NamedTextColor.GRAY
                            ),

                            Component.text(
                                    "습득 레벨: Lv.80",
                                    NamedTextColor.DARK_GRAY
                            )
                    )
            );
        }


        /*
         * =====================================================
         * REGISTRY SKILLS
         * =====================================================
         */

        SkillDefinition definition =
                SkillRegistry.find(
                        skillId
                );


        if (definition == null) {
            return null;
        }


        boolean warrior =
                definition.isClass(
                        "WARRIOR"
                );


        NamedTextColor titleColor =
                warrior
                        ? NamedTextColor.GOLD
                        : NamedTextColor.LIGHT_PURPLE;


        String classLabel =
                warrior
                        ? "[전사 스킬] "
                        : "[마법사 스킬] ";


        return createSkillBook(
                skillId,
                ownerUuid,

                Component.text(
                        classLabel
                                + definition.displayName(),
                        titleColor
                ),

                List.of(
                        Component.empty(),

                        Component.text(
                                "[핫바 1~5 장착] 숫자 키 / 스크롤로 사용",
                                NamedTextColor.YELLOW
                        ),

                        Component.empty(),

                        Component.text(
                                "스킬북 도감에서 관리되는 스킬입니다.",
                                NamedTextColor.WHITE
                        ),

                        Component.text(
                                "핫바 1~5번 칸에만 장착할 수 있습니다.",
                                NamedTextColor.GRAY
                        ),

                        Component.empty(),

                        Component.text(
                                "습득 레벨: Lv."
                                        + definition.requiredLevel(),
                                NamedTextColor.DARK_GRAY
                        ),

                        Component.text(
                                "귀속 스킬",
                                NamedTextColor.DARK_GRAY
                        )
                )
        );
    }


    /*
     * =========================================================
     * LEVEL 15 - WARRIOR DASH
     * =========================================================
     */
        /*
     * =========================================================
     * LEVEL 15 - MAGE TELEPORT
     * =========================================================
     */
        /*
     * =========================================================
     * LEVEL 90 - WARRIOR GATE OF BABYLON
     * =========================================================
     */
    public ItemStack createGateOfBabylonBook(
            UUID ownerUuid
    ) {

        return createSkillBook(
                SkillIds.WARRIOR_GATE_OF_BABYLON,
                ownerUuid,

                Component.text(
                        "[전사 스킬] 인피니트 블레이드",
                        NamedTextColor.GOLD
                ),

                List.of(
                        Component.empty(),

                        Component.text(
                                "[슬롯 1~5 등록] 숫자 키로 사용",
                                NamedTextColor.YELLOW
                        ),

                        Component.empty(),

                        Component.text(
                                "시점 방향으로 대량의 검기를 발사합니다.",
                                NamedTextColor.WHITE
                        ),

                        Component.text(
                                "일반 검기: 다단히트",
                                NamedTextColor.GRAY
                        ),

                        Component.text(
                                "마지막 대형 검기: 강력한 마무리 공격",
                                NamedTextColor.GOLD
                        ),

                        Component.empty(),

                        Component.text(
                                "MP 소모: 30",
                                NamedTextColor.AQUA
                        ),

                        Component.text(
                                "쿨타임: 45초",
                                NamedTextColor.GRAY
                        ),

                        Component.text(
                                "습득 레벨: Lv.90",
                                NamedTextColor.GOLD
                        )
                )
        );
    }


    /*
     * =========================================================
     * LEVEL 90 - MAGE METEOR
     * =========================================================
     */
    public ItemStack createMeteorBook(
            UUID ownerUuid
    ) {

        return createSkillBook(
                SkillIds.MAGE_METEOR,
                ownerUuid,

                Component.text(
                        "[마법사 스킬] 메테오 스트라이크",
                        NamedTextColor.RED
                ),

                List.of(
                        Component.empty(),

                        Component.text(
                                "[슬롯 1~5 등록] 숫자 키로 사용",
                                NamedTextColor.YELLOW
                        ),

                        Component.empty(),

                        Component.text(
                                "전방 광범위 지역에 거대한 운석을 소환합니다.",
                                NamedTextColor.WHITE
                        ),

                        Component.text(
                                "전체 폭격 범위: 반경 12블록",
                                NamedTextColor.GRAY
                        ),

                        Component.text(
                                "개별 착탄 범위: 반경 3블록",
                                NamedTextColor.GRAY
                        ),

                        Component.text(
                                "피해: 100 + 최대 MP의 40%",
                                NamedTextColor.RED
                        ),

                        Component.text(
                                "대상당 최대 3회 피격",
                                NamedTextColor.DARK_RED
                        ),

                        Component.empty(),

                        Component.text(
                                "MP 소모: 150",
                                NamedTextColor.AQUA
                        ),

                        Component.text(
                                "쿨타임: 45초",
                                NamedTextColor.GRAY
                        ),

                        Component.text(
                                "습득 레벨: Lv.90",
                                NamedTextColor.GOLD
                        )
                )
        );
    }
}
