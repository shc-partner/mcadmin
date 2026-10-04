package com.hcs.rpgcore.elixir;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.item.CustomItemFactory;
import com.hcs.rpgcore.item.CustomItemIds;
import com.hcs.rpgcore.mana.ManaService;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;


/*
 * =============================================================
 * ELIXIR SERVICE
 * =============================================================
 *
 * 하급:
 * - 최대 HP 25%
 * - 최대 MP 25%
 *
 * 중급:
 * - 최대 HP 50%
 * - 최대 MP 50%
 *
 * 상급:
 * - 최대 HP 100%
 * - 최대 MP 100%
 *
 * 섭취는 CustomItemFactory의 CONSUMABLE component가 담당하고,
 * 실제 회복은 PlayerItemConsumeEvent 완료 시 처리한다.
 */
public final class ElixirService
        implements Listener {

    private static final double LESSER_RATIO =
            0.25D;

    private static final double MEDIUM_RATIO =
            0.50D;

    private static final double GREATER_RATIO =
            1.00D;


    private final RPGCorePlugin plugin;

    private final ManaService manaService;

    private final CustomItemFactory
            customItemFactory;


    public ElixirService(
            RPGCorePlugin plugin,
            ManaService manaService
    ) {

        this.plugin = plugin;

        this.manaService = manaService;

        this.customItemFactory =
                new CustomItemFactory(
                        plugin
                );
    }


    /*
     * =========================================================
     * CONSUME
     * =========================================================
     */
    @EventHandler
    public void onConsume(
            PlayerItemConsumeEvent event
    ) {

        ItemStack item =
                event.getItem();

        String itemId =
                customItemFactory.getItemId(
                        item
                );

        if (itemId == null) {
            return;
        }


        double ratio;

        switch (itemId) {

            case CustomItemIds.LESSER_ELIXIR ->
                    ratio = LESSER_RATIO;

            case CustomItemIds.MEDIUM_ELIXIR ->
                    ratio = MEDIUM_RATIO;

            case CustomItemIds.GREATER_ELIXIR ->
                    ratio = GREATER_RATIO;

            default -> {
                return;
            }
        }


        Player player =
                event.getPlayer();

        restoreHealth(
                player,
                ratio
        );

        restoreMana(
                player,
                ratio
        );
    }


    /*
     * =========================================================
     * HP
     * =========================================================
     */
    private void restoreHealth(
            Player player,
            double ratio
    ) {

        AttributeInstance maxHealthAttribute =
                player.getAttribute(
                        Attribute.MAX_HEALTH
                );

        if (maxHealthAttribute == null) {
            return;
        }

        double maximumHealth =
                maxHealthAttribute.getValue();

        if (maximumHealth <= 0.0D) {
            return;
        }

        double restoreAmount =
                maximumHealth * ratio;

        double newHealth =
                Math.min(
                        maximumHealth,
                        player.getHealth()
                                + restoreAmount
                );

        player.setHealth(
                newHealth
        );
    }


    /*
     * =========================================================
     * MP
     * =========================================================
     */
    private void restoreMana(
            Player player,
            double ratio
    ) {

        UUID uuid =
                player.getUniqueId();

        if (
                !manaService.isInitialized(
                        uuid
                )
        ) {
            return;
        }

        double maximumMana =
                manaService.getMaximumMana(
                        uuid
                );

        if (maximumMana <= 0.0D) {
            return;
        }

        manaService.restore(
                uuid,
                maximumMana * ratio
        );
    }


    /*
     * =========================================================
     * RECIPE REGISTRATION
     * =========================================================
     */
    public void registerRecipes() {

        registerLesserRecipe();
        registerMediumRecipe();
        registerGreaterRecipe();
    }


    /*
     * 설탕 | 당근 | 설탕
     *      | 물병 |     
     * 설탕 | 당근 | 설탕
     *
     * -> 하급 엘릭서 x16
     */
    private void registerLesserRecipe() {

        ItemStack result =
                createResult(
                        CustomItemIds.LESSER_ELIXIR,
                        16
                );

        NamespacedKey key =
                new NamespacedKey(
                        plugin,
                        "lesser_elixir_recipe"
                );

        plugin.getServer()
                .removeRecipe(
                        key
                );

        ShapedRecipe recipe =
                new ShapedRecipe(
                        key,
                        result
                );

        recipe.shape(
                "SCS",
                " W ",
                "SCS"
        );

        recipe.setIngredient(
                'S',
                Material.SUGAR
        );

        recipe.setIngredient(
                'C',
                Material.CARROT
        );

        recipe.setIngredient(
                'W',
                new RecipeChoice.ExactChoice(
                        createWaterBottle()
                )
        );

        plugin.getServer()
                .addRecipe(
                        recipe
                );
    }


    /*
     * 설탕 | 당근 | 설탕
     *      | 하급 |     
     * 설탕 | 당근 | 설탕
     *
     * -> 중급 엘릭서 x8
     */
    private void registerMediumRecipe() {

        ItemStack result =
                createResult(
                        CustomItemIds.MEDIUM_ELIXIR,
                        8
                );

        ItemStack lesser =
                createResult(
                        CustomItemIds.LESSER_ELIXIR,
                        1
                );

        NamespacedKey key =
                new NamespacedKey(
                        plugin,
                        "medium_elixir_recipe"
                );

        plugin.getServer()
                .removeRecipe(
                        key
                );

        ShapedRecipe recipe =
                new ShapedRecipe(
                        key,
                        result
                );

        recipe.shape(
                "SCS",
                " E ",
                "SCS"
        );

        recipe.setIngredient(
                'S',
                Material.SUGAR
        );

        recipe.setIngredient(
                'C',
                Material.CARROT
        );

        recipe.setIngredient(
                'E',
                new RecipeChoice.ExactChoice(
                        lesser
                )
        );

        plugin.getServer()
                .addRecipe(
                        recipe
                );
    }


    /*
     * 설탕 | 당근 | 설탕
     *      | 중급 |     
     * 설탕 | 당근 | 설탕
     *
     * -> 상급 엘릭서 x4
     */
    private void registerGreaterRecipe() {

        ItemStack result =
                createResult(
                        CustomItemIds.GREATER_ELIXIR,
                        4
                );

        ItemStack medium =
                createResult(
                        CustomItemIds.MEDIUM_ELIXIR,
                        1
                );

        NamespacedKey key =
                new NamespacedKey(
                        plugin,
                        "greater_elixir_recipe"
                );

        plugin.getServer()
                .removeRecipe(
                        key
                );

        ShapedRecipe recipe =
                new ShapedRecipe(
                        key,
                        result
                );

        recipe.shape(
                "SCS",
                " E ",
                "SCS"
        );

        recipe.setIngredient(
                'S',
                Material.SUGAR
        );

        recipe.setIngredient(
                'C',
                Material.CARROT
        );

        recipe.setIngredient(
                'E',
                new RecipeChoice.ExactChoice(
                        medium
                )
        );

        plugin.getServer()
                .addRecipe(
                        recipe
                );
    }


    /*
     * =========================================================
     * RECIPE HELPERS
     * =========================================================
     */
    private ItemStack createResult(
            String itemId,
            int amount
    ) {

        ItemStack result =
                customItemFactory.create(
                        itemId
                );

        if (result == null) {

            throw new IllegalStateException(
                    "엘릭서 아이템 생성 실패: "
                            + itemId
            );
        }

        result.setAmount(
                amount
        );

        return result;
    }


    private ItemStack createWaterBottle() {

        ItemStack bottle =
                new ItemStack(
                        Material.POTION
                );

        PotionMeta meta =
                (PotionMeta)
                        bottle.getItemMeta();

        meta.setBasePotionType(
                PotionType.WATER
        );

        bottle.setItemMeta(
                meta
        );

        return bottle;
    }
}
