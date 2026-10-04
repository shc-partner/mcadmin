/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.InvalidConfigurationException
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.jetbrains.annotations.NotNull
 */
package com.hcs.rpgcore;

import com.hcs.rpgcore.boss.BossHealthListener;
import com.hcs.rpgcore.classjob.ClassSelectionReminderTask;
import com.hcs.rpgcore.combat.CombatListener;
import com.hcs.rpgcore.combat.CombatService;
import com.hcs.rpgcore.combat.DragonProximityDamageTask;
import com.hcs.rpgcore.combat.EnderDragonBlackHoleTask;
import com.hcs.rpgcore.combat.MonsterFallDamageListener;
import com.hcs.rpgcore.command.ClassCommand;
import com.hcs.rpgcore.command.RPGAdminCommand;
import com.hcs.rpgcore.command.RpgClaimCommand;
import com.hcs.rpgcore.command.StatsCommand;
import com.hcs.rpgcore.database.DatabaseManager;
import com.hcs.rpgcore.dungeon.AncientDepthsDungeonEntranceListener;
import com.hcs.rpgcore.dungeon.AncientDepthsDungeonMobListener;
import com.hcs.rpgcore.dungeon.AncientDepthsDungeonService;
import com.hcs.rpgcore.dungeon.EndermanDungeonEntranceListener;
import com.hcs.rpgcore.dungeon.EndermanDungeonMobListener;
import com.hcs.rpgcore.dungeon.EndermanDungeonService;
import com.hcs.rpgcore.dungeon.GraveGuardianDamageListener;
import com.hcs.rpgcore.dungeon.DungeonNaturalSpawnBlockListener;
import com.hcs.rpgcore.dungeon.NetherFortressDungeonEntranceListener;
import com.hcs.rpgcore.dungeon.NetherFortressDungeonMobListener;
import com.hcs.rpgcore.dungeon.NetherFortressDungeonService;
import com.hcs.rpgcore.dungeon.ShulkerDungeonEntranceListener;
import com.hcs.rpgcore.dungeon.ShulkerDungeonMobListener;
import com.hcs.rpgcore.dungeon.ShulkerDungeonService;
import com.hcs.rpgcore.dungeon.UndeadFortressDungeonEntranceListener;
import com.hcs.rpgcore.dungeon.UndeadFortressDungeonMobListener;
import com.hcs.rpgcore.dungeon.UndeadFortressDungeonService;
import com.hcs.rpgcore.dungeon.ZombieDungeonEntranceListener;
import com.hcs.rpgcore.dungeon.ZombieDungeonMobListener;
import com.hcs.rpgcore.dungeon.ZombieDungeonService;
import com.hcs.rpgcore.dungeon.SimpleDungeonService;
import com.hcs.rpgcore.dungeon.reward.DungeonRewardItemFactory;
import com.hcs.rpgcore.dungeon.reward.DungeonRewardService;
import com.hcs.rpgcore.dungeon.reward.PendingRewardRepository;
import com.hcs.rpgcore.elixir.ElixirService;
import com.hcs.rpgcore.enchant.FreeEnchantListener;
import com.hcs.rpgcore.enchant.ForestLumberjackListener;
import com.hcs.rpgcore.hud.HudListener;
import com.hcs.rpgcore.hud.HudService;
import com.hcs.rpgcore.level.LevelService;
import com.hcs.rpgcore.level.MobExperienceService;
import com.hcs.rpgcore.level.MobKillListener;
import com.hcs.rpgcore.listener.CreeperExplosionListener;
import com.hcs.rpgcore.listener.EndermanBlockProtectionListener;
import com.hcs.rpgcore.mana.ManaListener;
import com.hcs.rpgcore.mana.ManaRegenerationTask;
import com.hcs.rpgcore.mana.ManaService;
import com.hcs.rpgcore.player.PlayerConnectionListener;
import com.hcs.rpgcore.placeholder.RPGCorePlaceholderExpansion;
import com.hcs.rpgcore.player.PlayerRepository;
import com.hcs.rpgcore.player.VanillaExperienceListener;
import com.hcs.rpgcore.skill.MageBlackHoleSkill;
import com.hcs.rpgcore.skill.MageBlizzardSkill;
import com.hcs.rpgcore.skill.MageChainLightningSkill;
import com.hcs.rpgcore.skill.MageFireBoltSkill;
import com.hcs.rpgcore.skill.MageFrostNovaSkill;
import com.hcs.rpgcore.skill.MageManaOverloadSkill;
import com.hcs.rpgcore.skill.MageManaShieldSkill;
import com.hcs.rpgcore.skill.MageMeteorSkill;
import com.hcs.rpgcore.skill.MageThunderStormSkill;
import com.hcs.rpgcore.skill.SkillAccessService;
import com.hcs.rpgcore.skill.SkillCollectionListener;
import com.hcs.rpgcore.skill.SkillCollectionMenu;
import com.hcs.rpgcore.skill.SkillCooldownService;
import com.hcs.rpgcore.skill.SkillDefenseBuffService;
import com.hcs.rpgcore.skill.SkillHotbarActivationListener;
import com.hcs.rpgcore.skill.SkillInputListener;
import com.hcs.rpgcore.skill.SkillInputWindowService;
import com.hcs.rpgcore.skill.SkillItemFactory;
import com.hcs.rpgcore.skill.SkillItemProtectionListener;
import com.hcs.rpgcore.skill.SkillOffenseBuffService;
import com.hcs.rpgcore.skill.SkillRewardService;
import com.hcs.rpgcore.skill.SkillUseListener;
import com.hcs.rpgcore.skill.WarriorBashSkill;
import com.hcs.rpgcore.skill.WarriorBattleCrySkill;
import com.hcs.rpgcore.skill.WarriorBerserkerRageSkill;
import com.hcs.rpgcore.skill.WarriorBladeStormSkill;
import com.hcs.rpgcore.skill.WarriorEarthquakeSlamSkill;
import com.hcs.rpgcore.skill.WarriorExecutionSlashSkill;
import com.hcs.rpgcore.skill.WarriorInfiniteBladeSkill;
import com.hcs.rpgcore.skill.WarriorIronWallSkill;
import com.hcs.rpgcore.skill.WarriorWeaponAttackListener;
import com.hcs.rpgcore.skill.WarriorWeaponAttackService;
import com.hcs.rpgcore.skill.WarriorWhirlwindSkill;
import com.hcs.rpgcore.starter.StarterKitListener;
import com.hcs.rpgcore.stat.EquipmentStatListener;
import com.hcs.rpgcore.stat.EquipmentStatReader;
import com.hcs.rpgcore.stat.ItemStatKeys;
import com.hcs.rpgcore.stat.PlayerStatApplier;
import com.hcs.rpgcore.stat.PlayerStatListener;
import com.hcs.rpgcore.stat.StatService;
import com.hcs.rpgcore.storage.StorageChestListener;
import com.hcs.rpgcore.storage.StorageChestRepository;
import com.hcs.rpgcore.storage.StorageChestService;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class RPGCorePlugin
extends JavaPlugin {
    private ZombieDungeonService zombieDungeonService;
    private SimpleDungeonService simpleDungeonService;

    private com.hcs.rpgcore.dungeon.MinotaurDungeonService
            minotaurDungeonService;

    private com.hcs.rpgcore.dungeon.RedDragonDungeonService
            redDragonDungeonService;

    private com.hcs.rpgcore.dungeon.VoidSanctumDungeonService
            voidSanctumDungeonService;
    private ShulkerDungeonService shulkerDungeonService;
    private EndermanDungeonService endermanDungeonService;
    private UndeadFortressDungeonService undeadFortressDungeonService;
    private NetherFortressDungeonService netherFortressDungeonService;
    private AncientDepthsDungeonService ancientDepthsDungeonService;
    private PendingRewardRepository pendingRewardRepository;
    private com.hcs.rpgcore.dismantle.EquipmentDismantleListener
            dismantleListener;
    private com.hcs.rpgcore.craft.EquipmentCraftPreviewService
            equipmentCraftPreviewService;
    private com.hcs.rpgcore.dismantle.EquipmentDismantleReturnRepository
            dismantleReturnRepository;
    private DungeonRewardItemFactory dungeonRewardItemFactory;
    private DungeonRewardService dungeonRewardService;
    private com.hcs.rpgcore.horse.HorseRaceNpcListener
            horseRaceNpcListener;

    private com.hcs.rpgcore.slot.SlotMachineNpcListener
            slotMachineNpcListener;

    private com.hcs.rpgcore.level.reward.LevelRewardService
            levelRewardService;

    private DatabaseManager databaseManager;
    private FileConfiguration databaseConfig;

    private StorageChestRepository storageChestRepository;
    private StorageChestService storageChestService;
    private PlayerRepository playerRepository;

    /*
     * =========================================================
     * SHOP / MARKET
     * =========================================================
     */
    private com.hcs.rpgcore.market.MarketPriceRepository
            marketPriceRepository;

    private com.hcs.rpgcore.market.MarketPriceService
            marketPriceService;

    private com.hcs.rpgcore.shop.ShopRegistry
            shopRegistry;

    private com.hcs.rpgcore.shop.ShopPriceService
            shopPriceService;

    private com.hcs.rpgcore.shop.ShopEconomyService
            shopEconomyService;

    private com.hcs.rpgcore.shop.ShopItemProvider
            shopItemProvider;

    private com.hcs.rpgcore.shop.ShopGuiService
            shopGuiService;

    private com.hcs.rpgcore.shop.ShopTransactionService
            shopTransactionService;

    private com.hcs.rpgcore.player.PlayerNameTagService
            playerNameTagService;

    private com.hcs.rpgcore.title.PlayerTitleDisplayService
            titleDisplayService;

    private com.hcs.rpgcore.title.LevelTitleCheckTask
            levelTitleCheckTask;

    private com.hcs.rpgcore.title.WealthyTitleUnlockService
            wealthyTitleUnlockService;

    private com.hcs.rpgcore.title.AchievementTitleUnlockService
            achievementTitleUnlockService;
    private LevelService levelService;
    private MobExperienceService mobExperienceService;
    private StatService statService;
    private PlayerStatApplier playerStatApplier;
    private CombatService combatService;
    private SkillDefenseBuffService skillDefenseBuffService;
    private SkillOffenseBuffService skillOffenseBuffService;
    private ManaService manaService;
    private ElixirService elixirService;
    private HudService hudService;

    private com.hcs.rpgcore.locator.LocatorBarService
            locatorBarService;
    private SkillItemFactory skillItemFactory;
    private SkillCooldownService skillCooldownService;
    private WarriorBashSkill warriorBashSkill;
    private WarriorWeaponAttackService warriorWeaponAttackService;
    private MageFireBoltSkill mageFireBoltSkill;
    private WarriorWhirlwindSkill warriorWhirlwindSkill;
    private MageFrostNovaSkill mageFrostNovaSkill;
    private WarriorExecutionSlashSkill warriorExecutionSlashSkill;
    private MageChainLightningSkill mageChainLightningSkill;
    private WarriorIronWallSkill warriorIronWallSkill;
    private MageManaShieldSkill mageManaShieldSkill;
    private WarriorEarthquakeSlamSkill warriorEarthquakeSlamSkill;
    private MageBlizzardSkill mageBlizzardSkill;
    private WarriorBattleCrySkill warriorBattleCrySkill;
    private MageBlackHoleSkill mageBlackHoleSkill;
    private WarriorBerserkerRageSkill warriorBerserkerRageSkill;
    private MageManaOverloadSkill mageManaOverloadSkill;
    private WarriorBladeStormSkill warriorBladeStormSkill;
    private MageThunderStormSkill mageThunderStormSkill;
    private MageMeteorSkill mageMeteorSkill;
    private WarriorInfiniteBladeSkill warriorGateOfBabylonSkill;
    private SkillAccessService skillAccessService;
    private SkillRewardService skillRewardService;

    /*
     * 워브링어의 이동/공격 애니메이션 상태를
     * Listener와 runtime task에서 공유한다.
     */
    private com.hcs.rpgcore.mob.WarbringerAnimationListener
            warbringerAnimationListener;

    private com.hcs.rpgcore.mob.DemonKnightAnimationListener
            demonKnightAnimationListener;

    private com.hcs.rpgcore.mob.FallenAngelAnimationListener
            fallenAngelAnimationListener;

    /*
     * Fallen Angel의 페이즈 / 특수 공격 패턴 상태를
     * Listener와 runtime task에서 공유한다.
     */
    private com.hcs.rpgcore.mob.FallenAngelBossPatternTask
            fallenAngelBossPatternTask;

    private com.hcs.rpgcore.dungeon.MinotaurAnimationListener
            minotaurAnimationListener;

    private com.hcs.rpgcore.dungeon.MinotaurBossPatternTask
            minotaurBossPatternTask;

    private SkillCollectionMenu skillCollectionMenu;

    public void onEnable() {
        this.zombieDungeonService = new ZombieDungeonService(this);
        this.simpleDungeonService = new SimpleDungeonService(this);

        this.minotaurDungeonService =
                new com.hcs.rpgcore.dungeon.MinotaurDungeonService(
                        this
                );

        this.redDragonDungeonService =
                new com.hcs.rpgcore.dungeon.RedDragonDungeonService(
                        this
                );

        this.voidSanctumDungeonService =
                new com.hcs.rpgcore.dungeon.VoidSanctumDungeonService(
                        this
                );
        this.shulkerDungeonService = new ShulkerDungeonService(this);
        this.endermanDungeonService = new EndermanDungeonService(this);
        this.undeadFortressDungeonService = new UndeadFortressDungeonService(this);
        this.netherFortressDungeonService = new NetherFortressDungeonService(this);
        this.ancientDepthsDungeonService = new AncientDepthsDungeonService(this);
        this.getServer().getPluginManager().registerEvents((Listener)new EndermanBlockProtectionListener(), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new CreeperExplosionListener(), (Plugin)this);

        this.getServer().getPluginManager().registerEvents(
                (Listener)new DungeonNaturalSpawnBlockListener(),
                (Plugin)this
        );
        this.getServer().getPluginManager().registerEvents((Listener)new ZombieDungeonEntranceListener(this, this.zombieDungeonService), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)this.simpleDungeonService, (Plugin)this);

        this.getServer().getPluginManager().registerEvents(
                (Listener)new com.hcs.rpgcore.dungeon.MinotaurDungeonEntranceListener(
                        this,
                        this.minotaurDungeonService
                ),
                (Plugin)this
        );

        this.getServer().getPluginManager().registerEvents(
                (Listener)new com.hcs.rpgcore.dungeon.MinotaurDungeonMobListener(
                        this.minotaurDungeonService
                ),
                (Plugin)this
        );

        this.getServer().getPluginManager().registerEvents(
                (Listener)new com.hcs.rpgcore.dungeon.MinotaurDamageListener(
                        this.minotaurDungeonService
                ),
                (Plugin)this
        );

        this.getServer().getPluginManager().registerEvents(
                (Listener)new com.hcs.rpgcore.dungeon.MinotaurDungeonPlayerListener(
                        this,
                        this.minotaurDungeonService
                ),
                (Plugin)this
        );

        /*
         * =====================================================
         * RED DRAGON DUNGEON
         * =====================================================
         */
        this.getServer().getPluginManager().registerEvents(
                (Listener)new com.hcs.rpgcore.dungeon.RedDragonDungeonEntranceListener(
                        this,
                        this.redDragonDungeonService
                ),
                (Plugin)this
        );

        this.getServer().getPluginManager().registerEvents(
                (Listener)new com.hcs.rpgcore.dungeon.RedDragonDungeonMobListener(
                        this.redDragonDungeonService
                ),
                (Plugin)this
        );

        this.getServer().getPluginManager().registerEvents(
                (Listener)new com.hcs.rpgcore.dungeon.RedDragonDungeonProtectionListener(
                        this.redDragonDungeonService
                ),
                (Plugin)this
        );

        this.getServer().getPluginManager().registerEvents(
                (Listener)new com.hcs.rpgcore.dungeon.RedDragonDungeonPlayerListener(
                        this,
                        this.redDragonDungeonService
                ),
                (Plugin)this
        );


        /*
         * =====================================================
         * VOID SANCTUM DUNGEON
         * =====================================================
         */
        this.getServer().getPluginManager().registerEvents(
                (Listener)new com.hcs.rpgcore.dungeon.VoidSanctumDungeonEntranceListener(
                        this,
                        this.voidSanctumDungeonService
                ),
                (Plugin)this
        );

        this.getServer().getPluginManager().registerEvents(
                (Listener)new com.hcs.rpgcore.dungeon.VoidSanctumDungeonMobListener(
                        this.voidSanctumDungeonService
                ),
                (Plugin)this
        );

        this.getServer().getPluginManager().registerEvents(
                (Listener)new com.hcs.rpgcore.dungeon.VoidSanctumDungeonPlayerListener(
                        this,
                        this.voidSanctumDungeonService
                ),
                (Plugin)this
        );

        this.getServer().getPluginManager().registerEvents(
                (Listener)new com.hcs.rpgcore.dungeon.VoidSanctumNaturalSpawnBlockListener(),
                (Plugin)this
        );


        this.getServer().getPluginManager().registerEvents((Listener)new ShulkerDungeonEntranceListener(this, this.shulkerDungeonService), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new ZombieDungeonMobListener(this.zombieDungeonService), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new ShulkerDungeonMobListener(this.shulkerDungeonService), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new GraveGuardianDamageListener(this.zombieDungeonService), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new EndermanDungeonEntranceListener(this, this.endermanDungeonService), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new UndeadFortressDungeonEntranceListener(this, this.undeadFortressDungeonService), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new NetherFortressDungeonEntranceListener(this, this.netherFortressDungeonService), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new AncientDepthsDungeonEntranceListener(this, this.ancientDepthsDungeonService), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new EndermanDungeonMobListener(this.endermanDungeonService), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new UndeadFortressDungeonMobListener(this.undeadFortressDungeonService), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new NetherFortressDungeonMobListener(this.netherFortressDungeonService), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new AncientDepthsDungeonMobListener(this.ancientDepthsDungeonService), (Plugin)this);
        this.saveDefaultConfig();
        try {
            this.loadDatabaseConfig();
            this.initializeDatabase();
            this.initializeServices();

            this.equipmentCraftPreviewService =
                    new com.hcs.rpgcore.craft.EquipmentCraftPreviewService(
                            this
                    );

            this.registerPlaceholderExpansion();
            this.endermanDungeonService.setRewardService(this.dungeonRewardService);
            this.undeadFortressDungeonService.setRewardService(this.dungeonRewardService);
            this.netherFortressDungeonService.setRewardService(this.dungeonRewardService);
            this.ancientDepthsDungeonService.setRewardService(this.dungeonRewardService);
        this.minotaurDungeonService.setRewardService(this.dungeonRewardService);
            this.registerListeners();
            this.registerCommands();
            this.startRuntimeTasks();
            this.getComponentLogger().info((Component)Component.text((String)"RPGCore enabled.", (TextColor)NamedTextColor.GREEN));
            this.getComponentLogger().info((Component)Component.text((String)"MariaDB connection: connected", (TextColor)NamedTextColor.GREEN));
            this.getComponentLogger().info((Component)Component.text((String)("Configured maximum level: " + this.getConfig().getInt("server.maximum-level", 99)), (TextColor)NamedTextColor.GRAY));
            this.getComponentLogger().info((Component)Component.text((String)"MANA runtime system: enabled", (TextColor)NamedTextColor.AQUA));
            this.getComponentLogger().info((Component)Component.text((String)"RPG HUD system: enabled", (TextColor)NamedTextColor.YELLOW));
        }
        catch (Exception exception) {
            this.getLogger().severe("RPGCore \ucd08\uae30\ud654 \uc2e4\ud328: " + exception.getMessage());
            exception.printStackTrace();
            this.getServer().getPluginManager().disablePlugin((Plugin)this);
        }
        this.getServer().getScheduler().runTaskTimer((Plugin)this, (Runnable)new DragonProximityDamageTask(), 40L, 40L);

        this.getServer().getScheduler().runTaskTimer(
                (Plugin)this,
                (Runnable)new EnderDragonBlackHoleTask(this),
                100L,
                100L
        );
    }

    private void loadDatabaseConfig() throws IOException, InvalidConfigurationException {
        File databaseFile = new File(this.getDataFolder(), "database.yml");
        if (!databaseFile.exists()) {
            this.saveResource("database.yml", false);
        }
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        yamlConfiguration.load(databaseFile);
        this.databaseConfig = yamlConfiguration;
    }

    private void initializeDatabase() throws Exception {
        this.databaseManager = new DatabaseManager();
        this.databaseManager.connect(this.databaseConfig);
        this.databaseManager.testConnection();
        this.databaseManager.createTables();


    }

    private void initializeServices() throws SQLException {
        this.playerRepository = new PlayerRepository(this.databaseManager);

        /*
         * =====================================================
         * MARKET / SHOP
         * =====================================================
         */

        this.marketPriceRepository =
                new com.hcs.rpgcore.market.MarketPriceRepository(
                        this.databaseManager
                );

        this.marketPriceService =
                new com.hcs.rpgcore.market.MarketPriceService(
                        this,
                        this.marketPriceRepository
                );

        this.shopRegistry =
                new com.hcs.rpgcore.shop.ShopRegistry();

        this.shopPriceService =
                new com.hcs.rpgcore.shop.ShopPriceService(
                        this.marketPriceService
                );

        this.shopEconomyService =
                new com.hcs.rpgcore.shop.ShopEconomyService(
                        this
                );

        this.shopItemProvider =
                new com.hcs.rpgcore.shop.ShopItemProvider(
                        this
                );

        this.shopGuiService =
                new com.hcs.rpgcore.shop.ShopGuiService(
                        this.shopRegistry,
                        this.shopPriceService,
                        this.shopItemProvider
                );

        this.shopTransactionService =
                new com.hcs.rpgcore.shop.ShopTransactionService(
                        this.shopPriceService,
                        this.shopEconomyService,
                        this.shopItemProvider
                );

        new com.hcs.rpgcore.item.EnhancementStoneRecipeService(
                this
        ).register();

        this.titleDisplayService =
                new com.hcs.rpgcore.title.PlayerTitleDisplayService();

        this.achievementTitleUnlockService =
                new com.hcs.rpgcore.title.AchievementTitleUnlockService(
                        this
                );

        this.wealthyTitleUnlockService =
                new com.hcs.rpgcore.title.WealthyTitleUnlockService(
                        this,
                        this.shopEconomyService,
                        new com.hcs.rpgcore.title.PlayerTitleCollectionRepository(
                                this.getDatabaseManager()
                        )
                );

        this.wealthyTitleUnlockService.start();

        this.playerNameTagService =
                new com.hcs.rpgcore.player.PlayerNameTagService(
                        this,
                        this.titleDisplayService
                );

        this.storageChestRepository =
                new StorageChestRepository(
                        this.databaseManager,
                        this.getLogger()
                );

        this.storageChestService =
                new StorageChestService(
                        this,
                        this.storageChestRepository
                );
        this.levelService = new LevelService(this.getConfig().getInt("server.maximum-level", 99));
        this.mobExperienceService = new MobExperienceService();
        ItemStatKeys itemStatKeys = new ItemStatKeys(this);

        com.hcs.rpgcore.item.WeaponEnhancementService
                weaponEnhancementService =
                new com.hcs.rpgcore.item.WeaponEnhancementService(
                        this,
                        itemStatKeys
                );

        this.getServer()
                .getPluginManager()
                .registerEvents(
                        new com.hcs.rpgcore.item.WeaponEnhancementAnvilListener(
                                weaponEnhancementService
                        ),
                        this
                );

        EquipmentStatReader equipmentStatReader = new EquipmentStatReader(itemStatKeys);
        this.statService = new StatService(equipmentStatReader);
        this.playerStatApplier = new PlayerStatApplier();
        this.manaService = new ManaService();
        this.elixirService = new ElixirService(this, this.manaService);
        this.elixirService.registerRecipes();
        this.hudService = new HudService(this, this.levelService, this.statService, this.manaService);

        this.locatorBarService =
                new com.hcs.rpgcore.locator.LocatorBarService(
                        this
                );

        this.combatService = new CombatService(this.hudService);
        this.skillDefenseBuffService = new SkillDefenseBuffService(this.combatService, this.manaService);
        this.skillOffenseBuffService = new SkillOffenseBuffService();
        this.skillItemFactory = new SkillItemFactory(this);
        this.warriorWeaponAttackService = new WarriorWeaponAttackService(equipmentStatReader, this.statService, this.skillItemFactory);
        this.skillCooldownService = new SkillCooldownService();
        this.warriorBashSkill = new WarriorBashSkill(this.hudService, this.warriorWeaponAttackService);
        this.mageFireBoltSkill = new MageFireBoltSkill(this, this.manaService);
        this.warriorWhirlwindSkill = new WarriorWhirlwindSkill(this, this.hudService, this.warriorWeaponAttackService);
        this.mageFrostNovaSkill = new MageFrostNovaSkill(this, this.manaService, this.skillOffenseBuffService);
        this.warriorExecutionSlashSkill = new WarriorExecutionSlashSkill(this.hudService, this.warriorWeaponAttackService);
        this.mageChainLightningSkill = new MageChainLightningSkill(this.manaService, this.skillOffenseBuffService);
        this.warriorIronWallSkill = new WarriorIronWallSkill(this, this.skillDefenseBuffService);
        this.mageManaShieldSkill = new MageManaShieldSkill(this.skillDefenseBuffService);
        this.warriorEarthquakeSlamSkill = new WarriorEarthquakeSlamSkill(this.hudService, this.warriorWeaponAttackService);
        this.mageBlizzardSkill = new MageBlizzardSkill(this, this.manaService, this.skillOffenseBuffService);
        this.warriorBattleCrySkill = new WarriorBattleCrySkill();
        this.mageBlackHoleSkill = new MageBlackHoleSkill(this, this.manaService, this.skillOffenseBuffService);
        this.warriorBerserkerRageSkill = new WarriorBerserkerRageSkill(this, this.skillOffenseBuffService);
        this.mageManaOverloadSkill = new MageManaOverloadSkill(this.skillOffenseBuffService);
        this.warriorBladeStormSkill = new WarriorBladeStormSkill(this, this.hudService, this.warriorWeaponAttackService);
        this.mageThunderStormSkill = new MageThunderStormSkill(this, this.manaService, this.skillOffenseBuffService);
        this.mageMeteorSkill = new MageMeteorSkill(this, this.hudService, this.statService, this.skillOffenseBuffService);
        this.warriorGateOfBabylonSkill = new WarriorInfiniteBladeSkill(this, this.hudService, this.warriorWeaponAttackService);
        this.skillAccessService = new SkillAccessService();
        this.skillRewardService = new SkillRewardService(this.skillItemFactory, this.skillAccessService);

        this.warbringerAnimationListener =
                new com.hcs.rpgcore.mob.WarbringerAnimationListener(
                        this
                );

        this.demonKnightAnimationListener =
                new com.hcs.rpgcore.mob.DemonKnightAnimationListener(
                        this
                );

        this.fallenAngelAnimationListener =
                new com.hcs.rpgcore.mob.FallenAngelAnimationListener(
                        this
                );

        this.fallenAngelBossPatternTask =
                new com.hcs.rpgcore.mob.FallenAngelBossPatternTask(
                        this
                );

        this.minotaurAnimationListener =
                new com.hcs.rpgcore.dungeon.MinotaurAnimationListener(
                        this
                );

        this.minotaurBossPatternTask =
                new com.hcs.rpgcore.dungeon.MinotaurBossPatternTask(
                        this,
                        this.minotaurAnimationListener
                );

        this.skillCollectionMenu = new SkillCollectionMenu(this.hudService, this.skillAccessService, this.skillItemFactory);
        this.pendingRewardRepository = new PendingRewardRepository(this.databaseManager);
        this.pendingRewardRepository.createTable();
        this.dismantleReturnRepository =
                new com.hcs.rpgcore.dismantle.EquipmentDismantleReturnRepository(
                        this.databaseManager
                );
        this.dismantleReturnRepository.createTable();
        this.dungeonRewardItemFactory = new DungeonRewardItemFactory(this);
        MobKillListener dungeonExperienceAwarder = new MobKillListener(this, this.playerRepository, this.levelService, this.mobExperienceService, this.statService, this.playerStatApplier, this.manaService, this.hudService, this.skillItemFactory, this.skillAccessService, this.skillRewardService);
        this.dungeonRewardService = new DungeonRewardService(
                this,
                this.pendingRewardRepository,
                this.dungeonRewardItemFactory,
                dungeonExperienceAwarder,
                this.storageChestService
        );
        this.zombieDungeonService.setRewardService(this.dungeonRewardService);
        this.redDragonDungeonService.setRewardService(this.dungeonRewardService);
        this.voidSanctumDungeonService.setRewardService(this.dungeonRewardService);
    }

    private void registerPlaceholderExpansion() {

        if (
                this.getServer()
                        .getPluginManager()
                        .getPlugin("PlaceholderAPI")
                        == null
        ) {

            this.getLogger().warning(
                    "PlaceholderAPI is not installed. "
                            + "RPGCore placeholders are disabled."
            );

            return;
        }

        new RPGCorePlaceholderExpansion(
                this,
                this.manaService
        ).register();

        this.getLogger().info(
                "RPGCore PlaceholderAPI expansion registered."
        );
    }


    private void registerListeners() {

        this.getServer()
                .getPluginManager()
                .registerEvents(
                        new com.hcs.rpgcore.shop.ShopListener(
                                this.shopRegistry,
                                this.shopTransactionService
                        ),
                        this
                );


        /*
         * =====================================================
         * EQUIPMENT DISMANTLE GUI
         * =====================================================
         */
        this.dismantleListener =
                new com.hcs.rpgcore.dismantle.EquipmentDismantleListener(
                        this
                );

        this.getServer()
                .getPluginManager()
                .registerEvents(
                        this.dismantleListener,
                        this
                );

        /*
         * =====================================================
         * CITIZENS SHOP NPC
         * =====================================================
         */
        if (
                this.getServer()
                        .getPluginManager()
                        .getPlugin("Citizens")
                        != null
        ) {

            this.getServer()
                    .getPluginManager()
                    .registerEvents(
                            new com.hcs.rpgcore.shop.ShopNpcListener(
                                    this.shopGuiService,
                                    this.dismantleListener
                            ),
                            this
                    );

            this.getServer()
                    .getPluginManager()
                    .registerEvents(
                            new com.hcs.rpgcore.shop.EnchantShopNpcListener(
                                    this,
                                    this.shopTransactionService,
                                    this.shopItemProvider
                            ),
                            this
                    );

            this.getServer()
                    .getPluginManager()
                    .registerEvents(
                            new com.hcs.rpgcore.enchant.EnchantApplyNpcListener(this),
                            this
                    );

            this.getServer()
                    .getPluginManager()
                    .registerEvents(
                            new com.hcs.rpgcore.starter.GuideNpcListener(),
                            this
                    );

            /*
             * =====================================================
             * HORSE RACE NPC 22
             * =====================================================
             *
             * 경마 기록 테이블이 준비된 경우에만 NPC 기능을 등록한다.
             */
            try {

                com.hcs.rpgcore.horse.HorseRaceRepository
                        horseRaceRepository =
                        new com.hcs.rpgcore.horse.HorseRaceRepository(
                                this.databaseManager
                        );

                horseRaceRepository.createTable();

                this.horseRaceNpcListener =
                        new com.hcs.rpgcore.horse.HorseRaceNpcListener(
                                this,
                                this.shopEconomyService,
                                horseRaceRepository,
                                this.achievementTitleUnlockService
                        );

                this.getServer()
                        .getPluginManager()
                        .registerEvents(
                                this.horseRaceNpcListener,
                                this
                        );

                this.getLogger().info(
                        "Horse race NPC 22 registered."
                );

            } catch (java.sql.SQLException exception) {

                this.getLogger().log(
                        java.util.logging.Level.SEVERE,
                        "Horse race table initialization failed. "
                                + "Horse race NPC will not be registered.",
                        exception
                );
            }

            /*
             * =====================================================
             * SLOT MACHINE NPC 23
             * =====================================================
             */
            try {

                com.hcs.rpgcore.slot.SlotMachineRepository
                        slotRepository =
                        new com.hcs.rpgcore.slot.SlotMachineRepository(
                                this.databaseManager
                        );

                slotRepository.createTable();

                this.slotMachineNpcListener =
                        new com.hcs.rpgcore.slot.SlotMachineNpcListener(
                                this,
                                this.shopEconomyService,
                                slotRepository,
                                this.achievementTitleUnlockService
                        );

                this.getServer()
                        .getPluginManager()
                        .registerEvents(
                                this.slotMachineNpcListener,
                                this
                        );

                this.getLogger().info(
                        "Slot machine NPC 23 registered."
                );

            } catch (java.sql.SQLException exception) {

                this.getLogger().log(
                        java.util.logging.Level.SEVERE,
                        "Slot machine table initialization failed. "
                                + "NPC 23 will not be registered.",
                        exception
                );
            }

            /*
             * =====================================================
             * LEVEL 10 REWARD - STURDY SHIELD
             * =====================================================
             */
            com.hcs.rpgcore.level.reward.LevelRewardRepository
                    levelRewardRepository =
                    new com.hcs.rpgcore.level.reward.LevelRewardRepository(
                            this.databaseManager
                    );

            try {

                levelRewardRepository.createTable();

                this.levelRewardService =
                        new com.hcs.rpgcore.level.reward.LevelRewardService(
                                this,
                                levelRewardRepository
                        );

                this.getServer()
                        .getPluginManager()
                        .registerEvents(
                                this.levelRewardService,
                                this
                        );

                this.getLogger().info(
                        "Level 10 sturdy shield reward service registered."
                );

            } catch (java.sql.SQLException exception) {

                this.getLogger().log(
                        java.util.logging.Level.SEVERE,
                        "Level reward table initialization failed.",
                        exception
                );
            }

            com.hcs.rpgcore.craft.EquipmentCraftNpcListener
                    equipmentCraftNpcListener =
                    new com.hcs.rpgcore.craft.EquipmentCraftNpcListener(
                            this,
                            this.equipmentCraftPreviewService
                    );

            this.getServer()
                    .getPluginManager()
                    .registerEvents(
                            equipmentCraftNpcListener,
                            this
                    );

            this.getServer()
                    .getPluginManager()
                    .registerEvents(
                            new com.hcs.rpgcore.craft.EquipmentPreviewResetListener(
                                    this.equipmentCraftPreviewService
                            ),
                            this
                    );

            this.getLogger().info(
                    "Citizens equipment preview NPC 18 reset listener registered."
            );

            this.getServer()
                    .getPluginManager()
                    .registerEvents(
                            equipmentCraftNpcListener.getCreationGui(),
                            this
                    );

            this.getLogger().info(
                    "Citizens equipment creation GUI listener registered."
            );

            this.getLogger().info(
                    "Citizens equipment craft NPC 15 listener registered."
            );

            this.getServer()
                    .getPluginManager()
                    .registerEvents(
                            new com.hcs.rpgcore.furniture.shop.FurnitureShopNpcListener(
                                    this,
                                    this.shopEconomyService,
                                    this.shopItemProvider
                            ),
                            this
                    );

            this.getLogger().info(
                    "Citizens furniture shop NPC 25 listener registered."
            );

            this.getServer()
                    .getPluginManager()
                    .registerEvents(
                            new com.hcs.rpgcore.title.PlayerTitleNpcListener(
                                    this,
                                    new com.hcs.rpgcore.title.PlayerTitleCollectionRepository(
                                            this.getDatabaseManager()
                                    ),
                                    this.titleDisplayService,
                                    this.playerNameTagService
                            ),
                            this
                    );

            this.getLogger().info(
                    "Citizens title management NPC 26 listener registered."
            );


            this.getLogger().info(
                    "Citizens shop NPC listener registered."
            );

        } else {

            this.getLogger().warning(
                    "Citizens is not installed. "
                            + "Shop NPC integration is disabled."
            );
        }

        this.getServer()
                .getPluginManager()
                .registerEvents(
                        new StorageChestListener(
                                this.storageChestService
                        ),
                        this
                );

        /*
         * initializeServices()에서 생성된 동일 인스턴스를
         * Listener와 runtime task가 공유한다.
         */
        this.getServer().getPluginManager().registerEvents(
                (Listener)this.minotaurAnimationListener,
                (Plugin)this
        );
        this.getServer().getPluginManager().registerEvents((Listener)new PlayerConnectionListener(
                        this,
                        this.playerRepository,
                        this.playerNameTagService,
                        new com.hcs.rpgcore.title.PlayerTitleRepository(
                                this.getDatabaseManager()
                        ),
                        this.titleDisplayService
                ), (Plugin)this);

        this.getServer().getPluginManager().registerEvents(
                this.titleDisplayService,
                this
        );
        this.getServer().getPluginManager().registerEvents((Listener)new VanillaExperienceListener(), (Plugin)this);

        this.getServer()
                .getPluginManager()
                .registerEvents(
                        new com.hcs.rpgcore.locator.LocatorBarListener(
                                this,
                                this.locatorBarService
                        ),
                        this
                );
        this.getServer().getPluginManager().registerEvents((Listener)new MobKillListener(this, this.playerRepository, this.levelService, this.mobExperienceService, this.statService, this.playerStatApplier, this.manaService, this.hudService, this.skillItemFactory, this.skillAccessService, this.skillRewardService), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new PlayerStatListener(this, this.playerRepository, this.statService, this.playerStatApplier), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new ManaListener(this, this.playerRepository, this.statService, this.manaService), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)this.elixirService, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new HudListener(this, this.playerRepository, this.hudService), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new EquipmentStatListener(this, this.hudService, this.statService, this.playerStatApplier, this.manaService), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new CombatListener(this.combatService, this.skillDefenseBuffService, this.skillOffenseBuffService, this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new FreeEnchantListener(), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new ForestLumberjackListener(), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new com.hcs.rpgcore.enchant.LoggingWaveListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new StarterKitListener(), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new BossHealthListener(), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new MonsterFallDamageListener(), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new com.hcs.rpgcore.mob.EliteDemonKnightListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)this.demonKnightAnimationListener, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)this.fallenAngelAnimationListener, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)this.fallenAngelBossPatternTask, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)this.warbringerAnimationListener, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new com.hcs.rpgcore.mob.FallenAngelBossDungeonListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new com.hcs.rpgcore.item.NetherLordArmorListener(this), (Plugin)this);


        this.getServer().getPluginManager().registerEvents(
                (Listener)new com.hcs.rpgcore.item.AuroriteArmorListener(
                        this,
                        this.endermanDungeonService
                ),
                (Plugin)this
        );

        com.hcs.rpgcore.item.ChestplateGlideListener
                chestplateGlideListener =
                        new com.hcs.rpgcore.item.ChestplateGlideListener(
                                this
                        );

        this.getServer().getPluginManager().registerEvents(
                (Listener)chestplateGlideListener,
                (Plugin)this
        );

        this.getServer().getScheduler().runTaskTimer(
                this,
                chestplateGlideListener,
                1L,
                1L
        );
        SkillUseListener skillUseListener = new SkillUseListener(this.skillItemFactory, this.skillCooldownService, this.manaService, this.hudService, this.warriorBashSkill, this.mageFireBoltSkill, this.warriorWhirlwindSkill, this.mageFrostNovaSkill, this.warriorExecutionSlashSkill, this.mageChainLightningSkill, this.warriorIronWallSkill, this.mageManaShieldSkill, this.warriorEarthquakeSlamSkill, this.mageBlizzardSkill, this.warriorBattleCrySkill, this.mageBlackHoleSkill, this.warriorBerserkerRageSkill, this.mageManaOverloadSkill, this.warriorBladeStormSkill, this.mageThunderStormSkill, this.mageMeteorSkill, this.warriorGateOfBabylonSkill, this.skillAccessService, this.skillOffenseBuffService);
        this.getServer().getPluginManager().registerEvents((Listener)skillUseListener, (Plugin)this);

        SkillInputWindowService skillInputWindowService = new SkillInputWindowService();

        this.getServer().getPluginManager().registerEvents((Listener)new SkillInputListener(this.skillItemFactory, skillInputWindowService), (Plugin)this);

        this.getServer().getPluginManager().registerEvents((Listener)new SkillHotbarActivationListener(this, this.skillItemFactory, skillUseListener, skillInputWindowService), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new SkillItemProtectionListener(this, this.skillItemFactory), (Plugin)this);
        this.getServer().getPluginManager().registerEvents(
                (Listener)new com.hcs.rpgcore.item.CustomItemDeathProtectionListener(
                        this,
                        this.skillItemFactory
                ),
                (Plugin)this
        );
        this.getServer().getPluginManager().registerEvents((Listener)new SkillCollectionListener(this.skillItemFactory, this.skillCollectionMenu), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new WarriorWeaponAttackListener(this, this.warriorWeaponAttackService), (Plugin)this);
    }

    private void registerCommands() {

        Objects.requireNonNull(
                this.getCommand("rpgpreview"),
                "Command 'rpgpreview' is missing from plugin.yml"
        ).setExecutor(
                new com.hcs.rpgcore.craft.EquipmentCraftPreviewCommand(
                        this.equipmentCraftPreviewService,
                        this.getDatabaseManager()
                )
        );


        Objects.requireNonNull(
                this.getCommand("rpgshop"),
                "Command 'rpgshop' is missing from plugin.yml"
        ).setExecutor(
                (CommandExecutor)new com.hcs.rpgcore.command.RpgShopCommand(
                        this.shopRegistry,
                        this.shopGuiService
                )
        );
        Objects.requireNonNull(this.getCommand("rpgcore"), "Command 'rpgcore' is missing from plugin.yml").setExecutor((CommandExecutor)this);
        Objects.requireNonNull(this.getCommand("stats"), "Command 'stats' is missing from plugin.yml").setExecutor((CommandExecutor)new StatsCommand(this, this.playerRepository, this.levelService, this.statService, this.manaService));
        Objects.requireNonNull(this.getCommand("rpgadmin"), "Command 'rpgadmin' is missing from plugin.yml").setExecutor((CommandExecutor)new RPGAdminCommand(
                this,
                this.playerRepository,
                this.playerNameTagService,
                this.levelService,
                this.statService,
                this.playerStatApplier,
                this.manaService,
                this.hudService,
                this.skillRewardService
        ));
        Objects.requireNonNull(this.getCommand("class"), "Command 'class' is missing from plugin.yml").setExecutor((CommandExecutor)new ClassCommand(this, this.playerRepository, this.levelService, this.statService, this.playerStatApplier, this.manaService, this.hudService, this.skillItemFactory, this.skillRewardService));
        this.getServer()
                .getPluginManager()
                .registerEvents(
                        new com.hcs.rpgcore.check.GoldCheckRedeemListener(
                                this,
                                new com.hcs.rpgcore.check.GoldCheckRepository(
                                        this.getDatabaseManager()
                                ),
                                new com.hcs.rpgcore.check.GoldCheckItemFactory(
                                        this
                                ),
                                this.shopEconomyService
                        ),
                        this
                );

        this.getLogger().info(
                "RPGCore gold check redemption listener registered."
        );

        this.levelTitleCheckTask =
                new com.hcs.rpgcore.title.LevelTitleCheckTask(
                        this,
                        this.playerRepository,
                        new com.hcs.rpgcore.title.LevelTitleUnlockService(
                                this
                        )
                );

        this.levelTitleCheckTask.start();

        Objects.requireNonNull(
                this.getCommand("칭호"),
                "Command '칭호' is missing from plugin.yml"
        ).setExecutor(
                new com.hcs.rpgcore.title.PlayerTitleCommand(
                        this,
                        new com.hcs.rpgcore.title.PlayerTitleRepository(
                                this.getDatabaseManager()
                        ),
                        this.playerNameTagService,
                        this.titleDisplayService
                )
        );

        Objects.requireNonNull(
                this.getCommand("수표"),
                "Command '수표' is missing from plugin.yml"
        ).setExecutor(
                new com.hcs.rpgcore.check.GoldCheckCommand(
                        this,
                        new com.hcs.rpgcore.check.GoldCheckRepository(
                                this.getDatabaseManager()
                        ),
                        new com.hcs.rpgcore.check.GoldCheckItemFactory(
                                this
                        ),
                        this.shopEconomyService
                )
        );

        Objects.requireNonNull(this.getCommand("rpgclaim"), "Command 'rpgclaim' is missing from plugin.yml").setExecutor((CommandExecutor)new RpgClaimCommand(this.pendingRewardRepository, this.dungeonRewardItemFactory));
    }

    private void startRuntimeTasks() {
        double regenerationPerSecond = this.getConfig().getDouble("mana.regeneration-per-second", 2.0);
        this.getServer().getScheduler().runTaskTimer((Plugin)this, (Runnable)new ManaRegenerationTask(this.manaService, regenerationPerSecond), 20L, 20L);
        /*
         * Legacy RPGCore HUD renderer disabled.
         *
         * HudService remains active as the RPG profile/stat data service.
         * Visual HUD rendering is handled by BetterHud.
         */

        /*
         * Minotaur animation update.
         *
         * 2 ticks = 0.1 seconds.
         */
        this.getServer().getScheduler().runTaskTimer(
                (Plugin)this,
                (Runnable)this.minotaurAnimationListener,
                2L,
                2L
        );

        /*
         * Minotaur phase / boss pattern update.
         *
         * 5 ticks = 0.25 seconds.
         */
        this.getServer().getScheduler().runTaskTimer(
                (Plugin)this,
                (Runnable)this.minotaurBossPatternTask,
                20L,
                5L
        );

        /*
         * Fallen Angel boss phase / pattern update.
         *
         * 같은 인스턴스를 Listener와 Runnable에서 공유한다.
         *
         * 5 ticks = 0.25 seconds
         */
        this.getServer().getScheduler().runTaskTimer(
                (Plugin)this,
                (Runnable)this.fallenAngelBossPatternTask,
                20L,
                5L
        );

        /*
         * Fallen Angel animation state update.
         *
         * Listener와 동일한 인스턴스를 사용하여
         * 공격 loop 종료와 idle/walk 상태를 공유한다.
         *
         * 2 ticks = 0.1 seconds
         */
        this.getServer().getScheduler().runTaskTimer(
                (Plugin)this,
                (Runnable)this.fallenAngelAnimationListener,
                2L,
                2L
        );

        /*
         * Demon Knight animation state update.
         *
         * Listener와 동일한 인스턴스를 사용하여
         * 공격 순서와 이동 애니메이션 상태를 공유한다.
         *
         * 2 ticks = 0.1 seconds
         */
        this.getServer().getScheduler().runTaskTimer(
                (Plugin)this,
                (Runnable)this.demonKnightAnimationListener,
                2L,
                2L
        );

        /*
         * Warbringer animation state update.
         *
         * Listener와 동일한 인스턴스를 사용하여
         * 공격 상태와 이동 애니메이션 상태를 공유한다.
         *
         * 2 ticks = 0.1 seconds
         */
        this.getServer().getScheduler().runTaskTimer(
                (Plugin)this,
                (Runnable)this.warbringerAnimationListener,
                2L,
                2L
        );

        /*
         * Redstone Golem animation update.
         *
         * 정지 상태에서는 idle,
         * 이동 상태에서는 walk 애니메이션만 사용한다.
         *
         * 2 ticks = 0.1 seconds
         */
        this.getServer().getScheduler().runTaskTimer(
                (Plugin)this,
                (Runnable)new com.hcs.rpgcore.mob.RedstoneGolemAnimationTask(
                        this
                ),
                2L,
                2L
        );

        /*
         * Demon Knight field elite spawn check.
         *
         * Zombie 자연 스폰 변환을 사용하지 않고
         * 플레이어 주변의 로드된 필드에서 직접 생성한다.
         *
         * Redstone Golem / Warbringer와 같은
         * 600 ticks = 30 seconds 주기를 사용한다.
         *
         * 초기 실행 시점은 600 ticks로 분산한다.
         */
        this.getServer().getScheduler().runTaskTimer(
                (Plugin)this,
                (Runnable)new com.hcs.rpgcore.mob.DemonKnightSpawnService(
                        this
                ),
                600L,
                600L
        );

        /*
         * Redstone Golem field elite spawn check.
         *
         * 200 ticks initial delay
         * 600 ticks = 30 seconds
         */
        this.getServer().getScheduler().runTaskTimer(
                (Plugin)this,
                (Runnable)new com.hcs.rpgcore.mob.RedstoneGolemSpawnService(this),
                200L,
                600L
        );

        /*
         * Warbringer field elite spawn check.
         *
         * Redstone Golem과 같은 30초 주기를 사용하되
         * 같은 tick에 두 대형 엘리트 스폰 검사가 겹치지 않도록
         * 초기 실행 시점을 400 ticks로 분산한다.
         */
        this.getServer().getScheduler().runTaskTimer(
                (Plugin)this,
                (Runnable)new com.hcs.rpgcore.mob.WarbringerSpawnService(this),
                400L,
                600L
        );

        this.getServer().getScheduler().runTaskTimer((Plugin)this, (Runnable)new ClassSelectionReminderTask(this, this.playerRepository), 100L, 600L);
    }

    /**
     * 레벨 저장 완료 후 Lv.10 최초 달성 보상을 등록한다.
     */
    public void registerLevel10ShieldReward(
            java.util.UUID uuid,
            int oldLevel,
            int newLevel
    ) {
        if (this.levelRewardService != null) {
            this.levelRewardService.onLevelSaved(
                    uuid,
                    oldLevel,
                    newLevel
            );
        }
    }

    public void onDisable() {

        if (this.slotMachineNpcListener != null) {
            this.slotMachineNpcListener.shutdown();
        }

        if (this.horseRaceNpcListener != null) {
            this.horseRaceNpcListener.shutdown();
        }

        if (this.equipmentCraftPreviewService != null) {
            this.equipmentCraftPreviewService.shutdown();
        }

        if (this.dismantleListener != null) {
            this.dismantleListener.savePendingOnShutdown();
        }
        if (this.locatorBarService != null) {
            this.locatorBarService.removeAll();
        }

        if (this.hudService != null) {
            this.hudService.clear();
        }
        if (this.manaService != null) {
            this.manaService.clear();
        }
        if (this.wealthyTitleUnlockService != null) {
            this.wealthyTitleUnlockService.shutdown();
        }

        if (this.levelTitleCheckTask != null) {
            this.levelTitleCheckTask.shutdown();
        }

        if (this.databaseManager != null) {
            this.databaseManager.close();
        }
        this.getComponentLogger().info((Component)Component.text((String)"RPGCore disabled.", (TextColor)NamedTextColor.YELLOW));
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("rpgcore")) {
            return false;
        }
        boolean databaseConnected = this.databaseManager != null && this.databaseManager.isConnected();
        sender.sendMessage(Component.text((String)"RPGCore", (TextColor)NamedTextColor.GOLD).append((Component)Component.text((String)(" v" + this.getPluginMeta().getVersion()), (TextColor)NamedTextColor.YELLOW)));
        sender.sendMessage((Component)Component.text((String)"\ud50c\ub7ec\uadf8\uc778 \uc0c1\ud0dc: \uc815\uc0c1 \uc791\ub3d9 \uc911", (TextColor)NamedTextColor.GREEN));
        sender.sendMessage((Component)Component.text((String)("\ub370\uc774\ud130\ubca0\uc774\uc2a4: " + (databaseConnected ? "\uc5f0\uacb0\ub428" : "\uc5f0\uacb0\ub418\uc9c0 \uc54a\uc74c")), (TextColor)(databaseConnected ? NamedTextColor.GREEN : NamedTextColor.RED)));
        sender.sendMessage((Component)Component.text((String)("\ucd5c\ub300 \ub808\ubca8: " + this.getConfig().getInt("server.maximum-level", 99)), (TextColor)NamedTextColor.AQUA));
        sender.sendMessage((Component)Component.text((String)("MANA \ub7f0\ud0c0\uc784: " + (this.manaService != null ? "\ud65c\uc131" : "\ube44\ud65c\uc131")), (TextColor)(this.manaService != null ? NamedTextColor.AQUA : NamedTextColor.RED)));
        sender.sendMessage((Component)Component.text((String)("RPG HUD: " + (this.hudService != null ? "\ud65c\uc131" : "\ube44\ud65c\uc131")), (TextColor)(this.hudService != null ? NamedTextColor.GREEN : NamedTextColor.RED)));
        return true;
    }

    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    public com.hcs.rpgcore.dismantle.EquipmentDismantleReturnRepository
            getDismantleReturnRepository() {
        return this.dismantleReturnRepository;
    }

    public PlayerRepository getPlayerRepository() {
        return this.playerRepository;
    }

    public LevelService getLevelService() {
        return this.levelService;
    }

    public MobExperienceService getMobExperienceService() {
        return this.mobExperienceService;
    }

    public StatService getStatService() {
        return this.statService;
    }

    public PlayerStatApplier getPlayerStatApplier() {
        return this.playerStatApplier;
    }

    public ManaService getManaService() {
        return this.manaService;
    }

    public HudService getHudService() {
        return this.hudService;
    }


    public DungeonRewardService getDungeonRewardService() {
        return this.dungeonRewardService;
    }

}

