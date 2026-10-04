package com.hcs.rpgcore.shop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;


/**
 * 범용 상점 GUI 생성/오픈 서비스.
 */
public final class ShopGuiService {

    private final ShopRegistry
            shopRegistry;

    private final ShopPriceService
            priceService;

    private final ShopItemProvider
            itemProvider;


    public ShopGuiService(
            ShopRegistry shopRegistry,
            ShopPriceService priceService,
            ShopItemProvider itemProvider
    ) {

        this.shopRegistry = shopRegistry;
        this.priceService = priceService;
        this.itemProvider = itemProvider;
    }


    public void open(
            Player player,
            String shopId
    ) {

        ShopDefinition shop =
                shopRegistry.require(
                        shopId
                );


        ShopInventoryHolder holder =
                new ShopInventoryHolder(
                        shop.shopId()
                );


        Inventory inventory =
                Bukkit.createInventory(
                        holder,
                        shop.inventorySize(),
                        Component.text(
                                shop.title()
                        )
                );


        holder.setInventory(
                inventory
        );


        for (
                ShopItemDefinition definition
                : shop.items()
        ) {

            ItemStack displayItem;

            try {

                displayItem =
                        itemProvider.create(
                                player,
                                definition
                        );

            } catch (Exception exception) {

                continue;
            }


            applyShopLore(
                    shop,
                    definition,
                    displayItem
            );


            inventory.setItem(
                    definition.slot(),
                    displayItem
            );
        }


        if (isEnchantShop(shop.shopId())) {

            ItemStack backButton =
                    new ItemStack(
                            org.bukkit.Material.ARROW
                    );

            ItemMeta backMeta =
                    backButton.getItemMeta();

            backMeta.setCustomModelData(1004);

            backMeta.displayName(
                    Component.text(
                            "뒤로 가기",
                            NamedTextColor.YELLOW
                    )
            );

            backMeta.lore(
                    java.util.List.of(
                            Component.text(
                                    "인챈트북 종류 선택 화면으로 돌아갑니다.",
                                    NamedTextColor.GRAY
                            )
                    )
            );

            backButton.setItemMeta(backMeta);

            inventory.setItem(
                    22,
                    backButton
            );
        }

        /*
         * NPC 3 - 농작물 판매 시세 안내
         * 실제 판매 상품이 없는 마지막 슬롯에만 표시한다.
         */
        if (ShopRegistry.FARM_SELL.equals(shop.shopId())) {

            int infoSlot = inventory.getSize() - 1;

            if (shop.findBySlot(infoSlot) == null
                    && inventory.getItem(infoSlot) == null) {

                ItemStack info = new ItemStack(
                        org.bukkit.Material.PALE_OAK_HANGING_SIGN
                );

                ItemMeta infoMeta = info.getItemMeta();

                if (infoMeta != null) {

                    infoMeta.displayName(
                            Component.text(
                                    "농작물 시세 안내",
                                    NamedTextColor.YELLOW
                            )
                    );

                    long remainingSeconds =
                            priceService.getSecondsUntilNextMarketDay();

                    long remainingMinutes =
                            remainingSeconds / 60L;

                    long remainingSecondPart =
                            remainingSeconds % 60L;

                    infoMeta.lore(
                            java.util.List.of(
                                    Component.text(
                                            "게임 시간 기준 매일 가격이 변경됩니다",
                                            NamedTextColor.GRAY
                                    ),
                                    Component.text(
                                            "다음 가격 변경까지: "
                                                    + remainingMinutes
                                                    + "분 "
                                                    + remainingSecondPart
                                                    + "초",
                                            NamedTextColor.GREEN
                                    )
                            )
                    );

                    info.setItemMeta(infoMeta);
                }

                inventory.setItem(infoSlot, info);
            }
        }

        player.openInventory(
                inventory
        );
    }

    private boolean isEnchantShop(
            String shopId
    ) {

        return shopId.equals(ShopRegistry.ENCHANT_WEAPON)
                || shopId.equals(ShopRegistry.ENCHANT_ARMOR)
                || shopId.equals(ShopRegistry.ENCHANT_TOOL)
                || shopId.equals(ShopRegistry.ENCHANT_COMMON);
    }


    private void applyShopLore(
            ShopDefinition shop,
            ShopItemDefinition definition,
            ItemStack item
    ) {

        ItemMeta meta =
                item.getItemMeta();


        if (meta == null) {
            return;
        }


        List<Component> lore =
                meta.hasLore()
                        && meta.lore() != null
                        ? new ArrayList<>(
                                meta.lore()
                        )
                        : new ArrayList<>();


        if (!lore.isEmpty()) {

            lore.add(
                    Component.empty()
            );
        }


        if (
                shop.type() == ShopType.BUY
                        || shop.type() == ShopType.BOTH
        ) {

            long price =
                    priceService.getTotalBuyPrice(
                            definition,
                            1
                    );


            lore.add(
                    Component.text(
                            "구매 가격: ",
                            NamedTextColor.GRAY
                    ).append(
                            Component.text(
                                    price + "원",
                                    NamedTextColor.GOLD
                            )
                    )
            );


            lore.add(
                    Component.text(
                            "클릭하여 구매",
                            NamedTextColor.YELLOW
                    )
            );
        }


        if (
                shop.type() == ShopType.SELL
                        || shop.type() == ShopType.BOTH
        ) {

            long price =
                    priceService.getTotalSellPrice(
                            definition,
                            1
                    );


            lore.add(
                    Component.text(
                            "판매 가격: ",
                            NamedTextColor.GRAY
                    ).append(
                            Component.text(
                                    price + "원",
                                    NamedTextColor.GOLD
                            )
                    )
            );


            lore.add(
                    Component.text(
                            "클릭하여 1개 판매",
                            NamedTextColor.YELLOW
                    )
            );
        }


        meta.lore(
                lore
        );


        item.setItemMeta(
                meta
        );
    }
}
