package com.hcs.rpgcore.slot;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.shop.ShopEconomyService;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public final class SlotMachineNpcListener implements Listener {

    private static final int NPC_ID = 23;
    private static final int INVENTORY_SIZE = 45;

    private static final int REEL_1_SLOT = 20;
    private static final int REEL_2_SLOT = 22;
    private static final int REEL_3_SLOT = 24;

    private static final int DECREASE_SLOT = 36;
    private static final int INCREASE_SLOT = 37;
    private static final int START_SLOT = 40;
    private static final int BET_INFO_SLOT = 44;

    private static final String POTATO = "BAKED_POTATO";
    private static final String APPLE = "APPLE";
    private static final String GOLDEN_APPLE = "GOLDEN_APPLE";

    private static final String[] SYMBOLS = {
            POTATO,
            APPLE,
            GOLDEN_APPLE
    };

    private final RPGCorePlugin plugin;
    private final SlotMachineBetService betService;

    private final Map<UUID, SlotHolder> playerGames =
            new HashMap<>();

    private static final class SlotHolder
            implements InventoryHolder {

        private final UUID owner;

        private Inventory inventory;

        private long bet = SlotMachineBetService.MIN_BET;
        private long gameId;

        private String reel1 = POTATO;
        private String reel2 = APPLE;
        private String reel3 = GOLDEN_APPLE;

        private long payout;

        private boolean running;
        private boolean finished;
        private boolean settlementIssue;

        private SlotHolder(UUID owner) {
            this.owner = owner;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public SlotMachineNpcListener(
            RPGCorePlugin plugin,
            ShopEconomyService economy,
            SlotMachineRepository repository,
            com.hcs.rpgcore.title.AchievementTitleUnlockService
                    achievementTitleUnlockService
    ) {

        this.plugin = plugin;

        this.betService = new SlotMachineBetService(
                plugin,
                economy,
                repository,
                achievementTitleUnlockService
        );
    }

    @EventHandler
    public void onNpcRightClick(NPCRightClickEvent event) {

        if (event.getNPC().getId() != NPC_ID) {
            return;
        }

        Player player = event.getClicker();
        UUID uuid = player.getUniqueId();

        SlotHolder holder = playerGames.get(uuid);

        if (holder != null) {

            player.openInventory(holder.inventory);
            return;
        }

        boolean prepared = betService.preparePlayer(player);

        holder = new SlotHolder(uuid);

        if (!prepared) {
            holder.settlementIssue = true;
        }

        try {

            SlotMachineRepository.GameRecord latest =
                    betService.findLatest(uuid);

            if (latest != null
                    && !"PREPARED".equals(latest.status())
                    && !"CANCELLED".equals(latest.status())) {

                holder.gameId = latest.gameId();
                holder.bet = latest.wager();

                holder.reel1 = latest.reel1();
                holder.reel2 = latest.reel2();
                holder.reel3 = latest.reel3();

                holder.payout = latest.payout();
                holder.finished = true;
            }

        } catch (SQLException exception) {

            holder.settlementIssue = true;

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Slot machine latest game lookup failed: " + uuid,
                    exception
            );
        }

        Inventory inventory = Bukkit.createInventory(
                holder,
                INVENTORY_SIZE,
                Component.text(
                        "슬롯 머신",
                        NamedTextColor.GOLD
                )
        );

        holder.inventory = inventory;

        playerGames.put(uuid, holder);

        render(holder);

        player.openInventory(inventory);
    }

    private void render(SlotHolder holder) {

        Inventory inventory = holder.inventory;

        inventory.clear();

        ItemStack background = item(
                Material.BLACK_STAINED_GLASS_PANE,
                " ",
                NamedTextColor.DARK_GRAY,
                List.of()
        );

        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            inventory.setItem(slot, background.clone());
        }

        inventory.setItem(
                REEL_1_SLOT,
                symbolItem(holder.reel1)
        );

        inventory.setItem(
                REEL_2_SLOT,
                symbolItem(holder.reel2)
        );

        inventory.setItem(
                REEL_3_SLOT,
                symbolItem(holder.reel3)
        );

        inventory.setItem(
                DECREASE_SLOT,
                item(
                        Material.ARROW,
                        "베팅 금액 -100골드",
                        NamedTextColor.RED,
                        List.of(
                                Component.text(
                                        "현재 베팅: " + holder.bet + "골드"
                                ),
                                Component.text("최소 베팅: 100골드")
                        )
                )
        );

        inventory.setItem(
                INCREASE_SLOT,
                item(
                        Material.ARROW,
                        "베팅 금액 +100골드",
                        NamedTextColor.GREEN,
                        List.of(
                                Component.text(
                                        "현재 베팅: " + holder.bet + "골드"
                                )
                        )
                )
        );

        inventory.setItem(
                BET_INFO_SLOT,
                item(
                        Material.GOLD_NUGGET,
                        "현재 베팅: " + holder.bet + "G",
                        NamedTextColor.GOLD,
                        List.of()
                )
        );

        if (holder.running) {

            inventory.setItem(
                    START_SLOT,
                    item(
                            Material.CLOCK,
                            "회전 중...",
                            NamedTextColor.YELLOW,
                            List.of(
                                    Component.text(
                                            "결과가 나올 때까지 기다려 주세요."
                                    )
                            )
                    )
            );

        } else if (holder.finished) {

            inventory.setItem(
                    START_SLOT,
                    item(
                            holder.settlementIssue
                                    ? Material.BARRIER
                                    : Material.RECOVERY_COMPASS,
                            holder.settlementIssue
                                    ? "정산 확인 필요"
                                    : "초기화",
                            holder.settlementIssue
                                    ? NamedTextColor.RED
                                    : NamedTextColor.AQUA,
                            List.of(
                                    Component.text(
                                            holder.settlementIssue
                                                    ? "정산 확인 후 초기화할 수 있습니다."
                                                    : "베팅액을 유지하고 다음 게임 준비"
                                    )
                            )
                    )
            );

        } else {

            inventory.setItem(
                    START_SLOT,
                    item(
                            holder.settlementIssue
                                    ? Material.BARRIER
                                    : Material.END_CRYSTAL,
                            holder.settlementIssue
                                    ? "정산 확인 필요"
                                    : "돌리기",
                            holder.settlementIssue
                                    ? NamedTextColor.RED
                                    : NamedTextColor.GREEN,
                            List.of(
                                    Component.text(
                                            "베팅액: " + holder.bet + "G"
                                    )
                            )
                    )
            );
        }
    }

    private ItemStack symbolItem(String symbol) {

        Material material = switch (symbol) {
            case POTATO -> Material.BAKED_POTATO;
            case GOLDEN_APPLE -> Material.GOLDEN_APPLE;
            default -> Material.APPLE;
        };

        String name = switch (symbol) {
            case POTATO -> "구운 감자";
            case GOLDEN_APPLE -> "황금 사과";
            default -> "사과";
        };

        String multiplier = switch (symbol) {
            case POTATO -> "1.5배";
            case GOLDEN_APPLE -> "5배";
            default -> "2배";
        };

        return item(
                material,
                name,
                NamedTextColor.YELLOW,
                List.of(Component.text(multiplier))
        );
    }

    private ItemStack item(
            Material material,
            String name,
            NamedTextColor color,
            List<Component> lore
    ) {

        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();

        /*
         * 경마 GUI와 동일한 화살표 모델.
         * 1002: 왼쪽 / 1000: 오른쪽
         */
        if (material == Material.ARROW) {

            if ("베팅 금액 -100골드".equals(name)) {
                meta.setCustomModelData(1002);

            } else if ("베팅 금액 +100골드".equals(name)) {
                meta.setCustomModelData(1000);
            }
        }

        meta.displayName(
                Component.text(name, color)
        );

        meta.lore(lore);
        stack.setItemMeta(meta);

        return stack;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (!(event.getView()
                .getTopInventory()
                .getHolder() instanceof SlotHolder holder)) {
            return;
        }

        // 슬롯 머신 GUI의 아이템 이동을 모두 차단한다.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!player.getUniqueId().equals(holder.owner)) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot < 0 || slot >= INVENTORY_SIZE) {
            return;
        }

        // 회전 중에는 모든 버튼을 비활성화한다.
        if (holder.running) {
            return;
        }

        /*
         * 결과 화면에서는 가운데 버튼이 초기화 버튼이 된다.
         * 초기화해도 기존 베팅액은 유지한다.
         */
        if (holder.finished) {

            if (slot != START_SLOT) {
                return;
            }

            if (!betService.preparePlayer(player)) {

                holder.settlementIssue = true;
                render(holder);
                return;
            }

            holder.gameId = 0L;
            holder.payout = 0L;

            holder.reel1 = POTATO;
            holder.reel2 = APPLE;
            holder.reel3 = GOLDEN_APPLE;

            holder.finished = false;
            holder.settlementIssue = false;

            // holder.bet은 초기화하지 않는다.
            render(holder);

            player.sendMessage(
                    "§6[슬롯 머신] §f다음 게임을 준비했습니다."
            );

            return;
        }

        /*
         * 이전 게임에 미정산 기록이 있다면
         * 새로운 베팅 전에 정산 상태를 확인한다.
         */
        if (holder.settlementIssue) {

            if (!betService.preparePlayer(player)) {
                return;
            }

            holder.settlementIssue = false;
        }

        switch (slot) {

            case DECREASE_SLOT -> {

                holder.bet = Math.max(
                        SlotMachineBetService.MIN_BET,
                        holder.bet
                                - SlotMachineBetService.BET_STEP
                );

                render(holder);
            }

            case INCREASE_SLOT -> {

                if (holder.bet
                        > SlotMachineBetService.MAX_BET
                        - SlotMachineBetService.BET_STEP) {

                    player.sendMessage(
                            "§c[슬롯 머신] 최대 베팅액은 "
                                    + "100,000,000G입니다."
                    );

                    return;
                }

                holder.bet +=
                        SlotMachineBetService.BET_STEP;

                render(holder);
            }

            case START_SLOT -> {

                startGame(holder, player);
            }

            default -> {
                // 릴과 장식 아이템은 조작할 수 없다.
            }
        }
    }

    private void startGame(
            SlotHolder holder,
            Player player
    ) {

        if (holder.running || holder.finished) {
            return;
        }

        long gameId = betService.placeBet(
                player,
                holder.bet
        );

        if (gameId <= 0L) {
            return;
        }

        final SlotMachineRepository.GameRecord game;

        try {

            game = betService.findGame(gameId);

            if (game == null) {
                throw new SQLException(
                        "베팅 완료 게임을 찾지 못했습니다."
                );
            }

        } catch (SQLException exception) {

            holder.gameId = gameId;
            holder.settlementIssue = true;

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Slot machine started game lookup failed: "
                            + gameId,
                    exception
            );

            player.sendMessage(
                    "§c[슬롯 머신] 게임 기록 확인이 필요합니다. "
                            + "게임 번호: " + gameId
            );

            render(holder);
            return;
        }

        holder.gameId = gameId;
        holder.payout = game.payout();

        holder.running = true;
        holder.finished = false;
        holder.settlementIssue = false;

        render(holder);

        /*
         * 5틱 간격으로 12회 실행한다.
         * 총 60틱, 약 3초 동안 릴을 회전시킨다.
         *
         * GUI를 닫아도 작업은 서버에서 계속 진행된다.
         */
        new BukkitRunnable() {

            private int steps;

            @Override
            public void run() {

                if (!holder.running) {
                    cancel();
                    return;
                }

                steps++;

                if (steps >= 12) {

                    completeGame(holder, game);
                    cancel();
                    return;
                }

                ThreadLocalRandom random =
                        ThreadLocalRandom.current();

                holder.reel1 = randomSymbol(random);
                holder.reel2 = randomSymbol(random);
                holder.reel3 = randomSymbol(random);

                render(holder);
            }

        }.runTaskTimer(
                plugin,
                5L,
                5L
        );
    }

    private String randomSymbol(
            ThreadLocalRandom random
    ) {

        return SYMBOLS[
                random.nextInt(SYMBOLS.length)
        ];
    }

    private void completeGame(
            SlotHolder holder,
            SlotMachineRepository.GameRecord game
    ) {

        holder.running = false;
        holder.finished = true;

        // 애니메이션 결과가 아닌 DB에 저장된 확정 결과를 표시한다.
        holder.reel1 = game.reel1();
        holder.reel2 = game.reel2();
        holder.reel3 = game.reel3();

        boolean recorded = betService.finishGame(
                game.gameId()
        );

        Player player = Bukkit.getPlayer(holder.owner);

        if (!recorded) {

            holder.settlementIssue = true;

            if (player != null) {

                player.sendMessage(
                        "§c[슬롯 머신] 결과 기록 확인이 필요합니다. "
                                + "게임 번호: " + game.gameId()
                );
            }

        } else if (game.payout() > 0L && player != null) {

            if (!betService.settlePayout(
                    player,
                    game.gameId()
            )) {

                holder.settlementIssue = true;

                player.sendMessage(
                        "§c[슬롯 머신] 당첨금 정산 확인이 필요합니다. "
                                + "게임 번호: " + game.gameId()
                );
            }
        }

        if (player != null) {

            if (game.payout() > 0L) {

                player.sendMessage(
                        "§6[슬롯 머신] §f당첨! "
                                + "지급 예정 금액: "
                                + game.payout() + "G"
                );

            } else {

                player.sendMessage(
                        "§c[슬롯 머신] 아쉽게도 실패했습니다."
                );
            }
        }

        render(holder);
    }

    /**
     * DB 연결이 닫히기 전에 호출한다.
     *
     * 정상 종료 시 진행 중인 게임은
     * 이미 DB에 저장된 최종 결과대로 완료 처리한다.
     *
     * 당첨금은 PAYOUT_PENDING으로 남겨
     * 플레이어 재접속 시 정산한다.
     */
    public void shutdown() {

        for (SlotHolder holder : playerGames.values()) {

            if (!holder.running || holder.gameId <= 0L) {
                continue;
            }

            holder.running = false;

            if (!betService.finishGame(holder.gameId)) {

                plugin.getLogger().severe(
                        "Slot machine shutdown needs review: game="
                                + holder.gameId
                                + " player="
                                + holder.owner
                );
            }
        }

        playerGames.clear();
    }

    /**
     * 재접속 시 미정산 게임을 확인한다.
     *
     * GUI를 닫았다가 다시 여는 경우에는
     * NPC 클릭 처리에서 기존 Holder를 재사용한다.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        UUID uuid = event.getPlayer().getUniqueId();

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> {

                    Player player = Bukkit.getPlayer(uuid);

                    if (player == null) {
                        return;
                    }

                    SlotHolder holder = playerGames.get(uuid);

                    if (holder != null && holder.running) {
                        return;
                    }

                    if (betService.preparePlayer(player)) {

                        if (holder != null) {

                            holder.settlementIssue = false;
                            render(holder);
                        }
                    }
                },
                20L
        );
    }

    /**
     * 슬롯 머신 GUI로 아이템을 끌어 넣거나
     * GUI 아이템을 이동하는 동작을 차단한다.
     */
    @EventHandler
    public void onDrag(InventoryDragEvent event) {

        if (!(event.getView()
                .getTopInventory()
                .getHolder() instanceof SlotHolder)) {
            return;
        }

        boolean touchesTop =
                event.getRawSlots()
                        .stream()
                        .anyMatch(
                                slot -> slot < INVENTORY_SIZE
                        );

        if (touchesTop) {
            event.setCancelled(true);
        }
    }
}
