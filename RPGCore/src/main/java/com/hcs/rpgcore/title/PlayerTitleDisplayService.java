package com.hcs.rpgcore.title;

import io.papermc.paper.event.player.AsyncChatEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PlayerTitleDisplayService implements Listener {

    public record CachedPlayer(
            String displayName,
            PlayerTitleRepository.PlayerTitle title
    ) {}

    private final ConcurrentMap<UUID, CachedPlayer> players =
            new ConcurrentHashMap<>();

    /*
     * 운영자가 강제 적용한 특수 칭호.
     * 일반 칭호 캐시와 분리하여 기존 장착 상태를 보존한다.
     */
    private final ConcurrentMap<
            UUID,
            PlayerTitleRepository.PlayerTitle
    > specialTitles = new ConcurrentHashMap<>();

    public void setSpecialTitle(
            UUID uuid,
            PlayerTitleRepository.PlayerTitle title
    ) {
        if (uuid == null || title == null) {
            return;
        }

        specialTitles.put(uuid, title);
    }

    public void clearSpecialTitle(UUID uuid) {
        if (uuid != null) {
            specialTitles.remove(uuid);
        }
    }

    public boolean hasSpecialTitle(UUID uuid) {
        return uuid != null && specialTitles.containsKey(uuid);
    }

    /**
     * 로그인 시 DB 조회를 마친 뒤 메인 스레드에서 호출한다.
     */
    public void setLoaded(
            UUID uuid,
            String displayName,
            PlayerTitleRepository.PlayerTitle title
    ) {
        if (uuid == null) {
            return;
        }

        String safeName = displayName == null
                || displayName.isBlank()
                ? ""
                : displayName;

        players.put(
                uuid,
                new CachedPlayer(safeName, title)
        );
    }

    public PlayerTitleRepository.PlayerTitle getTitle(
            UUID uuid
    ) {
        PlayerTitleRepository.PlayerTitle special =
                specialTitles.get(uuid);

        if (special != null) {
            return special;
        }

        CachedPlayer cached = players.get(uuid);

        return cached == null
                ? null
                : cached.title();
    }

    /**
     * 관리자 명령어로 칭호를 설정한 직후 호출한다.
     */
    public void updateTitle(
            UUID uuid,
            PlayerTitleRepository.PlayerTitle title
    ) {
        players.computeIfPresent(
                uuid,
                (key, previous) -> new CachedPlayer(
                        previous.displayName(),
                        title
                )
        );
    }

    /**
     * 관리자 명령어로 칭호를 제거한 직후 호출한다.
     */
    public void clearTitle(UUID uuid) {
        players.computeIfPresent(
                uuid,
                (key, previous) -> new CachedPlayer(
                        previous.displayName(),
                        null
                )
        );
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onChat(AsyncChatEvent event) {

        event.renderer(
                (source, sourceDisplayName, message, viewer) -> {

                    CachedPlayer cached =
                            players.get(source.getUniqueId());

                    Component name;

                    if (cached == null
                            || cached.displayName().isBlank()) {
                        name = sourceDisplayName.color(
                                NamedTextColor.WHITE
                        );
                    } else {
                        name = Component.text(
                                cached.displayName(),
                                NamedTextColor.WHITE
                        );
                    }

                    Component prefix = Component.empty();

                    PlayerTitleRepository.PlayerTitle title =
                            getTitle(source.getUniqueId());

                    if (title != null) {

                        TextColor color =
                                TextColor.fromHexString(
                                        title.titleColor()
                                );

                        if (color == null) {
                            color = NamedTextColor.WHITE;
                        }

                        prefix = Component.text(
                                        "[" + title.titleText() + "]",
                                        color
                                )
                                .append(Component.space());
                    }

                    return prefix
                            .append(name)
                            .append(
                                    Component.text(
                                            ": ",
                                            NamedTextColor.WHITE
                                    )
                            )
                            .append(
                                    message.color(
                                            NamedTextColor.WHITE
                                    )
                            );
                }
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        players.remove(uuid);
        specialTitles.remove(uuid);
    }
}
