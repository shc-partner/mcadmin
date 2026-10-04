/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hcs.rpgcore.RPGCorePlugin
 *  com.hcs.rpgcore.item.CustomItemFactory$1
 *  com.hcs.rpgcore.item.RPGItemDefinition
 *  com.hcs.rpgcore.item.RPGItemRepository
 *  com.hcs.rpgcore.stat.ItemStatKeys
 *  io.papermc.paper.datacomponent.DataComponentBuilder
 *  io.papermc.paper.datacomponent.DataComponentTypes
 *  io.papermc.paper.datacomponent.item.Consumable
 *  io.papermc.paper.datacomponent.item.FoodProperties
 *  io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.TextDecoration
 *  net.kyori.adventure.text.minimessage.MiniMessage
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Registry
 *  org.bukkit.attribute.Attribute
 *  org.bukkit.attribute.AttributeModifier
 *  org.bukkit.attribute.AttributeModifier$Operation
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.EquipmentSlotGroup
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.ItemType
 *  org.bukkit.inventory.meta.Damageable
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.components.EquippableComponent
 *  org.bukkit.persistence.PersistentDataContainer
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 */
package com.hcs.rpgcore.item;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.item.CustomItemFactory;
import com.hcs.rpgcore.item.RPGItemDefinition;
import com.hcs.rpgcore.item.RPGItemRepository;
import com.hcs.rpgcore.stat.ItemStatKeys;
import io.papermc.paper.datacomponent.DataComponentBuilder;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class CustomItemFactory {
    private final RPGCorePlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final NamespacedKey customItemIdKey;
    private final NamespacedKey abilityGlideKey;
    private final NamespacedKey dungeonRewardTypeKey;
    private final ItemStatKeys itemStatKeys;
    private final RPGItemRepository itemRepository;
    private final Map<String, RPGItemDefinition> itemDefinitions = new LinkedHashMap<String, RPGItemDefinition>();
    private final Map<String, Map<String, Integer>> enchantmentDefinitions = new LinkedHashMap<String, Map<String, Integer>>();

    public CustomItemFactory(RPGCorePlugin plugin) {
        this.plugin = plugin;
        this.itemRepository = new RPGItemRepository(plugin.getDatabaseManager());
        this.customItemIdKey = new NamespacedKey((Plugin)plugin, "custom_item_id");
        this.abilityGlideKey = new NamespacedKey((Plugin)plugin, "ability_glide");
        this.dungeonRewardTypeKey = new NamespacedKey((Plugin)plugin, "dungeon_reward_type");
        this.itemStatKeys = new ItemStatKeys(plugin);
        this.load();
    }

    public void load() {
        this.itemDefinitions.clear();
        this.enchantmentDefinitions.clear();
        try {
            for (RPGItemDefinition definition : this.itemRepository.findAllEnabled()) {
                String normalizedId = this.normalizeItemId(definition.itemId());
                this.itemDefinitions.put(normalizedId, definition);
            }
            this.enchantmentDefinitions.putAll(this.itemRepository.findAllEnchantments());
            this.plugin.getLogger().info("RPG item definitions loaded from structured DB: " + this.itemDefinitions.size());
        }
        catch (Exception exception) {
            throw new IllegalStateException("Failed to load RPG items from structured MariaDB columns", exception);
        }
    }

    public ItemStack create(String itemId) {
        List<Component> list;
        Map<String, Integer> enchantments;
        NamespacedKey modelKey;
        String itemModel;
        Material material;
        RPGItemDefinition definition;
        String normalizedItemId = this.normalizeItemId(itemId);
        RPGItemDefinition rPGItemDefinition = definition = normalizedItemId == null ? null : this.itemDefinitions.get(normalizedItemId);
        if (definition == null) {
            this.plugin.getLogger().warning("\uc874\uc7ac\ud558\uc9c0 \uc54a\ub294 \ucee4\uc2a4\ud140 \uc544\uc774\ud15c ID: " + itemId);
            return null;
        }
        String materialName = definition.material();
        if (materialName == null || materialName.isBlank()) {
            materialName = "PAPER";
        }
        if ((material = Material.matchMaterial((String)materialName)) == null) {
            this.plugin.getLogger().warning("\uc798\ubabb\ub41c Material: " + materialName + " / item=" + itemId);
            return null;
        }
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (definition.unbreakable()) {
            meta.setUnbreakable(true);
        }
        String displayName = this.getRarityColor(definition.rarity()) + definition.displayName();
        meta.displayName(this.miniMessage.deserialize(displayName).decoration(TextDecoration.ITALIC, false));
        meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
        EquipmentSlot equipmentSlot = this.parseEquipmentSlot(definition.equipmentSlot(), itemId);
        /*
         * 방패는 바닐라 SHIELD의 사용 동작을 유지한다.
         * OFF_HAND equippable 컴포넌트를 별도로 덮어쓰지 않는다.
         */
        if (equipmentSlot != null && material != Material.SHIELD) {
            EquippableComponent equippable = meta.getEquippable();
            String equipmentModel = definition.equipmentModel();
            if (equipmentModel != null && !equipmentModel.isBlank()) {
                NamespacedKey modelKey2 = NamespacedKey.fromString((String)equipmentModel);
                if (modelKey2 == null) {
                    throw new IllegalArgumentException("Invalid equipment model: " + equipmentModel);
                }
                equippable.setModel(modelKey2);
            }
            equippable.setSlot(equipmentSlot);
            meta.setEquippable(equippable);
        }
        if ((itemModel = definition.itemModel()) != null && !itemModel.isBlank() && (modelKey = NamespacedKey.fromString((String)itemModel)) != null) {
            meta.setItemModel(modelKey);
        }
        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable)meta;
            if (definition.maxDurability() != null && definition.maxDurability() > 0) {
                damageable.setMaxDamage(definition.maxDurability());
                damageable.setDamage(0);
            }
        }
        if ((enchantments = this.enchantmentDefinitions.get(definition.itemId())) != null) {
            for (Map.Entry entry : enchantments.entrySet()) {
                String enchantmentName = (String)entry.getKey();
                int level = (Integer)entry.getValue();
                if (level <= 0) continue;
                NamespacedKey enchantmentKey = NamespacedKey.fromString((String)(enchantmentName.contains(":") ? enchantmentName : "minecraft:" + enchantmentName));
                if (enchantmentKey == null) {
                    throw new IllegalArgumentException("Invalid enchantment key: " + enchantmentName);
                }
                Enchantment enchantment = (Enchantment)Registry.ENCHANTMENT.get(enchantmentKey);
                if (enchantment == null) {
                    throw new IllegalArgumentException("Unknown enchantment: " + enchantmentName);
                }
                meta.addEnchant(enchantment, level, true);
            }
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(this.customItemIdKey, PersistentDataType.STRING, definition.itemId());
        if (definition.glide()) {
            meta.setGlider(true);
            pdc.set(this.abilityGlideKey, PersistentDataType.BYTE, (byte) 1);
        }
        if (definition.maxMana() != null && definition.maxMana() != 0.0) {
            pdc.set(this.itemStatKeys.maxManaKey(), PersistentDataType.DOUBLE, definition.maxMana());
        }
        if (definition.damageReductionFlat() != null && definition.damageReductionFlat() != 0.0) {
            pdc.set(this.itemStatKeys.damageReductionFlatKey(), PersistentDataType.DOUBLE, definition.damageReductionFlat());
        }
        if (definition.weaponAttack() != null) {
            double d = this.getVanillaAttack(material);
            double bonusAttack = definition.weaponAttack() - d;
            pdc.set(this.itemStatKeys.attackKey(), PersistentDataType.DOUBLE, bonusAttack);
        }
        if (definition.weaponAttackSpeed() != null) {
            double d = this.getVanillaAttackSpeed(material);
            double bonusAttackSpeed = definition.weaponAttackSpeed() - d;
            meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(new NamespacedKey((Plugin)this.plugin, "custom_attack_speed"), bonusAttackSpeed, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        }
        if (definition.armorBonus() != null || definition.toughnessBonus() != null) {
            if (equipmentSlot == null) {
                throw new IllegalArgumentException("Armor attribute requires equipment_slot: " + definition.itemId());
            }
            EquipmentSlotGroup equipmentSlotGroup = this.getArmorSlotGroup(equipmentSlot);
            double vanillaArmor = this.getVanillaEquipmentAttribute(material, equipmentSlot, Attribute.ARMOR);
            double vanillaToughness = this.getVanillaEquipmentAttribute(material, equipmentSlot, Attribute.ARMOR_TOUGHNESS);
            double vanillaKnockbackResistance = this.getVanillaEquipmentAttribute(material, equipmentSlot, Attribute.KNOCKBACK_RESISTANCE);
            double finalArmor = vanillaArmor + this.valueOrZero(definition.armorBonus());
            double finalToughness = vanillaToughness + this.valueOrZero(definition.toughnessBonus());
            meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey((Plugin)this.plugin, definition.itemId() + "_armor"), finalArmor, AttributeModifier.Operation.ADD_NUMBER, equipmentSlotGroup));
            meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(new NamespacedKey((Plugin)this.plugin, definition.itemId() + "_armor_toughness"), finalToughness, AttributeModifier.Operation.ADD_NUMBER, equipmentSlotGroup));
            if (vanillaKnockbackResistance != 0.0) {
                meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, new AttributeModifier(new NamespacedKey((Plugin)this.plugin, definition.itemId() + "_knockback_resistance"), vanillaKnockbackResistance, AttributeModifier.Operation.ADD_NUMBER, equipmentSlotGroup));
            }
        }
        if (!(list = this.buildLore(definition)).isEmpty()) {
            meta.lore(list);
        }
        item.setItemMeta(meta);
        if (definition.consumableEnabled()) {
            double configuredSeconds = definition.consumableSeconds() == null ? 1.6 : definition.consumableSeconds();
            float consumeSeconds = (float)configuredSeconds;
            item.setData(DataComponentTypes.FOOD, (DataComponentBuilder)FoodProperties.food().nutrition(0).saturation(0.0f).canAlwaysEat(true));
            item.setData(DataComponentTypes.CONSUMABLE, (DataComponentBuilder)Consumable.consumable().consumeSeconds(consumeSeconds).animation(ItemUseAnimation.EAT).hasConsumeParticles(true));
        }
        return item;
    }

    private List<Component> buildLore(RPGItemDefinition definition) {
        boolean hasEquipmentStats;
        ArrayList<String> lines = new ArrayList<String>();
        lines.add(this.getRarityColor(definition.rarity()) + "[" + definition.rarity() + "]");
        ArrayList<String> descriptions = new ArrayList<String>();
        this.addIfPresent(descriptions, definition.description());
        this.addIfPresent(descriptions, definition.description2());
        this.addIfPresent(descriptions, definition.description3());
        if (!descriptions.isEmpty()) {
            this.addBlank(lines);
            for (String description : descriptions) {
                lines.add("<gray>" + description);
            }
        }
        List<String> effects = new ArrayList<>();
        if (definition.specialEffect1() != null) {
            effects.add(this.formatSpecialEffect(definition.specialEffect1()));
        }
        if (definition.specialEffect2() != null) {
            effects.add(this.formatSpecialEffect(definition.specialEffect2()));
        }
        if (definition.damageReductionFlat() != null) {
            effects.add("<#55FFFF>\ubc1b\ub294 \ud53c\ud574 \uac10\uc18c +" + this.formatNumber(definition.damageReductionFlat()));
        }
        if (definition.glide()) {
            effects.add("<#55FFFF>\uacf5\uc911\uc5d0\uc11c \ud65c\uacf5 \uac00\ub2a5");
        }
        if (!effects.isEmpty()) {
            this.addBlank(lines);
            lines.addAll(effects);
        }
        if (definition.displayType() != null && !definition.displayType().isBlank()) {
            this.addBlank(lines);
            lines.add("<gray>\uc720\ud615:");
            lines.add("<#55FFFF>" + definition.displayType());
        }
        if (definition.restoreHpPercent() != null || definition.restoreMpPercent() != null) {
            this.addBlank(lines);
            lines.add("<gray>\uc12d\ucde8 \uc2dc:");
            if (definition.restoreHpPercent() != null) {
                lines.add("<#FF5555>\ucd5c\ub300 HP\uc758 " + this.formatNumber(definition.restoreHpPercent()) + "% \ud68c\ubcf5");
            }
            if (definition.restoreMpPercent() != null) {
                lines.add("<#5555FF>\ucd5c\ub300 MP\uc758 " + this.formatNumber(definition.restoreMpPercent()) + "% \ud68c\ubcf5");
            }
        }
        boolean bl = hasEquipmentStats = definition.weaponAttack() != null || definition.weaponAttackSpeed() != null || definition.displayArmor() != null || definition.displayToughness() != null || definition.displayKnockbackResistance() != null || definition.maxMana() != null;
        if (hasEquipmentStats) {
            this.addBlank(lines);
            lines.add("<gray>" + this.getEquipmentHeader(definition.equipmentSlot(), definition.weaponAttack() != null || definition.weaponAttackSpeed() != null));
            if (definition.maxMana() != null) {
                lines.add("<#55FFFF>\ucd5c\ub300 MP +" + this.formatNumber(definition.maxMana()));
            }
            if (definition.weaponAttack() != null) {
                lines.add("<#00AA00>" + this.formatNumber(definition.weaponAttack()) + " \uacf5\uaca9 \ud53c\ud574");
            }
            if (definition.weaponAttackSpeed() != null) {
                lines.add("<#00AA00>" + this.formatNumber(definition.weaponAttackSpeed()) + " \uacf5\uaca9 \uc18d\ub3c4");
            }
            if (definition.displayArmor() != null) {
                lines.add("<#5555FF>+" + this.formatNumber(definition.displayArmor()) + " \ubc29\uc5b4");
            }
            if (definition.displayToughness() != null) {
                lines.add("<#5555FF>+" + this.formatNumber(definition.displayToughness()) + " \ubc29\uc5b4 \uac15\ub3c4");
            }
            if (definition.displayKnockbackResistance() != null) {
                lines.add("<#5555FF>+" + this.formatNumber(definition.displayKnockbackResistance()) + " \ubc00\uce68 \uc800\ud56d");
            }
        }
        if (definition.unbreakable()) {
            this.addBlank(lines);
            lines.add("<#FF55FF>\ud30c\uad34 \ubd88\uac00");
        }
        ArrayList<Component> result = new ArrayList<Component>();
        for (String line : lines) {
            Component component = line.isEmpty() ? Component.empty() : this.miniMessage.deserialize(line);
            result.add(component.decoration(TextDecoration.ITALIC, false));
        }
        return result;
    }

    private String formatSpecialEffect(String value) {
        if (value.startsWith("\ud654\uc5fc \ud53c\ud574 \uac10\uc18c")) {
            return "<#FFAA00>" + value;
        }
        if (value.equals("\ucc29\uc6a9 \uc2dc \ud53c\uae00\ub9b0 \uc911\ub9bd\ud654")) {
            return "<#FFD700>" + value;
        }
        return "<#55FFFF>" + value;
    }

    private String getEquipmentHeader(String slot, boolean weapon) {
        if (weapon) {
            return "\uc8fc\ub85c \uc0ac\uc6a9\ud558\ub294 \uc190\uc5d0 \uc788\uc744 \ub54c:";
        }
        if (slot == null) {
            return "\uc7a5\ucc29\ud588\uc744 \ub54c:";
        }
        return switch (slot.trim().toUpperCase()) {
            case "HEAD" -> "\uba38\ub9ac\uc5d0 \uc788\uc744 \ub54c:";
            case "CHEST" -> "\ud749\ubd80\uc5d0 \uc788\uc744 \ub54c:";
            case "LEGS" -> "\ub2e4\ub9ac\uc5d0 \uc788\uc744 \ub54c:";
            case "FEET" -> "\ubc1c\uc5d0 \uc788\uc744 \ub54c:";
            case "HAND" -> "\uc8fc\ub85c \uc0ac\uc6a9\ud558\ub294 \uc190\uc5d0 \uc788\uc744 \ub54c:";
            case "OFF_HAND" -> "\ubcf4\uc870 \uc190\uc5d0 \uc788\uc744 \ub54c:";
            default -> "\uc7a5\ucc29\ud588\uc744 \ub54c:";
        };
    }

    private EquipmentSlot parseEquipmentSlot(String value, String itemId) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return EquipmentSlot.valueOf((String)value.trim().toUpperCase());
        }
        catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid equipment slot: " + value + " / item=" + itemId, exception);
        }
    }

    private EquipmentSlotGroup getArmorSlotGroup(
            EquipmentSlot slot
    ) {

        return switch (slot) {

            case HEAD ->
                    EquipmentSlotGroup.HEAD;

            case CHEST ->
                    EquipmentSlotGroup.CHEST;

            case LEGS ->
                    EquipmentSlotGroup.LEGS;

            case FEET ->
                    EquipmentSlotGroup.FEET;

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported armor slot: "
                                    + slot
                    );
        };
    }



    private double getVanillaEquipmentAttribute(Material material, EquipmentSlot slot, Attribute attribute) {
        ItemType itemType = material.asItemType();
        if (itemType == null) {
            return 0.0;
        }
        double total = 0.0;
        for (AttributeModifier modifier : itemType.getDefaultAttributeModifiers(slot).get(attribute)) {
            if (modifier.getOperation() != AttributeModifier.Operation.ADD_NUMBER) continue;
            total += modifier.getAmount();
        }
        return total;
    }

    private double getVanillaAttackSpeed(Material material) {
        ItemType itemType = material.asItemType();
        if (itemType == null) {
            return 4.0;
        }
        double total = 4.0;
        for (AttributeModifier modifier : itemType.getDefaultAttributeModifiers(EquipmentSlot.HAND).get(Attribute.ATTACK_SPEED)) {
            if (modifier.getOperation() != AttributeModifier.Operation.ADD_NUMBER) continue;
            total += modifier.getAmount();
        }
        return total;
    }

    private double getVanillaAttack(Material material) {
        ItemType itemType = material.asItemType();
        if (itemType == null) {
            return 0.0;
        }
        double total = 0.0;
        boolean hasAttackDamage = false;
        for (AttributeModifier modifier : itemType.getDefaultAttributeModifiers(EquipmentSlot.HAND).get(Attribute.ATTACK_DAMAGE)) {
            if (modifier.getOperation() != AttributeModifier.Operation.ADD_NUMBER) continue;
            total += modifier.getAmount();
            hasAttackDamage = true;
        }
        if (hasAttackDamage) {
            total += 1.0;
        }
        return total;
    }

    public boolean isCustomItem(ItemStack item) {
        return this.getItemId(item) != null;
    }

    public boolean isCustomItem(ItemStack item, String itemId) {
        String currentId = this.getItemId(item);
        return currentId != null && currentId.equals(itemId);
    }

    public String getItemId(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return (String)meta.getPersistentDataContainer().get(this.customItemIdKey, PersistentDataType.STRING);
    }

    private String getRarityColor(String rarity) {
        if (rarity == null) {
            return "<#FFFFFF>";
        }
        return switch (rarity) {
            case "\uace0\uae09" -> "<#55FF55>";
            case "\ud76c\uadc0" -> "<#55FFFF>";
            case "\uc601\uc6c5" -> "<#FF55FF>";
            case "\uc804\uc124" -> "<#FFAA00>";
            case "\uc2e0\ud654" -> "<#FDE879>";
            default -> "<#FFFFFF>";
        };
    }

    private String normalizeItemId(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private void addIfPresent(List<String> list, String value) {
        if (value != null && !value.isBlank()) {
            list.add(value);
        }
    }

    private void addBlank(List<String> lines) {
        if (!lines.isEmpty() && !lines.get(lines.size() - 1).isEmpty()) {
            lines.add("");
        }
    }

    private String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }
}
