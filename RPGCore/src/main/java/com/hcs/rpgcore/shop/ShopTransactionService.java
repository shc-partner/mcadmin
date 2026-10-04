package com.hcs.rpgcore.shop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;


/**
 * 상점 실제 거래 처리.
 *
 * GUI는 거래 규칙을 직접 수행하지 않고
 * 이 서비스를 통해 구매/판매를 처리한다.
 */
public final class ShopTransactionService {

    private final ShopPriceService
            priceService;

    private final ShopEconomyService
            economyService;

    private final ShopItemProvider
            itemProvider;


    public ShopTransactionService(
            ShopPriceService priceService,
            ShopEconomyService economyService,
            ShopItemProvider itemProvider
    ) {

        this.priceService = priceService;
        this.economyService = economyService;
        this.itemProvider = itemProvider;
    }


    /**
     * 현재는 1회 클릭 = 1묶음 구매.
     */
    public boolean buy(
            Player player,
            ShopDefinition shop,
            ShopItemDefinition definition
    ) {

        if (
                shop.type() != ShopType.BUY
                        && shop.type() != ShopType.BOTH
        ) {

            player.sendMessage(
                    Component.text(
                            "이 상점에서는 구매할 수 없습니다.",
                            NamedTextColor.RED
                    )
            );

            return false;
        }


        ItemStack item;

        try {

            item =
                    itemProvider.create(
                            player,
                            definition
                    );

        } catch (Exception exception) {

            player.sendMessage(
                    Component.text(
                            "아이템을 생성할 수 없습니다.",
                            NamedTextColor.RED
                    )
            );

            return false;
        }


        if (!canFit(
                player.getInventory(),
                item
        )) {

            player.sendMessage(
                    Component.text(
                            "인벤토리 공간이 부족합니다.",
                            NamedTextColor.RED
                    )
            );

            return false;
        }


        long price =
                priceService.getTotalBuyPrice(
                        definition,
                        1
                );


        if (!economyService.has(
                player,
                price
        )) {

            player.sendMessage(
                    Component.text(
                            "보유 금액이 부족합니다. 필요 금액: ",
                            NamedTextColor.RED
                    ).append(
                            Component.text(
                                    price + "원",
                                    NamedTextColor.GOLD
                            )
                    )
            );

            return false;
        }


        if (!economyService.withdraw(
                player,
                price
        )) {

            player.sendMessage(
                    Component.text(
                            "결제 처리에 실패했습니다.",
                            NamedTextColor.RED
                    )
            );

            return false;
        }


        /*
         * addItem()이 예외적으로 일부만 성공하는 경우에도
         * 거래 전 상태로 정확히 되돌릴 수 있도록
         * storage inventory를 백업한다.
         */
        ItemStack[] before =
                cloneContents(
                        player.getInventory()
                                .getStorageContents()
                );


        HashMap<Integer, ItemStack> leftovers =
                player.getInventory()
                        .addItem(
                                item
                        );


        if (!leftovers.isEmpty()) {

            /*
             * 일부 아이템이 이미 들어갔을 수도 있으므로
             * 먼저 인벤토리를 거래 전 상태로 복구한다.
             */
            player.getInventory()
                    .setStorageContents(
                            before
                    );


            boolean refunded =
                    economyService.deposit(
                            player,
                            price
                    );


            if (refunded) {

                player.sendMessage(
                        Component.text(
                                "아이템 지급에 실패하여 거래를 취소하고 결제 금액을 환불했습니다.",
                                NamedTextColor.RED
                        )
                );

            } else {

                player.sendMessage(
                        Component.text(
                                "아이템 지급에 실패했습니다. 인벤토리는 복구됐지만 환불 처리에 실패했습니다.",
                                NamedTextColor.RED
                        )
                );
            }


            return false;
        }


        player.sendMessage(
                Component.text(
                        definition.displayName(),
                        NamedTextColor.GREEN
                ).append(
                        Component.text(
                                " 구매 완료 - ",
                                NamedTextColor.GRAY
                        )
                ).append(
                        Component.text(
                                price + "원",
                                NamedTextColor.GOLD
                        )
                )
        );


        return true;
    }



    /**
     * 현재는 1회 클릭 = definition.amount() 만큼 판매.
     */
    public boolean sell(
            Player player,
            ShopDefinition shop,
            ShopItemDefinition definition
    ) {

        if (
                shop.type() != ShopType.SELL
                        && shop.type() != ShopType.BOTH
        ) {

            player.sendMessage(
                    Component.text(
                            "이 상점에서는 판매할 수 없습니다.",
                            NamedTextColor.RED
                    )
            );

            return false;
        }


        int requiredAmount =
                definition.amount();


        PlayerInventory inventory =
                player.getInventory();


        int availableAmount =
                countMatchingItems(
                        player,
                        inventory,
                        definition
                );


        if (availableAmount < requiredAmount) {

            player.sendMessage(
                    Component.text(
                            "판매할 아이템이 없습니다.",
                            NamedTextColor.RED
                    )
            );

            return false;
        }


        long price =
                priceService.getTotalSellPrice(
                        definition,
                        1
                );


        /*
         * 경제 처리 실패 시 정확한 상태로 되돌리기 위해
         * 변경 전 storage inventory 전체를 복사한다.
         */
        ItemStack[] before =
                cloneContents(
                        inventory.getStorageContents()
                );


        if (!removeMatchingItems(
                player,
                inventory,
                definition,
                requiredAmount
        )) {

            inventory.setStorageContents(
                    before
            );

            player.sendMessage(
                    Component.text(
                            "판매 아이템 처리에 실패했습니다.",
                            NamedTextColor.RED
                    )
            );

            return false;
        }


        if (!economyService.deposit(
                player,
                price
        )) {

            inventory.setStorageContents(
                    before
            );

            player.sendMessage(
                    Component.text(
                            "판매 대금 지급에 실패하여 아이템을 복구했습니다.",
                            NamedTextColor.RED
                    )
            );

            return false;
        }


        player.sendMessage(
                Component.text(
                        definition.displayName(),
                        NamedTextColor.GREEN
                ).append(
                        Component.text(
                                " 판매 완료 - ",
                                NamedTextColor.GRAY
                        )
                ).append(
                        Component.text(
                                price + "원",
                                NamedTextColor.GOLD
                        )
                )
        );


        return true;
    }


    private int countMatchingItems(
            Player player,
            PlayerInventory inventory,
            ShopItemDefinition definition
    ) {

        int count = 0;


        for (
                ItemStack current
                : inventory.getStorageContents()
        ) {

            if (!itemProvider.matches(
                    player,
                    current,
                    definition
            )) {
                continue;
            }


            count +=
                    current.getAmount();
        }


        return count;
    }


    private boolean removeMatchingItems(
            Player player,
            PlayerInventory inventory,
            ShopItemDefinition definition,
            int amount
    ) {

        int remaining =
                amount;


        ItemStack[] contents =
                inventory.getStorageContents();


        for (
                int slot = 0;
                slot < contents.length;
                slot++
        ) {

            ItemStack current =
                    contents[slot];


            if (!itemProvider.matches(
                    player,
                    current,
                    definition
            )) {
                continue;
            }


            int remove =
                    Math.min(
                            remaining,
                            current.getAmount()
                    );


            int newAmount =
                    current.getAmount()
                            - remove;


            if (newAmount <= 0) {

                contents[slot] =
                        null;

            } else {

                ItemStack replacement =
                        current.clone();

                replacement.setAmount(
                        newAmount
                );

                contents[slot] =
                        replacement;
            }


            remaining -=
                    remove;


            if (remaining <= 0) {

                inventory.setStorageContents(
                        contents
                );

                return true;
            }
        }


        return false;
    }


    private ItemStack[] cloneContents(
            ItemStack[] source
    ) {

        ItemStack[] result =
                new ItemStack[
                        source.length
                ];


        for (
                int i = 0;
                i < source.length;
                i++
        ) {

            result[i] =
                    source[i] == null
                            ? null
                            : source[i].clone();
        }


        return result;
    }


    /**
     * 아이템 1묶음이 플레이어 인벤토리에 들어갈 수 있는지
     * 실제 addItem 전에 계산한다.
     */
    private boolean canFit(
            PlayerInventory inventory,
            ItemStack item
    ) {

        int remaining =
                item.getAmount();


        int maxStack =
                item.getMaxStackSize();


        ItemStack[] storage =
                inventory.getStorageContents();


        /*
         * 기존 동일 스택의 남은 공간부터 계산한다.
         */
        for (
                ItemStack current
                : storage
        ) {

            if (
                    current == null
                            || current.getType().isAir()
            ) {
                continue;
            }


            if (!current.isSimilar(item)) {
                continue;
            }


            int available =
                    Math.max(
                            0,
                            Math.min(
                                    current.getMaxStackSize(),
                                    maxStack
                            ) - current.getAmount()
                    );


            remaining -=
                    available;


            if (remaining <= 0) {
                return true;
            }
        }


        /*
         * 빈 슬롯 계산.
         */
        for (
                ItemStack current
                : storage
        ) {

            if (
                    current != null
                            && !current.getType().isAir()
            ) {
                continue;
            }


            remaining -=
                    maxStack;


            if (remaining <= 0) {
                return true;
            }
        }


        return false;
    }
}
