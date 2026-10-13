package org.ayosynk.landClaimPlugin.gui;

import net.kyori.adventure.text.Component;
import org.ayosynk.landClaimPlugin.LandClaimPlugin;
import org.ayosynk.landClaimPlugin.config.menus.BannedPlayersConfig;
import org.ayosynk.landClaimPlugin.gui.framework.GuiItem;
import org.ayosynk.landClaimPlugin.gui.framework.PaginatedGui;
import org.ayosynk.landClaimPlugin.gui.framework.SlotDefinition;
import org.ayosynk.landClaimPlugin.models.ClaimProfile;
import org.ayosynk.landClaimPlugin.util.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BannedPlayersGUI {

    private BannedPlayersGUI() {
    }

    public static void open(Player player, ClaimProfile profile, LandClaimPlugin plugin) {
        if (!GuiHelper.checkMenuPermission(player, "banlist", plugin)) {
            return;
        }
        if (!profile.canManage(player)) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-owner"));
            return;
        }
        FoliaScheduler.runForPlayer(plugin, player, () -> openOnPlayerThread(player, profile, plugin));
    }

    private static void openOnPlayerThread(Player player, ClaimProfile profile, LandClaimPlugin plugin) {
        BannedPlayersConfig config = plugin.getConfigManager().getBannedPlayersConfig();
        String[] structure = GuiLayoutValidator.validate(config.rows, config.layout,
                Set.of("x", "F", "B", "P", "N", "."), "P", "N", "Banned Players", plugin);
        if (structure == null) {
            player.sendMessage(GuiHelper.deserialize(
                    "<red>Banned Players layout is invalid; check the plugin log."));
            return;
        }

        List<GuiItem> entries = new ArrayList<>();
        for (UUID bannedId : profile.getBannedPlayers()) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(bannedId);
            String name = offlinePlayer.getName() == null ? bannedId.toString() : offlinePlayer.getName();
            entries.add(new GuiItem() {
                @Override
                public ItemStack render(Player viewer) {
                    Material material = Material.matchMaterial(config.bannedPlayer.material);
                    ItemStack item = new ItemStack(material == null ? Material.PLAYER_HEAD : material);
                    ItemMeta meta = item.getItemMeta();
                    if (meta instanceof SkullMeta skullMeta) {
                        skullMeta.setOwningPlayer(offlinePlayer);
                        meta = skullMeta;
                    }
                    if (meta != null) {
                        String itemName = config.bannedPlayer.name == null
                                ? name
                                : config.bannedPlayer.name.replace("{player}", name);
                        meta.displayName(GuiHelper.deserialize(itemName));
                        List<Component> lore = new ArrayList<>();
                        if (config.bannedPlayer.lore != null) {
                            for (String line : config.bannedPlayer.lore) {
                                lore.add(GuiHelper.deserialize(line.replace("{player}", name)));
                            }
                        }
                        meta.lore(lore);
                        GuiHelper.applyItemModel(meta, config.bannedPlayer.itemModel, "banned player");
                        item.setItemMeta(meta);
                    }
                    return item;
                }

                @Override
                public org.ayosynk.landClaimPlugin.gui.framework.ClickAction clickAction() {
                    return (clicker, event) -> ConfirmationGUI.open(clicker,
                            "<red>Unban " + name + "?",
                            "<gray>Allow this player to enter the claim again.",
                            () -> {
                                if (!profile.isBanned(bannedId)) {
                                    open(clicker, profile, plugin);
                                    return;
                                }
                                profile.removeBannedPlayer(bannedId);
                                plugin.getClaimManager().saveAndSync(profile);
                                clicker.sendMessage(plugin.getConfigManager().getMessage(
                                        "player-unbanned", "<player>", name));
                                Player target = Bukkit.getPlayer(bannedId);
                                if (target != null) {
                                    FoliaScheduler.runForPlayer(plugin, target, () -> target.sendMessage(
                                            plugin.getConfigManager().getMessage(
                                                    "unbanned-by-owner", "<owner>", clicker.getName())));
                                }
                                open(clicker, profile, plugin);
                            },
                            () -> open(clicker, profile, plugin));
                }
            });
        }
        if (entries.isEmpty()) {
            entries.add(new GuiItem() {
                @Override
                public ItemStack render(Player viewer) {
                    return GuiHelper.buildItemStack(config.empty.material, config.empty.name,
                            config.empty.lore, config.empty.itemModel);
                }

                @Override
                public org.ayosynk.landClaimPlugin.gui.framework.ClickAction clickAction() {
                    return (clicker, event) -> { };
                }
            });
        }

        Map<Character, SlotDefinition> ingredients = new HashMap<>();
        ingredients.put('F', GuiHelper.buildSlot(config.filler.material, config.filler.name,
                config.filler.lore, config.filler.itemModel));
        ingredients.put('B', GuiHelper.buildSlot(config.back.material, config.back.name,
                config.back.lore, config.back.itemModel,
                (clicker, event) -> MainMenuGUI.open(clicker, profile, plugin)));

        PaginatedGui gui = new PaginatedGui(GuiHelper.deserialize(config.title), config.rows,
                structure, ingredients, 'x');
        gui.setPrevButton(GuiLayoutValidator.findSlot(structure, "P"),
                GuiHelper.buildItemStack(config.previousPage.material, config.previousPage.name,
                        config.previousPage.lore, config.previousPage.itemModel),
                GuiHelper.buildItemStack(config.filler.material, config.filler.name,
                        config.filler.lore, config.filler.itemModel));
        gui.setNextButton(GuiLayoutValidator.findSlot(structure, "N"),
                GuiHelper.buildItemStack(config.nextPage.material, config.nextPage.name,
                        config.nextPage.lore, config.nextPage.itemModel),
                GuiHelper.buildItemStack(config.filler.material, config.filler.name,
                        config.filler.lore, config.filler.itemModel));
        gui.setContent(entries, player);
        gui.open(player);
    }
}
