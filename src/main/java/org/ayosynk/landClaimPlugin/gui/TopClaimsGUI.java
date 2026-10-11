package org.ayosynk.landClaimPlugin.gui;

import net.kyori.adventure.text.Component;
import org.ayosynk.landClaimPlugin.LandClaimPlugin;
import org.ayosynk.landClaimPlugin.config.menus.TopClaimsConfig;
import org.ayosynk.landClaimPlugin.gui.framework.CustomGui;
import org.ayosynk.landClaimPlugin.gui.framework.SlotDefinition;
import org.ayosynk.landClaimPlugin.models.ClaimProfile;
import org.ayosynk.landClaimPlugin.util.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TopClaimsGUI {

    private TopClaimsGUI() {
    }

    public static void open(Player player, ClaimProfile viewedProfile, LandClaimPlugin plugin) {
        if (!GuiHelper.checkMenuPermission(player, "top", plugin)) {
            return;
        }
        FoliaScheduler.runForPlayer(plugin, player, () -> openOnPlayerThread(player, viewedProfile, plugin));
    }

    private static void openOnPlayerThread(Player player, ClaimProfile viewedProfile, LandClaimPlugin plugin) {
        TopClaimsConfig config = plugin.getConfigManager().getTopClaimsConfig();
        String[] layout = validateLayout(config, plugin);
        if (layout == null) {
            player.sendMessage(GuiHelper.MM.deserialize(
                    "<red>Top Claims layout is invalid; check the plugin log."));
            return;
        }

        List<ClaimProfile> rankedProfiles = plugin.getClaimManager().getAllProfiles().stream()
                .filter(profile -> !ClaimProfile.ADMIN_PROFILE_ID.equals(profile.getProfileId()))
                .filter(profile -> !profile.getOwnedChunks().isEmpty())
                .sorted(Comparator
                        .comparingInt((ClaimProfile profile) -> profile.getOwnedChunks().size()).reversed()
                        .thenComparing(profile -> profile.getName() == null ? "" : profile.getName(),
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(profile -> String.valueOf(profile.getOwnerId())))
                .toList();

        Map<UUID, Integer> ranks = new HashMap<>();
        for (int index = 0; index < rankedProfiles.size(); index++) {
            ranks.put(rankedProfiles.get(index).getProfileId(), index + 1);
        }

        Map<Character, SlotDefinition> ingredients = new HashMap<>();
        ingredients.put('F', new SlotDefinition(GuiHelper.buildItemStack(
                config.filler.material, config.filler.name, config.filler.lore, config.filler.itemModel)));
        ingredients.put('V', new SlotDefinition(buildViewerItem(config, viewedProfile, ranks)));
        ingredients.put('B', GuiHelper.buildSlot(config.back.material, config.back.name,
                config.back.lore, config.back.itemModel,
                (p, e) -> MainMenuGUI.open(p, viewedProfile, plugin)));

        Component title = GuiHelper.MM.deserialize(config.title);
        CustomGui gui = new CustomGui(title, config.rows);
        gui.fillFromStructure(layout, ingredients);

        List<Integer> entrySlots = findSlots(layout, "x");
        for (int index = 0; index < entrySlots.size(); index++) {
            if (index >= rankedProfiles.size() || index >= 10) {
                break;
            }
            ClaimProfile claim = rankedProfiles.get(index);
            int rank = index + 1;
            ItemStack head = buildEntryHead(config, claim, rank);
            gui.setItem(entrySlots.get(index), head);
        }
        gui.open(player);
    }

    private static ItemStack buildEntryHead(TopClaimsConfig config, ClaimProfile claim, int rank) {
        String owner = claim.getDisplayOwnerName();
        String claimName = claim.getName() == null ? "Unnamed Claim" : claim.getName();
        int chunks = claim.getOwnedChunks().size();
        ItemStack head = GuiHelper.buildItemStack("PLAYER_HEAD",
                resolve(config.entry.name, claimName, owner, rank, chunks),
                resolveLines(config.entry.lore, claimName, owner, rank, chunks),
                config.entry.itemModel);
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof SkullMeta skullMeta && claim.getOwnerId() != null) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(claim.getOwnerId()));
            head.setItemMeta(skullMeta);
        }
        return head;
    }

    private static ItemStack buildViewerItem(TopClaimsConfig config, ClaimProfile profile,
            Map<UUID, Integer> ranks) {
        String claimName = profile.getName() == null ? "Unnamed Claim" : profile.getName();
        String owner = profile.getDisplayOwnerName();
        int chunks = profile.getOwnedChunks().size();
        Integer rank = ranks.get(profile.getProfileId());
        String rankText = rank == null ? "Unranked" : String.valueOf(rank);
        ItemStack item = GuiHelper.buildItemStack(config.viewer.material,
                resolve(config.viewer.name, claimName, owner, rankText, chunks),
                resolveLines(config.viewer.lore, claimName, owner, rankText, chunks),
                config.viewer.itemModel);
        return item;
    }

    private static List<String> resolveLines(List<String> lines, String claimName, String owner,
            Object rank, int chunks) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        List<String> resolved = new ArrayList<>(lines.size());
        for (String line : lines) {
            resolved.add(resolve(line, claimName, owner, rank, chunks));
        }
        return resolved;
    }

    private static String resolve(String text, String claimName, String owner, Object rank, int chunks) {
        if (text == null) {
            return "";
        }
        return text.replace("{rank}", String.valueOf(rank))
                .replace("{claim_name}", claimName)
                .replace("{owner}", owner)
                .replace("{chunks}", String.valueOf(chunks));
    }

    private static String[] validateLayout(TopClaimsConfig config, LandClaimPlugin plugin) {
        if (config.rows < 1 || config.rows > 6 || config.layout == null
                || config.layout.size() != config.rows) {
            plugin.getLogger().severe("Invalid Top Claims layout: rows must be 1-6 and match the layout list.");
            return null;
        }
        int entries = 0;
        int viewers = 0;
        int backs = 0;
        Set<String> allowed = Set.of("F", "V", "B", "x", ".");
        for (int rowIndex = 0; rowIndex < config.layout.size(); rowIndex++) {
            String row = config.layout.get(rowIndex);
            if (row == null || row.trim().isEmpty()) {
                plugin.getLogger().severe("Invalid Top Claims layout row " + (rowIndex + 1) + ".");
                return null;
            }
            String[] slots = row.trim().split("\\s+");
            if (slots.length != 9) {
                plugin.getLogger().severe("Invalid Top Claims layout row " + (rowIndex + 1)
                        + ": each row must contain exactly nine slots.");
                return null;
            }
            for (String slot : slots) {
                if (!allowed.contains(slot)) {
                    plugin.getLogger().severe("Invalid Top Claims layout slot '" + slot
                            + "' in row " + (rowIndex + 1) + ".");
                    return null;
                }
                if ("x".equals(slot)) entries++;
                if ("V".equals(slot)) viewers++;
                if ("B".equals(slot)) backs++;
            }
        }
        if (entries != 10 || viewers != 1 || backs != 1) {
            plugin.getLogger().severe("Invalid Top Claims layout: it must contain exactly ten x slots, one V, and one B.");
            return null;
        }
        return config.layout.toArray(String[]::new);
    }

    private static List<Integer> findSlots(String[] layout, String target) {
        List<Integer> slots = new ArrayList<>();
        int inventorySlot = 0;
        for (String row : layout) {
            for (String slot : row.trim().split("\\s+")) {
                if (target.equals(slot)) {
                    slots.add(inventorySlot);
                }
                inventorySlot++;
            }
        }
        return slots;
    }
}
