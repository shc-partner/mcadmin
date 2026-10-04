package com.hcs.rpgcore.title;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.player.PlayerNameTagService;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * /칭호 <표시이름> <기존 칭호 이름>
 * /칭호 <표시이름> 제거
 *
 * 칭호 정의는 이 명령어에서 생성하지 않는다.
 * 지급 시 획득 기록만 추가하고 자동 장착하지 않는다.
 */
public final class PlayerTitleCommand implements CommandExecutor {

    private final RPGCorePlugin plugin;
    private final PlayerTitleRepository playerRepository;
    private final PlayerTitleCollectionRepository collectionRepository;
    private final SpecialTitleRepository specialTitleRepository;
    private final PlayerNameTagService nameTagService;
    private final PlayerTitleDisplayService titleDisplayService;

    public PlayerTitleCommand(
            RPGCorePlugin plugin,
            PlayerTitleRepository playerRepository,
            PlayerNameTagService nameTagService,
            PlayerTitleDisplayService titleDisplayService
    ) {
        this.plugin = plugin;
        this.playerRepository = playerRepository;
        this.collectionRepository =
                new PlayerTitleCollectionRepository(
                        plugin.getDatabaseManager()
                );
        this.specialTitleRepository =
                new SpecialTitleRepository(
                        plugin.getDatabaseManager()
                );
        this.nameTagService = nameTagService;
        this.titleDisplayService = titleDisplayService;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!sender.hasPermission("rpgcore.admin")) {
            sender.sendMessage("칭호를 관리할 권한이 없습니다.");
            return true;
        }

        if (args.length != 2 && args.length != 3) {
            sendUsage(sender);
            return true;
        }

        String displayName = args[0];
        String titleText = args[1];
        String specialColor = null;

        if (displayName.isBlank() || titleText.isBlank()) {
            sendUsage(sender);
            return true;
        }

        if (args.length == 3) {

            if ("빨강".equals(args[2])) {
                specialColor = "#c40000";
            } else if ("파랑".equals(args[2])) {
                specialColor = "#0041d4";
            } else {
                sender.sendMessage(
                        "특수 칭호 색상은 빨강 또는 파랑만 사용할 수 있습니다."
                );
                return true;
            }

            if ("제거".equals(titleText)
                    || "특수해제".equals(titleText)) {
                sendUsage(sender);
                return true;
            }
        }

        final String requestedColor = specialColor;
        final String assignedBy = sender.getName();

        Bukkit.getScheduler().runTaskAsynchronously(
                plugin,
                () -> processCommand(
                        sender,
                        displayName,
                        titleText,
                        requestedColor,
                        assignedBy
                )
        );

        return true;
    }

    private void processCommand(
            CommandSender sender,
            String displayName,
            String titleText,
            String specialColor,
            String assignedBy
    ) {

        try {

            /*
             * 관리자 입력값은 실제 계정명이 아닌
             * rpg_players.display_name이다.
             */
            UUID uuid =
                    playerRepository.findPlayerUuidByDisplayName(
                            displayName
                    );

            if (uuid == null) {
                reply(
                        sender,
                        "플레이어를 찾지 못했습니다: " + displayName
                );
                return;
            }

            /*
             * 운영자 전용 강제 특수 칭호 지급.
             * 일반 칭호 장착 기록은 변경하지 않는다.
             */
            if (specialColor != null) {

                specialTitleRepository.assign(
                        uuid,
                        titleText,
                        specialColor,
                        assignedBy
                );

                SpecialTitleRepository.SpecialTitle active =
                        specialTitleRepository.findActive(uuid);

                if (active == null) {
                    throw new SQLException(
                            "특수 칭호 저장 후 조회에 실패했습니다."
                    );
                }

                Bukkit.getScheduler().runTask(
                        plugin,
                        () -> {

                            titleDisplayService.setSpecialTitle(
                                    uuid,
                                    new PlayerTitleRepository.PlayerTitle(
                                            uuid,
                                            active.titleText(),
                                            active.titleColor()
                                    )
                            );

                            refreshNameTag(uuid);

                            sender.sendMessage(
                                    displayName
                                            + "에게 ["
                                            + active.titleText()
                                            + "] 특수 칭호를 강제 적용했습니다."
                            );
                        }
                );

                return;
            }

            /*
             * 운영자만 강제 특수 칭호를 해제한다.
             */
            if ("특수해제".equals(titleText)) {

                boolean removed =
                        specialTitleRepository.clear(uuid);

                Bukkit.getScheduler().runTask(
                        plugin,
                        () -> {

                            titleDisplayService.clearSpecialTitle(uuid);
                            refreshNameTag(uuid);

                            sender.sendMessage(
                                    removed
                                            ? displayName
                                                + "의 특수 칭호를 해제했습니다."
                                            : displayName
                                                + "에게 적용된 특수 칭호가 없습니다."
                            );
                        }
                );

                return;
            }

            /*
             * 제거는 획득 기록 삭제가 아니라 장착 해제.
             * 특수 칭호가 활성화된 동안 일반 칭호를 변경하지 않는다.
             */
            if ("제거".equals(titleText)) {

                if (specialTitleRepository.findActive(uuid) != null) {
                    reply(
                            sender,
                            "특수 칭호가 적용 중입니다. "
                                    + "먼저 /칭호 "
                                    + displayName
                                    + " 특수해제를 실행하세요."
                    );
                    return;
                }

                boolean removed =
                        collectionRepository.unequipTitle(uuid);

                Bukkit.getScheduler().runTask(
                        plugin,
                        () -> {

                            titleDisplayService.clearTitle(uuid);
                            refreshNameTag(uuid);

                            sender.sendMessage(
                                    removed
                                            ? displayName
                                                + "의 칭호 장착을 해제했습니다."
                                            : displayName
                                                + "은 현재 장착한 칭호가 없습니다."
                            );
                        }
                );

                return;
            }

            /*
             * DB에 이미 정의된 칭호만 지급한다.
             * 새로운 칭호 정의를 생성하지 않는다.
             */
            PlayerTitleCollectionRepository.Title title =
                    collectionRepository.findDefinitionByText(
                            titleText
                    );

            if (title == null) {
                reply(
                        sender,
                        "정의되지 않은 칭호입니다: " + titleText
                );
                return;
            }

            boolean newlyUnlocked =
                    collectionRepository.unlockTitle(
                            uuid,
                            title.titleId(),
                            "admin"
                    );

            reply(
                    sender,
                    newlyUnlocked
                            ? displayName + "에게 ["
                                + title.titleText()
                                + "] 칭호를 지급했습니다."
                            : displayName + "은 이미 ["
                                + title.titleText()
                                + "] 칭호를 획득했습니다."
            );

            /*
             * 지급만 수행한다.
             * 기존 장착 칭호와 표시 캐시는 변경하지 않는다.
             */

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "[PlayerTitle] 관리자 명령어 처리 실패",
                    exception
            );

            reply(
                    sender,
                    "칭호 처리에 실패했습니다. 서버 로그를 확인하세요."
            );

        } catch (RuntimeException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "[PlayerTitle] 관리자 명령어 오류",
                    exception
            );

            reply(
                    sender,
                    "칭호 처리 중 오류가 발생했습니다."
            );
        }
    }

    private void refreshNameTag(UUID uuid) {

        Player online = Bukkit.getPlayer(uuid);

        if (online == null || !online.isOnline()) {
            return;
        }

        String displayName =
                net.kyori.adventure.text.serializer.plain
                        .PlainTextComponentSerializer
                        .plainText()
                        .serialize(
                                online.displayName()
                        );

        nameTagService.apply(
                online,
                displayName
        );
    }

    private void reply(
            CommandSender sender,
            String message
    ) {

        Bukkit.getScheduler().runTask(
                plugin,
                () -> sender.sendMessage(message)
        );
    }

    private void sendUsage(CommandSender sender) {

        sender.sendMessage(
                "사용법: /칭호 <표시이름> <기존 칭호 이름>"
        );

        sender.sendMessage(
                "장착 해제: /칭호 <표시이름> 제거"
        );

        sender.sendMessage(
                "특수 칭호: /칭호 <표시이름> <칭호명> <빨강|파랑>"
        );

        sender.sendMessage(
                "특수 해제: /칭호 <표시이름> 특수해제"
        );
    }
}
