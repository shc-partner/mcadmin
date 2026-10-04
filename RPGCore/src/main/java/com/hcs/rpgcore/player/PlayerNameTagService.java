package com.hcs.rpgcore.player;

import com.hcs.rpgcore.RPGCorePlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;


public final class PlayerNameTagService {

    private static final String TEAM_NAME =
            "rpg_name_hide";


    private final RPGCorePlugin plugin;

    private final com.hcs.rpgcore.title.PlayerTitleDisplayService
            titleDisplayService;


    private final Map<UUID, TextDisplay> displays =
            new HashMap<>();


    private Team hideTeam;


    public PlayerNameTagService(
            RPGCorePlugin plugin,
            com.hcs.rpgcore.title.PlayerTitleDisplayService titleDisplayService
    ) {

        this.plugin =
                plugin;

        this.titleDisplayService =
                titleDisplayService;

        initializeTeam();

        startUpdateTask();
    }


    /*
     * =========================================================
     * TEAM
     * =========================================================
     */
    private void initializeTeam() {

        Scoreboard scoreboard =
                Bukkit.getScoreboardManager()
                        .getMainScoreboard();


        Team existing =
                scoreboard.getTeam(
                        TEAM_NAME
                );


        if (existing != null) {

            this.hideTeam =
                    existing;

        } else {

            this.hideTeam =
                    scoreboard.registerNewTeam(
                            TEAM_NAME
                    );
        }


        hideTeam.setOption(
                Team.Option.NAME_TAG_VISIBILITY,
                Team.OptionStatus.NEVER
        );
    }


    /*
     * =========================================================
     * APPLY
     * =========================================================
     */
    public void apply(
            Player player,
            String displayName
    ) {

        if (
                player == null
                || !player.isOnline()
        ) {
            return;
        }


        if (
                displayName == null
                || displayName.isBlank()
        ) {

            clear(
                    player
            );

            return;
        }


        /*
         * 바닐라 계정명 네임태그 숨김
         */
        hideTeam.addEntry(
                player.getName()
        );


        TextDisplay display =
                displays.get(
                        player.getUniqueId()
                );


        if (
                display == null
                || !display.isValid()
        ) {

            Location location =
                    getDisplayLocation(
                            player
                    );


            display =
                    player.getWorld()
                            .spawn(
                                    location,
                                    TextDisplay.class
                            );


            display.setPersistent(
                    false
            );

            display.setInvulnerable(
                    true
            );

            display.setBillboard(
                    Display.Billboard.CENTER
            );

            display.setSeeThrough(
                    false
            );

            display.setShadowed(
                    true
            );

            display.setDefaultBackground(
                    false
            );


            displays.put(
                    player.getUniqueId(),
                    display
            );
        }


        /*
         * 칭호가 있으면:
         * [ 운영자 ]
         * 말년용사
         *
         * 칭호가 없으면 기존 표시 이름 한 줄만 사용한다.
         */
        Component nameTag = Component.text(displayName);

        com.hcs.rpgcore.title.PlayerTitleRepository.PlayerTitle title =
                titleDisplayService.getTitle(
                        player.getUniqueId()
                );

        if (title != null) {
            net.kyori.adventure.text.format.TextColor color =
                    net.kyori.adventure.text.format.TextColor.fromHexString(
                            title.titleColor()
                    );

            if (color == null) {
                color = net.kyori.adventure.text.format.NamedTextColor.WHITE;
            }

            nameTag = Component.text(
                            "[" + title.titleText() + "]",
                            color
                    )
                    .append(Component.newline())
                    .append(
                            Component.text(
                                    displayName,
                                    net.kyori.adventure.text.format.NamedTextColor.WHITE
                            )
                    );
        }

        display.setAlignment(
                TextDisplay.TextAlignment.CENTER
        );

        display.text(nameTag);
    }


    /*
     * =========================================================
     * CLEAR
     * =========================================================
     */
    public void clear(
            Player player
    ) {

        if (player == null) {
            return;
        }


        hideTeam.removeEntry(
                player.getName()
        );


        TextDisplay display =
                displays.remove(
                        player.getUniqueId()
                );


        if (
                display != null
                && display.isValid()
        ) {

            display.remove();
        }
    }


    public void removeDisplay(
            UUID uuid
    ) {

        TextDisplay display =
                displays.remove(
                        uuid
                );


        if (
                display != null
                && display.isValid()
        ) {

            display.remove();
        }
    }


    /*
     * =========================================================
     * POSITION
     * =========================================================
     */
    private void startUpdateTask() {

        Bukkit.getScheduler()
                .runTaskTimer(
                        plugin,
                        () -> {

                            for (
                                    Map.Entry<UUID, TextDisplay> entry
                                    : displays.entrySet()
                            ) {

                                Player player =
                                        Bukkit.getPlayer(
                                                entry.getKey()
                                        );


                                TextDisplay display =
                                        entry.getValue();


                                if (
                                        player == null
                                        || !player.isOnline()
                                        || display == null
                                        || !display.isValid()
                                ) {
                                    continue;
                                }


                                Location target =
                                        getDisplayLocation(
                                                player
                                        );


                                display.teleport(
                                        target
                                );
                            }
                        },
                        1L,
                        1L
                );
    }


    private Location getDisplayLocation(
            Player player
    ) {

        return player.getLocation()
                .clone()
                .add(
                        0.0D,
                        player.getHeight()
                                + 0.45D,
                        0.0D
                );
    }


    /*
     * =========================================================
     * SHUTDOWN
     * =========================================================
     */
    public void shutdown() {

        for (
                TextDisplay display
                : displays.values()
        ) {

            if (
                    display != null
                    && display.isValid()
            ) {

                display.remove();
            }
        }


        displays.clear();
    }
}
