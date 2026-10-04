package com.hcs.rpgcore.check;

import com.hcs.rpgcore.RPGCorePlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * RPGCore 골드 수표 아이템.
 *
 * PDC에는 수표 UUID만 저장한다.
 * 실제 금액과 사용 여부는 MariaDB에서 조회한다.
 */
public final class GoldCheckItemFactory {

    private final NamespacedKey checkUuidKey;

    public GoldCheckItemFactory(
            RPGCorePlugin plugin
    ) {
        this.checkUuidKey = new NamespacedKey(
                plugin,
                "gold_check_uuid"
        );
    }

    /**
     * 발행할 수표 아이템 생성.
     *
     * 이 메서드는 DB 기록을 생성하거나 골드를 차감하지 않는다.
     */
    public ItemStack create(
            UUID checkUuid,
            long amountGold
    ) {

        if (checkUuid == null) {
            throw new IllegalArgumentException(
                    "checkUuid must not be null"
            );
        }

        if (amountGold <= 0L) {
            throw new IllegalArgumentException(
                    "amountGold must be positive"
            );
        }

        ItemStack item = new ItemStack(
                Material.PAPER,
                1
        );

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            throw new IllegalStateException(
                    "Cannot create gold check item metadata"
            );
        }

        String formattedAmount =
                NumberFormat.getIntegerInstance(
                        Locale.US
                ).format(amountGold);

        meta.displayName(
                Component.text(
                        "황금 수표",
                        NamedTextColor.GOLD
                ).decoration(
                        TextDecoration.ITALIC,
                        false
                )
        );

        meta.lore(
                List.of(
                        Component.text(
                                "금액: " + formattedAmount + "G",
                                NamedTextColor.GREEN
                        ).decoration(
                                TextDecoration.ITALIC,
                                false
                        ),

                        Component.empty(),

                        Component.text(
                                "우클릭하여 골드로 환전",
                                NamedTextColor.YELLOW
                        ).decoration(
                                TextDecoration.ITALIC,
                                false
                        ),

                        Component.text(
                                "다른 플레이어에게 전달할 수 있습니다.",
                                NamedTextColor.GRAY
                        ).decoration(
                                TextDecoration.ITALIC,
                                false
                        ),

                        Component.text(
                                "수표 번호: "
                                        + checkUuid.toString()
                                                .substring(0, 8),
                                NamedTextColor.DARK_GRAY
                        ).decoration(
                                TextDecoration.ITALIC,
                                false
                        )
                )
        );

        meta.getPersistentDataContainer().set(
                checkUuidKey,
                PersistentDataType.STRING,
                checkUuid.toString()
        );

        item.setItemMeta(meta);

        return item;
    }

    /**
     * RPGCore 수표인지 확인하고 고유번호를 반환한다.
     *
     * 수표가 아니거나 UUID 데이터가 올바르지 않으면 null.
     */
    public UUID readCheckUuid(
            ItemStack item
    ) {

        if (item == null
                || item.getType() != Material.PAPER
                || !item.hasItemMeta()) {

            return null;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return null;
        }

        PersistentDataContainer pdc =
                meta.getPersistentDataContainer();

        String rawUuid = pdc.get(
                checkUuidKey,
                PersistentDataType.STRING
        );

        if (rawUuid == null) {
            return null;
        }

        try {
            return UUID.fromString(rawUuid);

        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public boolean isGoldCheck(
            ItemStack item
    ) {
        return readCheckUuid(item) != null;
    }
}
