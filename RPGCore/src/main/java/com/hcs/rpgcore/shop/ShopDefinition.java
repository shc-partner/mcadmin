package com.hcs.rpgcore.shop;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * 하나의 상점 전체 정의.
 */
public record ShopDefinition(

        String shopId,

        String title,

        ShopType type,

        int rows,

        List<ShopItemDefinition> items
) {

    public ShopDefinition {

        if (
                shopId == null
                        || shopId.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "shopId must not be blank"
            );
        }


        if (
                title == null
                        || title.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "title must not be blank"
            );
        }


        if (type == null) {
            throw new IllegalArgumentException(
                    "type must not be null"
            );
        }


        if (
                rows < 1
                        || rows > 6
        ) {
            throw new IllegalArgumentException(
                    "rows must be between 1 and 6"
            );
        }


        if (items == null) {
            throw new IllegalArgumentException(
                    "items must not be null"
            );
        }


        items =
                List.copyOf(
                        items
                );


        int inventorySize =
                rows * 9;


        Set<Integer> occupiedSlots =
                new HashSet<>();


        for (
                ShopItemDefinition item
                : items
        ) {

            if (item.slot() >= inventorySize) {

                throw new IllegalArgumentException(
                        "Item slot is outside inventory: "
                                + item.entryId()
                                + " slot="
                                + item.slot()
                                + " size="
                                + inventorySize
                );
            }


            if (!occupiedSlots.add(item.slot())) {

                throw new IllegalArgumentException(
                        "Duplicate shop slot: "
                                + item.slot()
                );
            }
        }
    }


    public int inventorySize() {

        return rows * 9;
    }


    public ShopItemDefinition findBySlot(
            int slot
    ) {

        for (
                ShopItemDefinition item
                : items
        ) {

            if (item.slot() == slot) {
                return item;
            }
        }


        return null;
    }
}
