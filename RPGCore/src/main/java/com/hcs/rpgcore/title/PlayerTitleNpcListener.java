package com.hcs.rpgcore.title;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.player.PlayerNameTagService;

import net.citizensnpcs.api.event.NPCRightClickEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

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

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class PlayerTitleNpcListener implements Listener {

    /*
     * =========================================================
     * NPC / GUI
     * =========================================================
     */

    private static final int NPC_ID = 26;

    private static final int INVENTORY_SIZE = 27;
    private static final int PAGE_SIZE = 18;

    private static final int PREVIOUS_SLOT = 18;
    private static final int UNEQUIP_SLOT = 22;
    private static final int NEXT_SLOT = 26;

    /*
     * 기존 리소스팩 버튼 모델.
     */
    private static final int PREVIOUS_MODEL = 1002;
    private static final int RECEIVE_MODEL = 1005;
    private static final int NEXT_MODEL = 1000;

    /*
     * 반드시 NPC 25번의 버튼 재질과 같아야 한다.
     * NPC 25번이 PAPER 이외의 재질을 사용한다면 이 상수를 변경한다.
     */
    private static final Material BUTTON_MATERIAL =
            Material.ARROW;

    /*
     * =========================================================
     * SERVICES
     * =========================================================
     */

    private final RPGCorePlugin plugin;

    private final PlayerTitleCollectionRepository repository;

    private final SpecialTitleRepository specialTitleRepository;

    private final PlayerTitleDisplayService titleDisplayService;

    private final PlayerNameTagService nameTagService;

    /*
     * 플레이어별 DB 중복 요청 방지.
     */
    private final Set<UUID> busy =
            ConcurrentHashMap.newKeySet();

    /*
     * =========================================================
     * INVENTORY HOLDER
     * =========================================================
     */

    private static final class MenuHolder
            implements InventoryHolder {

        private final UUID owner;

        private final List<
                PlayerTitleCollectionRepository.Title
                > titles;

        private Inventory inventory;

        private int page;

        private String equippedTitleId;

        private MenuHolder(
                UUID owner,
                List<PlayerTitleCollectionRepository.Title> titles,
                String equippedTitleId
        ) {

            this.owner = owner;

            this.titles = List.copyOf(titles);

            this.equippedTitleId = equippedTitleId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    /*
     * =========================================================
     * CONSTRUCTOR
     * =========================================================
     */

    public PlayerTitleNpcListener(
            RPGCorePlugin plugin,
            PlayerTitleCollectionRepository repository,
            PlayerTitleDisplayService titleDisplayService,
            PlayerNameTagService nameTagService
    ) {

        this.plugin = plugin;

        this.repository = repository;

        this.specialTitleRepository =
                new SpecialTitleRepository(
                        plugin.getDatabaseManager()
                );

        this.titleDisplayService = titleDisplayService;

        this.nameTagService = nameTagService;
    }

    /*
     * =========================================================
     * NPC 26 RIGHT CLICK
     * =========================================================
     */

    @EventHandler
    public void onNpcRightClick(
            NPCRightClickEvent event
    ) {

        if (event.getNPC().getId() != NPC_ID) {
            return;
        }

        Player player = event.getClicker();

        UUID uuid = player.getUniqueId();

        if (!busy.add(uuid)) {

            player.sendMessage(
                    Component.text(
                            "칭호 데이터를 처리 중입니다. 잠시 기다려 주세요.",
                            NamedTextColor.YELLOW
                    )
            );

            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(
                plugin,
                () -> loadMenu(player, uuid)
        );
    }

    /*
     * =========================================================
     * LOAD MENU
     * =========================================================
     */

    private void loadMenu(
            Player player,
            UUID uuid
    ) {

        final List<
                PlayerTitleCollectionRepository.Title
                > titles;

        final PlayerTitleCollectionRepository.Title equipped;

        try {

            titles = repository.findUnlockedTitles(uuid);

            equipped = repository.findEquippedTitle(uuid);

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "[PlayerTitle] 칭호 목록 조회 실패: " + uuid,
                    exception
            );

            Bukkit.getScheduler().runTask(
                    plugin,
                    () -> {

                        busy.remove(uuid);

                        if (isSameOnlinePlayer(player, uuid)) {

                            player.sendMessage(
                                    Component.text(
                                            "칭호 목록을 불러오지 못했습니다.",
                                            NamedTextColor.RED
                                    )
                            );
                        }
                    }
            );

            return;
        }

        Bukkit.getScheduler().runTask(
                plugin,
                () -> {

                    busy.remove(uuid);

                    if (!isSameOnlinePlayer(player, uuid)) {
                        return;
                    }

                    MenuHolder holder = new MenuHolder(
                            uuid,
                            titles,
                            equipped == null
                                    ? null
                                    : equipped.titleId()
                    );

                    Inventory inventory =
                            Bukkit.createInventory(
                                    holder,
                                    INVENTORY_SIZE,
                                    Component.text("칭호 관리")
                            );

                    holder.inventory = inventory;

                    render(holder);

                    player.openInventory(inventory);
                }
        );
    }

    /*
     * =========================================================
     * RENDER GUI
     * =========================================================
     */

    private void render(
            MenuHolder holder
    ) {

        Inventory inventory = holder.inventory;

        inventory.clear();

        int pages = pageCount(holder);

        holder.page = Math.max(
                0,
                Math.min(
                        holder.page,
                        pages - 1
                )
        );

        int start = holder.page * PAGE_SIZE;

        int end = Math.min(
                start + PAGE_SIZE,
                holder.titles.size()
        );

        /*
         * 획득 칭호 목록: 0~17번 슬롯.
         */
        for (
                int index = start;
                index < end;
                index++
        ) {

            PlayerTitleCollectionRepository.Title title =
                    holder.titles.get(index);

            boolean equipped =
                    title.titleId().equals(
                            holder.equippedTitleId
                    );

            inventory.setItem(
                    index - start,
                    titleItem(
                            title,
                            equipped
                    )
            );
        }

        /*
         * 획득한 칭호가 없는 경우.
         */
        if (holder.titles.isEmpty()) {

            inventory.setItem(
                    13,
                    item(
                            Material.NAME_TAG,
                            "획득한 칭호가 없습니다.",
                            NamedTextColor.GRAY,
                            "칭호 획득 조건을 달성해 보세요."
                    )
            );
        }

        /*
         * 왼쪽 화살표.
         */
        inventory.setItem(
                PREVIOUS_SLOT,
                buttonItem(
                        "이전 페이지",
                        PREVIOUS_MODEL,
                        holder.page > 0
                                ? "이전 칭호 목록으로 이동합니다."
                                : "첫 번째 페이지입니다."
                )
        );

        /*
         * 가운데 받기 아이콘.
         *
         * 현재 기능은 칭호 장착 해제다.
         * 획득 기록은 삭제하지 않는다.
         */
        inventory.setItem(
                UNEQUIP_SLOT,
                buttonItem(
                        "칭호 장착 해제",
                        RECEIVE_MODEL,
                        holder.equippedTitleId == null
                                ? "현재 장착한 칭호가 없습니다."
                                : "현재 칭호의 장착을 해제합니다."
                )
        );

        /*
         * 오른쪽 화살표.
         */
        inventory.setItem(
                NEXT_SLOT,
                buttonItem(
                        "다음 페이지",
                        NEXT_MODEL,
                        holder.page + 1 < pages
                                ? "다음 칭호 목록으로 이동합니다."
                                : "마지막 페이지입니다."
                )
        );
    }

    /*
     * =========================================================
     * TITLE ITEM
     * =========================================================
     */

    private ItemStack titleItem(
            PlayerTitleCollectionRepository.Title title,
            boolean equipped
    ) {

        TextColor color =
                TextColor.fromHexString(
                        title.titleColor()
                );

        if (color == null) {
            color = NamedTextColor.WHITE;
        }

        ItemStack stack = new ItemStack(
                equipped
                        ? Material.ENCHANTED_BOOK
                        : Material.NAME_TAG
        );

        ItemMeta meta = stack.getItemMeta();

        if (meta == null) {
            return stack;
        }

        meta.displayName(
                Component.text(
                        "[ " + title.titleText() + " ]",
                        color
                )
        );

        meta.lore(
                List.of(
                        Component.text(
                                equipped
                                        ? "현재 장착 중"
                                        : "클릭하여 장착",
                                equipped
                                        ? NamedTextColor.GREEN
                                        : NamedTextColor.GRAY
                        )
                )
        );

        stack.setItemMeta(meta);

        return stack;
    }

    /*
     * =========================================================
     * CUSTOM MODEL BUTTON
     * =========================================================
     */

    private ItemStack buttonItem(
            String name,
            int modelData,
            String description
    ) {

        ItemStack stack = new ItemStack(
                BUTTON_MATERIAL
        );

        ItemMeta meta = stack.getItemMeta();

        if (meta == null) {
            return stack;
        }

        meta.displayName(
                Component.text(
                        name,
                        NamedTextColor.YELLOW
                )
        );

        meta.lore(
                List.of(
                        Component.text(
                                description,
                                NamedTextColor.GRAY
                        )
                )
        );

        meta.setCustomModelData(modelData);

        stack.setItemMeta(meta);

        return stack;
    }

    /*
     * =========================================================
     * NORMAL ITEM
     * =========================================================
     */

    private ItemStack item(
            Material material,
            String name,
            TextColor color,
            String description
    ) {

        ItemStack stack = new ItemStack(material);

        ItemMeta meta = stack.getItemMeta();

        if (meta == null) {
            return stack;
        }

        meta.displayName(
                Component.text(
                        name,
                        color
                )
        );

        meta.lore(
                List.of(
                        Component.text(
                                description,
                                NamedTextColor.GRAY
                        )
                )
        );

        stack.setItemMeta(meta);

        return stack;
    }

    /*
     * =========================================================
     * PAGE COUNT
     * =========================================================
     */

    private int pageCount(
            MenuHolder holder
    ) {

        return Math.max(
                1,
                (
                        holder.titles.size()
                                + PAGE_SIZE
                                - 1
                ) / PAGE_SIZE
        );
    }

    /*
     * =========================================================
     * INVENTORY CLICK
     * =========================================================
     */

    @EventHandler
    public void onClick(
            InventoryClickEvent event
    ) {

        if (!(event.getView()
                .getTopInventory()
                .getHolder() instanceof MenuHolder holder)) {

            return;
        }

        /*
         * Shift 클릭, 숫자키 등으로
         * GUI 아이템을 이동할 수 없도록 차단.
         */
        event.setCancelled(true);

        if (!(event.getWhoClicked()
                instanceof Player player)) {

            return;
        }

        UUID uuid = player.getUniqueId();

        if (!holder.owner.equals(uuid)) {
            return;
        }

        int slot = event.getRawSlot();

        if (
                slot < 0
                        || slot >= INVENTORY_SIZE
        ) {
            return;
        }

        if (busy.contains(uuid)) {

            player.sendMessage(
                    Component.text(
                            "칭호 변경을 처리 중입니다.",
                            NamedTextColor.YELLOW
                    )
            );

            return;
        }

        /*
         * 칭호 목록 클릭.
         * 클릭한 칭호를 즉시 장착.
         */
        if (slot < PAGE_SIZE) {

            int index =
                    holder.page * PAGE_SIZE + slot;

            if (index >= holder.titles.size()) {
                return;
            }

            PlayerTitleCollectionRepository.Title title =
                    holder.titles.get(index);

            if (
                    title.titleId().equals(
                            holder.equippedTitleId
                    )
            ) {

                player.sendMessage(
                        Component.text(
                                "이미 장착 중인 칭호입니다.",
                                NamedTextColor.YELLOW
                        )
                );

                return;
            }

            changeTitle(
                    player,
                    holder,
                    title.titleId(),
                    false
            );

            return;
        }

        /*
         * 이전 페이지.
         */
        if (slot == PREVIOUS_SLOT) {

            if (holder.page > 0) {

                holder.page--;

                render(holder);
            }

            return;
        }

        /*
         * 다음 페이지.
         */
        if (slot == NEXT_SLOT) {

            if (
                    holder.page + 1
                            < pageCount(holder)
            ) {

                holder.page++;

                render(holder);
            }

            return;
        }

        /*
         * 장착 해제.
         */
        if (slot == UNEQUIP_SLOT) {

            if (holder.equippedTitleId == null) {

                player.sendMessage(
                        Component.text(
                                "현재 장착한 칭호가 없습니다.",
                                NamedTextColor.YELLOW
                        )
                );

                return;
            }

            changeTitle(
                    player,
                    holder,
                    null,
                    true
            );
        }
    }

    /*
     * =========================================================
     * EQUIP / UNEQUIP
     * =========================================================
     */

    private void changeTitle(
            Player player,
            MenuHolder holder,
            String titleId,
            boolean unequip
    ) {

        UUID uuid = player.getUniqueId();

        /*
         * 표시 캐시 기준으로 우선 차단한다.
         * 실제 DB 상태는 아래 비동기 처리에서 재확인한다.
         */
        if (titleDisplayService.hasSpecialTitle(uuid)) {
            player.sendMessage(
                    Component.text(
                            "특수 칭호가 적용 중이므로 "
                                    + "일반 칭호를 변경할 수 없습니다.",
                            NamedTextColor.RED
                    )
            );
            return;
        }

        if (!busy.add(uuid)) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(
                plugin,
                () -> {

                    final boolean success;

                    final PlayerTitleCollectionRepository.Title
                            equipped;

                    try {

                        /*
                         * DB에 강제 특수 칭호가 있으면
                         * 일반 칭호 장착·해제를 실행하지 않는다.
                         */
                        if (specialTitleRepository.findActive(uuid) != null) {

                            Bukkit.getScheduler().runTask(
                                    plugin,
                                    () -> {

                                        busy.remove(uuid);

                                        if (isSameOnlinePlayer(player, uuid)) {
                                            player.sendMessage(
                                                    Component.text(
                                                            "특수 칭호가 적용 중이므로 "
                                                                    + "일반 칭호를 변경할 수 없습니다.",
                                                            NamedTextColor.RED
                                                    )
                                            );
                                        }
                                    }
                            );

                            return;
                        }

                        if (unequip) {

                            repository.unequipTitle(uuid);

                            success = true;

                            equipped = null;

                        } else {

                            success =
                                    repository.equipTitle(
                                            uuid,
                                            titleId
                                    );

                            equipped = success
                                    ? repository.findEquippedTitle(
                                            uuid
                                    )
                                    : null;
                        }

                    } catch (SQLException exception) {

                        plugin.getLogger().log(
                                Level.SEVERE,
                                "[PlayerTitle] 칭호 변경 실패: "
                                        + uuid,
                                exception
                        );

                        Bukkit.getScheduler().runTask(
                                plugin,
                                () -> {

                                    busy.remove(uuid);

                                    if (
                                            isSameOnlinePlayer(
                                                    player,
                                                    uuid
                                            )
                                    ) {

                                        player.sendMessage(
                                                Component.text(
                                                        "칭호 변경에 실패했습니다.",
                                                        NamedTextColor.RED
                                                )
                                        );
                                    }
                                }
                        );

                        return;
                    }

                    Bukkit.getScheduler().runTask(
                            plugin,
                            () -> {

                                busy.remove(uuid);

                                if (
                                        !isSameOnlinePlayer(
                                                player,
                                                uuid
                                        )
                                ) {
                                    return;
                                }

                                if (!success) {

                                    player.sendMessage(
                                            Component.text(
                                                    "획득하지 않은 칭호는 장착할 수 없습니다.",
                                                    NamedTextColor.RED
                                            )
                                    );

                                    return;
                                }

                                if (equipped == null) {

                                    titleDisplayService.clearTitle(
                                            uuid
                                    );

                                    holder.equippedTitleId = null;

                                    player.sendMessage(
                                            Component.text(
                                                    "칭호 장착을 해제했습니다.",
                                                    NamedTextColor.GREEN
                                            )
                                    );

                                } else {

                                    titleDisplayService.updateTitle(
                                            uuid,
                                            new PlayerTitleRepository.PlayerTitle(
                                                    uuid,
                                                    equipped.titleText(),
                                                    equipped.titleColor()
                                            )
                                    );

                                    holder.equippedTitleId =
                                            equipped.titleId();

                                    player.sendMessage(
                                            Component.text(
                                                    "["
                                                            + equipped.titleText()
                                                            + "] 칭호를 장착했습니다.",
                                                    NamedTextColor.GREEN
                                            )
                                    );
                                }

                                /*
                                 * 기존 DB display_name이 적용된
                                 * Player.displayName()을 사용한다.
                                 */
                                String displayName =
                                        PlainTextComponentSerializer
                                                .plainText()
                                                .serialize(
                                                        player.displayName()
                                                );

                                nameTagService.apply(
                                        player,
                                        displayName
                                );

                                /*
                                 * 같은 GUI가 열려 있을 때만
                                 * 장착 상태를 다시 렌더링한다.
                                 */
                                if (
                                        player.getOpenInventory()
                                                .getTopInventory()
                                                == holder.inventory
                                ) {

                                    render(holder);
                                }
                            }
                    );
                }
        );
    }

    /*
     * =========================================================
     * INVENTORY DRAG
     * =========================================================
     */

    @EventHandler
    public void onDrag(
            InventoryDragEvent event
    ) {

        if (
                event.getView()
                        .getTopInventory()
                        .getHolder() instanceof MenuHolder
        ) {

            event.setCancelled(true);
        }
    }

    /*
     * =========================================================
     * PLAYER CHECK
     * =========================================================
     */

    private boolean isSameOnlinePlayer(
            Player player,
            UUID uuid
    ) {

        return player.isOnline()
                && Bukkit.getPlayer(uuid) == player;
    }
}
