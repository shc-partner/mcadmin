package com.hcs.rpgcore.command;

import com.hcs.rpgcore.shop.ShopDefinition;
import com.hcs.rpgcore.shop.ShopGuiService;
import com.hcs.rpgcore.shop.ShopRegistry;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;


/**
 * 상점 시스템 관리자 테스트 명령.
 *
 * NPC 연결 후에도 관리자 테스트 용도로 유지 가능.
 */
public final class RpgShopCommand
        implements CommandExecutor {

    private final ShopRegistry
            shopRegistry;

    private final ShopGuiService
            shopGuiService;


    public RpgShopCommand(
            ShopRegistry shopRegistry,
            ShopGuiService shopGuiService
    ) {

        this.shopRegistry = shopRegistry;
        this.shopGuiService = shopGuiService;
    }


    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {

        if (!(
                sender instanceof Player player
        )) {

            sender.sendMessage(
                    "플레이어만 사용할 수 있습니다."
            );

            return true;
        }


        if (!player.hasPermission(
                "rpgcore.admin"
        )) {

            player.sendMessage(
                    Component.text(
                            "권한이 없습니다.",
                            NamedTextColor.RED
                    )
            );

            return true;
        }


        if (args.length != 1) {

            player.sendMessage(
                    Component.text(
                            "사용법: /rpgshop <shopId>",
                            NamedTextColor.YELLOW
                    )
            );


            player.sendMessage(
                    Component.text(
                            "등록된 상점:",
                            NamedTextColor.GRAY
                    )
            );


            for (
                    ShopDefinition definition
                    : shopRegistry.all()
            ) {

                player.sendMessage(
                        Component.text(
                                " - "
                                        + definition.shopId()
                                        + " : "
                                        + definition.title(),
                                NamedTextColor.GRAY
                        )
                );
            }


            return true;
        }


        String shopId =
                args[0];


        ShopDefinition definition =
                shopRegistry.get(
                        shopId
                );


        if (definition == null) {

            player.sendMessage(
                    Component.text(
                            "존재하지 않는 상점입니다: "
                                    + shopId,
                            NamedTextColor.RED
                    )
            );

            return true;
        }


        shopGuiService.open(
                player,
                shopId
        );


        return true;
    }
}
