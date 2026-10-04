package com.hcs.rpgcore.command;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.hud.HudService;
import com.hcs.rpgcore.level.LevelService;
import com.hcs.rpgcore.level.LevelUpResult;
import com.hcs.rpgcore.mana.ManaService;
import com.hcs.rpgcore.item.CustomItemFactory;
import com.hcs.rpgcore.player.PlayerData;
import com.hcs.rpgcore.player.PlayerRepository;
import com.hcs.rpgcore.stat.PlayerStatApplier;
import com.hcs.rpgcore.stat.PlayerStats;
import com.hcs.rpgcore.stat.StatService;

import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

import com.hcs.rpgcore.skill.SkillIds;
import com.hcs.rpgcore.skill.SkillItemFactory;
import com.hcs.rpgcore.skill.SkillRewardService;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
public final class ClassCommand
        implements CommandExecutor {

    private final RPGCorePlugin plugin;
    private final PlayerRepository playerRepository;
    private final LevelService levelService;

    private final StatService statService;
    private final PlayerStatApplier statApplier;

    private final ManaService manaService;
    private final HudService hudService;

    private final SkillItemFactory skillItemFactory;
    private final SkillRewardService skillRewardService;

    private final CustomItemFactory customItemFactory;



    public ClassCommand(
RPGCorePlugin plugin,
            PlayerRepository playerRepository,
            LevelService levelService,
            StatService statService,
            PlayerStatApplier statApplier,
            ManaService manaService,
            HudService hudService,
            SkillItemFactory skillItemFactory,
            SkillRewardService skillRewardService
    ) {

        this.plugin =
                plugin;

        this.playerRepository =
                playerRepository;

        this.levelService =
                levelService;

        this.statService =
                statService;

        this.statApplier =
                statApplier;

        this.manaService =
                manaService;

        this.hudService =
                hudService;
    

        this.skillItemFactory =
                skillItemFactory;

        this.skillRewardService =
                skillRewardService;

        this.customItemFactory =
                new CustomItemFactory(
                        plugin
                );
}


    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    Component.text(
                            "직업 선택은 게임 내 플레이어만 사용할 수 있습니다.",
                            NamedTextColor.RED
                    )
            );

            return true;
        }


        if (args.length != 1) {

            sendUsage(
                    player
            );

            return true;
        }


        String selectedClass =
                normalizeClass(
                        args[0]
                );


        if (selectedClass == null) {

            sendUsage(
                    player
            );

            return true;
        }


        UUID uuid =
                player.getUniqueId();


        Bukkit.getScheduler()
                .runTaskAsynchronously(
                        plugin,
                        () -> selectClass(
                                uuid,
                                selectedClass
                        )
                );


        return true;
    }


    /*
     * =========================================================
     * CLASS SELECTION
     * =========================================================
     */

    private void selectClass(
            UUID uuid,
            String selectedClass
    ) {

        try {

            PlayerData playerData =
                    playerRepository.findPlayer(
                            uuid
                    );


            if (playerData == null) {

                sendSync(
                        uuid,
                        Component.text(
                                "RPG 플레이어 데이터를 찾을 수 없습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            /*
             * Lv.10 미만에서는 전직할 수 없다.
             */
            if (playerData.getLevel() < 10) {

                sendSync(
                        uuid,
                        Component.text(
                                "직업은 Lv.10부터 선택할 수 있습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            /*
             * 이미 직업을 선택했다면
             * 다시 선택할 수 없다.
             */
            String currentClass =
                    playerData.getPlayerClass();


            if (
                    currentClass != null
                    && !currentClass.isBlank()
                    && !"NONE".equalsIgnoreCase(
                            currentClass
                    )
            ) {

                sendSync(
                        uuid,
                        Component.text(
                                "이미 직업을 선택했습니다: "
                                        + displayClassName(
                                                currentClass
                                        ),
                                NamedTextColor.RED
                        )
                );

                return;
            }


            /*
             * 먼저 PlayerData에 직업을 적용한다.
             *
             * 그래야 LevelService에서
             * Lv.10 전직 잠금이 해제된다.
             */
            playerData.setPlayerClass(
                    selectedClass
            );


            /*
             * Lv.10에서 쌓아 둔 EXP.
             */
            long storedExperience =
                    playerData.getExperience();


            /*
             * 기존 누적 EXP를 다시 레벨 계산에 넣기 위해
             * 현재 EXP를 0으로 만든다.
             */
            playerData.setExperience(
                    0L
            );


            LevelUpResult result =
                    levelService.addExperience(
                            playerData,
                            storedExperience
                    );


            /*
             * 직업 / 레벨 / 남은 EXP를
             * 하나의 SQL UPDATE로 저장한다.
             */
            playerRepository
                    .updateClassLevelAndExperience(
                            uuid,
                            selectedClass,
                            playerData.getLevel(),
                            playerData.getExperience()
                    );

            if (result.leveledUp() && playerData.getLevel() >= 80) {
                new com.hcs.rpgcore.title.LevelTitleUnlockService(plugin)
                        .checkLevel(uuid, playerData.getLevel());
            }

            plugin.registerLevel10ShieldReward(
                    uuid,
                    result.oldLevel(),
                    result.newLevel()
            );


            /*
             * HUD 프로필 즉시 갱신.
             */
            hudService.updateProfile(
                    playerData
            );


            /*
             * 선택한 직업과 현재 레벨 기준
             * 플레이어 스탯 재계산.
             */
            PlayerStats stats =
                    statService.calculate(
                            playerData
                    );


            /*
             * 최대 MANA 갱신.
             *
             * 현재 마나 비율은 유지한다.
             */
            manaService.updateMaximumMana(
                    uuid,
                    stats.maxMana(),
                    true
            );


            /*
             * Bukkit Player API 처리는
             * 메인 스레드에서 수행한다.
             */
            Bukkit.getScheduler()
                    .runTask(
                            plugin,
                            () -> applySelection(
                                    uuid,
                                    selectedClass,
                                    playerData,
                                    result,
                                    stats
                            )
                    );


        } catch (SQLException exception) {

            plugin.getLogger()
                    .severe(
                            "직업 선택 DB 처리 실패: "
                                    + exception.getMessage()
                    );

            exception.printStackTrace();


            sendSync(
                    uuid,
                    Component.text(
                            "직업 선택 중 데이터베이스 오류가 발생했습니다.",
                            NamedTextColor.RED
                    )
            );
        }
    }


    /*
     * =========================================================
     * APPLY TO ONLINE PLAYER
     * =========================================================
     */

    private void applySelection(
            UUID uuid,
            String selectedClass,
            PlayerData playerData,
            LevelUpResult result,
            PlayerStats stats
    ) {

        Player player =
                Bukkit.getPlayer(
                        uuid
                );


        if (
                player == null
                || !player.isOnline()
        ) {
            return;
        }

        skillRewardService.syncRewards(
                player,
                playerData
        );

        giveStarterWeapon(
                player
        );


        /*
         * MAX_HEALTH / ATTACK_DAMAGE 실제 Attribute 갱신.
         *
         * HP / MP는 레벨 + 직업만 사용하고,
         * 실제 공격력 적용 시에는 현재 착용 장비까지
         * 포함한 최종 스탯을 다시 계산한다.
         */
        PlayerStats equippedStats =
                statService.calculate(
                        playerData,
                        player
                );

        statApplier.apply(
                player,
                equippedStats
        );


        /*
         * HUD를 한 번 더 갱신해서
         * 직업명/레벨/EXP 표시를 즉시 반영한다.
         */
        hudService.updateProfile(
                playerData
        );


        player.sendMessage(
                Component.text(
                        "직업을 선택했습니다: "
                                + displayClassName(
                                        selectedClass
                                ),
                        NamedTextColor.GOLD
                )
        );


        /*
         * Lv.10에서 누적한 EXP 때문에
         * 전직 직후 추가 레벨업한 경우.
         */
        if (result.leveledUp()) {

            player.sendMessage(
                    Component.text(
                            "누적 EXP가 적용되어 Lv."
                                    + playerData.getLevel()
                                    + "이 되었습니다.",
                            NamedTextColor.GREEN
                    )
            );
        }
    }


    /*
     * =========================================================
     * STARTER WEAPON
     * =========================================================
     */
    private void giveStarterWeapon(
            Player player
    ) {

        ItemStack weapon =
                customItemFactory.create(
                        "teyvat_key_of_khaj_nisut"
                );

        if (weapon == null) {

            plugin.getLogger()
                    .warning(
                            "Failed to create starter weapon: "
                                    + "teyvat_key_of_khaj_nisut"
                    );

            return;
        }

        Map<Integer, ItemStack> leftovers =
                player.getInventory()
                        .addItem(
                                weapon
                        );

        for (
                ItemStack leftover
                : leftovers.values()
        ) {

            org.bukkit.entity.Item droppedItem =
                    player.getWorld()
                            .dropItemNaturally(
                                    player.getLocation(),
                                    leftover
                            );

            droppedItem.setOwner(
                    player.getUniqueId()
            );
        }

        ItemStack enhancementStone =
                customItemFactory.create(
                        "enhancement_stone"
                );

        if (
                enhancementStone != null
                        && !enhancementStone
                                .getType()
                                .isAir()
        ) {

            enhancementStone.setAmount(
                    2
            );

            Map<Integer, ItemStack> stoneLeftovers =
                    player.getInventory()
                            .addItem(
                                    enhancementStone
                            );

            for (
                    ItemStack leftover
                    : stoneLeftovers.values()
            ) {

                org.bukkit.entity.Item droppedItem =
                        player.getWorld()
                                .dropItemNaturally(
                                        player.getLocation(),
                                        leftover
                                );

                droppedItem.setOwner(
                        player.getUniqueId()
                );
            }

        } else {

            plugin.getLogger()
                    .warning(
                            "Failed to create starter enhancement stones: "
                                    + "enhancement_stone"
                    );
        }


        /*
         * =========================================================
         * CLASS SELECTION REWARD - METAL ARMOR SET
         * =========================================================
         */
        String[] metalArmorIds = {
                "metal_helmet",
                "metal_chestplate",
                "metal_leggings",
                "metal_boots"
        };

        int metalArmorGiven = 0;

        for (String itemId : metalArmorIds) {

            ItemStack armor =
                    customItemFactory.create(
                            itemId
                    );

            if (
                    armor == null
                            || armor.getType().isAir()
            ) {

                plugin.getLogger()
                        .warning(
                                "Failed to create class reward armor: "
                                        + itemId
                        );

                continue;
            }

            Map<Integer, ItemStack> armorLeftovers =
                    player.getInventory()
                            .addItem(
                                    armor
                            );

            for (
                    ItemStack leftover
                    : armorLeftovers.values()
            ) {

                org.bukkit.entity.Item droppedItem =
                        player.getWorld()
                                .dropItemNaturally(
                                        player.getLocation(),
                                        leftover
                                );

                droppedItem.setOwner(
                        player.getUniqueId()
                );
            }

            metalArmorGiven++;
        }

        if (metalArmorGiven == metalArmorIds.length) {

            player.sendMessage(
                    Component.text(
                            "[전직 보상] ",
                            NamedTextColor.GOLD
                    ).append(
                            Component.text(
                                    "Metal 방어구 세트 4종을 획득했습니다.",
                                    NamedTextColor.GREEN
                            )
                    )
            );

        } else {

            player.sendMessage(
                    Component.text(
                            "[전직 보상] ",
                            NamedTextColor.GOLD
                    ).append(
                            Component.text(
                                    "Metal 방어구 지급 중 오류가 발생했습니다. "
                                            + "관리자에게 문의하세요.",
                                    NamedTextColor.RED
                            )
                    )
            );
        }


        player.sendMessage(
                Component.text(
                        "[전직 보상] ",
                        NamedTextColor.GOLD
                ).append(
                        Component.text(
                                "숙련된 모험가의 검을 획득했습니다.",
                                NamedTextColor.YELLOW
                        )
                )
        );

        player.sendMessage(
                Component.text(
                        "[전직 보상] ",
                        NamedTextColor.GOLD
                ).append(
                        Component.text(
                                "강화석 x2를 획득했습니다.",
                                NamedTextColor.GREEN
                        )
                )
        );
    }


    /*
     * =========================================================
     * CLASS NAME
     * =========================================================
     */

    private String normalizeClass(
            String value
    ) {

        if (value == null) {
            return null;
        }


        String normalized =
                value.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );


        return switch (normalized) {

            case "warrior", "전사" ->
                    "WARRIOR";

            case "mage", "마법사" ->
                    "MAGE";

            default ->
                    null;
        };
    }


    private String displayClassName(
            String value
    ) {

        if (value == null) {
            return "무직";
        }


        return switch (
                value.toUpperCase(
                        Locale.ROOT
                )
        ) {

            case "WARRIOR" ->
                    "전사";

            case "MAGE" ->
                    "마법사";

            case "NONE" ->
                    "무직";

            default ->
                    value;
        };
    }


    /*
     * =========================================================
     * USAGE
     * =========================================================
     */

    private void sendUsage(
            Player player
    ) {

        player.sendMessage(
                Component.text(
                        "──── 직업 선택 ────",
                        NamedTextColor.GOLD
                )
        );


        player.sendMessage(
                Component.text(
                        "/class warrior  - 전사",
                        NamedTextColor.YELLOW
                )
        );


        player.sendMessage(
                Component.text(
                        "/class mage  - 마법사",
                        NamedTextColor.AQUA
                )
        );
    }


    /*
     * =========================================================
     * SYNC MESSAGE
     * =========================================================
     */

    private void sendSync(
            UUID uuid,
            Component component
    ) {

        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> {

                            Player player =
                                    Bukkit.getPlayer(
                                            uuid
                                    );


                            if (
                                    player == null
                                    || !player.isOnline()
                            ) {
                                return;
                            }


                            player.sendMessage(
                                    component
                            );
                        }
                );
    }




}
