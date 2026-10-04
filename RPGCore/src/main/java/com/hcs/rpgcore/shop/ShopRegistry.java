package com.hcs.rpgcore.shop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * RPGCore 전체 상점 등록소.
 *
 * NPC와 직접 결합하지 않고 shopId만 관리한다.
 */
public final class ShopRegistry {

    public static final String FARM_BUY =
            "farm_buy";

    public static final String FARM_SELL =
            "farm_sell";

    public static final String FARM_FERTILIZER =
            "farm_fertilizer";

    public static final String FARM_TOOLS =
            "farm_tools";

    public static final String EQUIPMENT_MATERIALS =
            "equipment_materials";


    public static final String ENCHANT_WEAPON =
            "enchant_weapon";

    public static final String ENCHANT_ARMOR =
            "enchant_armor";

    public static final String ENCHANT_TOOL =
            "enchant_tool";

    public static final String ENCHANT_COMMON =
            "enchant_common";

    public static final String FARM_MARKET =
            "farm";


    private final Map<String, ShopDefinition>
            shops =
            new LinkedHashMap<>();


    public ShopRegistry() {
        registerBuiltInShops();
    }


    public void register(
            ShopDefinition definition
    ) {

        if (
                shops.putIfAbsent(
                        definition.shopId(),
                        definition
                ) != null
        ) {

            throw new IllegalArgumentException(
                    "Duplicate shop ID: "
                            + definition.shopId()
            );
        }
    }


    public ShopDefinition get(
            String shopId
    ) {

        return shops.get(
                shopId
        );
    }


    public ShopDefinition require(
            String shopId
    ) {

        ShopDefinition definition =
                get(
                        shopId
                );


        if (definition == null) {

            throw new IllegalArgumentException(
                    "Unknown shop ID: "
                            + shopId
            );
        }


        return definition;
    }


    public Collection<ShopDefinition> all() {

        return List.copyOf(
                shops.values()
        );
    }


    private void registerBuiltInShops() {

        registerFarmBuy();
        registerFarmSell();
        registerFarmFertilizer();
        registerFarmTools();
        registerEquipmentMaterials();
        registerEnchantWeapon();
        registerEnchantArmor();
        registerEnchantTool();
        registerEnchantCommon();
    }


    /*
     * =========================================================
     * 농작물 구매
     * =========================================================
     *
     * 씨앗만 판매한다.
     * 전 품목 개당 10원.
     */
    private void registerFarmBuy() {

        List<ShopItemDefinition> items =
                List.of(

                        seed(
                                "tomato",
                                "토마토 씨앗",
                                0
                        ),

                        seed(
                                "cabbage",
                                "양배추 씨앗",
                                1
                        ),

                        seed(
                                "chinese_cabbage",
                                "배추 씨앗",
                                2
                        ),

                        seed(
                                "corn",
                                "옥수수 씨앗",
                                3
                        ),

                        seed(
                                "eggplant",
                                "가지 씨앗",
                                4
                        ),

                        seed(
                                "garlic",
                                "마늘 씨앗",
                                5
                        ),

                        seed(
                                "grape",
                                "포도 씨앗",
                                6
                        ),

                        seed(
                                "hop",
                                "홉 씨앗",
                                7
                        ),

                        seed(
                                "pepper",
                                "고추 씨앗",
                                8
                        ),

                        seed(
                                "pineapple",
                                "파인애플 씨앗",
                                9
                        ),

                        seed(
                                "pitaya",
                                "용과 씨앗",
                                10
                        ),

                        seed(
                                "redpacket",
                                "홍봉나무 씨앗",
                                11
                        )
                );


        register(
                new ShopDefinition(
                        FARM_BUY,
                        "농작물 구매",
                        ShopType.BUY,
                        2,
                        items
                )
        );
    }


    private ShopItemDefinition seed(
            String id,
            String displayName,
            int slot
    ) {

        return ShopItemDefinition.fixedBuy(
                id + "_seeds",
                displayName,
                ShopItemSource.CRAFTENGINE,
                "customcrops:"
                        + id
                        + "_seeds",
                slot,
                1,
                10
        );
    }


    /*
     * =========================================================
     * 농작물 판매
     * =========================================================
     *
     * 일반       × 1.0
     * Silver     × 1.5
     * Golden     × 2.5
     *
     * redpacket은 현재 Silver/Golden 아이템이 없으므로
     * 일반 품질만 등록한다.
     */
    private void registerFarmSell() {

        List<ShopItemDefinition> items =
                new ArrayList<>();


        String[][] crops = {

                {
                        "tomato",
                        "토마토"
                },

                {
                        "cabbage",
                        "양배추"
                },

                {
                        "chinese_cabbage",
                        "배추"
                },

                {
                        "corn",
                        "옥수수"
                },

                {
                        "eggplant",
                        "가지"
                },

                {
                        "garlic",
                        "마늘"
                },

                {
                        "grape",
                        "포도"
                },

                {
                        "hop",
                        "홉"
                },

                {
                        "pepper",
                        "고추"
                },

                {
                        "pineapple",
                        "파인애플"
                },

                {
                        "pitaya",
                        "용과"
                },

                {
                        "redpacket",
                        "홍봉나무 열매"
                }
        };


        /*
         * 일반 품질
         * slot 0 ~ 11
         */
        for (
                int i = 0;
                i < crops.length;
                i++
        ) {

            String id =
                    crops[i][0];

            String name =
                    crops[i][1];


            items.add(
                    marketCrop(
                            id,
                            name,
                            "customcrops:" + id,
                            i,
                            id,
                            1.0D
                    )
            );
        }


        /*
         * 고급 품질
         * redpacket 제외
         * slot 12 ~ 22
         */
        for (
                int i = 0;
                i < 11;
                i++
        ) {

            String id =
                    crops[i][0];

            String name =
                    crops[i][1];


            items.add(
                    marketCrop(
                            id + "_silver_star",
                            name + " [고급]",
                            "customcrops:"
                                    + id
                                    + "_silver_star",
                            12 + i,
                            id,
                            1.5D
                    )
            );
        }


        /*
         * 최상급 품질
         * redpacket 제외
         * slot 23 ~ 33
         */
        for (
                int i = 0;
                i < 11;
                i++
        ) {

            String id =
                    crops[i][0];

            String name =
                    crops[i][1];


            items.add(
                    marketCrop(
                            id + "_golden_star",
                            name + " [최상급]",
                            "customcrops:"
                                    + id
                                    + "_golden_star",
                            23 + i,
                            id,
                            2.5D
                    )
            );
        }


        register(
                new ShopDefinition(
                        FARM_SELL,
                        "농작물 판매",
                        ShopType.SELL,
                        4,
                        items
                )
        );
    }


    private ShopItemDefinition marketCrop(
            String entryId,
            String displayName,
            String itemId,
            int slot,
            String marketItemId,
            double multiplier
    ) {

        return ShopItemDefinition.marketSell(
                entryId,
                displayName,
                ShopItemSource.CRAFTENGINE,
                itemId,
                slot,
                1,
                FARM_MARKET,
                marketItemId,
                multiplier
        );
    }


    /*
     * =========================================================
     * 농사 비료 구매
     * =========================================================
     */
    private void registerFarmFertilizer() {

        List<ShopItemDefinition> items =
                List.of(

                        buy(
                                "quality_1",
                                "품질 증가 비료 I",
                                0,
                                150
                        ),

                        buy(
                                "quality_2",
                                "품질 증가 비료 II",
                                1,
                                400
                        ),

                        buy(
                                "quality_3",
                                "품질 증가 비료 III",
                                2,
                                1000
                        ),


                        buy(
                                "yield_increase_1",
                                "수확량 증가 비료 I",
                                3,
                                200
                        ),

                        buy(
                                "yield_increase_2",
                                "수확량 증가 비료 II",
                                4,
                                550
                        ),

                        buy(
                                "yield_increase_3",
                                "수확량 증가 비료 III",
                                5,
                                1400
                        ),


                        buy(
                                "speed_grow_1",
                                "성장 촉진 비료 I",
                                6,
                                120
                        ),

                        buy(
                                "speed_grow_2",
                                "성장 촉진 비료 II",
                                7,
                                350
                        ),

                        buy(
                                "speed_grow_3",
                                "성장 촉진 비료 III",
                                8,
                                900
                        ),


                        buy(
                                "soil_retain_1",
                                "토양 수분 유지 비료 I",
                                9,
                                100
                        ),

                        buy(
                                "soil_retain_2",
                                "토양 수분 유지 비료 II",
                                10,
                                300
                        ),

                        buy(
                                "soil_retain_3",
                                "토양 수분 유지 비료 III",
                                11,
                                750
                        ),


                        buy(
                                "variation_1",
                                "변이 촉진 비료 I",
                                12,
                                250
                        ),

                        buy(
                                "variation_2",
                                "변이 촉진 비료 II",
                                13,
                                700
                        ),

                        buy(
                                "variation_3",
                                "변이 촉진 비료 III",
                                14,
                                1800
                        )
                );


        register(
                new ShopDefinition(
                        FARM_FERTILIZER,
                        "농사 비료 구매",
                        ShopType.BUY,
                        2,
                        items
                )
        );
    }


    /*
     * =========================================================
     * 농사 도구 구매
     * =========================================================
     */
    private void registerFarmTools() {

        List<ShopItemDefinition> items =
                List.of(

                        buy(
                                "dry_pot",
                                "재배용 토양",
                                0,
                                25
                        ),


                        buy(
                                "watering_can_1",
                                "하급 물뿌리개",
                                1,
                                500
                        ),

                        buy(
                                "watering_can_2",
                                "중급 물뿌리개",
                                2,
                                1000
                        ),

                        buy(
                                "watering_can_3",
                                "상급 물뿌리개",
                                3,
                                2500
                        ),

                        buy(
                                "watering_can_4",
                                "최상급 물뿌리개",
                                4,
                                5000
                        ),


                        buy(
                                "sprinkler_1_item",
                                "스프링클러 I",
                                5,
                                2000
                        ),

                        buy(
                                "sprinkler_2_item",
                                "스프링클러 II",
                                6,
                                5000
                        ),

                        buy(
                                "sprinkler_3_item",
                                "스프링클러 III",
                                7,
                                12000
                        ),


                        buy(
                                "scarecrow",
                                "허수아비",
                                8,
                                4000
                        ),

                        buy(
                                "greenhouse_glass",
                                "온실 유리",
                                9,
                                200
                        ),

                        buy(
                                "soil_surveyor",
                                "토양 조사기",
                                10,
                                1500
                        )
                );


        register(
                new ShopDefinition(
                        FARM_TOOLS,
                        "농사 도구 구매",
                        ShopType.BUY,
                        2,
                        items
                )
        );
    }



    /*
     * =========================================================
     * 장비 재료 구매
     * =========================================================
     *
     * NPC 11
     *
     * RPGCore DB 아이템 11종.
     */
    private void registerEquipmentMaterials() {

        List<ShopItemDefinition> items =
                List.of(

                        rpgBuy(
                                "hero_weapon_recipe",
                                "영웅 무기 제작서",
                                0,
                                50000
                        ),

                        rpgBuy(
                                "legendary_weapon_recipe",
                                "전설 무기 제작서",
                                1,
                                250000
                        ),

                        rpgBuy(
                                "mythic_weapon_recipe",
                                "신화 무기 제작서",
                                2,
                                1500000
                        ),

                        rpgBuy(
                                "hero_armor_recipe",
                                "영웅 방어구 제작서",
                                3,
                                25000
                        ),

                        rpgBuy(
                                "legendary_armor_recipe",
                                "전설 방어구 제작서",
                                4,
                                150000
                        ),

                        rpgBuy(
                                "mythic_armor_recipe",
                                "신화 방어구 제작서",
                                5,
                                700000
                        ),

                        rpgBuy(
                                "enhancement_stone",
                                "강화석",
                                6,
                                2000
                        ),

                        rpgBuy(
                                "better_enhancement_stone",
                                "상급 강화석",
                                7,
                                20000
                        ),

                        rpgBuy(
                                "hero_ingot",
                                "영웅 장비 주괴",
                                8,
                                100000
                        ),

                        rpgBuy(
                                "legendary_ingot",
                                "전설 장비 주괴",
                                9,
                                1000000
                        ),

                        rpgBuy(
                                "mythic_ingot",
                                "신화 장비 주괴",
                                10,
                                5000000
                        )
                );


        register(
                new ShopDefinition(
                        EQUIPMENT_MATERIALS,
                        "장비 재료 구매",
                        ShopType.BUY,
                        2,
                        items
                )
        );
    }


    /*
     * RPGCore DB 아이템 구매.
     *
     * 기존 CraftEngine 전용 buy()와 분리한다.
     */
    private ShopItemDefinition rpgBuy(
            String itemId,
            String displayName,
            int slot,
            int price
    ) {

        return ShopItemDefinition.fixedBuy(
                itemId,
                displayName,
                ShopItemSource.RPGCORE,
                itemId,
                slot,
                1,
                price
        );
    }


    private ShopItemDefinition buy(
            String itemId,
            String displayName,
            int slot,
            int price
    ) {

        return ShopItemDefinition.fixedBuy(
                itemId,
                displayName,
                ShopItemSource.CRAFTENGINE,
                "customcrops:" + itemId,
                slot,
                1,
                price
        );
    }


    /*
     * =========================================================
     * NPC 19 - 인챈트북 상점
     * =========================================================
     *
     * 가격은 테스트용 초기값이다.
     * 정식 배포 전 가격 확정 필요.
     *
     * 슬롯 22는 뒤로가기 버튼으로 예약한다.
     */

    private void registerEnchantWeapon() {

        register(
                new ShopDefinition(
                        ENCHANT_WEAPON,
                        "무기 인챈트북 구매",
                        ShopType.BUY,
                        3,
                        List.of(
                                enchantBook(
                                        "sharpness_5",
                                        "날카로움 V",
                                        "minecraft:sharpness",
                                        5,
                                        10,
                                        10000
                                ),
                                enchantBook(
                                        "looting_3",
                                        "약탈 III",
                                        "minecraft:looting",
                                        3,
                                        12,
                                        12000
                                ),
                                enchantBook(
                                        "fire_aspect_2",
                                        "발화 II",
                                        "minecraft:fire_aspect",
                                        2,
                                        14,
                                        8000
                                )
                        )
                )
        );
    }

    private void registerEnchantArmor() {

        register(
                new ShopDefinition(
                        ENCHANT_ARMOR,
                        "방어구 인챈트북 구매",
                        ShopType.BUY,
                        3,
                        List.of(
                                enchantBook(
                                        "protection_4",
                                        "보호 IV",
                                        "minecraft:protection",
                                        4,
                                        10,
                                        10000
                                ),
                                enchantBook(
                                        "feather_falling_4",
                                        "가벼운 착지 IV",
                                        "minecraft:feather_falling",
                                        4,
                                        12,
                                        9000
                                ),
                                enchantBook(
                                        "respiration_3",
                                        "호흡 III",
                                        "minecraft:respiration",
                                        3,
                                        14,
                                        7000
                                )
                        )
                )
        );
    }

    private void registerEnchantTool() {

        register(
                new ShopDefinition(
                        ENCHANT_TOOL,
                        "도구 인챈트북 구매",
                        ShopType.BUY,
                        3,
                        List.of(
                                enchantBook(
                                        "efficiency_5",
                                        "효율 V",
                                        "minecraft:efficiency",
                                        5,
                                        10,
                                        12000
                                ),
                                enchantBook(
                                        "fortune_3",
                                        "행운 III",
                                        "minecraft:fortune",
                                        3,
                                        11,
                                        14000
                                ),
                                enchantBook(
                                        "silk_touch_1",
                                        "섬세한 손길",
                                        "minecraft:silk_touch",
                                        1,
                                        12,
                                        12000
                                ),
                                enchantBook(
                                        "forest_lumberjack_1",
                                        "숲의 나무꾼 I",
                                        "rpgcore:forest_lumberjack",
                                        1,
                                        14,
                                        6000
                                ),
                                enchantBook(
                                        "forest_lumberjack_2",
                                        "숲의 나무꾼 II",
                                        "rpgcore:forest_lumberjack",
                                        2,
                                        15,
                                        12000
                                ),
                                enchantBook(
                                        "forest_lumberjack_3",
                                        "숲의 나무꾼 III",
                                        "rpgcore:forest_lumberjack",
                                        3,
                                        16,
                                        24000
                                )
                        )
                )
        );
    }

    private void registerEnchantCommon() {

        register(
                new ShopDefinition(
                        ENCHANT_COMMON,
                        "공통·특수 인챈트북 구매",
                        ShopType.BUY,
                        3,
                        List.of(
                                enchantBook(
                                        "mending_1",
                                        "수선",
                                        "minecraft:mending",
                                        1,
                                        11,
                                        20000
                                ),
                                enchantBook(
                                        "unbreaking_3",
                                        "내구성 III",
                                        "minecraft:unbreaking",
                                        3,
                                        15,
                                        10000
                                )
                        )
                )
        );
    }

    private ShopItemDefinition enchantBook(
            String entryId,
            String displayName,
            String enchantmentId,
            int level,
            int slot,
            int price
    ) {

        return ShopItemDefinition.fixedBuy(
                entryId,
                displayName,
                ShopItemSource.ENCHANT_BOOK,
                enchantmentId + "@" + level,
                slot,
                1,
                price
        );
    }
}
