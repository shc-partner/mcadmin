package com.hcs.rpgcore.command;

import com.hcs.rpgcore.dungeon.reward.DungeonRewardItemFactory;
import com.hcs.rpgcore.dungeon.reward.PendingReward;
import com.hcs.rpgcore.dungeon.reward.PendingRewardRepository;

import java.sql.SQLException;

import java.util.List;
import java.util.Map;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import org.bukkit.entity.Player;

import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;

public final class RpgClaimCommand
        implements CommandExecutor {

    private final PendingRewardRepository repository;

    private final DungeonRewardItemFactory itemFactory;

    public RpgClaimCommand(
            PendingRewardRepository repository,
            DungeonRewardItemFactory itemFactory
    ) {

        this.repository =
                repository;

        this.itemFactory =
                itemFactory;
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
                    "This command can only be used by a player."
            );

            return true;
        }

        try {

            List<PendingReward> rewards =
                    repository.findByPlayer(
                            player.getUniqueId()
                    );

            if (rewards.isEmpty()) {

                player.sendMessage(
                        Component.text(
                                "보관 중인 던전 보상이 없습니다.",
                                NamedTextColor.GRAY
                        )
                );

                return true;
            }

            int claimedRows = 0;
            boolean inventoryFull = false;

            for (PendingReward reward : rewards) {

                ItemStack item =
                        itemFactory.create(
                                reward.rewardType(),
                                reward.amount(),
                                player.getUniqueId(),
                                player.getName()
                        );

                Map<Integer, ItemStack> leftovers =
                        player.getInventory()
                                .addItem(item);

                if (leftovers.isEmpty()) {

                    repository.delete(
                            reward.id(),
                            player.getUniqueId()
                    );

                    claimedRows++;

                    continue;
                }

                int remaining =
                        leftovers
                                .values()
                                .stream()
                                .mapToInt(
                                        ItemStack::getAmount
                                )
                                .sum();

                if (remaining <= 0) {

                    repository.delete(
                            reward.id(),
                            player.getUniqueId()
                    );

                    claimedRows++;

                } else {

                    repository.updateAmount(
                            reward.id(),
                            player.getUniqueId(),
                            remaining
                    );

                    inventoryFull = true;

                    /*
                     * 뒤의 보상까지 계속 시도하면
                     * 슬롯 상태가 불분명해지므로 여기서 중단.
                     */
                    break;
                }
            }

            if (claimedRows > 0) {

                player.sendMessage(
                        Component.text(
                                "보관된 던전 보상을 수령했습니다.",
                                NamedTextColor.GREEN
                        )
                );
            }

            if (inventoryFull) {

                player.sendMessage(
                        Component.text(
                                "인벤토리 공간이 부족합니다. "
                                        + "공간을 확보한 뒤 /rpgclaim을 다시 사용하세요.",
                                NamedTextColor.YELLOW
                        )
                );
            }

            return true;

        } catch (SQLException exception) {

            player.sendMessage(
                    Component.text(
                            "보상 정보를 불러오지 못했습니다.",
                            NamedTextColor.RED
                    )
            );

            exception.printStackTrace();

            return true;
        }
    }
}
