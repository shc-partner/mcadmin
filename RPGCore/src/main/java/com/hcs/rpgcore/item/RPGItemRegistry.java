package com.hcs.rpgcore.item;

import com.hcs.rpgcore.RPGCorePlugin;

import com.hcs.rpgcore.dungeon.reward.DungeonRewardItemFactory;

import com.hcs.rpgcore.skill.SkillItemFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.entity.Player;

import org.bukkit.inventory.ItemStack;


/*
 * =========================================================
 * RPG ITEM REGISTRY
 * =========================================================
 *
 * RPGCore가 직접 생성하는 아이템을
 * 하나의 ID 체계로 관리한다.
 *
 * 관리자 명령:
 *
 * /rpgadmin item list
 * /rpgadmin item give <player> <id> [amount]
 *
 * 앞으로 신규 무기/방어구/던전 아이템이 생기면
 * register(...) 한 줄만 추가하여 확장한다.
 */
public final class RPGItemRegistry {

    private final Map<String, RegisteredItem>
            items =
                    new LinkedHashMap<>();


    private final DungeonRewardItemFactory
            dungeonRewardItemFactory;

    private final SkillItemFactory
            skillItemFactory;

    private final CustomItemFactory
            customItemFactory;

    private final RPGItemRepository
            itemRepository;


    public RPGItemRegistry(
            RPGCorePlugin plugin
    ) {

        this.dungeonRewardItemFactory =
                new DungeonRewardItemFactory(
                        plugin
                );

        this.skillItemFactory =
                new SkillItemFactory(
                        plugin
                );

        this.customItemFactory =
                new CustomItemFactory(
                        plugin
                );

        this.itemRepository =
                new RPGItemRepository(
                        plugin.getDatabaseManager()
                );


        reload();
    }


    /*
     * =========================================================
     * DEFAULT RPG ITEMS
     * =========================================================
     */
    /*
     * =========================================================
     * DATABASE RPG ITEM REGISTRY
     * =========================================================
     *
     * 일반 RPG 아이템은 MariaDB rpg_items에서 읽는다.
     *
     * 스킬북처럼 플레이어 UUID를 기반으로 별도 생성 로직이
     * 필요한 특수 아이템만 Java 코드에 유지한다.
     */
    public void reload() {

        items.clear();


        /*
         * CustomItemFactory도 동일한 DB 원본을 다시 읽는다.
         */
        customItemFactory.load();


        try {

            for (
                    RPGItemDefinition definition
                    : itemRepository.findAllEnabled()
            ) {

                final String itemId =
                        definition.itemId();


                register(
                        itemId,
                        definition.displayName(),
                        definition.bound(),
                        player ->
                                customItemFactory.create(
                                        itemId
                                )
                );
            }

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Failed to load RPG item registry from MariaDB",
                    exception
            );
        }


        /*
         * =====================================================
         * SPECIAL JAVA-CREATED ITEMS
         * =====================================================
         *
         * 이 아이템들은 일반 CustomItemFactory 아이템과 달리
         * 지급 대상 플레이어 UUID를 사용하므로 Java에 유지한다.
         */

        register(
                "warrior_bash_book",
                "전사 스킬북 - 배쉬",
                true,
                player ->
                        skillItemFactory.createBashBook(
                                player.getUniqueId()
                        )
        );


        register(
                "mage_fire_bolt_book",
                "마법사 스킬북 - 파이어 볼트",
                true,
                player ->
                        skillItemFactory.createFireBoltBook(
                                player.getUniqueId()
                        )
        );
    }


    /*
     * =========================================================
     * REGISTER
     * =========================================================
     */
    private void register(
            String id,
            String displayName,
            boolean bound,
            ItemCreator creator
    ) {

        String normalizedId =
                normalize(
                        id
                );


        if (
                normalizedId == null
                || normalizedId.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "RPG item id cannot be empty"
            );
        }


        if (
                items.containsKey(
                        normalizedId
                )
        ) {

            throw new IllegalStateException(
                    "Duplicate RPG item id: "
                            + normalizedId
            );
        }


        items.put(
                normalizedId,
                new RegisteredItem(
                        normalizedId,
                        displayName,
                        bound,
                        creator
                )
        );
    }


    /*
     * =========================================================
     * EXISTS
     * =========================================================
     */
    public boolean contains(
            String id
    ) {

        String normalized =
                normalize(
                        id
                );


        return normalized != null
                && items.containsKey(
                        normalized
                );
    }


    /*
     * =========================================================
     * CREATE
     * =========================================================
     *
     * amount가 2 이상이면 RPG 아이템을
     * 각각 독립된 ItemStack으로 생성한다.
     *
     * PDC / Lore / 귀속 UUID가 각 아이템마다
     * 실제 팩토리를 통해 생성되므로 테스트용
     * 바닐라 /give와 다르다.
     */
    public List<ItemStack> create(
            String id,
            Player target,
            int amount
    ) {

        if (target == null) {

            throw new IllegalArgumentException(
                    "target cannot be null"
            );
        }


        if (
                amount < 1
                || amount > 64
        ) {

            throw new IllegalArgumentException(
                    "amount must be between 1 and 64"
            );
        }


        RegisteredItem registered =
                items.get(
                        normalize(
                                id
                        )
                );


        if (registered == null) {

            return List.of();
        }


        List<ItemStack> result =
                new ArrayList<>(
                        amount
                );


        for (
                int i = 0;
                i < amount;
                i++
        ) {

            ItemStack item =
                    registered.creator()
                            .create(
                                    target
                            );


            if (
                    item == null
                    || item.getType().isAir()
            ) {

                throw new IllegalStateException(
                        "RPG item factory returned empty item: "
                                + registered.id()
                );
            }


            result.add(
                    item
            );
        }


        return result;
    }


    /*
     * =========================================================
     * LIST
     * =========================================================
     */
    public Collection<RegisteredItemInfo>
    list() {

        return items.values()
                .stream()
                .map(
                        item ->
                                new RegisteredItemInfo(
                                        item.id(),
                                        item.displayName(),
                                        item.bound()
                                )
                )
                .toList();
    }


    private String normalize(
            String id
    ) {

        if (id == null) {
            return null;
        }


        return id.trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }


    /*
     * 외부에서는 생성 로직 자체를
     * 직접 만지지 않도록 정보만 노출.
     */
    public record RegisteredItemInfo(
            String id,
            String displayName,
            boolean bound
    ) {
    }


    private record RegisteredItem(
            String id,
            String displayName,
            boolean bound,
            ItemCreator creator
    ) {
    }


    @FunctionalInterface
    private interface ItemCreator {

        ItemStack create(
                Player target
        );
    }
}
