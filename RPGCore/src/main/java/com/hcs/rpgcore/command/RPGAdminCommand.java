package com.hcs.rpgcore.command;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.hud.HudService;
import com.hcs.rpgcore.item.RPGItemRegistry;
import com.hcs.rpgcore.level.LevelService;
import com.hcs.rpgcore.level.LevelUpResult;
import com.hcs.rpgcore.mana.ManaService;
import com.hcs.rpgcore.mob.EliteDemonKnightListener;
import com.hcs.rpgcore.mob.RedstoneGolemSpawnService;
import com.hcs.rpgcore.mob.WarbringerSpawnService;
import com.hcs.rpgcore.player.PlayerData;
import com.hcs.rpgcore.player.PlayerRepository;
import com.hcs.rpgcore.stat.PlayerStatApplier;
import com.hcs.rpgcore.stat.PlayerStats;
import com.hcs.rpgcore.stat.StatService;
import com.hcs.rpgcore.stat.ItemStatKeys;
import com.hcs.rpgcore.stat.ItemStatWriter;

import com.hcs.rpgcore.starter.StarterKitListener;
import com.hcs.rpgcore.skill.SkillRewardService;
import java.sql.SQLException;
import java.util.Locale;
import java.util.List;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

import com.google.common.collect.Multimap;

import org.jetbrains.annotations.NotNull;

public final class RPGAdminCommand
        implements CommandExecutor {

    private final RPGCorePlugin plugin;
    private final PlayerRepository playerRepository;

    private final com.hcs.rpgcore.player.PlayerNameTagService
            playerNameTagService;

    private final LevelService levelService;

    private final StatService statService;
    private final PlayerStatApplier statApplier;

    private final ManaService manaService;
    private final HudService hudService;
    private final SkillRewardService skillRewardService;

    private final ItemStatWriter itemStatWriter;

    private final RPGItemRegistry rpgItemRegistry;

    public RPGAdminCommand(
            RPGCorePlugin plugin,
            PlayerRepository playerRepository,
            com.hcs.rpgcore.player.PlayerNameTagService playerNameTagService,
            LevelService levelService,
            StatService statService,
            PlayerStatApplier statApplier,
            ManaService manaService,
            HudService hudService,
            SkillRewardService skillRewardService
    ) {
        this.plugin = plugin;
        this.playerRepository = playerRepository;
        this.playerNameTagService = playerNameTagService;
        this.levelService = levelService;
        this.statService = statService;
        this.statApplier = statApplier;
        this.manaService = manaService;
        this.hudService = hudService;
        this.skillRewardService = skillRewardService;

        this.itemStatWriter =
                new ItemStatWriter(
                        new ItemStatKeys(
                                plugin
                        )
                );

        this.rpgItemRegistry =
                new RPGItemRegistry(
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

        if (!sender.hasPermission("rpgcore.admin")) {

            sender.sendMessage(
                    Component.text(
                            "권한이 없습니다.",
                            NamedTextColor.RED
                    )
            );

            return true;
        }

        if (args.length < 1) {

            sendUsage(sender);

            return true;
        }

        String category =
                args[0].toLowerCase(Locale.ROOT);

        /*
         * MANA 명령은 인자 수가 다르므로
         * EXP / LEVEL보다 먼저 처리한다.
         */
        if ("mana".equals(category)) {

            handleManaCommand(
                    sender,
                    args
            );

            return true;
        }


        /*
         * =====================================================
         * ELITE TEST SPAWN
         * =====================================================
         *
         * /rpgadmin elite spawn demon_knight
         * /rpgadmin elite spawn redstone_golem
         * /rpgadmin elite spawn warbringer
         */
        if ("elite".equals(category)) {

            handleEliteCommand(
                    sender,
                    args
            );

            return true;
        }


        /*
         * =====================================================
         * NICKNAME
         * =====================================================
         *
         * /rpgadmin nickname set <player> <name>
         * /rpgadmin nickname clear <player>
         */
        if ("nickname".equals(category)) {

            handleNicknameCommand(
                    sender,
                    args
            );

            return true;
        }


        /*
         * =====================================================
         * RPG ITEM
         * =====================================================
         *
         * /rpgadmin item list
         * /rpgadmin item give <player> <itemId> [amount]
         */
        if ("item".equals(category)) {

            handleItemCommand(
                    sender,
                    args
            );

            return true;
        }


        /*
         * =====================================================
         * WORLD
         * =====================================================
         *
         * /rpgadmin world create <world> <seed>
         * /rpgadmin world tp <player> <world>
         * /rpgadmin world list
         */
        if ("world".equals(category)) {

            handleWorldCommand(
                    sender,
                    args
            );

            return true;
        }


        /*
         * =====================================================
         * VISUAL TEST
         * =====================================================
         *
         */
        /*
         * =====================================================
         * REPAIR
         * =====================================================
         *
         * /rpgadmin repair <player> all
         */
        if ("repair".equals(category)) {

            handleRepairCommand(
                    sender,
                    args
            );

            return true;
        }



        /*
         * ITEMSTAT은 플레이어가 현재 주손에 들고 있는
         * 아이템을 대상으로 한다.
         */
        if ("itemstat".equals(category)) {

            handleItemStatCommand(
                    sender,
                    args
            );

            return true;
        }


        /*
         * STARTER KIT
         *
         * /rpgadmin starterkit <player>
         */
        if ("starterkit".equals(category)) {

            handleStarterKitCommand(
                    sender,
                    args
            );

            return true;
        }


        if (args.length < 4) {

            sendUsage(sender);

            return true;
        }

        String operation =
                args[1].toLowerCase(Locale.ROOT);

        String targetName =
                args[2];

        long amount;

        try {

            amount =
                    Long.parseLong(
                            args[3]
                    );

        } catch (NumberFormatException exception) {

            sender.sendMessage(
                    Component.text(
                            "숫자를 정확하게 입력해 주세요.",
                            NamedTextColor.RED
                    )
            );

            return true;
        }

        OfflinePlayer target =
                Bukkit.getOfflinePlayer(
                        targetName
                );

        UUID uuid =
                target.getUniqueId();

        switch (category) {

            case "exp" -> {

                if ("add".equals(operation)) {

                    handleExperienceAdd(
                            sender,
                            uuid,
                            targetName,
                            amount
                    );

                    return true;
                }

                if ("set".equals(operation)) {

                    handleExperienceSet(
                            sender,
                            uuid,
                            targetName,
                            amount
                    );

                    return true;
                }
            }

            case "level" -> {

                if ("set".equals(operation)) {

                    handleLevelSet(
                            sender,
                            uuid,
                            targetName,
                            amount
                    );

                    return true;
                }
            }

            default -> {
            }
        }

        sendUsage(sender);

        return true;
    }

    /*
     * =========================================================
     * NICKNAME COMMAND
     * =========================================================
     */
    private void handleNicknameCommand(
            CommandSender sender,
            String[] args
    ) {

        if (args.length < 3) {

            sender.sendMessage(
                    Component.text(
                            "/rpgadmin nickname set <player> <name>",
                            NamedTextColor.YELLOW
                    )
            );

            sender.sendMessage(
                    Component.text(
                            "/rpgadmin nickname clear <player>",
                            NamedTextColor.YELLOW
                    )
            );

            return;
        }


        String operation =
                args[1].toLowerCase(
                        Locale.ROOT
                );

        String targetName =
                args[2];

        OfflinePlayer target =
                Bukkit.getOfflinePlayer(
                        targetName
                );

        UUID uuid =
                target.getUniqueId();


        try {

            if ("set".equals(operation)) {

                if (args.length < 4) {

                    sender.sendMessage(
                            Component.text(
                                    "설정할 닉네임을 입력해 주세요.",
                                    NamedTextColor.RED
                            )
                    );

                    return;
                }


                String displayName =
                        String.join(
                                " ",
                                java.util.Arrays.copyOfRange(
                                        args,
                                        3,
                                        args.length
                                )
                        ).trim();


                if (
                        displayName.isEmpty()
                                ||
                        displayName.length() > 32
                ) {

                    sender.sendMessage(
                            Component.text(
                                    "닉네임은 1~32자로 설정해 주세요.",
                                    NamedTextColor.RED
                            )
                    );

                    return;
                }


                playerRepository.updateDisplayName(
                        uuid,
                        displayName
                );


                Player online =
                        Bukkit.getPlayer(
                                uuid
                        );

                if (online != null) {

                    Component component =
                            Component.text(
                                    displayName
                            );

                    online.displayName(
                            component
                    );

                    online.playerListName(
                            component
                    );

                    playerNameTagService.apply(
                            online,
                            displayName
                    );
                }


                sender.sendMessage(
                        Component.text(
                                targetName
                                        + "의 표시 이름을 "
                                        + displayName
                                        + "(으)로 설정했습니다.",
                                NamedTextColor.GREEN
                        )
                );

                return;
            }


            if ("clear".equals(operation)) {

                playerRepository.clearDisplayName(
                        uuid
                );


                Player online =
                        Bukkit.getPlayer(
                                uuid
                        );

                if (online != null) {

                    Component component =
                            Component.text(
                                    online.getName()
                            );

                    online.displayName(
                            component
                    );

                    online.playerListName(
                            component
                    );

                    playerNameTagService.clear(
                            online
                    );
                }


                sender.sendMessage(
                        Component.text(
                                targetName
                                        + "의 표시 이름을 해제했습니다.",
                                NamedTextColor.GREEN
                        )
                );

                return;
            }


            sender.sendMessage(
                    Component.text(
                            "사용법: /rpgadmin nickname "
                                    + "<set|clear>",
                            NamedTextColor.RED
                    )
            );


        } catch (SQLException exception) {

            sender.sendMessage(
                    Component.text(
                            "닉네임 DB 저장에 실패했습니다.",
                            NamedTextColor.RED
                    )
            );

            plugin.getLogger()
                    .severe(
                            "[Nickname] "
                                    + exception.getMessage()
                    );
        }
    }


    /*
     * =========================================================
     * RPG ITEM COMMAND
     * =========================================================
     */
    private void handleItemCommand(
            CommandSender sender,
            String[] args
    ) {

        if (args.length < 2) {

            sendItemCommandUsage(
                    sender
            );

            return;
        }


        String operation =
                args[1].toLowerCase(
                        Locale.ROOT
                );


        /*
         * =====================================================
         * LIST
         * =====================================================
         */
        if ("list".equals(operation)) {

            sender.sendMessage(
                    Component.text(
                            "──── RPGCore Item Registry ────",
                            NamedTextColor.GOLD
                    )
            );


            for (
                    RPGItemRegistry.RegisteredItemInfo item
                    : rpgItemRegistry.list()
            ) {

                String binding =
                        item.bound()
                                ? "귀속"
                                : "비귀속";


                sender.sendMessage(
                        Component.text(
                                item.id()
                                        + " - "
                                        + item.displayName()
                                        + " ["
                                        + binding
                                        + "]",
                                item.bound()
                                        ? NamedTextColor.AQUA
                                        : NamedTextColor.YELLOW
                        )
                );
            }


            return;
        }


        /*
         * =====================================================
         * RELOAD
         * =====================================================
         *
         * /rpgadmin item reload
         */
        if ("reload".equals(operation)) {

            try {

                rpgItemRegistry.reload();

                sender.sendMessage(
                        Component.text(
                                "RPG 아이템 DB Registry를 다시 불러왔습니다. "
                                        + "등록 아이템: "
                                        + rpgItemRegistry.list().size(),
                                NamedTextColor.GREEN
                        )
                );

            } catch (RuntimeException exception) {

                sender.sendMessage(
                        Component.text(
                                "RPG 아이템 DB Registry reload에 실패했습니다.",
                                NamedTextColor.RED
                        )
                );

                plugin.getLogger().severe(
                        "RPG item registry reload failed: "
                                + exception.getMessage()
                );

                exception.printStackTrace();
            }

            return;
        }


        /*
         * =====================================================
         * GIVE
         * =====================================================
         *
         * /rpgadmin item give <player> <itemId> [amount]
         */
        if (!"give".equals(operation)) {

            sendItemCommandUsage(
                    sender
            );

            return;
        }


        if (args.length < 4) {

            sendItemCommandUsage(
                    sender
            );

            return;
        }


        String targetName =
                args[2];


        String itemId =
                args[3]
                        .toLowerCase(
                                Locale.ROOT
                        );


        /*
         * =====================================================
         * ITEM SET
         * =====================================================
         *
         * /rpgadmin item give <player> mal_nyun set
         * /rpgadmin item give <player> sky_guardian set
         * /rpgadmin item give <player> ancient_dragon set
         * /rpgadmin item give <player> dragon_lord set
         * /rpgadmin item give <player> forgotten_ruins set
         *
         * 기존 [amount] 숫자 인자보다 먼저 판별한다.
         */
        if (
                args.length >= 5
                && "set".equalsIgnoreCase(
                        args[4]
                )
        ) {

            handleItemSetGive(
                    sender,
                    targetName,
                    itemId
            );

            return;
        }


        int amount =
                1;


        if (args.length >= 5) {

            try {

                amount =
                        Integer.parseInt(
                                args[4]
                        );

            } catch (NumberFormatException exception) {

                sender.sendMessage(
                        Component.text(
                                "수량은 숫자로 입력해 주세요.",
                                NamedTextColor.RED
                        )
                );

                return;
            }
        }


        if (
                amount < 1
                || amount > 64
        ) {

            sender.sendMessage(
                    Component.text(
                            "수량은 1~64 사이로 입력해 주세요.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        if (
                !rpgItemRegistry.contains(
                        itemId
                )
        ) {

            sender.sendMessage(
                    Component.text(
                            "등록되지 않은 RPGCore 아이템 ID입니다: "
                                    + itemId,
                            NamedTextColor.RED
                    )
            );

            sender.sendMessage(
                    Component.text(
                            "/rpgadmin item list",
                            NamedTextColor.YELLOW
                    )
            );

            return;
        }


        Player target =
                Bukkit.getPlayerExact(
                        targetName
                );


        if (
                target == null
                || !target.isOnline()
        ) {

            sender.sendMessage(
                    Component.text(
                            "아이템 지급 대상은 온라인 상태여야 합니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        List<ItemStack> items;

        try {

            items =
                    rpgItemRegistry.create(
                            itemId,
                            target,
                            amount
                    );

        } catch (RuntimeException exception) {

            sender.sendMessage(
                    Component.text(
                            "RPGCore 아이템 생성에 실패했습니다.",
                            NamedTextColor.RED
                    )
            );


            plugin.getLogger()
                    .severe(
                            "RPG item creation failed: "
                                    + itemId
                                    + " / "
                                    + exception.getMessage()
                    );


            exception.printStackTrace();

            return;
        }


        if (items.isEmpty()) {

            sender.sendMessage(
                    Component.text(
                            "아이템을 생성하지 못했습니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        /*
         * 인벤토리에 전체 아이템이 들어갈 수 있는지
         * 먼저 가상 계산한다.
         *
         * 공간이 부족하면 일부만 지급하지 않고
         * 전체 지급을 취소한다.
         */
        if (
                !canFitAllItems(
                        target,
                        items
                )
        ) {

            sender.sendMessage(
                    Component.text(
                            target.getName()
                                    + "의 인벤토리 공간이 부족합니다. "
                                    + "아무 아이템도 지급하지 않았습니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        for (ItemStack item : items) {

            target.getInventory()
                    .addItem(
                            item
                    );
        }


        sender.sendMessage(
                Component.text(
                        "[RPG ITEM] "
                                + target.getName()
                                + "에게 "
                                + itemId
                                + " x"
                                + amount
                                + " 지급 완료.",
                        NamedTextColor.GREEN
                )
        );


        if (!sender.equals(target)) {

            target.sendMessage(
                    Component.text(
                            "[RPG ITEM] "
                                    + itemId
                                    + " x"
                                    + amount
                                    + " 아이템을 지급받았습니다.",
                            NamedTextColor.GREEN
                    )
            );
        }
    }


    /*
     * =========================================================
     * RPG ITEM SET GIVE
     * =========================================================
     *
     * /rpgadmin item give <player> mal_nyun set
     * /rpgadmin item give <player> sky_guardian set
     *
     * 세트 전체를 먼저 생성한 뒤
     * 인벤토리에 모두 들어갈 수 있을 때만 지급한다.
     *
     * 공간이 부족하면 일부만 지급하지 않는다.
     */
    private void handleItemSetGive(
            CommandSender sender,
            String targetName,
            String setId
    ) {

        Player target =
                Bukkit.getPlayerExact(
                        targetName
                );


        if (
                target == null
                || !target.isOnline()
        ) {

            sender.sendMessage(
                    Component.text(
                            "아이템 지급 대상은 온라인 상태여야 합니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        String[] itemIds;

        String setDisplayName;


        /*
         * =====================================================
         * UNDEAD KNIGHT / NETHER LORD / MAL_NYUN SETS
         * =====================================================
         */
        if (
                "undead_knight".equals(
                        setId
                )
        ) {

            setDisplayName =
                    "언데드 기사 세트";

            itemIds =
                    new String[] {
                            "undead_knight_helmet",
                            "undead_knight_chestplate",
                            "undead_knight_leggings",
                            "undead_knight_boots"
                    };


        } else if (
                "nether_lord".equals(
                        setId
                )
        ) {

            setDisplayName =
                    "네더 군주 세트";

            itemIds =
                    new String[] {
                            "nether_lord_sword",
                            "nether_lord_helmet",
                            "nether_lord_chestplate",
                            "nether_lord_leggings",
                            "nether_lord_boots"
                    };


        } else if (
                "nether_lord_legend".equals(
                        setId
                )
        ) {

            setDisplayName =
                    "네더 군주 전설 세트";

            itemIds =
                    new String[] {
                            "nether_lord_legend_deathside",
                            "nether_lord_legend_helmet",
                            "nether_lord_legend_chestplate",
                            "nether_lord_legend_leggings",
                            "nether_lord_legend_boots"
                    };


        } else if (
                "mal_nyun".equals(
                        setId
                )
        ) {

            setDisplayName =
                    "말년용사 세트";

            itemIds =
                    new String[] {
                            "mal_nyun_sword",
                            "mal_nyun_helmet",
                            "mal_nyun_chestplate",
                            "mal_nyun_leggings",
                            "mal_nyun_boots"
                    };


        /*
         * =====================================================
         * SKY GUARDIAN SET
         * =====================================================
         */
        } else if (
                "sky_guardian".equals(
                        setId
                )
        ) {

            setDisplayName =
                    "천공 수호자 세트";

            itemIds =
                    new String[] {
                            "sky_guardian_sword",
                            "sky_guardian_hammer",
                            "sky_guardian_staff",
                            "sky_guardian_scythe",
                            "sky_guardian_spear",
                            "sky_guardian_rapier_sword",
                            "sky_guardian_dagger",
                            "sky_guardian_axe",
                            "sky_guardian_trident",
                            "sky_guardian_bow",
                            "sky_guardian_crossbow",
                            "sky_guardian_shield",
                            "sky_guardian_helmet",
                            "sky_guardian_chestplate",
                            "sky_guardian_leggings",
                            "sky_guardian_boots",
                            "sky_guardian_wing",
                            "sky_guardian_hat"
                    };


        /*
         * =====================================================
         * ANCIENT DRAGON SET
         * =====================================================
         */
        } else if (
                "ancient_dragon".equals(
                        setId
                )
        ) {

            setDisplayName =
                    "고대 용기사 세트";

            itemIds =
                    new String[] {
                            "ancient_dragon_sword",
                            "ancient_dragon_hammer",
                            "ancient_dragon_staff",
                            "ancient_dragon_scythe",
                            "ancient_dragon_spear",
                            "ancient_dragon_rapier_sword",
                            "ancient_dragon_dagger",
                            "ancient_dragon_axe",
                            "ancient_dragon_trident",
                            "ancient_dragon_bow",
                            "ancient_dragon_crossbow",
                            "ancient_dragon_shield",
                            "ancient_dragon_helmet",
                            "ancient_dragon_chestplate",
                            "ancient_dragon_leggings",
                            "ancient_dragon_boots",
                            "ancient_dragon_wing",
                            "ancient_dragon_hat"
                    };


        /*
         * =====================================================
         * DRAGON LORD SET
         * =====================================================
         */
        } else if (
                "dragon_lord".equals(
                        setId
                )
        ) {

            setDisplayName =
                    "마룡 군주 세트";

            itemIds =
                    new String[] {
                            "dragon_lord_sword",
                            "dragon_lord_hammer",
                            "dragon_lord_staff",
                            "dragon_lord_scythe",
                            "dragon_lord_spear",
                            "dragon_lord_rapier_sword",
                            "dragon_lord_dagger",
                            "dragon_lord_axe",
                            "dragon_lord_trident",
                            "dragon_lord_bow",
                            "dragon_lord_crossbow",
                            "dragon_lord_shield",
                            "dragon_lord_helmet",
                            "dragon_lord_chestplate",
                            "dragon_lord_leggings",
                            "dragon_lord_boots",
                            "dragon_lord_wing",
                            "dragon_lord_hat"
                    };


        /*
         * =====================================================
         * ILLUSION RULER SET
         * =====================================================
         */
        } else if (
                "illusion_ruler".equals(
                        setId
                )
        ) {

            setDisplayName =
                    "환영 지배자의 세트";

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


        /*
         * =====================================================
         * FORGOTTEN RUINS SET
         * =====================================================
         */
        } else if (
                "forgotten_ruins".equals(
                        setId
                )
        ) {

            setDisplayName =
                    "잊혀진 유적 세트";

            itemIds =
                    new String[] {
                            "forgotten_ruins_sword",
                            "forgotten_ruins_helmet",
                            "forgotten_ruins_chestplate",
                            "forgotten_ruins_leggings",
                            "forgotten_ruins_boots"
                    };


        /*
         * =====================================================
         * AURORITE HEROIC SET
         * =====================================================
         */
        } else if (
                "aurorite".equals(
                        setId
                )
        ) {

            setDisplayName =
                    "오로라이트 영웅 세트";

            itemIds =
                    new String[] {
                            "aurorite_helmet",
                            "aurorite_chestplate",
                            "aurorite_leggings",
                            "aurorite_boots"
                    };


        /*
         * =====================================================
         * AURORITE LEGENDARY SET
         * =====================================================
         */
        } else if (
                "purple_aurorite".equals(
                        setId
                )
        ) {

            setDisplayName =
                    "오로라이트 전설 세트";

            itemIds =
                    new String[] {
                            "purple_aurorite_helmet",
                            "purple_aurorite_chestplate",
                            "purple_aurorite_leggings",
                            "purple_aurorite_boots"
                    };


        } else {

            sender.sendMessage(
                    Component.text(
                            "등록되지 않은 RPGCore 세트 ID입니다: "
                                    + setId,
                            NamedTextColor.RED
                    )
            );

            sender.sendMessage(
                    Component.text(
                            "사용 가능한 세트: "
                                    + "undead_knight, nether_lord, nether_lord_legend, mal_nyun, sky_guardian, ancient_dragon, dragon_lord, forgotten_ruins, aurorite, purple_aurorite",
                            NamedTextColor.YELLOW
                    )
            );

            return;
        }


        /*
         * 모든 세트 아이템 ID가 Registry에 존재하는지
         * 실제 생성 전에 먼저 확인한다.
         */
        for (String currentItemId : itemIds) {

            if (
                    !rpgItemRegistry.contains(
                            currentItemId
                    )
            ) {

                sender.sendMessage(
                        Component.text(
                                "세트 지급 실패: "
                                        + "등록되지 않은 아이템 ID "
                                        + currentItemId,
                                NamedTextColor.RED
                        )
                );

                return;
            }
        }


        java.util.List<ItemStack> allItems =
                new java.util.ArrayList<>();


        /*
         * 세트 전체 생성.
         *
         * 각 아이템은 기존 Registry create()를 그대로 사용하므로
         * CustomItemFactory의 최신 PDC / 옵션이 동일하게 적용된다.
         */
        try {

            for (String currentItemId : itemIds) {

                java.util.List<ItemStack> created =
                        rpgItemRegistry.create(
                                currentItemId,
                                target,
                                1
                        );


                if (
                        created == null
                        || created.isEmpty()
                ) {

                    sender.sendMessage(
                            Component.text(
                                    "세트 지급 실패: "
                                            + currentItemId
                                            + " 아이템을 생성하지 못했습니다.",
                                    NamedTextColor.RED
                            )
                    );

                    return;
                }


                allItems.addAll(
                        created
                );
            }

        } catch (RuntimeException exception) {

            sender.sendMessage(
                    Component.text(
                            "RPGCore 세트 아이템 생성에 실패했습니다.",
                            NamedTextColor.RED
                    )
            );


            plugin.getLogger()
                    .severe(
                            "RPG item set creation failed: "
                                    + setId
                                    + " / "
                                    + exception.getMessage()
                    );


            exception.printStackTrace();

            return;
        }


        if (allItems.isEmpty()) {

            sender.sendMessage(
                    Component.text(
                            "세트 아이템을 생성하지 못했습니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        /*
         * 기존 단일 지급과 동일하게
         * 세트 전체가 들어갈 수 있는지 사전 검사한다.
         */
        if (
                !canFitAllItems(
                        target,
                        allItems
                )
        ) {

            sender.sendMessage(
                    Component.text(
                            target.getName()
                                    + "의 인벤토리 공간이 부족합니다. "
                                    + "세트를 지급하지 않았습니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        /*
         * 전체 지급.
         */
        for (ItemStack item : allItems) {

            target.getInventory()
                    .addItem(
                            item
                    );
        }


        sender.sendMessage(
                Component.text(
                        "[RPG ITEM SET] "
                                + target.getName()
                                + "에게 "
                                + setDisplayName
                                + " 지급 완료. "
                                + "("
                                + allItems.size()
                                + "개)",
                        NamedTextColor.GREEN
                )
        );


        if (!sender.equals(target)) {

            target.sendMessage(
                    Component.text(
                            "[RPG ITEM SET] "
                                    + setDisplayName
                                    + "을 지급받았습니다. "
                                    + "("
                                    + allItems.size()
                                    + "개)",
                            NamedTextColor.GREEN
                    )
            );
        }
    }


    /*
     * =========================================================
     * INVENTORY CAPACITY SIMULATION
     * =========================================================
     */
    private boolean canFitAllItems(
            Player player,
            List<ItemStack> items
    ) {

        ItemStack[] current =
                player.getInventory()
                        .getStorageContents();


        ItemStack[] simulated =
                new ItemStack[
                        current.length
                ];


        for (
                int i = 0;
                i < current.length;
                i++
        ) {

            simulated[i] =
                    current[i] == null
                            ? null
                            : current[i].clone();
        }


        for (ItemStack source : items) {

            if (
                    source == null
                    || source.getType().isAir()
            ) {
                continue;
            }


            ItemStack incoming =
                    source.clone();


            int remaining =
                    incoming.getAmount();


            /*
             * 기존 동일 스택에 합치기.
             */
            for (
                    int i = 0;
                    i < simulated.length
                            && remaining > 0;
                    i++
            ) {

                ItemStack existing =
                        simulated[i];


                if (
                        existing == null
                        || existing.getType().isAir()
                        || !existing.isSimilar(
                                incoming
                        )
                ) {
                    continue;
                }


                int maxStack =
                        Math.min(
                                existing.getMaxStackSize(),
                                incoming.getMaxStackSize()
                        );


                int free =
                        maxStack
                                - existing.getAmount();


                if (free <= 0) {
                    continue;
                }


                int moved =
                        Math.min(
                                free,
                                remaining
                        );


                existing.setAmount(
                        existing.getAmount()
                                + moved
                );


                remaining -=
                        moved;
            }


            /*
             * 빈 슬롯 사용.
             */
            for (
                    int i = 0;
                    i < simulated.length
                            && remaining > 0;
                    i++
            ) {

                ItemStack existing =
                        simulated[i];


                if (
                        existing != null
                        && !existing.getType().isAir()
                ) {
                    continue;
                }


                int moved =
                        Math.min(
                                incoming.getMaxStackSize(),
                                remaining
                        );


                ItemStack placed =
                        incoming.clone();


                placed.setAmount(
                        moved
                );


                simulated[i] =
                        placed;


                remaining -=
                        moved;
            }


            if (remaining > 0) {

                return false;
            }
        }


        return true;
    }


    private void sendItemCommandUsage(
            CommandSender sender
    ) {

        sender.sendMessage(
                Component.text(
                        "/rpgadmin item list",
                        NamedTextColor.YELLOW
                )
        );


        sender.sendMessage(
                Component.text(
                        "/rpgadmin item reload",
                        NamedTextColor.YELLOW
                )
        );


        sender.sendMessage(
                Component.text(
                        "/rpgadmin item give <player> <itemId> [amount]",
                        NamedTextColor.YELLOW
                )
        );


        sender.sendMessage(
                Component.text(
                        "/rpgadmin item give <player> mal_nyun set",
                        NamedTextColor.YELLOW
                )
        );


        sender.sendMessage(
                Component.text(
                        "/rpgadmin item give <player> sky_guardian set",
                        NamedTextColor.YELLOW
                )
        );
        sender.sendMessage(
                Component.text(
                        "/rpgadmin item give <player> ancient_dragon set",
                        NamedTextColor.YELLOW
                )
        );

        sender.sendMessage(
                Component.text(
                        "/rpgadmin item give <player> dragon_lord set",
                        NamedTextColor.YELLOW
                )
        );

        sender.sendMessage(
                Component.text(
                        "/rpgadmin item give <player> forgotten_ruins set",
                        NamedTextColor.YELLOW
                )
        );


        sender.sendMessage(
                Component.text(
                        "/rpgadmin item give <player> undead_knight set",
                        NamedTextColor.YELLOW
                )
        );

        sender.sendMessage(
                Component.text(
                        "/rpgadmin item give <player> nether_lord set",
                        NamedTextColor.YELLOW
                )
        );


    }


    /*
     * =========================================================
     * ITEM STAT
     * =========================================================
     *
     * /rpgadmin itemstat attack <value>
     * /rpgadmin itemstat defense <value>
     * /rpgadmin itemstat info
     * /rpgadmin itemstat clear
     *
     * 현재 주손 아이템을 대상으로 한다.
     */
    private void handleItemStatCommand(
            CommandSender sender,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    Component.text(
                            "ITEMSTAT 명령은 게임 내 플레이어만 사용할 수 있습니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        if (args.length < 2) {

            sendItemStatUsage(
                    sender
            );

            return;
        }


        ItemStack item =
                player.getInventory()
                        .getItemInMainHand();


        if (
                item == null
                || item.getType().isAir()
        ) {

            sender.sendMessage(
                    Component.text(
                            "주손에 아이템을 들어 주세요.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        String operation =
                args[1].toLowerCase(
                        Locale.ROOT
                );


        /*
         * VANILLA ATTRIBUTE DEBUG
         *
         * 아이템을 수정하지 않는다.
         *
         * Paper가 해당 Material에 기본으로 제공하는
         * main-hand Attribute Modifier를 출력한다.
         */
        if ("vanilla".equals(operation)) {

            ItemType itemType =
                    item.getType()
                            .asItemType();

            if (itemType == null) {

                sender.sendMessage(
                        Component.text(
                                "이 Material은 ItemType으로 변환할 수 없습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            Multimap<Attribute, AttributeModifier> modifiers =
                    itemType.getDefaultAttributeModifiers(
                            EquipmentSlot.HAND
                    );


            sender.sendMessage(
                    Component.text(
                            "──── Vanilla Item Attribute ────",
                            NamedTextColor.GOLD
                    )
            );


            sender.sendMessage(
                    Component.text(
                            "Material: "
                                    + item.getType().name(),
                            NamedTextColor.YELLOW
                    )
            );


            double attackDamageModifier =
                    0.0;

            double armorModifier =
                    0.0;


            for (
                    var entry
                    : modifiers.entries()
            ) {

                Attribute attribute =
                        entry.getKey();

                AttributeModifier modifier =
                        entry.getValue();


                sender.sendMessage(
                        Component.text(
                                attribute.getKey()
                                        + " = "
                                        + format(
                                                modifier.getAmount()
                                        )
                                        + " ["
                                        + modifier.getOperation()
                                        + "]",
                                NamedTextColor.GRAY
                        )
                );


                if (
                        attribute.equals(
                                Attribute.ATTACK_DAMAGE
                        )
                ) {

                    attackDamageModifier +=
                            modifier.getAmount();
                }


                if (
                        attribute.equals(
                                Attribute.ARMOR
                        )
                ) {

                    armorModifier +=
                            modifier.getAmount();
                }
            }


            sender.sendMessage(
                    Component.text(
                            "ATTACK_DAMAGE modifier 합계: "
                                    + format(
                                            attackDamageModifier
                                    ),
                            NamedTextColor.RED
                    )
            );


            sender.sendMessage(
                    Component.text(
                            "ARMOR modifier 합계: "
                                    + format(
                                            armorModifier
                                    ),
                            NamedTextColor.BLUE
                    )
            );


            sender.sendMessage(
                    Component.text(
                            "PDC 추가 공격력: "
                                    + format(
                                            itemStatWriter.getAttack(
                                                    item
                                            )
                                    ),
                            NamedTextColor.RED
                    )
            );


            sender.sendMessage(
                    Component.text(
                            "PDC 추가 방어력: "
                                    + format(
                                            itemStatWriter.getDefense(
                                                    item
                                            )
                                    ),
                            NamedTextColor.BLUE
                    )
            );


            return;
        }


        /*
         * INFO
         */
        if ("info".equals(operation)) {

            double attack =
                    itemStatWriter.getAttack(
                            item
                    );

            double defense =
                    itemStatWriter.getDefense(
                            item
                    );


            sender.sendMessage(
                    Component.text(
                            "──── RPG Item Stat ────",
                            NamedTextColor.GOLD
                    )
            );


            sender.sendMessage(
                    Component.text(
                            "공격력: "
                                    + format(attack),
                            NamedTextColor.RED
                    )
            );


            sender.sendMessage(
                    Component.text(
                            "방어력: "
                                    + format(defense),
                            NamedTextColor.BLUE
                    )
            );


            return;
        }


        /*
         * CLEAR
         */
        if ("clear".equals(operation)) {

            if (!itemStatWriter.clear(item)) {

                sender.sendMessage(
                        Component.text(
                                "아이템 스탯 제거에 실패했습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            hudService.refreshEquipmentStats(
                    player
            );


            sender.sendMessage(
                    Component.text(
                            "현재 아이템의 RPG 공격력/방어력 PDC를 제거했습니다.",
                            NamedTextColor.GREEN
                    )
            );


            return;
        }


        /*
         * ATTACK / DEFENSE는 값이 필요하다.
         */
        if (args.length < 3) {

            sendItemStatUsage(
                    sender
            );

            return;
        }


        double value;


        try {

            value =
                    Double.parseDouble(
                            args[2]
                    );

        } catch (NumberFormatException exception) {

            sender.sendMessage(
                    Component.text(
                            "스탯 값은 숫자로 입력해 주세요.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        if (value < 0.0) {

            sender.sendMessage(
                    Component.text(
                            "스탯 값은 0 이상이어야 합니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        switch (operation) {

            case "attack" -> {

                if (!itemStatWriter.setAttack(
                        item,
                        value
                )) {

                    sender.sendMessage(
                            Component.text(
                                    "공격력 저장에 실패했습니다.",
                                    NamedTextColor.RED
                            )
                    );

                    return;
                }


                hudService.refreshEquipmentStats(
                        player
                );


                sender.sendMessage(
                        Component.text(
                                "현재 아이템 RPG 공격력 = "
                                        + format(value),
                                NamedTextColor.GREEN
                        )
                );
            }


            case "defense" -> {

                if (!itemStatWriter.setDefense(
                        item,
                        value
                )) {

                    sender.sendMessage(
                            Component.text(
                                    "방어력 저장에 실패했습니다.",
                                    NamedTextColor.RED
                            )
                    );

                    return;
                }


                hudService.refreshEquipmentStats(
                        player
                );


                sender.sendMessage(
                        Component.text(
                                "현재 아이템 RPG 방어력 = "
                                        + format(value),
                                NamedTextColor.GREEN
                        )
                );
            }


            default ->
                    sendItemStatUsage(
                            sender
                    );
        }
    }


    private void sendItemStatUsage(
            CommandSender sender
    ) {

        sender.sendMessage(
                Component.text(
                        "/rpgadmin itemstat attack <value>",
                        NamedTextColor.YELLOW
                )
        );

        sender.sendMessage(
                Component.text(
                        "/rpgadmin itemstat defense <value>",
                        NamedTextColor.YELLOW
                )
        );

        sender.sendMessage(
                Component.text(
                        "/rpgadmin itemstat info",
                        NamedTextColor.YELLOW
                )
        );

        sender.sendMessage(
                Component.text(
                        "/rpgadmin itemstat vanilla",
                        NamedTextColor.YELLOW
                )
        );

        sender.sendMessage(
                Component.text(
                        "/rpgadmin itemstat clear",
                        NamedTextColor.YELLOW
                )
        );
    }


    /*
     * =========================================================
     * MANA
     * =========================================================
     */

    private void handleManaCommand(
            CommandSender sender,
            String[] args
    ) {

        if (args.length < 3) {

            sendManaUsage(sender);

            return;
        }

        String operation =
                args[1].toLowerCase(
                        Locale.ROOT
                );

        String targetName =
                args[2];

        Player target =
                Bukkit.getPlayerExact(
                        targetName
                );

        if (target == null
                || !target.isOnline()) {

            sender.sendMessage(
                    Component.text(
                            "MANA 런타임 명령은 접속 중인 플레이어에게만 사용할 수 있습니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }

        UUID uuid =
                target.getUniqueId();

        if (!manaService.isInitialized(uuid)) {

            sender.sendMessage(
                    Component.text(
                            target.getName()
                                    + "의 MANA 런타임 데이터가 아직 초기화되지 않았습니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }

        /*
         * GET
         */
        if ("get".equals(operation)) {

            sendManaStatus(
                    sender,
                    target
            );

            return;
        }

        /*
         * REFILL
         */
        if ("refill".equals(operation)) {

            manaService.refill(
                    uuid
            );

            sender.sendMessage(
                    Component.text(
                            target.getName()
                                    + "의 MANA를 완전히 회복했습니다.",
                            NamedTextColor.GREEN
                    )
            );

            sendManaStatus(
                    sender,
                    target
            );

            return;
        }

        /*
         * SET / ADD / CONSUME
         */
        if (args.length < 4) {

            sendManaUsage(sender);

            return;
        }

        double amount;

        try {

            amount =
                    Double.parseDouble(
                            args[3]
                    );

        } catch (NumberFormatException exception) {

            sender.sendMessage(
                    Component.text(
                            "MANA 값은 숫자로 입력해 주세요.",
                            NamedTextColor.RED
                    )
            );

            return;
        }

        if (amount < 0.0) {

            sender.sendMessage(
                    Component.text(
                            "MANA 값은 0 이상이어야 합니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }

        switch (operation) {

            case "set" -> {

                manaService.setCurrentMana(
                        uuid,
                        amount
                );

                sender.sendMessage(
                        Component.text(
                                target.getName()
                                        + "의 현재 MANA를 "
                                        + format(amount)
                                        + "로 설정했습니다.",
                                NamedTextColor.GREEN
                        )
                );

                sendManaStatus(
                        sender,
                        target
                );
            }

            case "add" -> {

                manaService.restore(
                        uuid,
                        amount
                );

                sender.sendMessage(
                        Component.text(
                                target.getName()
                                        + "에게 MANA "
                                        + format(amount)
                                        + "을 회복시켰습니다.",
                                NamedTextColor.GREEN
                        )
                );

                sendManaStatus(
                        sender,
                        target
                );
            }

            case "consume" -> {

                boolean success =
                        manaService.consume(
                                uuid,
                                amount
                        );

                if (!success) {

                    sender.sendMessage(
                            Component.text(
                                    target.getName()
                                            + "의 MANA가 부족합니다.",
                                    NamedTextColor.RED
                            )
                    );

                    sendManaStatus(
                            sender,
                            target
                    );

                    return;
                }

                sender.sendMessage(
                        Component.text(
                                target.getName()
                                        + "의 MANA "
                                        + format(amount)
                                        + "을 소비했습니다.",
                                NamedTextColor.YELLOW
                        )
                );

                sendManaStatus(
                        sender,
                        target
                );
            }

            default -> sendManaUsage(
                    sender
            );
        }
    }

    private void sendManaStatus(
            CommandSender sender,
            Player target
    ) {

        UUID uuid =
                target.getUniqueId();

        double currentMana =
                manaService.getCurrentMana(
                        uuid
                );

        double maximumMana =
                manaService.getMaximumMana(
                        uuid
                );

        sender.sendMessage(
                Component.text(
                        target.getName()
                                + " MANA: "
                                + format(currentMana)
                                + " / "
                                + format(maximumMana),
                        NamedTextColor.AQUA
                )
        );
    }

    /*
     * =========================================================
     * EXP ADD
     * =========================================================
     */

    private void handleExperienceAdd(
            CommandSender sender,
            UUID uuid,
            String targetName,
            long amount
    ) {

        if (amount < 0L) {

            sender.sendMessage(
                    Component.text(
                            "추가 EXP는 0 이상이어야 합니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }

        Bukkit.getScheduler()
                .runTaskAsynchronously(
                        plugin,
                        () -> {

                            try {

                                PlayerData playerData =
                                        playerRepository
                                                .findPlayer(
                                                        uuid
                                                );

                                if (playerData == null) {

                                    sendSync(
                                            sender,
                                            Component.text(
                                                    "플레이어 RPG 데이터를 찾을 수 없습니다.",
                                                    NamedTextColor.RED
                                            )
                                    );

                                    return;
                                }

                                LevelUpResult result =
                                        levelService
                                                .addExperience(
                                                        playerData,
                                                        amount
                                                );

                                playerRepository
                                        .updateLevelAndExperience(
                                                uuid,
                                                playerData.getLevel(),
                                                playerData.getExperience()
                                        );

                                /*
                                 * DB 저장 성공 후 레벨 칭호 즉시 획득.
                                 * 실제 레벨업이 발생한 경우에만 검사한다.
                                 */
                                if (result.leveledUp()
                                        && playerData.getLevel() >= 80) {

                                    new com.hcs.rpgcore.title.LevelTitleUnlockService(
                                            plugin
                                    ).checkLevel(
                                            uuid,
                                            playerData.getLevel()
                                    );
                                }

                                plugin.registerLevel10ShieldReward(
                                        uuid,
                                        result.oldLevel(),
                                        result.newLevel()
                                );

                                /*
                                 * HUD 캐시는 레벨업 여부와 관계없이
                                 * 항상 갱신.
                                 */
                                hudService.updateProfile(
                                        playerData
                                );

                                syncSkillRewardsIfOnline(
                                        uuid,
                                        playerData
                                );

                                /*
                                 * 레벨업이 발생했다면
                                 * HP / MAX MANA 즉시 적용.
                                 */
                                if (result.leveledUp()) {

                                    applyStatsIfOnline(
                                            uuid,
                                            playerData
                                    );
                                }

                                sendSync(
                                        sender,
                                        Component.text(
                                                targetName
                                                        + " EXP +"
                                                        + amount
                                                        + " → Lv."
                                                        + playerData.getLevel()
                                                        + " / EXP "
                                                        + playerData.getExperience(),
                                                NamedTextColor.GREEN
                                        )
                                );

                            } catch (SQLException exception) {

                                handleSqlError(
                                        sender,
                                        "EXP 추가",
                                        exception
                                );
                            }
                        }
                );
    }

    /*
     * =========================================================
     * EXP SET
     * =========================================================
     */

    private void handleExperienceSet(
            CommandSender sender,
            UUID uuid,
            String targetName,
            long amount
    ) {

        if (amount < 0L) {

            sender.sendMessage(
                    Component.text(
                            "EXP는 0 이상이어야 합니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }

        Bukkit.getScheduler()
                .runTaskAsynchronously(
                        plugin,
                        () -> {

                            try {

                                PlayerData playerData =
                                        playerRepository
                                                .findPlayer(
                                                        uuid
                                                );

                                if (playerData == null) {

                                    sendSync(
                                            sender,
                                            Component.text(
                                                    "플레이어 RPG 데이터를 찾을 수 없습니다.",
                                                    NamedTextColor.RED
                                            )
                                    );

                                    return;
                                }

                                long newExperience =
                                        amount;

                                if (playerData.getLevel()
                                        >= levelService
                                        .getMaximumLevel()) {

                                    newExperience =
                                            0L;
                                }

                                playerRepository
                                        .updateExperience(
                                                uuid,
                                                newExperience
                                        );

                                /*
                                 * 메모리의 PlayerData도
                                 * DB와 동일하게 맞춘다.
                                 */
                                playerData.setExperience(
                                        newExperience
                                );

                                /*
                                 * HUD EXP 즉시 갱신.
                                 */
                                hudService.updateProfile(
                                        playerData
                                );

                                sendSync(
                                        sender,
                                        Component.text(
                                                targetName
                                                        + " EXP = "
                                                        + newExperience,
                                                NamedTextColor.GREEN
                                        )
                                );

                            } catch (SQLException exception) {

                                handleSqlError(
                                        sender,
                                        "EXP 설정",
                                        exception
                                );
                            }
                        }
                );
    }

    /*
     * =========================================================
     * LEVEL SET
     * =========================================================
     */

    private void handleLevelSet(
            CommandSender sender,
            UUID uuid,
            String targetName,
            long levelValue
    ) {

        if (levelValue < 1L
                || levelValue
                > levelService.getMaximumLevel()) {

            sender.sendMessage(
                    Component.text(
                            "레벨은 1 ~ "
                                    + levelService
                                    .getMaximumLevel()
                                    + " 사이여야 합니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }

        int newLevel =
                (int) levelValue;

        Bukkit.getScheduler()
                .runTaskAsynchronously(
                        plugin,
                        () -> {

                            try {

                                PlayerData existing =
                                        playerRepository
                                                .findPlayer(
                                                        uuid
                                                );

                                if (existing == null) {

                                    sendSync(
                                            sender,
                                            Component.text(
                                                    "플레이어 RPG 데이터를 찾을 수 없습니다.",
                                                    NamedTextColor.RED
                                            )
                                    );

                                    return;
                                }

                                playerRepository
                                        .updateLevelAndExperience(
                                                uuid,
                                                newLevel,
                                                0L
                                        );

                                if (newLevel >= 80) {
                                    new com.hcs.rpgcore.title.LevelTitleUnlockService(plugin)
                                            .checkLevel(uuid, newLevel);
                                }

                                plugin.registerLevel10ShieldReward(
                                        uuid,
                                        existing.getLevel(),
                                        newLevel
                                );

                                PlayerData updated =
                                        playerRepository
                                                .findPlayer(
                                                        uuid
                                                );

                                if (updated != null) {

                                    /*
                                     * HUD 즉시 갱신.
                                     */
                                    hudService.updateProfile(
                                            updated
                                    );

                                    /*
                                     * MAX HEALTH / MAX MANA
                                     * 즉시 적용.
                                     */
                                    applyStatsIfOnline(
                                            uuid,
                                            updated
                                    );

                                    syncSkillRewardsIfOnline(
                                            uuid,
                                            updated
                                    );
                                }

                                sendSync(
                                        sender,
                                        Component.text(
                                                targetName
                                                        + " 레벨 = "
                                                        + newLevel,
                                                NamedTextColor.GREEN
                                        )
                                );

                            } catch (SQLException exception) {

                                handleSqlError(
                                        sender,
                                        "레벨 설정",
                                        exception
                                );
                            }
                        }
                );
    }

    /*
     * =========================================================
     * STAT APPLY
     * =========================================================
     */

    private void syncSkillRewardsIfOnline(
            UUID uuid,
            PlayerData playerData
    ) {

        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> {

                            Player onlinePlayer =
                                    Bukkit.getPlayer(uuid);

                            if (
                                    onlinePlayer == null
                                    || !onlinePlayer.isOnline()
                            ) {
                                return;
                            }

                            skillRewardService.syncRewards(
                                    onlinePlayer,
                                    playerData
                            );
                        }
                );
    }


    private void applyStatsIfOnline(
            UUID uuid,
            PlayerData playerData
    ) {

        PlayerStats stats =
                statService.calculate(
                        playerData
                );

        /*
         * 최대 MANA 변경.
         * 현재 MANA 비율 유지.
         */
        manaService.updateMaximumMana(
                uuid,
                stats.maxMana(),
                true
        );

        /*
         * HUD에는 새 계산 결과가 이미
         * updateProfile()을 통해 반영되어 있다.
         */

        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> {

                            Player onlinePlayer =
                                    Bukkit.getPlayer(
                                            uuid
                                    );

                            if (onlinePlayer == null
                                    || !onlinePlayer.isOnline()) {

                                return;
                            }

                            /*
                             * 실제 MAX_HEALTH / ATTACK_DAMAGE 갱신.
                             *
                             * HP / MP는 레벨 + 직업 기준을 유지하고,
                             * 실제 공격력 적용 시에는 현재 착용 장비까지
                             * 포함한 최종 스탯을 다시 계산한다.
                             */
                            PlayerStats equippedStats =
                                    statService.calculate(
                                            playerData,
                                            onlinePlayer
                                    );

                            statApplier.apply(
                                    onlinePlayer,
                                    equippedStats
                            );
                        }
                );
    }

    /*
     * =========================================================
     * ERROR
     * =========================================================
     */

    /*
     * =========================================================
     * STARTER KIT
     * =========================================================
     *
     * /rpgadmin starterkit <player>
     */
    private void handleStarterKitCommand(
            CommandSender sender,
            String[] args
    ) {

        if (args.length != 2) {

            sender.sendMessage(
                    Component.text(
                            "사용법: /rpgadmin starterkit <player>",
                            NamedTextColor.YELLOW
                    )
            );

            return;
        }

        String targetName =
                args[1];

        Player target =
                Bukkit.getPlayerExact(
                        targetName
                );

        if (target == null) {

            sender.sendMessage(
                    Component.text(
                            "접속 중인 플레이어를 찾을 수 없습니다: "
                                    + targetName,
                            NamedTextColor.RED
                    )
            );

            return;
        }

        StarterKitListener.giveStarterKit(
                target
        );

        sender.sendMessage(
                Component.text(
                        target.getName()
                                + " 님에게 시작 장비를 지급했습니다.",
                        NamedTextColor.GREEN
                )
        );
    }


    private void handleSqlError(
            CommandSender sender,
            String operation,
            SQLException exception
    ) {

        plugin.getLogger()
                .severe(
                        operation
                                + " 실패: "
                                + exception.getMessage()
                );

        exception.printStackTrace();

        sendSync(
                sender,
                Component.text(
                        operation
                                + " 중 데이터베이스 오류가 발생했습니다.",
                        NamedTextColor.RED
                )
        );
    }

    /*
     * =========================================================
     * ELITE TEST SPAWN
     * =========================================================
     */

    private void handleEliteCommand(
            CommandSender sender,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    Component.text(
                            "엘리트 테스트 소환은 플레이어만 사용할 수 있습니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }

        if (
                args.length != 3
                        || !"spawn".equalsIgnoreCase(
                                args[1]
                        )
        ) {

            sendEliteUsage(
                    sender
            );

            return;
        }

        String eliteId =
                args[2].toLowerCase(
                        Locale.ROOT
                );

        /*
         * 플레이어가 바라보는 방향 3블록 앞을
         * 테스트 소환 위치로 사용한다.
         */
        org.bukkit.Location spawnLocation =
                player.getLocation()
                        .clone()
                        .add(
                                player.getLocation()
                                        .getDirection()
                                        .normalize()
                                        .multiply(
                                                3.0D
                                        )
                        );

        boolean success =
                true;

        switch (eliteId) {

            case "demon_knight" -> {

                new EliteDemonKnightListener(
                        plugin
                ).spawnForAdmin(
                        spawnLocation
                );
            }

            case "redstone_golem" -> {

                success =
                        new RedstoneGolemSpawnService(
                                plugin
                        ).spawnForAdmin(
                                spawnLocation
                        );
            }

            case "warbringer" -> {

                success =
                        new WarbringerSpawnService(
                                plugin
                        ).spawnForAdmin(
                                spawnLocation
                        );
            }

            default -> {

                sendEliteUsage(
                        sender
                );

                return;
            }
        }

        if (!success) {

            sender.sendMessage(
                    Component.text(
                            "엘리트 모델을 불러오지 못해 소환을 취소했습니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }

        sender.sendMessage(
                Component.text(
                        "[엘리트 테스트] ",
                        NamedTextColor.GOLD
                ).append(
                        Component.text(
                                eliteId
                                        + " 소환 완료",
                                NamedTextColor.GREEN
                        )
                )
        );
    }


    private void sendEliteUsage(
            CommandSender sender
    ) {

        sender.sendMessage(
                Component.text(
                        "사용법:",
                        NamedTextColor.YELLOW
                )
        );

        sender.sendMessage(
                Component.text(
                        "/rpgadmin elite spawn demon_knight",
                        NamedTextColor.YELLOW
                )
        );

        sender.sendMessage(
                Component.text(
                        "/rpgadmin elite spawn redstone_golem",
                        NamedTextColor.YELLOW
                )
        );

        sender.sendMessage(
                Component.text(
                        "/rpgadmin elite spawn warbringer",
                        NamedTextColor.YELLOW
                )
        );
    }


    /*
     * =========================================================
     * USAGE
     * =========================================================
     */

    private void sendUsage(
            CommandSender sender
    ) {

        sender.sendMessage(
                Component.text(
                        "──── RPGAdmin ────",
                        NamedTextColor.GOLD
                )
        );

        sender.sendMessage(
                Component.text(
                        "/rpgadmin exp add <player> <amount>",
                        NamedTextColor.YELLOW
                )
        );

        sender.sendMessage(
                Component.text(
                        "/rpgadmin exp set <player> <amount>",
                        NamedTextColor.YELLOW
                )
        );

        sender.sendMessage(
                Component.text(
                        "/rpgadmin level set <player> <level>",
                        NamedTextColor.YELLOW
                )
        );

        sender.sendMessage(
                Component.text(
                        "/rpgadmin starterkit <player>",
                        NamedTextColor.GREEN
                )
        );


        sendEliteUsage(
                sender
        );


        sendManaUsage(
                sender
        );


        sendItemStatUsage(
                sender
        );
    }

    private void sendManaUsage(
            CommandSender sender
    ) {

        sender.sendMessage(
                Component.text(
                        "/rpgadmin mana get <player>",
                        NamedTextColor.AQUA
                )
        );

        sender.sendMessage(
                Component.text(
                        "/rpgadmin mana set <player> <amount>",
                        NamedTextColor.AQUA
                )
        );

        sender.sendMessage(
                Component.text(
                        "/rpgadmin mana add <player> <amount>",
                        NamedTextColor.AQUA
                )
        );

        sender.sendMessage(
                Component.text(
                        "/rpgadmin mana consume <player> <amount>",
                        NamedTextColor.AQUA
                )
        );

        sender.sendMessage(
                Component.text(
                        "/rpgadmin mana refill <player>",
                        NamedTextColor.AQUA
                )
        );
    }

    /*
     * =========================================================
     * UTIL
     * =========================================================
     */

    private String format(
            double value
    ) {

        if (value == Math.floor(value)) {

            return Long.toString(
                    (long) value
            );
        }

        return String.format(
                Locale.US,
                "%.1f",
                value
        );
    }

    private void sendSync(
            CommandSender sender,
            Component component
    ) {

        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> sender.sendMessage(
                                component
                        )
                );
    }

    /*
     * =========================================================
     * REPAIR COMMAND
     * =========================================================
     *
     * /rpgadmin repair <player> all
     *
     * all:
     * - 주손
     * - 보조손
     * - 투구
     * - 흉갑
     * - 각반
     * - 장화
     */


    private void handleRepairCommand(
            CommandSender sender,
            String[] args
    ) {

        if (args.length != 3) {

            sender.sendMessage(
                    Component.text(
                            "사용법: /rpgadmin repair <player> all",
                            NamedTextColor.YELLOW
                    )
            );

            return;
        }


        String targetName =
                args[1];

        String mode =
                args[2].toLowerCase(
                        Locale.ROOT
                );


        if (!"all".equals(mode)) {

            sender.sendMessage(
                    Component.text(
                            "현재 지원되는 수리 방식은 all 입니다.",
                            NamedTextColor.RED
                    )
            );

            sender.sendMessage(
                    Component.text(
                            "/rpgadmin repair <player> all",
                            NamedTextColor.YELLOW
                    )
            );

            return;
        }


        Player target =
                Bukkit.getPlayerExact(
                        targetName
                );


        if (
                target == null
                || !target.isOnline()
        ) {

            sender.sendMessage(
                    Component.text(
                            "수리 대상 플레이어는 온라인 상태여야 합니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        int repairedCount = 0;


        /*
         * 주손
         */
        repairedCount += repairItem(
                target.getInventory()
                        .getItemInMainHand()
        );


        /*
         * 보조손
         */
        repairedCount += repairItem(
                target.getInventory()
                        .getItemInOffHand()
        );


        /*
         * 투구
         */
        repairedCount += repairItem(
                target.getInventory()
                        .getHelmet()
        );


        /*
         * 흉갑
         */
        repairedCount += repairItem(
                target.getInventory()
                        .getChestplate()
        );


        /*
         * 각반
         */
        repairedCount += repairItem(
                target.getInventory()
                        .getLeggings()
        );


        /*
         * 장화
         */
        repairedCount += repairItem(
                target.getInventory()
                        .getBoots()
        );


        sender.sendMessage(
                Component.text(
                        target.getName()
                                + "의 착용 장비 "
                                + repairedCount
                                + "개를 완전히 수리했습니다.",
                        NamedTextColor.GREEN
                )
        );


        if (!sender.equals(target)) {

            target.sendMessage(
                    Component.text(
                            "관리자에 의해 착용 장비가 완전히 수리되었습니다.",
                            NamedTextColor.GREEN
                    )
            );
        }
    }


    /*
     * =========================================================
     * ITEM REPAIR
     * =========================================================
     */
    private int repairItem(
            ItemStack item
    ) {

        if (
                item == null
                || item.getType().isAir()
        ) {

            return 0;
        }


        ItemMeta meta =
                item.getItemMeta();


        if (!(meta instanceof Damageable damageable)) {

            return 0;
        }


        if (damageable.getDamage() <= 0) {

            return 0;
        }


        damageable.setDamage(
                0
        );

        item.setItemMeta(
                meta
        );


        return 1;
    }



    /*
     * =========================================================
     * WORLD ADMIN COMMAND
     * =========================================================
     *
     * /rpgadmin world create <world> <seed>
     * /rpgadmin world tp <player> <world>
     * /rpgadmin world list
     */
    private void handleWorldCommand(
            CommandSender sender,
            String[] args
    ) {

        if (args.length < 2) {

            sendWorldCommandUsage(
                    sender
            );

            return;
        }


        String operation =
                args[1].toLowerCase(
                        Locale.ROOT
                );


        /*
         * =====================================================
         * LIST
         * =====================================================
         */
        if ("list".equals(operation)) {

            sender.sendMessage(
                    Component.text(
                            "로드된 월드:",
                            NamedTextColor.GOLD
                    )
            );


            for (
                    World world
                    : Bukkit.getWorlds()
            ) {

                sender.sendMessage(
                        Component.text(
                                " - "
                                        + world.getName()
                                        + " | seed="
                                        + world.getSeed()
                                        + " | environment="
                                        + world.getEnvironment(),
                                NamedTextColor.GRAY
                        )
                );
            }


            return;
        }


        /*
         * =====================================================
         * CREATE
         * =====================================================
         *
         * /rpgadmin world create <world> <seed>
         */
        if ("create".equals(operation)) {

            if (args.length < 4) {

                sendWorldCommandUsage(
                        sender
                );

                return;
            }


            String worldName =
                    args[2];


            /*
             * 디렉토리 탈출 및 특수 경로 사용 방지.
             */
            if (
                    !worldName.matches(
                            "[A-Za-z0-9_-]+"
                    )
            ) {

                sender.sendMessage(
                        Component.text(
                                "월드 이름은 영문, 숫자, _, - 만 사용할 수 있습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            if (
                    Bukkit.getWorld(
                            worldName
                    ) != null
            ) {

                sender.sendMessage(
                        Component.text(
                                "이미 로드된 월드입니다: "
                                        + worldName,
                                NamedTextColor.RED
                        )
                );

                return;
            }


            java.io.File worldFolder =
                    new java.io.File(
                            Bukkit.getWorldContainer(),
                            worldName
                    );


            /*
             * 같은 이름의 기존 월드 폴더가 있으면
             * 새 시드로 덮어쓰지 않는다.
             */
            if (
                    worldFolder.exists()
            ) {

                sender.sendMessage(
                        Component.text(
                                "같은 이름의 월드 폴더가 이미 존재합니다: "
                                        + worldFolder.getAbsolutePath(),
                                NamedTextColor.RED
                        )
                );

                return;
            }


            long seed;

            try {

                seed =
                        Long.parseLong(
                                args[3]
                        );

            } catch (NumberFormatException exception) {

                sender.sendMessage(
                        Component.text(
                                "잘못된 시드입니다: "
                                        + args[3],
                                NamedTextColor.RED
                        )
                );

                return;
            }


            sender.sendMessage(
                    Component.text(
                            "월드 생성을 시작합니다: "
                                    + worldName
                                    + " / seed="
                                    + seed,
                            NamedTextColor.YELLOW
                    )
            );


            WorldCreator creator =
                    new WorldCreator(
                            worldName
                    );


            creator.seed(
                    seed
            );


            creator.environment(
                    World.Environment.NORMAL
            );


            World createdWorld =
                    Bukkit.createWorld(
                            creator
                    );


            if (
                    createdWorld == null
            ) {

                sender.sendMessage(
                        Component.text(
                                "월드 생성에 실패했습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            sender.sendMessage(
                    Component.text(
                            "월드 생성 완료: "
                                    + createdWorld.getName()
                                    + " / seed="
                                    + createdWorld.getSeed(),
                            NamedTextColor.GREEN
                    )
            );


            return;
        }


        /*
         * =====================================================
         * TELEPORT
         * =====================================================
         *
         * /rpgadmin world tp <player> <world>
         */
        if ("tp".equals(operation)) {

            if (args.length < 4) {

                sendWorldCommandUsage(
                        sender
                );

                return;
            }


            Player target =
                    Bukkit.getPlayerExact(
                            args[2]
                    );


            if (
                    target == null
                    || !target.isOnline()
            ) {

                sender.sendMessage(
                        Component.text(
                                "온라인 플레이어를 찾을 수 없습니다: "
                                        + args[2],
                                NamedTextColor.RED
                        )
                );

                return;
            }


            World world =
                    Bukkit.getWorld(
                            args[3]
                    );


            if (world == null) {

                sender.sendMessage(
                        Component.text(
                                "로드된 월드를 찾을 수 없습니다: "
                                        + args[3],
                                NamedTextColor.RED
                        )
                );

                return;
            }


            target.teleport(
                    world.getSpawnLocation()
            );


            sender.sendMessage(
                    Component.text(
                            target.getName()
                                    + " -> "
                                    + world.getName()
                                    + " 이동 완료",
                            NamedTextColor.GREEN
                    )
            );


            return;
        }


        sendWorldCommandUsage(
                sender
        );
    }


    private void sendWorldCommandUsage(
            CommandSender sender
    ) {

        sender.sendMessage(
                Component.text(
                        "/rpgadmin world create <world> <seed>",
                        NamedTextColor.YELLOW
                )
        );

        sender.sendMessage(
                Component.text(
                        "/rpgadmin world tp <player> <world>",
                        NamedTextColor.YELLOW
                )
        );

        sender.sendMessage(
                Component.text(
                        "/rpgadmin world list",
                        NamedTextColor.YELLOW
                )
        );
    }

}
