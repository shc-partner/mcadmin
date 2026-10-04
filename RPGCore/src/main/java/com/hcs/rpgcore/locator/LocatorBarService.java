package com.hcs.rpgcore.locator;

import com.hcs.rpgcore.RPGCorePlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

public final class LocatorBarService {

    private static final String DUMMY_TAG =
            "rpgcore_locator_dummy";

    private final RPGCorePlugin plugin;

    private final Map<UUID, UUID> dummyByPlayer =
            new HashMap<>();

    public LocatorBarService(
            RPGCorePlugin plugin
    ) {
        this.plugin = plugin;
    }


    public void ensureDummy(
            Player player
    ) {

        UUID existingUuid =
                this.dummyByPlayer.get(
                        player.getUniqueId()
                );

        if (existingUuid != null) {

            org.bukkit.entity.Entity existing =
                    plugin.getServer()
                            .getEntity(existingUuid);

            if (
                    existing instanceof ArmorStand
                            && existing.isValid()
            ) {

                moveDummy(
                        (ArmorStand) existing,
                        player
                );

                return;
            }
        }


        Location location =
                getDummyLocation(player);

        ArmorStand armorStand =
                player.getWorld()
                        .spawn(
                                location,
                                ArmorStand.class,
                                stand -> {

                                    stand.setInvisible(true);
                                    stand.setMarker(true);
                                    stand.setGravity(false);
                                    stand.setInvulnerable(true);
                                    stand.setSilent(true);
                                    stand.setPersistent(false);
                                    stand.setCustomNameVisible(false);

                                    /*
                                     * Locator Bar 유지용 dummy는
                                     * 리소스팩의 완전 투명 waypoint style을 사용한다.
                                     */
                                    stand.setWaypointStyle(
                                            Key.key("minecraft", "bowtie")
                                    );

                                    stand.addScoreboardTag(
                                            DUMMY_TAG
                                    );
                                }
                        );


        AttributeInstance transmit =
                armorStand.getAttribute(
                        Attribute.WAYPOINT_TRANSMIT_RANGE
                );

        if (transmit == null) {

            armorStand.registerAttribute(
                    Attribute.WAYPOINT_TRANSMIT_RANGE
            );

            transmit =
                    armorStand.getAttribute(
                            Attribute.WAYPOINT_TRANSMIT_RANGE
                    );
        }

        if (transmit != null) {

            transmit.setBaseValue(
                    60000000.0D
            );
        }


        this.dummyByPlayer.put(
                player.getUniqueId(),
                armorStand.getUniqueId()
        );
    }


    public void removeDummy(
            Player player
    ) {

        UUID entityUuid =
                this.dummyByPlayer.remove(
                        player.getUniqueId()
                );

        if (entityUuid == null) {
            return;
        }

        org.bukkit.entity.Entity entity =
                plugin.getServer()
                        .getEntity(entityUuid);

        if (entity != null) {
            entity.remove();
        }
    }


    public void moveDummy(
            Player player
    ) {

        UUID entityUuid =
                this.dummyByPlayer.get(
                        player.getUniqueId()
                );

        if (entityUuid == null) {

            ensureDummy(player);

            return;
        }

        org.bukkit.entity.Entity entity =
                plugin.getServer()
                        .getEntity(entityUuid);

        if (
                !(entity instanceof ArmorStand)
                        || !entity.isValid()
        ) {

            this.dummyByPlayer.remove(
                    player.getUniqueId()
            );

            ensureDummy(player);

            return;
        }

        moveDummy(
                (ArmorStand) entity,
                player
        );
    }


    private void moveDummy(
            ArmorStand armorStand,
            Player player
    ) {

        if (
                !armorStand.getWorld()
                        .equals(player.getWorld())
        ) {

            armorStand.remove();

            this.dummyByPlayer.remove(
                    player.getUniqueId()
            );

            ensureDummy(player);

            return;
        }

        armorStand.teleport(
                getDummyLocation(player)
        );
    }


    private Location getDummyLocation(
            Player player
    ) {

        /*
         * 플레이어 바로 뒤/아래쪽에 유지한다.
         *
         * 1차 목적:
         * - 다른 플레이어가 없어도 Locator Bar가
         *   활성 상태로 유지되는지 검증.
         */
        Location location =
                player.getLocation().clone();

        location.add(
                0.0D,
                -4.0D,
                0.0D
        );

        return location;
    }


    public void removeAll() {

        for (
                UUID uuid :
                this.dummyByPlayer.values()
        ) {

            org.bukkit.entity.Entity entity =
                    plugin.getServer()
                            .getEntity(uuid);

            if (entity != null) {
                entity.remove();
            }
        }

        this.dummyByPlayer.clear();
    }
}
