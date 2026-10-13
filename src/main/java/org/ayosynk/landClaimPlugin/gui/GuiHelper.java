package org.ayosynk.landClaimPlugin.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.ayosynk.landClaimPlugin.gui.framework.ClickAction;
import org.ayosynk.landClaimPlugin.gui.framework.SlotDefinition;
import org.ayosynk.landClaimPlugin.models.ClaimProfile;
import org.ayosynk.landClaimPlugin.LandClaimPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared GUI utility class — eliminates duplicated item-building boilerplate
 * across all GUI files. Returns plain Bukkit {@link ItemStack} instances.
 */
public final class GuiHelper {

    /** Cached MiniMessage singleton — avoids re-creation per call. */
    public static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Pattern LEGACY_HEX = Pattern.compile("(?i)§x((?:§[0-9a-f]){6})");
    private static final Pattern LEGACY_CODE = Pattern.compile("(?i)§([0-9a-fk-or])");
    private static final Map<Character, String> LEGACY_TAGS = Map.ofEntries(
            Map.entry('0', "<black>"), Map.entry('1', "<dark_blue>"), Map.entry('2', "<dark_green>"),
            Map.entry('3', "<dark_aqua>"), Map.entry('4', "<dark_red>"), Map.entry('5', "<dark_purple>"),
            Map.entry('6', "<gold>"), Map.entry('7', "<gray>"), Map.entry('8', "<dark_gray>"),
            Map.entry('9', "<blue>"), Map.entry('a', "<green>"), Map.entry('b', "<aqua>"),
            Map.entry('c', "<red>"), Map.entry('d', "<light_purple>"), Map.entry('e', "<yellow>"),
            Map.entry('f', "<white>"), Map.entry('k', "<obfuscated>"), Map.entry('l', "<bold>"),
            Map.entry('m', "<strikethrough>"), Map.entry('n', "<underlined>"), Map.entry('o', "<italic>"),
            Map.entry('r', "<reset>"));

    /** Pre-allocated ItemFlag array — avoids ItemFlag.values() clone per item. */
    private static final ItemFlag[] ALL_FLAGS = ItemFlag.values();

    private GuiHelper() {
    }

    // --- Non-placeholder variants ---

    public static ItemStack buildItemStack(String material, String name, List<String> lore) {
        return buildItemStack(material, name, lore, null);
    }

    public static ItemStack buildItemStack(String material, String name, List<String> lore, String itemModel) {
        return buildItemStackInternal(material, name, lore, itemModel);
    }

    // --- Placeholder variants (for GUIs that inject claim/player data) ---

    public static ItemStack buildItemStack(String material, String name, List<String> lore,
            ClaimProfile profile, Player player, String ownerName, String claimName) {
        return buildItemStack(material, name, lore, profile, player, ownerName, claimName, null);
    }

    public static ItemStack buildItemStack(String material, String name, List<String> lore,
            ClaimProfile profile, Player player, String ownerName, String claimName, String itemModel) {
        String resolvedName = name != null ? replacePlaceholders(name, profile, player, ownerName, claimName) : null;
        List<String> resolvedLore = null;
        if (lore != null && !lore.isEmpty()) {
            resolvedLore = new ArrayList<>(lore.size());
            for (String line : lore) {
                resolvedLore.add(replacePlaceholders(line, profile, player, ownerName, claimName));
            }
        }
        return buildItemStackInternal(material, resolvedName, resolvedLore, itemModel);
    }

    // --- SlotDefinition convenience builders ---

    public static SlotDefinition buildSlot(String material, String name, List<String> lore) {
        return new SlotDefinition(buildItemStack(material, name, lore));
    }

    public static SlotDefinition buildSlot(String material, String name, List<String> lore, String itemModel) {
        return new SlotDefinition(buildItemStack(material, name, lore, itemModel));
    }

    public static SlotDefinition buildSlot(String material, String name, List<String> lore, ClickAction action) {
        return new SlotDefinition(buildItemStack(material, name, lore), action);
    }

    public static SlotDefinition buildSlot(String material, String name, List<String> lore, String itemModel,
            ClickAction action) {
        return new SlotDefinition(buildItemStack(material, name, lore, itemModel), action);
    }

    public static SlotDefinition buildSlot(String material, String name, List<String> lore,
            ClaimProfile profile, Player player, String ownerName, String claimName) {
        return new SlotDefinition(buildItemStack(material, name, lore, profile, player, ownerName, claimName));
    }

    public static SlotDefinition buildSlot(String material, String name, List<String> lore,
            ClaimProfile profile, Player player, String ownerName, String claimName, String itemModel) {
        return new SlotDefinition(
                buildItemStack(material, name, lore, profile, player, ownerName, claimName, itemModel));
    }

    public static SlotDefinition buildSlot(String material, String name, List<String> lore,
            ClaimProfile profile, Player player, String ownerName, String claimName, ClickAction action) {
        return new SlotDefinition(buildItemStack(material, name, lore, profile, player, ownerName, claimName), action);
    }

    public static SlotDefinition buildSlot(String material, String name, List<String> lore,
            ClaimProfile profile, Player player, String ownerName, String claimName, String itemModel,
            ClickAction action) {
        return new SlotDefinition(
                buildItemStack(material, name, lore, profile, player, ownerName, claimName, itemModel), action);
    }

    // --- Placeholder resolution ---

    public static String replacePlaceholders(String text, ClaimProfile profile, Player player,
            String ownerName, String claimName) {
        return text.replace("{claim_name}", claimName)
                .replace("<claim_name>", claimName)
                .replace("{owner}", ownerName)
                .replace("<owner>", ownerName)
                .replace("{size}", String.valueOf(profile.getOwnedChunks().size()))
                .replace("<size>", String.valueOf(profile.getOwnedChunks().size()))
                .replace("{members}", String.valueOf(profile.getMemberRoles().size()))
                .replace("<members>", String.valueOf(profile.getMemberRoles().size()))
                .replace("{world}", player.getWorld().getName())
                .replace("<world>", player.getWorld().getName())
                .replace("{x}", String.valueOf(player.getLocation().getBlockX()))
                .replace("<x>", String.valueOf(player.getLocation().getBlockX()))
                .replace("{z}", String.valueOf(player.getLocation().getBlockZ()))
                .replace("<z>", String.valueOf(player.getLocation().getBlockZ()));
    }

    // --- Core builder logic ---

    private static ItemStack buildItemStackInternal(String materialName, String name, List<String> lore,
            String itemModel) {
        Material mat = Material.matchMaterial(materialName.toUpperCase());
        if (mat == null)
            mat = Material.STONE;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        // Hide all default tooltip attributes
        meta.addItemFlags(ALL_FLAGS);

        // Strip default attributes to shrink the NMS component map
        try {
            meta.setAttributeModifiers(
                    com.google.common.collect.LinkedListMultimap.create());
        } catch (Exception ignored) {
        }

        if (name != null && !name.isEmpty()) {
            Component comp = deserialize("<i:false>" + name);
            meta.displayName(comp);
        }

        if (lore != null && !lore.isEmpty()) {
            List<Component> loreComponents = new ArrayList<>(lore.size());
            for (String line : lore) {
                loreComponents.add(deserialize("<i:false>" + line));
            }
            meta.lore(loreComponents);
        }

        applyItemModel(meta, itemModel, materialName);

        item.setItemMeta(meta);
        return item;
    }

    public static Component deserialize(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        String normalized = normalizeLegacyFormatting(text);
        return MM.deserialize(normalized);
    }

    private static String normalizeLegacyFormatting(String text) {
        Matcher hexMatcher = LEGACY_HEX.matcher(text);
        StringBuffer normalized = new StringBuffer();
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1).replace("§", "");
            hexMatcher.appendReplacement(normalized, Matcher.quoteReplacement("<#" + hex + ">"));
        }
        hexMatcher.appendTail(normalized);

        Matcher codeMatcher = LEGACY_CODE.matcher(normalized.toString());
        StringBuffer result = new StringBuffer();
        while (codeMatcher.find()) {
            String tag = LEGACY_TAGS.get(codeMatcher.group(1).toLowerCase(Locale.ROOT).charAt(0));
            codeMatcher.appendReplacement(result, Matcher.quoteReplacement(tag));
        }
        codeMatcher.appendTail(result);
        return result.toString();
    }

    public static void applyItemModel(ItemMeta meta, String itemModel, String itemName) {
        if (itemModel == null || itemModel.isBlank()) return;
        org.bukkit.NamespacedKey model = org.bukkit.NamespacedKey.fromString(itemModel);
        if (model == null) {
            LandClaimPlugin plugin = LandClaimPlugin.getInstance();
            if (plugin != null) {
                plugin.getLogger().warning("Invalid item_model '" + itemModel + "' for GUI item '"
                        + itemName + "'. Expected a namespaced key such as 'minecraft:my_model'.");
            }
            return;
        }
        meta.setItemModel(model);
    }

    public static boolean checkMenuPermission(Player player, String menuName, LandClaimPlugin plugin) {
        if (!plugin.getConfigManager().useSeparatePremission()) {
            return true;
        }
        if (player.hasPermission("landclaim.admin") || player.hasPermission("landclaim.menu.*") || player.hasPermission("landclaim.menu." + menuName.toLowerCase())) {
            return true;
        }
        player.sendMessage(plugin.getConfigManager().getMessage("access-denied"));
        return false;
    }

    public static boolean checkPermission(Player player, String permission, LandClaimPlugin plugin) {
        if (!plugin.getConfigManager().useSeparatePremission()) {
            return true;
        }
        if (player.hasPermission("landclaim.admin") || player.hasPermission(permission)) {
            return true;
        }
        player.sendMessage(plugin.getConfigManager().getMessage("access-denied"));
        return false;
    }
}
