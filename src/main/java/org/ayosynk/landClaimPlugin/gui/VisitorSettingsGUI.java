package org.ayosynk.landClaimPlugin.gui;

import org.ayosynk.landClaimPlugin.util.FoliaScheduler;
import net.kyori.adventure.text.Component;
import org.ayosynk.landClaimPlugin.LandClaimPlugin;
import org.ayosynk.landClaimPlugin.config.menus.VisitorSettingsConfig;
import org.ayosynk.landClaimPlugin.gui.framework.GuiItem;
import org.ayosynk.landClaimPlugin.gui.framework.PaginatedGui;
import org.ayosynk.landClaimPlugin.gui.framework.SlotDefinition;
import org.ayosynk.landClaimPlugin.models.ClaimProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VisitorSettingsGUI {

        public static void open(Player player, ClaimProfile profile, LandClaimPlugin plugin) {
                open(player, profile, plugin, "visitor");
        }

        public static void open(Player player, ClaimProfile profile, LandClaimPlugin plugin, String category) {
                open(player, profile, plugin, category, 0);
        }

        private static void open(Player player, ClaimProfile profile, LandClaimPlugin plugin, String category,
                        int page) {
                String role = category.toLowerCase(java.util.Locale.ROOT);
                if (!role.equals("resident") && !role.equals("trusted") && !role.equals("visitor")) {
                        throw new IllegalArgumentException("Unknown flag category: " + category);
                }
                if (!GuiHelper.checkMenuPermission(player, "flags", plugin)) {
                        return;
                }
                FoliaScheduler.runAsync(plugin, () -> {
                        VisitorSettingsConfig config = plugin.getConfigManager().getVisitorSettingsConfig();

                        List<GuiItem> contentItems = new ArrayList<>();

                        for (Map.Entry<String, VisitorSettingsConfig.ItemConfig> entry : config.flags.entrySet()) {
                                String flagId = entry.getKey();
                                if (flagId.equals("CLAIM_LAND") || flagId.equals("ADMIN_MENU")
                                        || flagId.equals("MANAGE_SETTINGS") || flagId.equals("MANAGE_MEMBERS")
                                        || flagId.equals("MANAGE_ROLES")) continue;
                                VisitorSettingsConfig.ItemConfig flagConfig = entry.getValue();

                                contentItems.add(new GuiItem() {
                                        @Override
                                        public ItemStack render(Player viewer) {
                                                boolean hasFlag = profile.hasCategoryFlag(role, flagId);
                                                ItemStack item = GuiHelper.buildItemStack(flagConfig.material,
                                                                flagConfig.name, flagConfig.lore, flagConfig.itemModel);
                                                ItemMeta meta = item.getItemMeta();
                                                if (meta != null) {
                                                        List<Component> lore = meta.lore();
                                                        if (lore == null)
                                                                lore = new ArrayList<>();

                                                        lore.add(Component.empty());
                                                        String statusText = hasFlag ? config.enabledStatus
                                                                        : config.disabledStatus;
                                                        lore.add(GuiHelper.MM.deserialize(statusText));
                                                        meta.lore(lore);

                                                        // Optionally add a glow if enabled
                                                        if (hasFlag) {
                                                                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING,
                                                                                1, true);
                                                                meta.addItemFlags(
                                                                                org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                                                        }
                                                        item.setItemMeta(meta);
                                                }
                                                return item;
                                        }

                                        @Override
                                        public org.ayosynk.landClaimPlugin.gui.framework.ClickAction clickAction() {
                                                return (p, e) -> {
                                                        if (role.equals("visitor")
                                                                        && plugin.getConfigManager().isVisitorSettingsLocked()) {
                                                                p.sendMessage(plugin.getConfigManager()
                                                                                .getMessage("visitor-settings-locked"));
                                                                return;
                                                        }
                                                        Set<String> flags = profile.getCategoryFlags(role);
                                                        if (!flags.remove(flagId.toUpperCase(java.util.Locale.ROOT)))
                                                                flags.add(flagId.toUpperCase(java.util.Locale.ROOT));

                                                        // Save asynchronously and invalidate cache
                                                        plugin.getDatabaseManager().getProfileDao().saveProfile(profile)
                                                                        .thenRun(() -> {
                                                                                if (plugin.getRedisManager() != null) {
                                                                                        plugin.getRedisManager()
                                                                                                        .publishUpdate("INVALIDATE_PROFILE",
                                                                                                                        profile.getProfileId());
                                                                                }
                                                                        });

                                                        // Refresh the current GUI page to show updated status
                                                        if (e.getView().getTopInventory()
                                                                        .getHolder() instanceof PaginatedGui currentGui) {
                                                                currentGui.setPage(currentGui.getCurrentPage(), p);
                                                        }
                                                };
                                        }
                                });
                        }

                        String[] structure = validateLayout(config, plugin);
                        if (structure == null) {
                                FoliaScheduler.runForPlayer(plugin, player, () -> player.sendMessage(
                                                GuiHelper.MM.deserialize("<red>Flags menu layout is invalid; check the plugin log.")));
                                return;
                        }

                        Map<Character, SlotDefinition> ingredients = new HashMap<>();
                        ingredients.put('F', new SlotDefinition(
                                        GuiHelper.buildItemStack(config.frame.material, config.frame.name,
                                                        config.frame.lore, config.frame.itemModel)));
                        ingredients.put('B', new SlotDefinition(
                                        GuiHelper.buildItemStack(config.back.material, config.back.name,
                                                        config.back.lore, config.back.itemModel),
                                        (p, e) -> MainMenuGUI.open(p, profile, plugin)));
                        VisitorSettingsConfig.ItemConfig selector = switch (role) {
                                case "resident" -> config.residentCategory;
                                case "trusted" -> config.trustedCategory;
                                default -> config.visitorCategory;
                        };
                        ingredients.put('C', GuiHelper.buildSlot(selector.material, selector.name,
                                        selector.lore, selector.itemModel, (p, e) -> {
                                                String nextRole = switch (role) {
                                                        case "resident" -> "trusted";
                                                        case "trusted" -> "visitor";
                                                        default -> "resident";
                                                };
                                                int currentPage = e.getView().getTopInventory()
                                                                .getHolder() instanceof PaginatedGui currentGui
                                                                                ? currentGui.getCurrentPage()
                                                                                : page;
                                                VisitorSettingsGUI.open(p, profile, plugin, nextRole, currentPage);
                                        }));

                        Component title = GuiHelper.MM.deserialize(config.title + " <gray>(" + role + ")");
                        PaginatedGui gui = new PaginatedGui(title, config.rows, structure, ingredients, 'x');

                        gui.setPrevButton(findSlot(structure, "P"),
                                        GuiHelper.buildItemStack(config.previousPage.material, config.previousPage.name,
                                        config.previousPage.lore, config.previousPage.itemModel),
                                        GuiHelper.buildItemStack(config.bottomFill.material, config.bottomFill.name,
                                        config.bottomFill.lore, config.bottomFill.itemModel));
                        gui.setNextButton(findSlot(structure, "N"),
                                        GuiHelper.buildItemStack(config.nextPage.material, config.nextPage.name,
                                        config.nextPage.lore, config.nextPage.itemModel),
                                        GuiHelper.buildItemStack(config.bottomFill.material, config.bottomFill.name,
                                                        config.bottomFill.lore, config.bottomFill.itemModel));

                        gui.setContent(contentItems, player);
                        if (page > 0) {
                                gui.setPage(page, player);
                        }
                        gui.open(player);
                });
        }

        private static String[] validateLayout(VisitorSettingsConfig config, LandClaimPlugin plugin) {
                return GuiLayoutValidator.validate(config.rows, config.layout,
                        Set.of("F", "C", "B", "P", "N", "x", "."),
                        "P", "N", "Flags", plugin);
        }

        private static int findSlot(String[] layout, String target) {
                return GuiLayoutValidator.findSlot(layout, target);
        }
}
