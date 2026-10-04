package com.hcs.rpgcore.item;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.skill.SkillItemFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.persistence.PersistentDataType;


public final class CustomItemDeathProtectionListener
        implements Listener {


    private final RPGCorePlugin plugin;

    private final SkillItemFactory
            skillItemFactory;

    private final NamespacedKey
            customItemIdKey;


    /*
     * 사망 시 보호한 RPGCore 아이템.
     *
     * 슬롯 위치는 보존하지 않는다.
     * 리스폰 후 일반 addItem()으로 복구한다.
     */
    private final Map<UUID, List<ItemStack>>
            retainedItems =
                    new ConcurrentHashMap<>();


    public CustomItemDeathProtectionListener(
            RPGCorePlugin plugin,
            SkillItemFactory skillItemFactory
    ) {

        this.plugin =
                plugin;

        this.skillItemFactory =
                skillItemFactory;

        this.customItemIdKey =
                new NamespacedKey(
                        plugin,
                        "custom_item_id"
                );
    }


    /*
     * =========================================================
     * DEATH PROTECTION
     * =========================================================
     */
    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onDeath(
            PlayerDeathEvent event
    ) {

        Player player =
                event.getEntity();

        UUID uuid =
                player.getUniqueId();


        /*
         * keepInventory=true 상태에서는
         * Minecraft가 이미 모든 아이템을 보존하므로
         * 추가 복구하면 복제가 발생한다.
         */
        if (event.getKeepInventory()) {

            retainedItems.remove(
                    uuid
            );

            return;
        }


        List<ItemStack> restore =
                new ArrayList<>();


        event.getDrops()
                .removeIf(
                        item -> {

                            /*
                             * 기존 SkillItemProtectionListener가
                             * 스킬북을 별도로 관리하므로 제외한다.
                             */
                            if (
                                    skillItemFactory
                                            .isSkillItem(
                                                    item
                                            )
                            ) {
                                return false;
                            }


                            if (!isRpgCustomItem(item)) {
                                return false;
                            }


                            restore.add(
                                    item.clone()
                            );

                            return true;
                        }
                );


        if (restore.isEmpty()) {

            retainedItems.remove(
                    uuid
            );

            return;
        }


        retainedItems.put(
                uuid,
                restore
        );
    }


    /*
     * =========================================================
     * RESPAWN RESTORE
     * =========================================================
     */
    @EventHandler
    public void onRespawn(
            PlayerRespawnEvent event
    ) {

        UUID uuid =
                event.getPlayer()
                        .getUniqueId();

        List<ItemStack> restore =
                retainedItems.remove(
                        uuid
                );


        if (
                restore == null
                        ||
                restore.isEmpty()
        ) {
            return;
        }


        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () ->
                                restoreItems(
                                        uuid,
                                        restore
                                )
                );
    }


    private void restoreItems(
            UUID uuid,
            List<ItemStack> items
    ) {

        Player player =
                Bukkit.getPlayer(
                        uuid
                );


        if (
                player == null
                        ||
                !player.isOnline()
        ) {

            /*
             * 아직 복구하지 못했으면
             * 목록을 다시 유지한다.
             */
            retainedItems.put(
                    uuid,
                    items
            );

            return;
        }


        for (
                ItemStack item
                : items
        ) {

            /*
             * 바닐라 아이템은 사망 시 모두 드롭되므로
             * 일반적으로 리스폰 인벤토리는 충분히 비어 있다.
             */
            player.getInventory()
                    .addItem(
                            item
                    );
        }
    }


    /*
     * =========================================================
     * RPG CUSTOM ITEM CHECK
     * =========================================================
     */
    private boolean isRpgCustomItem(
            ItemStack item
    ) {

        if (
                item == null
                        ||
                item.getType().isAir()
        ) {
            return false;
        }


        ItemMeta meta =
                item.getItemMeta();


        if (meta == null) {
            return false;
        }


        String itemId =
                meta.getPersistentDataContainer()
                        .get(
                                customItemIdKey,
                                PersistentDataType.STRING
                        );


        return itemId != null
                && !itemId.isBlank();
    }
}
