package com.hcs.rpgcore.dungeon.reward;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.level.MobKillListener;
import com.hcs.rpgcore.storage.StorageChestService;
import com.hcs.rpgcore.storage.StorageChestRepository;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import java.util.concurrent.ThreadLocalRandom;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class DungeonRewardService {


    private static final long CLEAR_EXP =
            500L;

    private static final double UNDEAD_KNIGHT_DROP_CHANCE =
            0.10D;

    private static final double NETHER_LORD_DROP_CHANCE =
            0.08D;


    /*
     * =========================================================
     * NETHER FORTRESS SPECIAL WEAPON
     * =========================================================
     *
     * 네더 요새 클리어마다 한 번만 판정한다.
     *
     * 전체 획득 확률: 10%
     * 성공 시 공용 무기 4종 중 무작위 1개 지급.
     * 직업 제한 없음.
     */
    private static final double
            NETHER_FORTRESS_SPECIAL_WEAPON_CHANCE =
                    0.10D;

    private final RPGCorePlugin plugin;

    private final PendingRewardRepository
            pendingRewardRepository;

    private final DungeonRewardItemFactory
            itemFactory;

    private final MobKillListener
            experienceAwarder;


    private final StorageChestService
            storageChestService;

    public DungeonRewardService(
            RPGCorePlugin plugin,
            PendingRewardRepository pendingRewardRepository,
            DungeonRewardItemFactory itemFactory,
            MobKillListener experienceAwarder,
            StorageChestService storageChestService
    ) {

        this.plugin = plugin;

        this.pendingRewardRepository =
                pendingRewardRepository;

        this.itemFactory =
                itemFactory;

        this.experienceAwarder =
                experienceAwarder;


        this.storageChestService =
                storageChestService;
    }

    /*
     * =========================================================
     * ENHANCEMENT STONE REWARD
     * =========================================================
     *
     * baseAmount:
     *   확정 지급 수량.
     *
     * bonusChance / bonusAmount:
     *   추가 확률 지급.
     *
     * randomExtraMax:
     *   0 ~ N개 랜덤 추가 지급.
     *
     * 예:
     *
     * Zombie:
     * 0, 0.20, 1, 0
     *
     * Nether:
     * 1, 0.30, 1, 0
     *
     * Red Dragon:
     * 4, 0.00, 0, 2
     */

    /*
     * =========================================================
     * INDEPENDENT EQUIPMENT INGOT REWARDS
     * =========================================================
     *
     * 영웅 / 전설 / 신화 주괴를 각각 독립적으로 추첨한다.
     * 한 번의 클리어에서 3종 모두 획득할 수 있다.
     */
    public void rewardEquipmentIngots(
            Set<UUID> participants,
            double heroChance,
            double legendaryChance,
            double mythicChance
    ) {

        if (participants == null || participants.isEmpty()) {
            return;
        }

        double[] chances = {
                heroChance,
                legendaryChance,
                mythicChance
        };

        String[] itemIds = {
                "hero_ingot",
                "legendary_ingot",
                "mythic_ingot"
        };

        String[] names = {
                "영웅 장비 주괴",
                "전설 장비 주괴",
                "신화 장비 주괴"
        };

        for (double chance : chances) {

            if (chance < 0.0D || chance > 1.0D) {
                throw new IllegalArgumentException(
                        "Equipment ingot chance must be 0.0 ~ 1.0"
                );
            }
        }

        for (UUID uuid : participants) {

            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) {
                continue;
            }

            for (int i = 0; i < itemIds.length; i++) {

                if (
                        ThreadLocalRandom.current()
                                .nextDouble() >= chances[i]
                ) {
                    continue;
                }

                ItemStack item =
                        itemFactory.createCustomItem(itemIds[i]);

                if (item == null || item.getType().isAir()) {

                    plugin.getLogger().severe(
                            "[DungeonReward] Failed to create ingot: "
                                    + itemIds[i]
                                    + " player=" + player.getName()
                    );

                    continue;
                }

                item.setAmount(1);

                Map<Integer, ItemStack> leftovers =
                        player.getInventory().addItem(item);

                for (ItemStack leftover : leftovers.values()) {

                    player.getWorld().dropItemNaturally(
                            player.getLocation(),
                            leftover
                    );
                }

                player.sendMessage(
                        Component.text(
                                "[던전 보상] ",
                                NamedTextColor.GOLD
                        ).append(
                                Component.text(
                                        names[i] + " x1",
                                        NamedTextColor.GREEN
                                )
                        )
                );
            }
        }
    }




    /*
     * =========================================================
     * INDEPENDENT TEYVAT RARE WEAPON REWARDS
     * =========================================================
     *
     * 참가자별로 무기 4종을 각각 5% 확률로 독립 추첨한다.
     * 한 번의 클리어에서 여러 종류를 획득할 수 있다.
     */
    public void rewardTeyvatRareWeapons(
            Set<UUID> participants
    ) {

        if (participants == null || participants.isEmpty()) {
            return;
        }

        String[] itemIds = {
                "teyvat_freedom_sworn",
                "teyvat_jadefalls_splendor",
                "teyvat_lumidouce_elegy",
                "teyvat_skyward_spine"
        };

        String[] names = {
                "푸른 파도의 검",
                "녹색의 마도구",
                "창백한 스태프",
                "옅은 청금의 창"
        };

        for (UUID uuid : participants) {

            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) {
                continue;
            }

            for (int i = 0; i < itemIds.length; i++) {

                if (
                        ThreadLocalRandom.current()
                                .nextDouble() >= 0.05D
                ) {
                    continue;
                }

                ItemStack item =
                        itemFactory.createCustomItem(itemIds[i]);

                if (item == null || item.getType().isAir()) {

                    plugin.getLogger().severe(
                            "[DungeonReward] Failed to create rare weapon: "
                                    + itemIds[i]
                                    + " player=" + player.getName()
                    );

                    continue;
                }

                item.setAmount(1);

                Map<Integer, ItemStack> leftovers =
                        player.getInventory().addItem(item);

                for (ItemStack leftover : leftovers.values()) {

                    player.getWorld().dropItemNaturally(
                            player.getLocation(),
                            leftover
                    );
                }

                player.sendMessage(
                        Component.text(
                                "[던전 보상] ",
                                NamedTextColor.GOLD
                        ).append(
                                Component.text(
                                        names[i] + " x1",
                                        NamedTextColor.GREEN
                                )
                        )
                );
            }
        }
    }

    /*
     * =========================================================
     * DUNGEON GOLD REWARD
     * =========================================================
     *
     * 클리어 참가자별 확정 골드 지급.
     * 확정 입금 성공 후 20% 확률로 추가 골드를 지급한다.
     * 기존 EXP / 재료 / 강화석 / 주괴 보상과 독립적이다.
     */
    public void rewardDungeonGold(
            Set<UUID> participants,
            long baseGold,
            long bonusGold,
            double bonusChance
    ) {

        if (participants == null || participants.isEmpty()) {
            return;
        }

        if (
                baseGold < 0L
                        || bonusGold < 0L
                        || bonusChance < 0.0D
                        || bonusChance > 1.0D
        ) {
            throw new IllegalArgumentException(
                    "Invalid dungeon gold reward configuration."
            );
        }

        RegisteredServiceProvider<Economy> registration =
                plugin.getServer()
                        .getServicesManager()
                        .getRegistration(Economy.class);

        if (
                registration == null
                        || registration.getProvider() == null
        ) {

            plugin.getLogger().severe(
                    "[DungeonReward] Vault Economy provider 없음. "
                            + "던전 골드 지급을 건너뜁니다."
            );

            return;
        }

        Economy economy = registration.getProvider();

        for (UUID uuid : participants) {

            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) {
                continue;
            }

            if (
                    baseGold > 0L
                            && !depositDungeonGold(
                                    economy,
                                    player,
                                    baseGold
                            )
            ) {
                continue;
            }

            if (baseGold > 0L) {

                player.sendMessage(
                        Component.text(
                                "[던전 보상] ",
                                NamedTextColor.GOLD
                        ).append(
                                Component.text(
                                        "골드 +" + baseGold + "G",
                                        NamedTextColor.YELLOW
                                )
                        )
                );
            }

            if (
                    bonusGold <= 0L
                            || ThreadLocalRandom.current()
                                    .nextDouble() >= bonusChance
            ) {
                continue;
            }

            if (
                    !depositDungeonGold(
                            economy,
                            player,
                            bonusGold
                    )
            ) {
                continue;
            }

            player.sendMessage(
                    Component.text(
                            "[던전 보상] ",
                            NamedTextColor.GOLD
                    ).append(
                            Component.text(
                                    "추가 골드 +" + bonusGold + "G",
                                    NamedTextColor.GREEN
                            )
                    )
            );
        }
    }


    private boolean depositDungeonGold(
            Economy economy,
            Player player,
            long amount
    ) {

        try {

            EconomyResponse response =
                    economy.depositPlayer(
                            player,
                            (double) amount
                    );

            if (!response.transactionSuccess()) {

                plugin.getLogger().severe(
                        "[DungeonReward] 골드 입금 실패: "
                                + player.getName()
                                + " amount=" + amount
                                + " error=" + response.errorMessage
                );

                return false;
            }

            return true;

        } catch (RuntimeException exception) {

            plugin.getLogger().severe(
                    "[DungeonReward] 골드 입금 예외: "
                            + player.getName()
                            + " amount=" + amount
                            + " error=" + exception.getMessage()
            );

            return false;
        }
    }


    public void rewardEnhancementStones(
            Set<UUID> participants,
            int baseAmount,
            double bonusChance,
            int bonusAmount,
            int randomExtraMax
    ) {

        if (
                participants == null
                || participants.isEmpty()
        ) {
            return;
        }


        for (UUID uuid : participants) {

            int amount =
                    Math.max(
                            0,
                            baseAmount
                    );


            if (
                    bonusAmount > 0
                    && bonusChance > 0.0D
                    && ThreadLocalRandom.current()
                            .nextDouble()
                            < bonusChance
            ) {

                amount +=
                        bonusAmount;
            }


            if (randomExtraMax > 0) {

                amount +=
                        ThreadLocalRandom.current()
                                .nextInt(
                                        randomExtraMax + 1
                                );
            }


            if (amount <= 0) {
                continue;
            }


            Player player =
                    Bukkit.getPlayer(
                            uuid
                    );


            if (
                    player == null
                    || !player.isOnline()
            ) {
                continue;
            }


            ItemStack item =
                    itemFactory.createCustomItem(
                            "enhancement_stone"
                    );


            if (
                    item == null
                    || item.getType().isAir()
            ) {

                plugin.getLogger()
                        .severe(
                                "[DungeonReward] "
                                        + "Failed to create enhancement_stone."
                        );

                continue;
            }


            item.setAmount(
                    amount
            );


            Map<Integer, ItemStack> leftovers =
                    player.getInventory()
                            .addItem(
                                    item
                            );


            /*
             * 강화석은 일반 던전 보관함 rewardType이 아니므로
             * 남은 수량은 플레이어 위치에 직접 드랍한다.
             */
            for (
                    ItemStack leftover
                    : leftovers.values()
            ) {

                player.getWorld()
                        .dropItemNaturally(
                                player.getLocation(),
                                leftover
                        );
            }


            player.sendMessage(
                    Component.text(
                            "[던전 보상] ",
                            NamedTextColor.GOLD
                    ).append(
                            Component.text(
                                    "강화석 x"
                                            + amount,
                                    NamedTextColor.GREEN
                            )
                    )
            );
        }
    }


    /*
     * =========================================================
     * DUNGEON CLEAR EXP ONLY
     * =========================================================
     *
     * 특정 던전이 자체 클리어 EXP만 지급할 때 사용한다.
     *
     * 기존 ZombieDungeon의 아이템 보상 로직에는
     * 영향을 주지 않는다.
     */
    public void rewardDungeonClearExperience(
            Set<UUID> participants,
            long experience
    ) {

        if (
                participants == null
                || participants.isEmpty()
                || experience <= 0L
        ) {
            return;
        }

        for (UUID uuid : participants) {

            experienceAwarder.awardExperience(
                    uuid,
                    experience
            );

            Player player =
                    Bukkit.getPlayer(uuid);

            if (
                    player == null
                    || !player.isOnline()
            ) {
                continue;
            }

            player.sendMessage(
                    Component.text(
                            "[던전 보상] ",
                            NamedTextColor.GOLD
                    ).append(
                            Component.text(
                                    "RPG EXP +"
                                            + experience,
                                    NamedTextColor.YELLOW
                            )
                    )
            );
        }
    }


    /*
     * =========================================================
     * ANCIENT DEPTHS EQUIPMENT REWARD
     * =========================================================
     *
     * Lv.40~50 고대 심층 클리어 시
     * 이름없는 기사 계열 장비 및 마도구 6종 중 1종을 확정 지급한다.
     *
     * 각 아이템의 선택 확률은 동일하다. (각 약 16.67%)
     */
    public void rewardAncientDepthsEquipment(
            Set<UUID> participants
    ) {

        if (
                participants == null
                || participants.isEmpty()
        ) {
            return;
        }

        for (UUID uuid : participants) {

            rewardAncientDepthsEquipmentPlayer(
                    uuid
            );
        }
    }


    private void rewardAncientDepthsEquipmentPlayer(
            UUID uuid
    ) {

        String[] rewardTypes = {
                DungeonRewardItemFactory.FACELESS_GOD_HELMET,
                DungeonRewardItemFactory.FACELESS_GOD_CHESTPLATE,
                DungeonRewardItemFactory.FACELESS_GOD_LEGGINGS,
                DungeonRewardItemFactory.FACELESS_GOD_BOOTS,
                DungeonRewardItemFactory.FACELESS_GOD_SPEAR,
                DungeonRewardItemFactory.GREEN_MAGIC_CATALYST
        };

        String rewardType;

        /*
         * 푸른 파도의 검은 고대 심층의 희귀 보상.
         *
         * 5%  : 푸른 파도의 검
         * 95% : 기존 장비 6종 중 균등 선택
         */
        if (
                ThreadLocalRandom.current()
                        .nextDouble()
                        < 0.05D
        ) {

            rewardType =
                    DungeonRewardItemFactory
                            .BLUE_WAVE_SWORD;

        } else {

            rewardType =
                    rewardTypes[
                            ThreadLocalRandom.current()
                                    .nextInt(
                                            rewardTypes.length
                                    )
                    ];
        }

        Player player =
                Bukkit.getPlayer(
                        uuid
                );

        boolean pendingStored =
                false;

        try {

            if (
                    player == null
                    || !player.isOnline()
            ) {

                pendingRewardRepository.insert(
                        uuid,
                        rewardType,
                        1
                );

                return;
            }

            ItemStack item =
                    itemFactory.create(
                            rewardType,
                            1,
                            uuid,
                            player.getName()
                    );

            Map<Integer, ItemStack> leftovers =
                    player.getInventory()
                            .addItem(
                                    item
                            );

            if (!leftovers.isEmpty()) {

                int remainingAmount =
                        leftovers
                                .values()
                                .stream()
                                .mapToInt(
                                        ItemStack::getAmount
                                )
                                .sum();

                if (remainingAmount > 0) {

                    pendingRewardRepository.insert(
                            uuid,
                            rewardType,
                            remainingAmount
                    );

                    pendingStored =
                            true;
                }
            }

        } catch (SQLException exception) {

            plugin.getLogger()
                    .severe(
                            "[DungeonReward] "
                                    + "Ancient Depths equipment "
                                    + "reward storage failed for "
                                    + uuid
                                    + ": "
                                    + exception.getMessage()
                    );

            return;
        }

        player.sendMessage(
                Component.text(
                        "[던전 보상] ",
                        NamedTextColor.GOLD
                ).append(
                        Component.text(
                                "이름없는 기사 장비를 획득했습니다.",
                                NamedTextColor.LIGHT_PURPLE
                        )
                )
        );

        if (pendingStored) {

            player.sendMessage(
                    Component.text(
                            "인벤토리에 들어가지 못한 보상은 "
                                    + "RPGCore에 보관되었습니다. "
                                    + "/rpgclaim",
                            NamedTextColor.AQUA
                    )
            );
        }
    }


    /*
     * =========================================================
     * ENDERMAN RUINS LEGENDARY EQUIPMENT
     * =========================================================
     *
     * 전체 드롭 확률: 15%
     *
     * 당첨 시 서리 군주 6종 + 업화 군주 6종,
     * 총 12종 중 정확히 1개를 균등 랜덤 지급한다.
     *
     * 개별 장비 확률:
     * 15% / 12 = 1.25%
     * =========================================================
     */

    public void rewardEndermanRuinsLegendaryEquipment(
            Set<UUID> participants
    ) {

        if (
                participants == null
                || participants.isEmpty()
        ) {
            return;
        }

        for (UUID uuid : participants) {

            rewardEndermanRuinsLegendaryEquipmentPlayer(
                    uuid
            );
        }
    }


    private void rewardEndermanRuinsLegendaryEquipmentPlayer(
            UUID uuid
    ) {

        ThreadLocalRandom random =
                ThreadLocalRandom.current();


        /*
         * 전체 전설 장비 드롭 확률 15%.
         */
        if (random.nextDouble() >= 0.15D) {
            return;
        }


        String[] rewardTypes =
                new String[] {

                        DungeonRewardItemFactory
                                .ICE_LORD_HELMET,

                        DungeonRewardItemFactory
                                .ICE_LORD_CHESTPLATE,

                        DungeonRewardItemFactory
                                .ICE_LORD_LEGGINGS,

                        DungeonRewardItemFactory
                                .ICE_LORD_BOOTS,

                        DungeonRewardItemFactory
                                .ICE_LORD_SPEAR,

                        DungeonRewardItemFactory
                                .ICE_LORD_GRIMOIRE,


                        DungeonRewardItemFactory
                                .FIRE_LORD_HELMET,

                        DungeonRewardItemFactory
                                .FIRE_LORD_CHESTPLATE,

                        DungeonRewardItemFactory
                                .FIRE_LORD_LEGGINGS,

                        DungeonRewardItemFactory
                                .FIRE_LORD_BOOTS,

                        DungeonRewardItemFactory
                                .FIRE_LORD_SWORD,

                        DungeonRewardItemFactory
                                .FIRE_LORD_DEATH_SCYTHE
                };


        String rewardType =
                rewardTypes[
                        random.nextInt(
                                rewardTypes.length
                        )
                ];


        Player player =
                Bukkit.getPlayer(
                        uuid
                );

        boolean pendingStored =
                false;


        try {

            /*
             * 오프라인 상태라면
             * /rpgclaim 보상으로 저장한다.
             */
            if (
                    player == null
                    || !player.isOnline()
            ) {

                pendingRewardRepository.insert(
                        uuid,
                        rewardType,
                        1
                );

                return;
            }


            ItemStack item =
                    itemFactory.create(
                            rewardType,
                            1,
                            uuid,
                            player.getName()
                    );


            if (item == null) {

                plugin.getLogger()
                        .severe(
                                "[DungeonReward] "
                                        + "Enderman legendary item creation failed: "
                                        + rewardType
                        );

                return;
            }


            Map<Integer, ItemStack> leftovers =
                    player.getInventory()
                            .addItem(
                                    item
                            );


            if (!leftovers.isEmpty()) {

                int remainingAmount =
                        leftovers.values()
                                .stream()
                                .mapToInt(
                                        ItemStack::getAmount
                                )
                                .sum();

                if (remainingAmount > 0) {

                    pendingRewardRepository.insert(
                            uuid,
                            rewardType,
                            remainingAmount
                    );

                    pendingStored =
                            true;
                }
            }

        } catch (SQLException exception) {

            plugin.getLogger()
                    .severe(
                            "[DungeonReward] "
                                    + "Enderman legendary reward "
                                    + "storage failed for "
                                    + uuid
                                    + ": "
                                    + exception.getMessage()
                    );

            return;
        }


        player.sendMessage(
                Component.text(
                        "[전설 보상] ",
                        NamedTextColor.GOLD
                ).append(
                        Component.text(
                                "군주의 전설 장비를 획득했습니다!",
                                NamedTextColor.YELLOW
                        )
                )
        );


        if (pendingStored) {

            player.sendMessage(
                    Component.text(
                            "인벤토리에 들어가지 못한 보상은 "
                                    + "RPGCore에 보관되었습니다. "
                                    + "/rpgclaim",
                            NamedTextColor.AQUA
                    )
            );
        }
    }


    /*
     * =========================================================
     * ENDERMAN RUINS BASIC REWARDS
     * =========================================================
     */

    public void rewardEndermanRuinsBasicRewards(
            Set<UUID> participants
    ) {

        if (
                participants == null
                || participants.isEmpty()
        ) {
            return;
        }

        for (UUID uuid : participants) {

            rewardEndermanRuinsBasicPlayer(
                    uuid
            );
        }
    }


    private void rewardEndermanRuinsBasicPlayer(
            UUID uuid
    ) {

        ThreadLocalRandom random =
                ThreadLocalRandom.current();

        List<RewardSpec> rewards =
                new ArrayList<>();

        /*
         * Enderman dungeon unique materials.
         */
        rewards.add(
                new RewardSpec(
                        DungeonRewardItemFactory.ENDER_PEARL,
                        random.nextInt(
                                8,
                                17
                        )
                )
        );

        rewards.add(
                new RewardSpec(
                        DungeonRewardItemFactory.AMETHYST_SHARD,
                        random.nextInt(
                                10,
                                21
                        )
                )
        );

        /*
         * Lv.50~60 basic resources.
         */
        rewards.add(
                new RewardSpec(
                        DungeonRewardItemFactory.IRON_INGOT,
                        random.nextInt(
                                20,
                                29
                        )
                )
        );

        rewards.add(
                new RewardSpec(
                        DungeonRewardItemFactory.GOLD_INGOT,
                        random.nextInt(
                                8,
                                13
                        )
                )
        );

        rewards.add(
                new RewardSpec(
                        DungeonRewardItemFactory.EMERALD,
                        random.nextInt(
                                4,
                                9
                        )
                )
        );

        rewards.add(
                new RewardSpec(
                        DungeonRewardItemFactory.LAPIS_LAZULI,
                        random.nextInt(
                                6,
                                13
                        )
                )
        );

        rewards.add(
                new RewardSpec(
                        DungeonRewardItemFactory.DIAMOND,
                        random.nextInt(
                                4,
                                9
                        )
                )
        );


        Player player =
                Bukkit.getPlayer(
                        uuid
                );

        boolean pendingStored =
                false;

        try {

            if (
                    player == null
                    || !player.isOnline()
            ) {

                for (RewardSpec reward : rewards) {

                    pendingRewardRepository.insert(
                            uuid,
                            reward.type(),
                            reward.amount()
                    );
                }

                return;
            }


            for (RewardSpec reward : rewards) {

                ItemStack item =
                        itemFactory.create(
                                reward.type(),
                                reward.amount(),
                                uuid,
                                player.getName()
                        );

                Map<Integer, ItemStack> leftovers =
                        player.getInventory()
                                .addItem(
                                        item
                                );

                if (leftovers.isEmpty()) {
                    continue;
                }

                int remainingAmount =
                        leftovers
                                .values()
                                .stream()
                                .mapToInt(
                                        ItemStack::getAmount
                                )
                                .sum();

                if (remainingAmount <= 0) {
                    continue;
                }

                pendingRewardRepository.insert(
                        uuid,
                        reward.type(),
                        remainingAmount
                );

                pendingStored =
                        true;
            }

        } catch (SQLException exception) {

            plugin.getLogger().severe(
                    "[DungeonReward] "
                            + "Enderman Ruins reward "
                            + "storage failed for "
                            + uuid
                            + ": "
                            + exception.getMessage()
            );

            return;
        }


        player.sendMessage(
                Component.text(
                        "기본 보상: 엔더 진주 8~16 / "
                                + "자수정 조각 10~20 / "
                                + "금 주괴 6~12 / "
                                + "다이아몬드 2~4",
                        NamedTextColor.GRAY
                )
        );


        if (pendingStored) {

            player.sendMessage(
                    Component.text(
                            "인벤토리에 들어가지 못한 보상은 "
                                    + "RPGCore에 보관되었습니다. "
                                    + "/rpgclaim",
                            NamedTextColor.AQUA
                    )
            );
        }
    }


    /*
     * =========================================================
     * MINOTAUR BASIC REWARDS
     * =========================================================
     *
     * 미궁의 주인 미노타우로스:
     *
     * 다이아몬드 5~8
     *
     * RPG EXP 보상 없음.
     */

    public void rewardMinotaurBasicRewards(
            Set<UUID> participants
    ) {

        if (
                participants == null
                        ||
                participants.isEmpty()
        ) {
            return;
        }


        for (UUID uuid : participants) {

            rewardMinotaurBasicPlayer(
                    uuid
            );
        }
    }


    private void rewardMinotaurBasicPlayer(
            UUID uuid
    ) {

        ThreadLocalRandom random =
                ThreadLocalRandom.current();


        int diamondAmount =
                random.nextInt(
                        5,
                        9
                );


        RewardSpec reward =
                new RewardSpec(
                        DungeonRewardItemFactory.DIAMOND,
                        diamondAmount
                );


        Player player =
                Bukkit.getPlayer(
                        uuid
                );


        boolean pendingStored =
                false;


        try {

            /*
             * 오프라인 상태라면
             * 전체 보상을 pending reward로 저장한다.
             */
            if (
                    player == null
                            ||
                    !player.isOnline()
            ) {

                pendingRewardRepository.insert(
                        uuid,
                        reward.type(),
                        reward.amount()
                );

                return;
            }


            ItemStack item =
                    itemFactory.create(
                            reward.type(),
                            reward.amount(),
                            uuid,
                            player.getName()
                    );


            Map<Integer, ItemStack> leftovers =
                    player.getInventory()
                            .addItem(
                                    item
                            );


            if (!leftovers.isEmpty()) {

                int remainingAmount =
                        leftovers
                                .values()
                                .stream()
                                .mapToInt(
                                        ItemStack::getAmount
                                )
                                .sum();


                if (remainingAmount > 0) {

                    pendingRewardRepository.insert(
                            uuid,
                            reward.type(),
                            remainingAmount
                    );

                    pendingStored =
                            true;
                }
            }

        } catch (SQLException exception) {

            plugin.getLogger()
                    .severe(
                            "[DungeonReward] "
                                    + "Minotaur reward storage failed for "
                                    + uuid
                                    + ": "
                                    + exception.getMessage()
                    );

            return;
        }


        player.sendMessage(
                Component.text(
                        "기본 보상: 다이아몬드 "
                                + diamondAmount,
                        NamedTextColor.GRAY
                )
        );


        if (pendingStored) {

            player.sendMessage(
                    Component.text(
                            "인벤토리에 들어가지 못한 보상은 "
                                    + "RPGCore에 보관되었습니다. "
                                    + "/rpgclaim",
                            NamedTextColor.AQUA
                    )
            );
        }
    }


    /*
     * =========================================================
     * FALLEN ANGEL BASIC REWARDS
     * =========================================================
     */

    public void rewardFallenAngelBasicRewards(
            Set<UUID> participants
    ) {

        if (
                participants == null
                || participants.isEmpty()
        ) {
            return;
        }

        for (UUID uuid : participants) {

            rewardFallenAngelBasicPlayer(
                    uuid
            );
        }
    }


    private void rewardFallenAngelBasicPlayer(
            UUID uuid
    ) {

        ThreadLocalRandom random =
                ThreadLocalRandom.current();

        List<RewardSpec> rewards =
                new ArrayList<>();

        /*
         * 다이아몬드 3~5
         */
        rewards.add(
                new RewardSpec(
                        DungeonRewardItemFactory.DIAMOND,
                        random.nextInt(
                                3,
                                6
                        )
                )
        );

        /*
         * 에메랄드 4~8
         */
        rewards.add(
                new RewardSpec(
                        DungeonRewardItemFactory.EMERALD,
                        random.nextInt(
                                4,
                                9
                        )
                )
        );

        /*
         * 금 주괴 6~12
         */
        rewards.add(
                new RewardSpec(
                        DungeonRewardItemFactory.GOLD_INGOT,
                        random.nextInt(
                                6,
                                13
                        )
                )
        );

        /*
         * 자수정 조각 8~16
         */
        rewards.add(
                new RewardSpec(
                        DungeonRewardItemFactory.AMETHYST_SHARD,
                        random.nextInt(
                                8,
                                17
                        )
                )
        );

        Player player =
                Bukkit.getPlayer(uuid);

        boolean pendingStored =
                false;

        try {

            if (
                    player == null
                    || !player.isOnline()
            ) {

                for (RewardSpec reward : rewards) {

                    pendingRewardRepository.insert(
                            uuid,
                            reward.type(),
                            reward.amount()
                    );
                }

                return;
            }

            for (RewardSpec reward : rewards) {

                ItemStack item =
                        itemFactory.create(
                                reward.type(),
                                reward.amount(),
                                uuid,
                                player.getName()
                        );

                Map<Integer, ItemStack> leftovers =
                        player.getInventory()
                                .addItem(item);

                if (leftovers.isEmpty()) {
                    continue;
                }

                int remainingAmount =
                        leftovers
                                .values()
                                .stream()
                                .mapToInt(
                                        ItemStack::getAmount
                                )
                                .sum();

                if (remainingAmount <= 0) {
                    continue;
                }

                pendingRewardRepository.insert(
                        uuid,
                        reward.type(),
                        remainingAmount
                );

                pendingStored =
                        true;
            }

        } catch (SQLException exception) {

            plugin.getLogger().severe(
                    "[DungeonReward] "
                            + "Fallen Angel basic reward "
                            + "storage failed for "
                            + uuid
                            + ": "
                            + exception.getMessage()
            );

            return;
        }

        player.sendMessage(
                Component.text(
                        "기본 보상: 다이아몬드 3~5 / "
                                + "에메랄드 4~8 / "
                                + "금 주괴 6~12 / "
                                + "자수정 조각 8~16",
                        NamedTextColor.GRAY
                )
        );

        if (pendingStored) {

            player.sendMessage(
                    Component.text(
                            "인벤토리에 들어가지 못한 보상은 "
                                    + "RPGCore에 보관되었습니다. "
                                    + "/rpgclaim",
                            NamedTextColor.AQUA
                    )
            );
        }
    }


    public void rewardUndeadFortressBasicRewards(
            Set<UUID> participants
    ) {

        if (
                participants == null
                || participants.isEmpty()
        ) {
            return;
        }

        for (UUID uuid : participants) {

            rewardUndeadFortressBasicPlayer(
                    uuid
            );
        }
    }


    private void rewardUndeadFortressBasicPlayer(
            UUID uuid
    ) {

        ThreadLocalRandom random =
                ThreadLocalRandom.current();

        List<RewardSpec> rewards =
                new ArrayList<>();

        /*
         * Lv.20~30 Undead Fortress basic resources.
         */
        rewards.add(
                new RewardSpec(
                        DungeonRewardItemFactory.IRON_INGOT,
                        random.nextInt(
                                8,
                                15
                        )
                )
        );

        rewards.add(
                new RewardSpec(
                        DungeonRewardItemFactory.GOLD_INGOT,
                        random.nextInt(
                                3,
                                6
                        )
                )
        );

        rewards.add(
                new RewardSpec(
                        DungeonRewardItemFactory.EMERALD,
                        random.nextInt(
                                1,
                                4
                        )
                )
        );


        Player player =
                Bukkit.getPlayer(uuid);

        boolean pendingStored =
                false;

        try {

            if (
                    player == null
                    || !player.isOnline()
            ) {

                for (RewardSpec reward : rewards) {

                    pendingRewardRepository.insert(
                            uuid,
                            reward.type(),
                            reward.amount()
                    );
                }

                return;
            }

            for (RewardSpec reward : rewards) {

                ItemStack item =
                        itemFactory.create(
                                reward.type(),
                                reward.amount(),
                                uuid,
                                player.getName()
                        );

                Map<Integer, ItemStack> leftovers =
                        player.getInventory()
                                .addItem(item);

                if (leftovers.isEmpty()) {
                    continue;
                }

                int remainingAmount =
                        leftovers
                                .values()
                                .stream()
                                .mapToInt(
                                        ItemStack::getAmount
                                )
                                .sum();

                if (remainingAmount <= 0) {
                    continue;
                }

                pendingRewardRepository.insert(
                        uuid,
                        reward.type(),
                        remainingAmount
                );

                pendingStored = true;
            }

        } catch (SQLException exception) {

            plugin.getLogger().severe(
                    "[DungeonReward] "
                            + "Undead Fortress basic reward "
                            + "storage failed for "
                            + uuid
                            + ": "
                            + exception.getMessage()
            );

            return;
        }

        player.sendMessage(
                Component.text(
                        "기본 보상: 철 주괴 8~16 / "
                                + "금 주괴 3~6 / "
                                + "에메랄드 2~4",
                        NamedTextColor.GRAY
                )
        );

        if (pendingStored) {

            player.sendMessage(
                    Component.text(
                            "인벤토리에 들어가지 못한 보상은 "
                                    + "RPGCore에 보관되었습니다. "
                                    + "/rpgclaim",
                            NamedTextColor.AQUA
                    )
            );
        }
    }


    public void rewardUndeadFortressEquipment(
            Set<UUID> participants
    ) {

        if (
                participants == null
                || participants.isEmpty()
        ) {
            return;
        }

        for (UUID uuid : participants) {

            rewardUndeadKnightEquipment(
                    uuid
            );
        }
    }

    /*
     * =========================================================
     * NETHER FORTRESS SPECIAL WEAPON REWARD
     * =========================================================
     *
     * 모든 참가자가 같은 공용 무기 풀을 사용한다.
     *
     * 클리어마다 10% 확률로 보상을 획득하며,
     * 성공 시 아래 4종 중 하나를 균등하게 선택한다.
     *
     * - 화염의 창
     * - 푸른 파도의 검
     * - 홍염의 스태프
     * - 뇌전의 마도구
     *
     * 각 아이템의 실질 획득 확률은 2.5%이다.
     */
    public void rewardNetherFortressSpecialWeapon(
            Set<UUID> participants
    ) {

        if (
                participants == null
                || participants.isEmpty()
        ) {
            return;
        }

        for (UUID uuid : participants) {

            rewardNetherFortressSpecialWeaponPlayer(
                    uuid
            );
        }
    }


    public void rewardNetherFortressEquipment(
            Set<UUID> participants
    ) {

        if (
                participants == null
                || participants.isEmpty()
        ) {
            return;
        }

        for (UUID uuid : participants) {

            rewardNetherLordEquipment(
                    uuid
            );
        }
    }



    private void rewardUndeadKnightEquipment(
            UUID uuid
    ) {

        ThreadLocalRandom random =
                ThreadLocalRandom.current();

        List<RewardSpec> rewards =
                new ArrayList<>();

        addChanceReward(
                rewards,
                random,
                DungeonRewardItemFactory
                        .GRAVE_GUARDIAN_HELMET
        );

        addChanceReward(
                rewards,
                random,
                DungeonRewardItemFactory
                        .GRAVE_GUARDIAN_CHESTPLATE
        );

        addChanceReward(
                rewards,
                random,
                DungeonRewardItemFactory
                        .GRAVE_GUARDIAN_LEGGINGS
        );

        addChanceReward(
                rewards,
                random,
                DungeonRewardItemFactory
                        .GRAVE_GUARDIAN_BOOTS
        );

        if (rewards.isEmpty()) {
            return;
        }

        Player player =
                Bukkit.getPlayer(uuid);

        boolean pendingStored =
                false;

        try {

            if (
                    player == null
                    || !player.isOnline()
            ) {

                for (RewardSpec reward : rewards) {

                    pendingRewardRepository.insert(
                            uuid,
                            reward.type(),
                            reward.amount()
                    );
                }

                return;
            }

            for (RewardSpec reward : rewards) {

                ItemStack item =
                        itemFactory.create(
                                reward.type(),
                                reward.amount(),
                                uuid,
                                player.getName()
                        );

                Map<Integer, ItemStack> leftovers =
                        player.getInventory()
                                .addItem(item);

                if (leftovers.isEmpty()) {
                    continue;
                }

                int remainingAmount =
                        leftovers
                                .values()
                                .stream()
                                .mapToInt(
                                        ItemStack::getAmount
                                )
                                .sum();

                if (remainingAmount <= 0) {
                    continue;
                }

                pendingRewardRepository.insert(
                        uuid,
                        reward.type(),
                        remainingAmount
                );

                pendingStored = true;
            }

        } catch (SQLException exception) {

            plugin.getLogger().severe(
                    "[DungeonReward] "
                            + "Undead Fortress reward "
                            + "storage failed for "
                            + uuid
                            + ": "
                            + exception.getMessage()
            );

            return;
        }

        for (RewardSpec reward : rewards) {

            String itemName =
                    switch (reward.type()) {

                        case DungeonRewardItemFactory
                                .GRAVE_GUARDIAN_HELMET ->
                                "언데드 기사의 투구";

                        case DungeonRewardItemFactory
                                .GRAVE_GUARDIAN_CHESTPLATE ->
                                "언데드 기사의 흉갑";

                        case DungeonRewardItemFactory
                                .GRAVE_GUARDIAN_LEGGINGS ->
                                "언데드 기사의 레깅스";

                        case DungeonRewardItemFactory
                                .GRAVE_GUARDIAN_BOOTS ->
                                "언데드 기사의 부츠";

                        default ->
                                "언데드 기사 장비";
                    };

            player.sendMessage(
                    Component.text(
                            "★ 레어 보상: "
                                    + itemName
                                    + "을(를) 획득했습니다!",
                            NamedTextColor.LIGHT_PURPLE
                    )
            );
        }

        if (pendingStored) {

            player.sendMessage(
                    Component.text(
                            "인벤토리에 들어가지 못한 보상은 "
                                    + "RPGCore에 보관되었습니다. "
                                    + "/rpgclaim",
                            NamedTextColor.AQUA
                    )
            );
        }

        /*
         * 창백한 스태프 추가 레어 보상.
         *
         * 기존 언데드 기사 장비 보상과 독립적으로
         * 클리어마다 20% 확률로 1개 지급한다.
         */
        if (
                ThreadLocalRandom.current()
                        .nextDouble()
                        < 0.20D
        ) {

            String rewardType =
                    DungeonRewardItemFactory.PALE_STAFF;

            try {

                if (
                        player == null
                        || !player.isOnline()
                ) {

                    pendingRewardRepository.insert(
                            uuid,
                            rewardType,
                            1
                    );

                    return;
                }

                ItemStack item =
                        itemFactory.create(
                                rewardType,
                                1,
                                uuid,
                                player.getName()
                        );

                Map<Integer, ItemStack> leftovers =
                        player.getInventory()
                                .addItem(
                                        item
                                );

                if (!leftovers.isEmpty()) {

                    int remainingAmount =
                            leftovers.values()
                                    .stream()
                                    .mapToInt(
                                            ItemStack::getAmount
                                    )
                                    .sum();

                    if (remainingAmount > 0) {

                        pendingRewardRepository.insert(
                                uuid,
                                rewardType,
                                remainingAmount
                        );
                    }
                }

                player.sendMessage(
                        Component.text(
                                "★ 레어 보상: 창백한 스태프",
                                NamedTextColor.LIGHT_PURPLE
                        )
                );

            } catch (SQLException exception) {

                plugin.getLogger()
                        .severe(
                                "[DungeonReward] "
                                        + "Pale Staff reward failed for "
                                        + uuid
                                        + ": "
                                        + exception.getMessage()
                        );
            }
        }

    }

    /*
     * =========================================================
     * NETHER FORTRESS SPECIAL WEAPON - PLAYER
     * =========================================================
     */
    private void rewardNetherFortressSpecialWeaponPlayer(
            UUID uuid
    ) {

        ThreadLocalRandom random =
                ThreadLocalRandom.current();


        /*
         * 전체 10% 단일 판정.
         */
        if (
                random.nextDouble()
                        >= NETHER_FORTRESS_SPECIAL_WEAPON_CHANCE
        ) {
            return;
        }


        String[] rewardTypes =
                new String[] {
                        DungeonRewardItemFactory
                                .NETHER_FIRE_SPEAR,

                        DungeonRewardItemFactory
                                .NETHER_BLUE_WAVE_SWORD,

                        DungeonRewardItemFactory
                                .NETHER_FIRE_STAFF,

                        DungeonRewardItemFactory
                                .NETHER_THUNDER_CATALYST
                };


        String rewardType =
                rewardTypes[
                        random.nextInt(
                                rewardTypes.length
                        )
                ];


        Player player =
                Bukkit.getPlayer(
                        uuid
                );

        boolean pendingStored =
                false;


        try {

            /*
             * 오프라인 참가자는 pending reward에 저장한다.
             */
            if (
                    player == null
                    || !player.isOnline()
            ) {

                pendingRewardRepository.insert(
                        uuid,
                        rewardType,
                        1
                );

                return;
            }


            ItemStack item =
                    itemFactory.create(
                            rewardType,
                            1,
                            uuid,
                            player.getName()
                    );


            Map<Integer, ItemStack> leftovers =
                    player.getInventory()
                            .addItem(
                                    item
                            );


            /*
             * 인벤토리가 가득 찬 경우 바닥에 버리지 않고
             * pending reward에 저장한다.
             */
            if (!leftovers.isEmpty()) {

                int remainingAmount =
                        leftovers
                                .values()
                                .stream()
                                .mapToInt(
                                        ItemStack::getAmount
                                )
                                .sum();

                if (remainingAmount > 0) {

                    pendingRewardRepository.insert(
                            uuid,
                            rewardType,
                            remainingAmount
                    );

                    pendingStored =
                            true;
                }
            }


        } catch (SQLException exception) {

            plugin.getLogger()
                    .severe(
                            "[DungeonReward] "
                                    + "Nether Fortress special weapon "
                                    + "reward storage failed for "
                                    + uuid
                                    + ": "
                                    + exception.getMessage()
                    );

            return;
        }


        String itemName =
                switch (rewardType) {

                    case DungeonRewardItemFactory
                            .NETHER_FIRE_SPEAR ->
                            "화염의 창";

                    case DungeonRewardItemFactory
                            .NETHER_BLUE_WAVE_SWORD ->
                            "푸른 파도의 검";

                    case DungeonRewardItemFactory
                            .NETHER_FIRE_STAFF ->
                            "홍염의 스태프";

                    case DungeonRewardItemFactory
                            .NETHER_THUNDER_CATALYST ->
                            "뇌전의 마도구";

                    default ->
                            "네더 요새 특수 무기";
                };


        player.sendMessage(
                Component.text(
                        "★ 특수 보상: "
                                + itemName
                                + "을(를) 획득했습니다!",
                        NamedTextColor.LIGHT_PURPLE
                )
        );


        if (pendingStored) {

            player.sendMessage(
                    Component.text(
                            "인벤토리에 들어가지 못한 보상은 "
                                    + "RPGCore에 보관되었습니다. "
                                    + "/rpgclaim",
                            NamedTextColor.AQUA
                    )
            );
        }
    }


    private void rewardNetherLordEquipment(
            UUID uuid
    ) {

        ThreadLocalRandom random =
                ThreadLocalRandom.current();

        List<RewardSpec> rewards =
                new ArrayList<>();

        /*
         * =====================================================
         * NETHER LORD EQUIPMENT
         * =====================================================
         *
         * 한 번의 네더 요새 클리어에서
         * 네더 군주 계열 장비는 최대 1개만 획득한다.
         *
         * 기존 네더 군주 장비 8%
         * -> 기존 세트 5종 중 무작위 1개
         *
         * 실패하면 장비 보상 없음
         * =====================================================
         */

        String rewardType =
                null;

        boolean legendary =
                false;

        if (
                random.nextDouble()
                        < NETHER_LORD_DROP_CHANCE
        ) {

            String[] rareItems =
                    new String[] {
                            DungeonRewardItemFactory
                                    .NETHER_LORD_SWORD,
                            DungeonRewardItemFactory
                                    .NETHER_LORD_HELMET,
                            DungeonRewardItemFactory
                                    .NETHER_LORD_CHESTPLATE,
                            DungeonRewardItemFactory
                                    .NETHER_LORD_LEGGINGS,
                            DungeonRewardItemFactory
                                    .NETHER_LORD_BOOTS
                    };

            rewardType =
                    rareItems[
                            random.nextInt(
                                    rareItems.length
                            )
                    ];
        }

        if (rewardType == null) {
            return;
        }

        rewards.add(
                new RewardSpec(
                        rewardType,
                        1
                )
        );

        Player player =
                Bukkit.getPlayer(uuid);

        boolean pendingStored =
                false;

        try {

            if (
                    player == null
                    || !player.isOnline()
            ) {

                for (RewardSpec reward : rewards) {

                    pendingRewardRepository.insert(
                            uuid,
                            reward.type(),
                            reward.amount()
                    );
                }

                return;
            }

            for (RewardSpec reward : rewards) {

                ItemStack item =
                        itemFactory.create(
                                reward.type(),
                                reward.amount(),
                                uuid,
                                player.getName()
                        );

                Map<Integer, ItemStack> leftover =
                        player.getInventory()
                                .addItem(item);

                for (
                        ItemStack remaining
                        : leftover.values()
                ) {

                    pendingRewardRepository.insert(
                            uuid,
                            reward.type(),
                            remaining.getAmount()
                    );

                    pendingStored =
                            true;
                }

                String itemName =
                        switch (reward.type()) {

                            case DungeonRewardItemFactory
                                    .NETHER_LORD_SWORD ->
                                    "네더 군주의 검";

                            case DungeonRewardItemFactory
                                    .NETHER_LORD_HELMET ->
                                    "네더 군주의 투구";

                            case DungeonRewardItemFactory
                                    .NETHER_LORD_CHESTPLATE ->
                                    "네더 군주의 흉갑";

                            case DungeonRewardItemFactory
                                    .NETHER_LORD_LEGGINGS ->
                                    "네더 군주의 레깅스";

                            case DungeonRewardItemFactory
                                    .NETHER_LORD_BOOTS ->
                                    "네더 군주의 부츠";

                            case DungeonRewardItemFactory.NETHER_LORD_LEGEND_DEATHSIDE ->
                                    "네더 군주의 데스사이드";

                            case DungeonRewardItemFactory.NETHER_LORD_LEGEND_HELMET ->
                                    "네더 군주의 투구";

                            case DungeonRewardItemFactory.NETHER_LORD_LEGEND_CHESTPLATE ->
                                    "네더 군주의 흉갑";

                            case DungeonRewardItemFactory.NETHER_LORD_LEGEND_LEGGINGS ->
                                    "네더 군주의 레깅스";

                            case DungeonRewardItemFactory.NETHER_LORD_LEGEND_BOOTS ->
                                    "네더 군주의 부츠";

                            default ->
                                    reward.type();
                        };

                player.sendMessage(
                        Component.text(
                                "★ 레어 보상: "
                                        + itemName
                                        + "을(를) 획득했습니다!",
                                NamedTextColor
                                        .LIGHT_PURPLE
                        )
                );
            }

        } catch (SQLException exception) {

            plugin.getLogger()
                    .severe(
                            "[DungeonReward] "
                                    + "Nether Lord reward "
                                    + "storage failed for "
                                    + uuid
                                    + ": "
                                    + exception.getMessage()
                    );

            return;
        }

        if (pendingStored) {

            player.sendMessage(
                    Component.text(
                            "인벤토리에 들어가지 못한 보상은 "
                                    + "RPGCore에 보관되었습니다. "
                                    + "/rpgclaim",
                            NamedTextColor.AQUA
                    )
            );
        }
    }



    private void addNetherLordChanceReward(
            List<RewardSpec> rewards,
            ThreadLocalRandom random,
            String rewardType
    ) {

        if (
                random.nextDouble()
                        < NETHER_LORD_DROP_CHANCE
        ) {

            rewards.add(
                    new RewardSpec(
                            rewardType,
                            1
                    )
            );
        }
    }


    private void addChanceReward(
            List<RewardSpec> rewards,
            ThreadLocalRandom random,
            String rewardType
    ) {

        if (
                random.nextDouble()
                        < UNDEAD_KNIGHT_DROP_CHANCE
        ) {

            rewards.add(
                    new RewardSpec(
                            rewardType,
                            1
                    )
            );
        }
    }



    /*
     * =========================================================
     * TIER BASIC RESOURCE REWARDS
     * =========================================================
     */

    public void rewardAncientDepthsBasicRewards(
            Set<UUID> participants
    ) {

        if (
                participants == null
                || participants.isEmpty()
        ) {
            return;
        }

        for (UUID uuid : participants) {

            rewardBasicResources(
                    uuid,
                    "고대 심층",
                    12,
                    20,
                    4,
                    7,
                    2,
                    5,
                    0,
                    0,
                    0,
                    0
            );
        }
    }


    public void rewardNetherFortressBasicRewards(
            Set<UUID> participants
    ) {

        if (
                participants == null
                || participants.isEmpty()
        ) {
            return;
        }

        for (UUID uuid : participants) {

            rewardBasicResources(
                    uuid,
                    "네더 군주",
                    16,
                    24,
                    6,
                    10,
                    3,
                    6,
                    4,
                    8,
                    2,
                    5
            );
        }
    }


    private void rewardBasicResources(
            UUID uuid,
            String dungeonName,
            int ironMin,
            int ironMax,
            int goldMin,
            int goldMax,
            int emeraldMin,
            int emeraldMax,
            int lapisMin,
            int lapisMax,
            int diamondMin,
            int diamondMax
    ) {

        ThreadLocalRandom random =
                ThreadLocalRandom.current();

        List<RewardSpec> rewards =
                new ArrayList<>();


        if (ironMax > 0) {

            rewards.add(
                    new RewardSpec(
                            DungeonRewardItemFactory.IRON_INGOT,
                            random.nextInt(
                                    ironMin,
                                    ironMax + 1
                            )
                    )
            );
        }


        if (goldMax > 0) {

            rewards.add(
                    new RewardSpec(
                            DungeonRewardItemFactory.GOLD_INGOT,
                            random.nextInt(
                                    goldMin,
                                    goldMax + 1
                            )
                    )
            );
        }


        if (emeraldMax > 0) {

            rewards.add(
                    new RewardSpec(
                            DungeonRewardItemFactory.EMERALD,
                            random.nextInt(
                                    emeraldMin,
                                    emeraldMax + 1
                            )
                    )
            );
        }


        if (lapisMax > 0) {

            rewards.add(
                    new RewardSpec(
                            DungeonRewardItemFactory.LAPIS_LAZULI,
                            random.nextInt(
                                    lapisMin,
                                    lapisMax + 1
                            )
                    )
            );
        }


        if (diamondMax > 0) {

            rewards.add(
                    new RewardSpec(
                            DungeonRewardItemFactory.DIAMOND,
                            random.nextInt(
                                    diamondMin,
                                    diamondMax + 1
                            )
                    )
            );
        }


        Player player =
                Bukkit.getPlayer(
                        uuid
                );

        boolean pendingStored =
                false;


        try {

            if (
                    player == null
                    || !player.isOnline()
            ) {

                for (RewardSpec reward : rewards) {

                    pendingRewardRepository.insert(
                            uuid,
                            reward.type(),
                            reward.amount()
                    );
                }

                return;
            }


            for (RewardSpec reward : rewards) {

                ItemStack item =
                        itemFactory.create(
                                reward.type(),
                                reward.amount(),
                                uuid,
                                player.getName()
                        );

                Map<Integer, ItemStack> leftovers =
                        player.getInventory()
                                .addItem(
                                        item
                                );

                if (leftovers.isEmpty()) {
                    continue;
                }


                int remainingAmount =
                        leftovers
                                .values()
                                .stream()
                                .mapToInt(
                                        ItemStack::getAmount
                                )
                                .sum();

                if (remainingAmount <= 0) {
                    continue;
                }


                pendingRewardRepository.insert(
                        uuid,
                        reward.type(),
                        remainingAmount
                );

                pendingStored =
                        true;
            }

        } catch (SQLException exception) {

            plugin.getLogger()
                    .severe(
                            "[DungeonReward] "
                                    + dungeonName
                                    + " basic resource reward failed for "
                                    + uuid
                                    + ": "
                                    + exception.getMessage()
                    );

            return;
        }


        player.sendMessage(
                Component.text(
                        "[던전 보상] 기본 재화를 획득했습니다.",
                        NamedTextColor.GRAY
                )
        );


        if (pendingStored) {

            player.sendMessage(
                    Component.text(
                            "인벤토리에 들어가지 못한 보상은 "
                                    + "RPGCore에 보관되었습니다. "
                                    + "/rpgclaim",
                            NamedTextColor.AQUA
                    )
            );
        }
    }


    public void rewardDungeonClear(
            Set<UUID> participants
    ) {

        if (participants == null
                || participants.isEmpty()) {

            return;
        }

        /*
         * 현재 던전 참가자로 등록된 플레이어마다
         * 개별적으로 보상을 판정한다.
         */
        for (UUID uuid : participants) {

            rewardPlayer(uuid);
        }
    }

    private void rewardPlayer(
            UUID uuid
    ) {

        /*
         * 기존 몬스터 EXP와 동일한 RPG EXP 처리 경로.
         *
         * DB / HUD / 레벨업 / 전직 잠금 /
         * 스탯 / 스킬 보상 동기화를 그대로 재사용한다.
         */
        experienceAwarder.awardExperience(
                uuid,
                CLEAR_EXP
        );

        ThreadLocalRandom random =
                ThreadLocalRandom.current();

        List<RewardSpec> rewards =
                new ArrayList<>();

        /*
         * Lv.10~20 Zombie Dungeon basic resources.
         *
         * Iron 4~8
         * Gold 1~3
         *
         * Emerald / Lapis / Diamond 없음.
         */
        rewards.add(
                new RewardSpec(
                        DungeonRewardItemFactory.IRON_INGOT,
                        random.nextInt(
                                4,
                                9
                        )
                )
        );

        rewards.add(
                new RewardSpec(
                        DungeonRewardItemFactory.GOLD_INGOT,
                        random.nextInt(
                                1,
                                4
                        )
                )
        );


        Player player =
                Bukkit.getPlayer(uuid);

        boolean pendingStored =
                false;

        try {

            if (player == null
                    || !player.isOnline()) {

                /*
                 * 오프라인 참가자는 모든 아이템을
                 * pending reward로 안전하게 저장한다.
                 */
                for (RewardSpec reward : rewards) {

                    pendingRewardRepository.insert(
                            uuid,
                            reward.type(),
                            reward.amount()
                    );
                }

                return;
            }

            for (RewardSpec reward : rewards) {

                ItemStack item =
                        itemFactory.create(
                                reward.type(),
                                reward.amount(),
                                uuid,
                                player.getName()
                        );

                Map<Integer, ItemStack> leftovers =
                        player.getInventory()
                                .addItem(item);

                if (leftovers.isEmpty()) {
                    continue;
                }

                int remainingAmount =
                        leftovers
                                .values()
                                .stream()
                                .mapToInt(
                                        ItemStack::getAmount
                                )
                                .sum();

                if (remainingAmount <= 0) {
                    continue;
                }

                /*
                 * 절대로 바닥에 떨어뜨리지 않는다.
                 */
                pendingRewardRepository.insert(
                        uuid,
                        reward.type(),
                        remainingAmount
                );

                pendingStored = true;
            }

        } catch (SQLException exception) {

            plugin.getLogger().severe(
                    "[DungeonReward] Reward storage failed for "
                            + uuid
                            + ": "
                            + exception.getMessage()
            );

            exception.printStackTrace();

            /*
             * DB 오류가 나더라도 바닥 드롭은 하지 않는다.
             */
        }

        if (player == null
                || !player.isOnline()) {

            return;
        }


        player.sendMessage(
                Component.text(
                        "[던전 보상] ",
                        NamedTextColor.GOLD
                ).append(
                        Component.text(
                                "RPG EXP +500",
                                NamedTextColor.YELLOW
                        )
                )
        );

        player.sendMessage(
                Component.text(
                        "철 주괴 4~8 / 금 주괴 1~3",
                        NamedTextColor.GRAY
                )
        );
        if (pendingStored) {

            player.sendMessage(
                    Component.text(
                            "인벤토리에 들어가지 못한 보상은 "
                                    + "RPGCore에 보관되었습니다. "
                                    + "/rpgclaim",
                            NamedTextColor.AQUA
                    )
            );
        }
    }





    /*
     * =========================================================
     * RED DRAGON BASIC REWARDS
     * =========================================================
     *
     * Diamond Block x1
     * Emerald Block x1
     */
    public void rewardRedDragonBasicRewards(
            Set<UUID> participants
    ) {

        if (
                participants == null
                        ||
                participants.isEmpty()
        ) {
            return;
        }

        for (UUID uuid : participants) {

            List<RewardSpec> rewards =
                    new ArrayList<>();

            rewards.add(
                    new RewardSpec(
                            DungeonRewardItemFactory.DIAMOND_BLOCK,
                            1
                    )
            );

            rewards.add(
                    new RewardSpec(
                            DungeonRewardItemFactory.EMERALD_BLOCK,
                            1
                    )
            );

            Player player =
                    Bukkit.getPlayer(uuid);

            boolean pendingStored =
                    false;

            try {

                if (
                        player == null
                                ||
                        !player.isOnline()
                ) {

                    for (RewardSpec reward : rewards) {

                        pendingRewardRepository.insert(
                                uuid,
                                reward.type(),
                                reward.amount()
                        );
                    }

                    continue;
                }

                for (RewardSpec reward : rewards) {

                    ItemStack item =
                            itemFactory.create(
                                    reward.type(),
                                    reward.amount(),
                                    uuid,
                                    player.getName()
                            );

                    Map<Integer, ItemStack> leftovers =
                            player.getInventory()
                                    .addItem(item);

                    for (ItemStack remaining : leftovers.values()) {

                        pendingRewardRepository.insert(
                                uuid,
                                reward.type(),
                                remaining.getAmount()
                        );

                        pendingStored =
                                true;
                    }
                }

            } catch (SQLException exception) {

                plugin.getLogger()
                        .severe(
                                "[DungeonReward] "
                                        + "Red Dragon basic reward "
                                        + "storage failed for "
                                        + uuid
                                        + ": "
                                        + exception.getMessage()
                        );

                continue;
            }

            player.sendMessage(
                    Component.text(
                            "기본 보상: 다이아몬드 블록 1 / "
                                    + "에메랄드 블록 1",
                            NamedTextColor.GRAY
                    )
            );

            if (pendingStored) {

                player.sendMessage(
                        Component.text(
                                "인벤토리에 들어가지 못한 보상은 "
                                        + "RPGCore에 보관되었습니다. "
                                        + "/rpgclaim",
                                NamedTextColor.AQUA
                        )
                );
            }
        }
    }


    /*
     * =========================================================
     * VOID SANCTUM BASIC REWARDS
     * =========================================================
     *
     * Lv.70~80 공허의 성전.
     *
     * Diamond:
     * 5~8 확정
     *
     * Echo Shard:
     * 30% 확률 / 1~2
     *
     * Chorus Fruit:
     * 6~12 확정
     * =========================================================
     */

    public void rewardVoidSanctumBasicRewards(
            Set<UUID> participants
    ) {

        if (
                participants == null
                        ||
                participants.isEmpty()
        ) {
            return;
        }


        for (UUID uuid : participants) {

            rewardVoidSanctumBasicPlayer(
                    uuid
            );
        }
    }


    private void rewardVoidSanctumBasicPlayer(
            UUID uuid
    ) {

        ThreadLocalRandom random =
                ThreadLocalRandom.current();


        List<RewardSpec> rewards =
                new ArrayList<>();


        rewards.add(
                new RewardSpec(
                        DungeonRewardItemFactory.DIAMOND,
                        random.nextInt(
                                5,
                                9
                        )
                )
        );


        /*
         * 메아리 조각:
         * 30% 확률로 1~2개.
         */
        if (
                random.nextDouble()
                        < 0.30D
        ) {

            rewards.add(
                    new RewardSpec(
                            DungeonRewardItemFactory.ECHO_SHARD,
                            random.nextInt(
                                    1,
                                    3
                            )
                    )
            );
        }


        rewards.add(
                new RewardSpec(
                        DungeonRewardItemFactory.CHORUS_FRUIT,
                        random.nextInt(
                                6,
                                13
                        )
                )
        );


        Player player =
                Bukkit.getPlayer(
                        uuid
                );


        boolean pendingStored =
                false;


        try {

            if (
                    player == null
                            ||
                    !player.isOnline()
            ) {

                for (RewardSpec reward : rewards) {

                    pendingRewardRepository.insert(
                            uuid,
                            reward.type(),
                            reward.amount()
                    );
                }

                return;
            }


            for (RewardSpec reward : rewards) {

                ItemStack item =
                        itemFactory.create(
                                reward.type(),
                                reward.amount(),
                                uuid,
                                player.getName()
                        );


                if (item == null) {

                    plugin.getLogger()
                            .severe(
                                    "[DungeonReward] "
                                            + "Void Sanctum basic item creation failed: "
                                            + reward.type()
                            );

                    continue;
                }


                Map<Integer, ItemStack> leftovers =
                        player.getInventory()
                                .addItem(
                                        item
                                );


                if (leftovers.isEmpty()) {
                    continue;
                }


                int remainingAmount =
                        leftovers.values()
                                .stream()
                                .mapToInt(
                                        ItemStack::getAmount
                                )
                                .sum();


                if (remainingAmount <= 0) {
                    continue;
                }


                pendingRewardRepository.insert(
                        uuid,
                        reward.type(),
                        remainingAmount
                );

                pendingStored =
                        true;
            }

        } catch (SQLException exception) {

            plugin.getLogger()
                    .severe(
                            "[DungeonReward] "
                                    + "Void Sanctum basic reward "
                                    + "storage failed for "
                                    + uuid
                                    + ": "
                                    + exception.getMessage()
                    );

            return;
        }


        player.sendMessage(
                Component.text(
                        "기본 보상: 다이아몬드 5~8 / "
                                + "메아리 조각 30% 1~2 / "
                                + "코러스 열매 6~12",
                        NamedTextColor.GRAY
                )
        );


        if (pendingStored) {

            player.sendMessage(
                    Component.text(
                            "인벤토리에 들어가지 못한 보상은 "
                                    + "RPGCore에 보관되었습니다. "
                                    + "/rpgclaim",
                            NamedTextColor.AQUA
                    )
            );
        }
    }


    /*
     * =========================================================
     * VOID SANCTUM LEGENDARY EQUIPMENT
     * =========================================================
     *
     * 전체 드롭 확률:
     * 20%
     *
     * 성공 시:
     * 서리 군주 6종 + 업화 군주 6종
     * 총 12종 중 정확히 1개.
     *
     * 신화 보관함 판정과 독립적이다.
     * =========================================================
     */

    public void rewardVoidSanctumLegendaryEquipment(
            Set<UUID> participants
    ) {

        if (
                participants == null
                        ||
                participants.isEmpty()
        ) {
            return;
        }


        for (UUID uuid : participants) {

            rewardVoidSanctumLegendaryEquipmentPlayer(
                    uuid
            );
        }
    }


    private void rewardVoidSanctumLegendaryEquipmentPlayer(
            UUID uuid
    ) {

        ThreadLocalRandom random =
                ThreadLocalRandom.current();


        if (
                random.nextDouble()
                        >= 0.20D
        ) {
            return;
        }


        String[] rewardTypes =
                new String[] {

                        DungeonRewardItemFactory
                                .ICE_LORD_HELMET,

                        DungeonRewardItemFactory
                                .ICE_LORD_CHESTPLATE,

                        DungeonRewardItemFactory
                                .ICE_LORD_LEGGINGS,

                        DungeonRewardItemFactory
                                .ICE_LORD_BOOTS,

                        DungeonRewardItemFactory
                                .ICE_LORD_SPEAR,

                        DungeonRewardItemFactory
                                .ICE_LORD_GRIMOIRE,


                        DungeonRewardItemFactory
                                .FIRE_LORD_HELMET,

                        DungeonRewardItemFactory
                                .FIRE_LORD_CHESTPLATE,

                        DungeonRewardItemFactory
                                .FIRE_LORD_LEGGINGS,

                        DungeonRewardItemFactory
                                .FIRE_LORD_BOOTS,

                        DungeonRewardItemFactory
                                .FIRE_LORD_SWORD,

                        DungeonRewardItemFactory
                                .FIRE_LORD_DEATH_SCYTHE
                };


        String rewardType =
                rewardTypes[
                        random.nextInt(
                                rewardTypes.length
                        )
                ];


        Player player =
                Bukkit.getPlayer(
                        uuid
                );


        boolean pendingStored =
                false;


        try {

            if (
                    player == null
                            ||
                    !player.isOnline()
            ) {

                pendingRewardRepository.insert(
                        uuid,
                        rewardType,
                        1
                );

                return;
            }


            ItemStack item =
                    itemFactory.create(
                            rewardType,
                            1,
                            uuid,
                            player.getName()
                    );


            if (item == null) {

                plugin.getLogger()
                        .severe(
                                "[DungeonReward] "
                                        + "Void Sanctum legendary item creation failed: "
                                        + rewardType
                        );

                return;
            }


            Map<Integer, ItemStack> leftovers =
                    player.getInventory()
                            .addItem(
                                    item
                            );


            if (!leftovers.isEmpty()) {

                int remainingAmount =
                        leftovers.values()
                                .stream()
                                .mapToInt(
                                        ItemStack::getAmount
                                )
                                .sum();


                if (remainingAmount > 0) {

                    pendingRewardRepository.insert(
                            uuid,
                            rewardType,
                            remainingAmount
                    );

                    pendingStored =
                            true;
                }
            }

        } catch (SQLException exception) {

            plugin.getLogger()
                    .severe(
                            "[DungeonReward] "
                                    + "Void Sanctum legendary reward "
                                    + "storage failed for "
                                    + uuid
                                    + ": "
                                    + exception.getMessage()
                    );

            return;
        }


        player.sendMessage(
                Component.text(
                        "[전설 보상] ",
                        NamedTextColor.GOLD
                ).append(
                        Component.text(
                                "군주의 전설 장비를 획득했습니다!",
                                NamedTextColor.YELLOW
                        )
                )
        );


        if (pendingStored) {

            player.sendMessage(
                    Component.text(
                            "인벤토리에 들어가지 못한 보상은 "
                                    + "RPGCore에 보관되었습니다. "
                                    + "/rpgclaim",
                            NamedTextColor.AQUA
                    )
            );
        }
    }


    /*
     * =========================================================
     * VOID SANCTUM MYTHIC FULL-SET CHEST
     * =========================================================
     *
     * 드롭 확률:
     * 7%
     *
     * 전설 장비 판정과 독립적이다.
     * =========================================================
     */

    public void rewardVoidSanctumMythicChest(
            Set<UUID> participants
    ) {

        rewardMythicFullSetChest(
                participants,
                0.07D
        );
    }


    /*
     * =========================================================
     * MINOTAUR MYTHIC FULL-SET CHEST REWARD
     * =========================================================
     *
     * 미노타우로스 던전 클리어 시
     * 5% 확률로 신화 세트 보관함을 획득한다.
     *
     * 성공 시 신화 세트 보관함 3종 중
     * 하나를 동일한 확률로 지급한다.
     */
    public void rewardMinotaurMythicChest(
            Set<UUID> participants
    ) {

        rewardMythicFullSetChest(
                participants,
                0.05D
        );
    }


    public void rewardRedDragonMythicChest(
            Set<UUID> participants
    ) {

        rewardMythicFullSetChest(
                participants,
                0.15D
        );
    }


    private void rewardMythicFullSetChest(
            Set<UUID> participants,
            double dropChance
    ) {

        if (
                participants == null
                || participants.isEmpty()
        ) {
            return;
        }

        for (UUID uuid : participants) {

            Player player =
                    Bukkit.getPlayer(uuid);

            if (
                    player == null
                    || !player.isOnline()
            ) {
                continue;
            }

            /*
             * 던전별로 전달된 dropChance를 사용하여
             * 신화 세트 보관함 획득 여부를 판정한다.
             */
            if (
                    ThreadLocalRandom
                            .current()
                            .nextDouble()
                            >= dropChance
            ) {
                continue;
            }

            int selected =
                    ThreadLocalRandom
                            .current()
                            .nextInt(4);

            String chestItemId;
            String[] itemIds;
            String rewardName;

            switch (selected) {

                case 0 -> {

                    chestItemId =
                            "ancient_dragon_chest";

                    rewardName =
                            "고대 용기사의 보관함";

                    itemIds =
                            new String[] {
                                    "ancient_dragon_axe",
                                    "ancient_dragon_boots",
                                    "ancient_dragon_chestplate",
                                    "ancient_dragon_dagger",
                                    "ancient_dragon_hammer",
                                    "ancient_dragon_hat",
                                    "ancient_dragon_helmet",
                                    "ancient_dragon_leggings",
                                    "ancient_dragon_rapier_sword",
                                    "ancient_dragon_scythe",
                                    "ancient_dragon_spear",
                                    "ancient_dragon_staff",
                                    "ancient_dragon_sword",
                                    "ancient_dragon_trident",
                                    "ancient_dragon_wing"
                            };
                }

                case 1 -> {

                    chestItemId =
                            "dragon_lord_chest";

                    rewardName =
                            "마룡 군주의 보관함";

                    itemIds =
                            new String[] {
                                    "dragon_lord_axe",
                                    "dragon_lord_boots",
                                    "dragon_lord_chestplate",
                                    "dragon_lord_dagger",
                                    "dragon_lord_hammer",
                                    "dragon_lord_hat",
                                    "dragon_lord_helmet",
                                    "dragon_lord_leggings",
                                    "dragon_lord_rapier_sword",
                                    "dragon_lord_scythe",
                                    "dragon_lord_spear",
                                    "dragon_lord_staff",
                                    "dragon_lord_sword",
                                    "dragon_lord_trident",
                                    "dragon_lord_wing"
                            };
                }

                case 2 -> {

                    chestItemId =
                            "illusion_ruler_chest";

                    rewardName =
                            "환영 지배자의 보관함";

                    itemIds =
                            new String[] {
                                    "illusion_ruler_sword",
                                    "illusion_ruler_hammer",
                                    "illusion_ruler_staff",
                                    "illusion_ruler_scythe",
                                    "illusion_ruler_spear",
                                    "illusion_ruler_rapier_sword",
                                    "illusion_ruler_dagger",
                                    "illusion_ruler_axe",
                                    "illusion_ruler_trident",
                                    "illusion_ruler_bow",
                                    "illusion_ruler_crossbow",
                                    "illusion_ruler_shield",
                                    "illusion_ruler_helmet",
                                    "illusion_ruler_chestplate",
                                    "illusion_ruler_leggings",
                                    "illusion_ruler_boots",
                                    "illusion_ruler_wing",
                                    "illusion_ruler_hat"
                            };
                }

                default -> {

                    chestItemId =
                            "sky_guardian_chest";

                    rewardName =
                            "천공 수호자의 보관함";

                    itemIds =
                            new String[] {
                                    "sky_guardian_axe",
                                    "sky_guardian_boots",
                                    "sky_guardian_bow",
                                    "sky_guardian_chestplate",
                                    "sky_guardian_crossbow",
                                    "sky_guardian_dagger",
                                    "sky_guardian_hammer",
                                    "sky_guardian_hat",
                                    "sky_guardian_helmet",
                                    "sky_guardian_leggings",
                                    "sky_guardian_rapier_sword",
                                    "sky_guardian_scythe",
                                    "sky_guardian_shield",
                                    "sky_guardian_spear",
                                    "sky_guardian_staff",
                                    "sky_guardian_sword",
                                    "sky_guardian_trident",
                                    "sky_guardian_wing",
                                    "sky_guardian_wing_1"
                            };
                }
            }

            ItemStack[] contents =
                    new ItemStack[
                            StorageChestRepository
                                    .INVENTORY_SIZE
                    ];

            int slot = 0;

            for (String itemId : itemIds) {

                ItemStack item =
                        itemFactory
                                .createCustomItem(
                                        itemId
                                );

                if (item == null) {

                    plugin.getLogger()
                            .warning(
                                    "[DungeonReward] "
                                            + "Minotaur mythic item creation failed: "
                                            + itemId
                            );

                    continue;
                }

                contents[slot++] =
                        item;
            }

            ItemStack baseChest =
                    itemFactory
                            .createCustomItem(
                                    chestItemId
                            );

            if (baseChest == null) {

                plugin.getLogger()
                        .severe(
                                "[DungeonReward] "
                                        + "Minotaur mythic chest creation failed: "
                                        + chestItemId
                        );

                continue;
            }

            ItemStack rewardChest;

            try {

                rewardChest =
                        storageChestService
                                .createPreloadedChest(
                                        baseChest,
                                        uuid,
                                        contents
                                );

            } catch (Exception exception) {

                plugin.getLogger()
                        .severe(
                                "[DungeonReward] "
                                        + "Minotaur mythic chest preload failed for "
                                        + uuid
                                        + ": "
                                        + exception.getMessage()
                        );

                exception.printStackTrace();

                continue;
            }

            Map<Integer, ItemStack> leftovers =
                    player.getInventory()
                            .addItem(
                                    rewardChest
                            );

            if (!leftovers.isEmpty()) {

                plugin.getLogger()
                        .warning(
                                "[DungeonReward] "
                                        + "Minotaur mythic chest could not fit "
                                        + "in inventory for "
                                        + player.getName()
                        );

                player.sendMessage(
                        Component.text(
                                "[던전 보상] 신화 보관함을 받을 "
                                        + "인벤토리 공간이 없습니다.",
                                NamedTextColor.RED
                        )
                );

                continue;
            }

            player.sendMessage(
                    Component.text(
                            "[신화 보상] ",
                            NamedTextColor.GOLD
                    ).append(
                            Component.text(
                                    rewardName
                                            + "을 획득했습니다!",
                                    NamedTextColor.LIGHT_PURPLE
                            )
                    )
            );
        }
    }


    private record RewardSpec(
            String type,
            int amount
    ) {
    }
}
