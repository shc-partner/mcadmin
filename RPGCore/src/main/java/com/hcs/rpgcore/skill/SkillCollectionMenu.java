package com.hcs.rpgcore.skill;

import com.hcs.rpgcore.hud.HudService;
import com.hcs.rpgcore.hud.HudSnapshot;

import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.Material;

import org.bukkit.entity.Player;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


public final class SkillCollectionMenu {

    public static final Component WARRIOR_TITLE =
            Component.text(
                    "전사 스킬 도감",
                    NamedTextColor.GOLD
            );

    public static final Component MAGE_TITLE =
            Component.text(
                    "마법사 스킬 도감",
                    NamedTextColor.LIGHT_PURPLE
            );


    /*
     * 5줄 인벤토리.
     *
     * 상단:
     *   직업 스킬 10개
     *
     * 하단:
     *   가상 스킬 슬롯 1~5
     */
    private static final int INVENTORY_SIZE =
            27;


    /*
     * 실제 스킬 아이콘 위치.
     */
    /*
     * 직업별 총 10개 스킬.
     *
     * 첫 번째 줄:
     *   10 11 12 13 14 15 16
     *
     * 두 번째 줄:
     *   20 21 22
     */
    private static final int[] SKILL_SLOTS = {
            11,
            12,
            13,
            14,
            15,

            20,
            21,
            22,
            23,
            24
    };


    /*
     * =========================================================
     * ACTIVATION SKILL AREA
     * =========================================================
     */
    private static final int PLAYER_INFO_SLOT = 4;
    private final HudService hudService;
    private final SkillAccessService skillAccessService;
    private final SkillItemFactory skillItemFactory;


    public SkillCollectionMenu(
            HudService hudService,
            SkillAccessService skillAccessService,
            SkillItemFactory skillItemFactory
    ) {

        this.hudService =
                hudService;

        this.skillAccessService =
                skillAccessService;

        this.skillItemFactory =
                skillItemFactory;
    }


    /*
     * =========================================================
     * OPEN
     * =========================================================
     */
    public void open(
            Player player,
            String requestedClass
    ) {

        if (
                player == null
                || requestedClass == null
                || requestedClass.isBlank()
        ) {
            return;
        }


        HudSnapshot snapshot =
                hudService.getProfile(
                        player.getUniqueId()
                );


        if (snapshot == null) {

            player.sendMessage(
                    Component.text(
                            "RPG 데이터를 불러오는 중입니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        String playerClass =
                snapshot.playerClass();


        boolean allClassPlayer =
                skillAccessService
                        .isAllClassSkillPlayer(
                                player
                        );


        if (
                !allClassPlayer
                && (
                        playerClass == null
                        || !requestedClass.equalsIgnoreCase(
                                playerClass
                        )
                )
        ) {

            player.sendMessage(
                    Component.text(
                            "현재 직업의 스킬북이 아닙니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        Component title =
                "WARRIOR".equalsIgnoreCase(
                        requestedClass
                )
                        ? WARRIOR_TITLE
                        : MAGE_TITLE;


        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        INVENTORY_SIZE,
                        title
                );


        fillBackground(
                inventory
        );


        List<SkillDefinition> skills =
                SkillRegistry.forClass(
                        requestedClass
                );


        int skillCount =
                Math.min(
                        skills.size(),
                        SKILL_SLOTS.length
                );


        for (
                int i = 0;
                i < skillCount;
                i++
        ) {

            SkillDefinition definition =
                    skills.get(
                            i
                    );


            boolean unlocked =
                    definition.isUnlocked(
                            snapshot.level()
                    );


            inventory.setItem(
                    SKILL_SLOTS[i],
                    createSkillIcon(
                            player,
                            definition,
                            unlocked
                    )
            );
        }


        /*
         * 아직 등록되지 않은 미래 스킬 칸.
         */
        for (
                int i = skillCount;
                i < SKILL_SLOTS.length;
                i++
        ) {

            inventory.setItem(
                    SKILL_SLOTS[i],
                    createFutureSkillIcon()
            );
        }


        inventory.setItem(
                PLAYER_INFO_SLOT,
                createPlayerInfoIcon(
                        snapshot,
                        requestedClass
                )
        );

        player.openInventory(
                inventory
        );
    }


    /*
     * =========================================================
     * SKILL ICON
     * =========================================================
     */
    private ItemStack createSkillIcon(
            Player player,
            SkillDefinition definition,
            boolean unlocked
    ) {

        if (unlocked) {

            ItemStack skillItem =
                    skillItemFactory
                            .createSkillBookById(
                                    definition.skillId(),
                                    player.getUniqueId()
                            );


            if (skillItem != null) {
                return skillItem;
            }
        }


        ItemStack item =
                new ItemStack(
                        Material.BARRIER
                );


        ItemMeta meta =
                item.getItemMeta();


        meta.displayName(
                Component.text(
                        definition.displayName(),
                        unlocked
                                ? NamedTextColor.YELLOW
                                : NamedTextColor.DARK_GRAY
                ).decoration(
                        TextDecoration.ITALIC,
                        false
                )
        );


        meta.lore(
                List.of(
                        Component.empty(),

                        Component.text(
                                "미해금 스킬",
                                NamedTextColor.RED
                        ).decoration(
                                TextDecoration.ITALIC,
                                false
                        ),

                        Component.text(
                                "요구 레벨: Lv."
                                        + definition.requiredLevel(),
                                NamedTextColor.GRAY
                        ).decoration(
                                TextDecoration.ITALIC,
                                false
                        )
                )
        );


        item.setItemMeta(
                meta
        );


        return item;
    }


    /*
     * =========================================================
     * FUTURE SKILL
     * =========================================================
     */
    private ItemStack createFutureSkillIcon() {

        ItemStack item =
                new ItemStack(
                        Material.GRAY_DYE
                );


        ItemMeta meta =
                item.getItemMeta();


        meta.displayName(
                Component.text(
                        "미구현 스킬",
                        NamedTextColor.DARK_GRAY
                ).decoration(
                        TextDecoration.ITALIC,
                        false
                )
        );


        item.setItemMeta(
                meta
        );


        return item;
    }


    /*
     * =========================================================
     * PLAYER / CLASS INFO
     * =========================================================
     */
    private ItemStack createPlayerInfoIcon(
            HudSnapshot snapshot,
            String requestedClass
    ) {

        String className =
                "WARRIOR".equalsIgnoreCase(requestedClass)
                        ? "전사"
                        : "마법사";

        ItemStack item =
                new ItemStack(Material.NETHER_STAR);

        ItemMeta meta =
                item.getItemMeta();

        meta.displayName(
                Component.text(
                        className + " 스킬 도감",
                        NamedTextColor.GOLD
                ).decoration(
                        TextDecoration.ITALIC,
                        false
                )
        );

        meta.lore(
                List.of(
                        Component.text(
                                "직업: " + className,
                                NamedTextColor.YELLOW
                        ),
                        Component.text(
                                "레벨: Lv." + snapshot.level(),
                                NamedTextColor.GRAY
                        )
                )
        );

        item.setItemMeta(meta);

        return item;
    }


    

    

    /*
     * =========================================================
     * MENU SLOT LOOKUP
     * =========================================================
     */

    /*
     * GUI의 raw slot이 몇 번째 스킬 아이콘인지 반환.
     *
     * 없으면 -1.
     */
    

    public int getSkillIndex(
            int rawSlot
    ) {

        for (
                int i = 0;
                i < SKILL_SLOTS.length;
                i++
        ) {

            if (
                    SKILL_SLOTS[i]
                            == rawSlot
            ) {
                return i;
            }
        }


        return -1;
    }

    /*
     * 현재 직업 도감의 특정 스킬 인덱스에서
     * 실제 SkillDefinition을 조회한다.
     */
    public SkillDefinition getSkillDefinition(
            String playerClass,
            int skillIndex
    ) {

        if (
                playerClass == null
                || playerClass.isBlank()
                || skillIndex < 0
        ) {
            return null;
        }


        List<SkillDefinition> skills =
                SkillRegistry.forClass(
                        playerClass
                );


        if (
                skillIndex
                        >= skills.size()
        ) {
            return null;
        }


        return skills.get(
                skillIndex
        );
    }


    





    

    /*
     * 도감 제목으로 현재 표시 중인 직업을 판별.
     */
    public String resolveMenuClass(
            Component title
    ) {

        if (title == null) {
            return null;
        }


        if (
                title.equals(
                        WARRIOR_TITLE
                )
        ) {
            return "WARRIOR";
        }


        if (
                title.equals(
                        MAGE_TITLE
                )
        ) {
            return "MAGE";
        }


        return null;
    }


    /*
     * =========================================================
     * BACKGROUND
     * =========================================================
     */
    private void fillBackground(
            Inventory inventory
    ) {

        ItemStack filler =
                new ItemStack(
                        Material.BLACK_STAINED_GLASS_PANE
                );


        ItemMeta meta =
                filler.getItemMeta();


        meta.displayName(
                Component.empty()
        );


        filler.setItemMeta(
                meta
        );


        for (
                int slot = 0;
                slot < inventory.getSize();
                slot++
        ) {

            inventory.setItem(
                    slot,
                    filler
            );
        }
    }
}
