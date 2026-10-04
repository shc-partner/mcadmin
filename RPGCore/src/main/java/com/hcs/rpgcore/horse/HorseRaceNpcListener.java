package com.hcs.rpgcore.horse;

import com.hcs.rpgcore.RPGCorePlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public final class HorseRaceNpcListener implements Listener {

    private static final int NPC_ID = 22;

    private static final int GOLD_START = 0;
    private static final int DIAMOND_START = 18;

    private static final int GOLD_FINISH = 8;
    private static final int DIAMOND_FINISH = 26;

    private static final int DECREASE_SLOT = 36;
    private static final int INCREASE_SLOT = 37;
    private static final int START_SLOT = 40;
    private static final int BET_INFO_SLOT = 44;

    private static final long MIN_BET = 100L;
    private static final long BET_STEP = 100L;

    private final RPGCorePlugin plugin;
    private final HorseRaceBetService betService;

    /*
     * 진행 중인 경기만 보관한다.
     * GUI를 닫아도 이 Map에서 경기가 유지된다.
     */
    private final Map<UUID, RaceHolder> activeRaces =
            new HashMap<>();

    public HorseRaceNpcListener(
            RPGCorePlugin plugin,
            com.hcs.rpgcore.shop.ShopEconomyService economy,
            HorseRaceRepository repository,
            com.hcs.rpgcore.title.AchievementTitleUnlockService
                    achievementTitleUnlockService
    ) {
        this.plugin = plugin;
        this.betService = new HorseRaceBetService(
                plugin,
                economy,
                repository,
                achievementTitleUnlockService
        );
    }

    private enum Horse {
        GOLD,
        DIAMOND
    }

    private static final class RaceHolder
            implements InventoryHolder {

        private final UUID owner;

        private Inventory inventory;

        private long bet = MIN_BET;
        private Horse selected;

        private int goldPosition = GOLD_START;
        private int diamondPosition = DIAMOND_START;

        private long raceId;
        private boolean running;
        private Horse winner;

        private RaceHolder(UUID owner) {
            this.owner = owner;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    @EventHandler
    public void onNpcRightClick(NPCRightClickEvent event) {

        if (event.getNPC().getId() != NPC_ID) {
            return;
        }

        Player player = event.getClicker();

        /*
         * 경기 도중 다시 NPC를 클릭하면
         * 기존 경기 화면을 다시 보여준다.
         */
        RaceHolder holder =
                activeRaces.get(player.getUniqueId());

        if (holder != null) {
            player.openInventory(holder.inventory);
            return;
        }

        if (!betService.preparePlayer(player)) {
            return;
        }

        holder = new RaceHolder(player.getUniqueId());

        Inventory inventory = Bukkit.createInventory(
                holder,
                45,
                Component.text(
                        "경마",
                        NamedTextColor.GOLD
                )
        );

        holder.inventory = inventory;

        render(holder);

        player.openInventory(inventory);
    }

    private void render(RaceHolder holder) {

        Inventory inventory = holder.inventory;

        inventory.clear();

        ItemStack divider = item(
                Material.BLACK_STAINED_GLASS_PANE,
                " ",
                NamedTextColor.DARK_GRAY,
                List.of()
        );

        for (int slot = 9; slot <= 17; slot++) {
            inventory.setItem(slot, divider.clone());
        }

        for (int slot = 27; slot <= 35; slot++) {
            inventory.setItem(slot, divider.clone());
        }

        /*
         * 말이 결승선에 도착하면
         * 결승선 아이콘 대신 말 갑옷이 표시된다.
         */
        inventory.setItem(
                GOLD_FINISH,
                item(
                        Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                        "금색 경주로 결승선",
                        NamedTextColor.AQUA,
                        List.of()
                )
        );

        inventory.setItem(
                DIAMOND_FINISH,
                item(
                        Material.PINK_STAINED_GLASS_PANE,
                        "다이아몬드 경주로 결승선",
                        NamedTextColor.LIGHT_PURPLE,
                        List.of()
                )
        );

        inventory.setItem(
                holder.goldPosition,
                item(
                        Material.GOLDEN_HORSE_ARMOR,
                        "금색 말",
                        NamedTextColor.GOLD,
                        List.of(
                                horseDescription(
                                        holder,
                                        Horse.GOLD
                                )
                        )
                )
        );

        inventory.setItem(
                holder.diamondPosition,
                item(
                        Material.DIAMOND_HORSE_ARMOR,
                        "다이아몬드 말",
                        NamedTextColor.AQUA,
                        List.of(
                                horseDescription(
                                        holder,
                                        Horse.DIAMOND
                                )
                        )
                )
        );

        inventory.setItem(
                DECREASE_SLOT,
                item(
                        Material.ARROW,
                        "베팅 금액 -100골드",
                        NamedTextColor.RED,
                        List.of(
                                "현재 베팅: "
                                        + holder.bet
                                        + "골드",
                                "최소 베팅: 100골드"
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
                                "현재 베팅: "
                                        + holder.bet
                                        + "골드"
                        )
                )
        );

        String startName;

        if (holder.winner != null) {
            startName = "다시 경주하기";
        } else if (holder.running) {
            startName = "경주 진행 중";
        } else {
            startName = "달리기 시작";
        }

        inventory.setItem(
                START_SLOT,
                item(
                        Material.END_CRYSTAL,
                        startName,
                        NamedTextColor.LIGHT_PURPLE,
                        List.of(
                                holder.selected == null
                                        ? "먼저 경주마를 선택하세요."
                                        : "선택한 말: "
                                                + horseName(
                                                        holder.selected
                                                ),
                                "베팅: " + holder.bet + "골드",
                                "경기 시작 시 베팅금이 차감됩니다.",
                                "승리 시 베팅금의 2배를 지급합니다."
                        )
                )
        );

        inventory.setItem(
                BET_INFO_SLOT,
                item(
                        Material.GOLD_NUGGET,
                        "현재 베팅: " + holder.bet + "골드",
                        NamedTextColor.GOLD,
                        List.of(
                                "승리 시 지급 예정: "
                                        + (holder.bet * 2L)
                                        + "골드",
                                "경기 시작 시 베팅금이 차감됩니다."
                        )
                )
        );
    }

    private String horseDescription(
            RaceHolder holder,
            Horse horse
    ) {

        if (holder.winner == horse) {
            return "우승!";
        }

        if (holder.winner != null) {
            return "경기 종료";
        }

        if (holder.selected == horse) {
            return "선택한 경주마";
        }

        if (holder.running) {
            return "경주 진행 중";
        }

        return "클릭하여 선택";
    }

    private String horseName(Horse horse) {

        return horse == Horse.GOLD
                ? "금색 말"
                : "다이아몬드 말";
    }

    private ItemStack item(
            Material material,
            String name,
            NamedTextColor nameColor,
            List<String> lore
    ) {

        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();

        if (meta == null) {
            return stack;
        }

        /*
         * 기존 NPC GUI의 화살표 모델 재사용
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
                Component.text(name, nameColor)
        );

        meta.lore(
                lore.stream()
                        .map(Component::text)
                        .toList()
        );

        stack.setItemMeta(meta);

        return stack;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (!(event.getView()
                .getTopInventory()
                .getHolder() instanceof RaceHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!player.getUniqueId().equals(holder.owner)) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot < 0 || slot >= 45) {
            return;
        }

        /*
         * 경기 진행 중에는 모든 조작을 차단한다.
         */
        if (holder.running) {
            return;
        }

        /*
         * 경기 종료 후에는 '다시 경주하기'만 허용한다.
         * 말 선택과 베팅 금액은 유지한다.
         */
        if (holder.winner != null) {

            if (slot == START_SLOT) {

                /*
                 * 이전 경기의 보상 지급까지 확인한 후
                 * 다음 경주를 준비한다.
                 */
                if (!betService.preparePlayer(player)) {
                    return;
                }

                holder.winner = null;

                holder.goldPosition = GOLD_START;
                holder.diamondPosition = DIAMOND_START;

                render(holder);

                player.sendMessage(
                        "§6[경마] §f다음 경주를 준비했습니다."
                );
            }

            return;
        }

        /*
         * 같은 플레이어가 여러 GUI를 열어 둔 경우에도
         * 진행 중인 경기를 중복 시작하지 않는다.
         */
        if (activeRaces.containsKey(holder.owner)) {
            player.sendMessage(
                    "§e이미 진행 중인 경마가 있습니다."
            );
            return;
        }

        switch (slot) {

            case GOLD_START -> {
                holder.selected = Horse.GOLD;
                render(holder);
            }

            case DIAMOND_START -> {
                holder.selected = Horse.DIAMOND;
                render(holder);
            }

            case DECREASE_SLOT -> {
                holder.bet = Math.max(
                        MIN_BET,
                        holder.bet - BET_STEP
                );
                render(holder);
            }

            case INCREASE_SLOT -> {

                if (holder.bet >
                        Long.MAX_VALUE / 2L - BET_STEP) {

                    player.sendMessage(
                            "§c베팅 금액을 더 올릴 수 없습니다."
                    );
                    return;
                }

                holder.bet += BET_STEP;
                render(holder);
            }

            case START_SLOT -> {

                if (holder.selected == null) {
                    player.sendMessage(
                            "§e먼저 경주마를 선택해 주세요."
                    );
                    return;
                }

                startRace(holder, player);
            }

            default -> {
                // 경주로 및 안내 아이콘 조작 차단
            }
        }
    }

    private void startRace(
            RaceHolder holder,
            Player player
    ) {

        if (holder.running
                || activeRaces.containsKey(holder.owner)) {
            return;
        }

        long raceId = betService.placeBet(
                player,
                holder.selected.name(),
                holder.bet
        );

        if (raceId <= 0L) {
            return;
        }

        holder.raceId = raceId;

        holder.goldPosition = GOLD_START;
        holder.diamondPosition = DIAMOND_START;

        holder.winner = null;
        holder.running = true;

        activeRaces.put(holder.owner, holder);

        player.sendMessage(
                "§6[경마] §f경주가 시작되었습니다!"
        );

        render(holder);

        /*
         * 10틱마다 각 말이 독립적으로 이동을 시도한다.
         * 이동 순서도 매번 섞어서 같은 틱 도착 시
         * 한 말만 일방적으로 유리해지지 않게 한다.
         */
        new BukkitRunnable() {

            @Override
            public void run() {

                if (!holder.running) {
                    cancel();
                    return;
                }

                ThreadLocalRandom random =
                        ThreadLocalRandom.current();

                boolean goldFirst =
                        random.nextBoolean();

                if (goldFirst) {

                    moveHorse(
                            holder,
                            Horse.GOLD,
                            random
                    );

                    if (holder.winner == null) {
                        moveHorse(
                                holder,
                                Horse.DIAMOND,
                                random
                        );
                    }

                } else {

                    moveHorse(
                            holder,
                            Horse.DIAMOND,
                            random
                    );

                    if (holder.winner == null) {
                        moveHorse(
                                holder,
                                Horse.GOLD,
                                random
                        );
                    }
                }

                if (holder.winner != null) {

                    boolean recorded =
                            betService.recordFinish(
                                    holder.raceId,
                                    holder.winner.name()
                            );

                    Player settlementPlayer =
                            Bukkit.getPlayer(holder.owner);

                    if (!recorded) {

                        if (settlementPlayer != null) {
                            settlementPlayer.sendMessage(
                                    "§c[경마] 경기 정산 확인이 필요합니다. "
                                            + "경기 번호: "
                                            + holder.raceId
                            );
                        }

                    } else if (holder.selected == holder.winner
                            && settlementPlayer != null) {

                        if (!betService.settlePayout(
                                settlementPlayer,
                                holder.raceId
                        )) {

                            settlementPlayer.sendMessage(
                                    "§c[경마] 우승 보상 지급 확인이 필요합니다. "
                                            + "경기 번호: "
                                            + holder.raceId
                            );
                        }
                    }

                    holder.running = false;

                    /*
                     * 경기 종료 상태를 먼저 반영해야
                     * '다시 경주하기' 버튼이 표시된다.
                     */
                    render(holder);

                    activeRaces.remove(
                            holder.owner,
                            holder
                    );

                    Player owner =
                            Bukkit.getPlayer(holder.owner);

                    if (owner != null) {

                        owner.sendMessage(
                                "§6[경마] §f우승: "
                                        + horseName(
                                                holder.winner
                                        )
                        );

                        if (holder.selected
                                == holder.winner) {

                            owner.sendMessage(
                                    "§a[경마] 선택한 말이 우승했습니다!"
                            );

                        } else {

                            owner.sendMessage(
                                    "§c[경마] 선택한 말이 패배했습니다."
                            );
                        }

                    }

                    cancel();

                } else {

                    render(holder);
                }
            }

        }.runTaskTimer(
                plugin,
                10L,
                10L
        );
    }


    /**
     * 데이터베이스 연결이 닫히기 전에 호출한다.
     *
     * 정상 종료 당시 진행 중인 경기만 환불 대기로 기록한다.
     * 실제 골드 환불은 플레이어가 다음에 접속할 때 처리한다.
     */
    public void shutdown() {

        for (RaceHolder holder :
                new java.util.ArrayList<>(
                        activeRaces.values()
                )) {

            if (!holder.running || holder.raceId <= 0L) {
                continue;
            }

            holder.running = false;

            if (!betService.markRefundPendingForShutdown(
                    holder.raceId
            )) {

                plugin.getLogger().severe(
                        "Horse race shutdown needs manual review: "
                                + "race="
                                + holder.raceId
                                + " player="
                                + holder.owner
                );
            }
        }

        activeRaces.clear();
    }

    private void moveHorse(
            RaceHolder holder,
            Horse horse,
            ThreadLocalRandom random
    ) {

        /*
         * 각 말의 이동 확률은 매번 달라진다.
         * 한 번 빠르던 말도 다음에는 느려질 수 있다.
         */
        /*
         * 두 말의 위치에 따라 이동 확률을 조정한다.
         * 뒤처진 말은 가끔 가속하고, 앞선 말은 잠시 주춤한다.
         *
         * 같은 위치:       40~70%
         * 한 칸 뒤처짐:    55~85%
         * 두 칸 이상 뒤처짐: 65~90%
         * 앞서가는 말:     35~60%
         */
        int goldProgress =
                holder.goldPosition - GOLD_START;

        int diamondProgress =
                holder.diamondPosition - DIAMOND_START;

        int myProgress =
                horse == Horse.GOLD
                        ? goldProgress
                        : diamondProgress;

        int otherProgress =
                horse == Horse.GOLD
                        ? diamondProgress
                        : goldProgress;

        int difference =
                myProgress - otherProgress;

        double moveChance;

        if (difference <= -2) {

            moveChance =
                    0.65D + random.nextDouble() * 0.25D;

        } else if (difference == -1) {

            moveChance =
                    0.55D + random.nextDouble() * 0.30D;

        } else if (difference > 0) {

            moveChance =
                    0.35D + random.nextDouble() * 0.25D;

        } else {

            moveChance =
                    0.40D + random.nextDouble() * 0.30D;
        }

        if (random.nextDouble() >= moveChance) {
            return;
        }

        if (horse == Horse.GOLD) {

            holder.goldPosition = Math.min(
                    GOLD_FINISH,
                    holder.goldPosition + 1
            );

            if (holder.goldPosition == GOLD_FINISH) {
                holder.winner = Horse.GOLD;
            }

        } else {

            holder.diamondPosition = Math.min(
                    DIAMOND_FINISH,
                    holder.diamondPosition + 1
            );

            if (holder.diamondPosition
                    == DIAMOND_FINISH) {

                holder.winner = Horse.DIAMOND;
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(
            org.bukkit.event.player.PlayerJoinEvent event
    ) {
        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> betService.preparePlayer(
                        event.getPlayer()
                ),
                20L
        );
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {

        if (!(event.getView()
                .getTopInventory()
                .getHolder() instanceof RaceHolder)) {
            return;
        }

        boolean touchesTop =
                event.getRawSlots()
                        .stream()
                        .anyMatch(slot -> slot < 45);

        if (touchesTop) {
            event.setCancelled(true);
        }
    }
}
