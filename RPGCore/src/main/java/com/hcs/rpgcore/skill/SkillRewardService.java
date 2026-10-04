package com.hcs.rpgcore.skill;

import com.hcs.rpgcore.player.PlayerData;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class SkillRewardService {

    private final SkillItemFactory skillItemFactory;
    private final SkillAccessService skillAccessService;

    public SkillRewardService(
            SkillItemFactory skillItemFactory,
            SkillAccessService skillAccessService
    ) {
        this.skillItemFactory = skillItemFactory;
        this.skillAccessService = skillAccessService;
    }

    public void syncRewards(
            Player player,
            PlayerData playerData
    ) {

        if (
                player == null
                || playerData == null
                || !player.isOnline()
        ) {
            return;
        }


        int level =
                playerData.getLevel();

        String playerClass =
                playerData.getPlayerClass();

        boolean allClassPlayer =
                skillAccessService.isAllClassSkillPlayer(
                        player
                );


        /*
         * =====================================================
         * Lv.10 CLASS SKILL TOME
         * =====================================================
         *
         * 개별 스킬북은 더 이상 레벨별로 지급하지 않는다.
         *
         * 전직 완료 후 직업별 영구 스킬북 1권만 지급한다.
         * 이후 스킬 해금 여부는 플레이어 레벨로 판정한다.
         */
        if (
                level < 10
        ) {
            return;
        }


        if (
                allClassPlayer
        ) {

            giveIfMissing(
                    player,
                    skillItemFactory.createWarriorSkillTome(
                            player.getUniqueId()
                    ),
                    "전사 스킬북",
                    NamedTextColor.GOLD
            );


            giveIfMissing(
                    player,
                    skillItemFactory.createMageSkillTome(
                            player.getUniqueId()
                    ),
                    "마법사 스킬북",
                    NamedTextColor.LIGHT_PURPLE
            );

            return;
        }


        if (
                isClass(
                        playerClass,
                        "WARRIOR"
                )
        ) {

            giveIfMissing(
                    player,
                    skillItemFactory.createWarriorSkillTome(
                            player.getUniqueId()
                    ),
                    "전사 스킬북",
                    NamedTextColor.GOLD
            );

            return;
        }


        if (
                isClass(
                        playerClass,
                        "MAGE"
                )
        ) {

            giveIfMissing(
                    player,
                    skillItemFactory.createMageSkillTome(
                            player.getUniqueId()
                    ),
                    "마법사 스킬북",
                    NamedTextColor.LIGHT_PURPLE
            );
        }
    }


    private boolean isClass(
            String actualClass,
            String requiredClass
    ) {
        return actualClass != null
                && requiredClass.equalsIgnoreCase(actualClass);
    }

    private boolean giveIfMissing(
            Player player,
            ItemStack skillBook,
            String displayName,
            NamedTextColor color
    ) {
        if (skillBook == null) {
            return false;
        }

        String desiredSkillId =
                skillItemFactory.getSkillId(skillBook);

        if (desiredSkillId == null) {
            return false;
        }

        for (ItemStack existing : player.getInventory().getContents()) {
            if (
                    desiredSkillId.equals(
                            skillItemFactory.getSkillId(existing)
                    )
                    && skillItemFactory.isOwnedBy(
                            existing,
                            player.getUniqueId()
                    )
            ) {
                return false;
            }
        }

        for (ItemStack existing : player.getEnderChest().getContents()) {
            if (
                    desiredSkillId.equals(
                            skillItemFactory.getSkillId(existing)
                    )
                    && skillItemFactory.isOwnedBy(
                            existing,
                            player.getUniqueId()
                    )
            ) {
                return false;
            }
        }

        Map<Integer, ItemStack> leftovers =
                player.getInventory().addItem(skillBook);

        for (ItemStack leftover : leftovers.values()) {
            org.bukkit.entity.Item dropped =
                    player.getWorld().dropItemNaturally(
                            player.getLocation(),
                            leftover
                    );
            dropped.setOwner(player.getUniqueId());
        }

        player.sendMessage(
                Component.text(
                        "[스킬 습득] ",
                        NamedTextColor.GOLD
                ).append(
                        Component.text(
                                displayName,
                                color
                        )
                )
        );

        return true;
    }
}
