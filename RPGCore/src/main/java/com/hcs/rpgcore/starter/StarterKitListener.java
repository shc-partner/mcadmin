package com.hcs.rpgcore.starter;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class StarterKitListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 이미 접속한 적이 있는 플레이어에게는 지급하지 않음
        if (player.hasPlayedBefore()) {
            return;
        }

        giveStarterKit(player);
        giveStarterGold(player);
    }

    /*
     * STARTER_GOLD_REWARD
     *
     * 최초 접속 시에만 Vault 경제 계정에 300골드 지급.
     * 관리자 시작 장비 재지급 명령에서는 호출하지 않는다.
     */
    private static void giveStarterGold(Player player) {

        RegisteredServiceProvider<Economy> registration =
                player.getServer()
                        .getServicesManager()
                        .getRegistration(Economy.class);

        if (registration == null
                || registration.getProvider() == null) {

            player.getServer().getLogger().severe(
                    "[RPGCore] 신규 플레이어 300골드 지급 실패: "
                            + player.getName()
                            + " - Vault Economy provider 없음"
            );

            player.sendMessage(
                    "§c[시작 골드] 골드 지급에 실패했습니다. 관리자에게 문의하세요."
            );

            return;
        }

        Economy economy = registration.getProvider();

        EconomyResponse response =
                economy.depositPlayer(player, 300.0D);

        if (!response.transactionSuccess()) {

            player.getServer().getLogger().severe(
                    "[RPGCore] 신규 플레이어 300골드 지급 실패: "
                            + player.getName()
                            + " - "
                            + response.errorMessage
            );

            player.sendMessage(
                    "§c[시작 골드] 골드 지급에 실패했습니다. 관리자에게 문의하세요."
            );

            return;
        }

        player.sendMessage(
                "§6[시작 골드] §f300골드가 지급되었습니다."
        );
    }

    public static void giveStarterKit(Player player) {
        PlayerInventory inventory = player.getInventory();

        // 기본 철 장비
        inventory.addItem(new ItemStack(Material.IRON_SWORD));
        inventory.addItem(new ItemStack(Material.IRON_PICKAXE));
        inventory.addItem(new ItemStack(Material.IRON_AXE));
        inventory.addItem(new ItemStack(Material.IRON_SHOVEL));
        inventory.addItem(new ItemStack(Material.IRON_HOE));

        // 기본 가죽 방어구 자동 장착
        inventory.setHelmet(new ItemStack(Material.LEATHER_HELMET));
        inventory.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
        inventory.setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
        inventory.setBoots(new ItemStack(Material.LEATHER_BOOTS));

        giveGuideBook(player);

        player.sendMessage(
                "§a[시작 장비] §f초보자용 기본 장비가 지급되었습니다."
        );
    }

    public static ItemStack createGuideBook() {

        ItemStack guideBook =
                new ItemStack(Material.WRITTEN_BOOK);

        org.bukkit.inventory.meta.BookMeta bookMeta =
                (org.bukkit.inventory.meta.BookMeta)
                        guideBook.getItemMeta();

        bookMeta.setTitle("모험가 가이드");
        bookMeta.setAuthor("모험가 월드");

        bookMeta.addPage(
                "§l1. 서버 소개§r\n\n모험가 월드에 오신 것을 환영합니다!\n\n몬스터 사냥과 던전 공략으로 성장하고, 농사와 모험으로 골드를 모아 장비를 갖추는 RPG 서버입니다.\n\n처음 스폰되는 마을회관에서 다양한 NPC를 만나보세요."
        );
        bookMeta.addPage(
                "§l쉐이더 이용 안내§r\n\n쉐이더 모드를 사용한다면 다음 두 모드를 함께 설치해 주세요.\n\n• Entity Texture Features (ETF)\n• Entity Model Features (EMF)\n\n미설치 시 일부 갑옷 외형이 불완전하게 표시될 수 있습니다."
        );
        bookMeta.addPage(
                "§l골드와 모험§r\n\n농작물을 수확하거나 던전을 클리어해 골드를 모을 수 있습니다.\n\n모은 골드로 NPC에게 아이템을 구매하고, 재료를 모아 장비를 제작해 보세요.\n\n성장할수록 더 강한 장비와 편의 기능을 이용할 수 있습니다."
        );
        bookMeta.addPage(
                "§l2. 레벨과 전직§r\n\n필드 몬스터를 사냥하거나 던전을 클리어하면 경험치를 얻습니다.\n\n10레벨을 달성하면 전사 또는 마법사로 전직할 수 있습니다.\n\n/class 전사 또는 /class 마법사 명령어로 원하는 직업을 선택하세요."
        );
        bookMeta.addPage(
                "§l3. 스킬 사용법§r\n\n스킬 아이콘을 아이템 슬롯 1~5번에 등록하세요.\n\n스킬을 사용할 때는 우클릭한 뒤 해당 숫자 키를 눌러 발동합니다.\n\n레벨이 오를수록 사용할 수 있는 스킬이 추가 개방됩니다."
        );
        bookMeta.addPage(
                "§l4. 던전 입장§r\n\n필드 곳곳에 있는 던전을 찾아보세요.\n\n던전 입장 게이트에 서 있으면 안내 문구가 표시되고 입장할 수 있습니다.\n\n던전 안의 몬스터를 처치하고 목표를 완료해 경험치와 보상을 획득하세요."
        );
        bookMeta.addPage(
                "§l5. 장비 제작§r\n\n처음 스폰되는 마을회관에서 NPC 상점을 이용할 수 있습니다.\n\n골드와 제작 재료를 모아 더 높은 단계의 장비를 제작하세요.\n\n장비를 강화하고 마법을 부여하면 전투 능력을 높일 수 있습니다."
        );
        bookMeta.addPage(
                "§l6. 농작물 생산 안내§r\n\n농작물을 재배하고 수확해 NPC에게 판매해 보세요.\n수확한 농작물의 가격은 시간에 따라 유동적으로 변경됩니다.\n같은 농작물이어도 때로는 더 비싼 가격에 판매하실 수 있습니다.\n\n골드를 모아 농경지를 늘리고, 더 좋은 도구들을 구매해 사용해 보세요.\n보다 효율적인 농작물 재배가 가능해집니다."
        );
        bookMeta.addPage(
                "§l모험을 시작하세요!§r\n\n먼저 지급된 기본 장비를 착용하고 마을회관의 NPC를 둘러보세요.\n\n던전을 공략하셔도 좋고, 농사를 지으셔도 좋습니다. 자신만의 모험을 즐겨주세요.\n\n즐거운 모험 되시길 바랍니다!"
        );

        guideBook.setItemMeta(bookMeta);

        return guideBook;
    }

    private static void giveGuideBook(Player player) {
        ItemStack guideBook = createGuideBook();

        java.util.Map<Integer, ItemStack> leftovers =
                player.getInventory().addItem(guideBook);

        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(
                    player.getLocation(),
                    leftover
            );
        }

        player.sendMessage(
                "§a[가이드] §f모험가 가이드가 지급되었습니다."
        );
    }
}
