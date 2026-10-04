package com.hcs.rpgcore.craft;

import com.hcs.rpgcore.database.DatabaseManager;

import net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot;

import java.sql.SQLException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

/**
 * NPC 18번 장비 미리보기 관리자 테스트 명령어.
 *
 * /rpgpreview single <item_id> <slot>
 * /rpgpreview stop
 */
public final class EquipmentCraftPreviewCommand
        implements CommandExecutor {

    private final EquipmentCraftPreviewService previewService;
    private final EquipmentCraftSetRepository setRepository;

    public EquipmentCraftPreviewCommand(
            EquipmentCraftPreviewService previewService,
            DatabaseManager databaseManager
    ) {
        this.previewService = previewService;
        this.setRepository =
                new EquipmentCraftSetRepository(databaseManager);
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    "이 명령어는 인게임에서만 사용할 수 있습니다."
            );
            return true;
        }

        if (!player.hasPermission("rpgcore.admin")) {
            player.sendMessage("권한이 없습니다.");
            return true;
        }

        if (args.length == 1
                && args[0].equalsIgnoreCase("stop")) {

            previewService.release(player);

            player.sendMessage(
                    "장비 미리보기 종료를 요청했습니다."
            );
            return true;
        }

        if (args.length == 2
                && args[0].equalsIgnoreCase("set")) {

            int setId;

            try {
                setId = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                player.sendMessage("올바른 세트 ID를 입력해 주세요.");
                return true;
            }

            if (setId <= 0) {
                player.sendMessage("세트 ID는 1 이상이어야 합니다.");
                return true;
            }

            try {
                var items = setRepository.findPreviewSet(setId);

                if (items.isEmpty()) {
                    player.sendMessage(
                            "해당 세트의 미리보기 장비가 없습니다."
                    );
                    return true;
                }

                if (previewService.preview(player, items)) {
                    player.sendMessage(
                            "NPC 18번 전체 세트 미리보기 적용: "
                                    + setId
                    );
                }

            } catch (SQLException | IllegalStateException exception) {
                player.sendMessage(
                        "세트 정보를 불러오지 못했습니다. "
                                + "서버 로그를 확인해 주세요."
                );
                org.bukkit.Bukkit.getLogger().warning(
                        "[RPGCore] Equipment preview set "
                                + setId
                                + ": "
                                + exception.getMessage()
                );
            }

            return true;
        }

        if (args.length != 3
                || !args[0].equalsIgnoreCase("single")) {

            sendUsage(player);
            return true;
        }

        String itemId = args[1];

        EquipmentSlot slot;

        try {
            slot = EquipmentSlot.valueOf(
                    args[2].toUpperCase()
            );
        } catch (IllegalArgumentException exception) {
            sendUsage(player);
            return true;
        }

        if (!isAllowedSlot(slot)) {
            sendUsage(player);
            return true;
        }

        boolean success = previewService.previewSingle(
                player,
                itemId,
                slot
        );

        if (success) {
            player.sendMessage(
                    "NPC 18번 미리보기 적용: "
                            + itemId
                            + " / "
                            + slot.name()
            );
        }

        return true;
    }

    private boolean isAllowedSlot(
            EquipmentSlot slot
    ) {
        return switch (slot) {
            case HAND,
                 OFF_HAND,
                 HELMET,
                 CHESTPLATE,
                 LEGGINGS,
                 BOOTS -> true;

            default -> false;
        };
    }

    private void sendUsage(
            Player player
    ) {
        player.sendMessage(
                "/rpgpreview single <item_id> <slot>"
        );

        player.sendMessage(
                "슬롯: HAND, OFF_HAND, HELMET, "
                        + "CHESTPLATE, LEGGINGS, BOOTS"
        );

        player.sendMessage(
                "/rpgpreview set <set_id>"
        );

        player.sendMessage(
                "/rpgpreview stop"
        );
    }
}
