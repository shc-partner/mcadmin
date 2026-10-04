package com.hcs.rpgcore.item;

import com.hcs.rpgcore.RPGCorePlugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;


public final class EnhancementStoneRecipeService {

    private final RPGCorePlugin plugin;

    private final CustomItemFactory customItemFactory;

    private final NamespacedKey recipeKey;


    public EnhancementStoneRecipeService(
            RPGCorePlugin plugin
    ) {

        this.plugin =
                plugin;

        this.customItemFactory =
                new CustomItemFactory(
                        plugin
                );

        this.recipeKey =
                new NamespacedKey(
                        plugin,
                        "enhancement_stone_recipe"
                );
    }


    public void register() {

        ItemStack result =
                customItemFactory.create(
                        "enhancement_stone"
                );


        if (
                result == null
                || result.getType().isAir()
        ) {

            plugin.getLogger().severe(
                    "강화석 제작법 등록 실패: enhancement_stone 아이템을 생성할 수 없습니다."
            );

            return;
        }


        result.setAmount(
                1
        );


        /*
         * 재등록 시 동일 키 충돌 방지.
         */
        Bukkit.removeRecipe(
                recipeKey
        );


        ShapedRecipe recipe =
                new ShapedRecipe(
                        recipeKey,
                        result
                );


        recipe.shape(
                "LLL",
                "DDD",
                "EEE"
        );


        recipe.setIngredient(
                'L',
                Material.LAPIS_LAZULI
        );

        recipe.setIngredient(
                'D',
                Material.DIAMOND
        );

        recipe.setIngredient(
                'E',
                Material.EMERALD
        );


        Bukkit.addRecipe(
                recipe
        );


        plugin.getLogger().info(
                "강화석 제작법 등록 완료: 청금석 3 + 다이아몬드 3 + 에메랄드 3"
        );
    }
}
