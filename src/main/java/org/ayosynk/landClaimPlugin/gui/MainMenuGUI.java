package org.ayosynk.landClaimPlugin.gui;

import net.kyori.adventure.text.Component;
import org.ayosynk.landClaimPlugin.LandClaimPlugin;
import org.ayosynk.landClaimPlugin.config.menus.MainMenuConfig;
import org.ayosynk.landClaimPlugin.gui.framework.CustomGui;
import org.ayosynk.landClaimPlugin.gui.framework.GuiLayoutParser;
import org.ayosynk.landClaimPlugin.gui.framework.SlotDefinition;
import org.ayosynk.landClaimPlugin.managers.PermissionResolver;
import org.ayosynk.landClaimPlugin.models.ClaimProfile;
import org.ayosynk.landClaimPlugin.util.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MainMenuGUI {

        @FunctionalInterface
        public interface MenuFunction {
                void execute(Player player, ClaimProfile profile, LandClaimPlugin plugin);
        }

        private static final Set<String> RESERVED_IDS = Set.of("1", "2", "S", "T", "V", "C", "B", "P", ".", "-");
        private static final Map<String, MenuFunction> MENU_FUNCTIONS = new ConcurrentHashMap<>();

        static {
                registerFunction("settings", (player, profile, plugin) -> {
                        if (canManageSettings(player, profile)) {
                                ClaimSettingsGUI.open(player, profile, plugin);
                        } else {
                                player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                        }
                });
                registerFunction("manage", (player, profile, plugin) -> {
                        if (canManageMembers(player, profile)) {
                                ManageGUI.open(player, profile, plugin);
                        } else {
                                player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                        }
                });
                registerFunction("flags", (player, profile, plugin) -> {
                        if (canManageSettings(player, profile)) {
                                VisitorSettingsGUI.open(player, profile, plugin, "visitor");
                        } else {
                                player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                        }
                });
                registerFunction("top-claims", (player, profile, plugin) ->
                                TopClaimsGUI.open(player, profile, plugin));
                registerFunction("banned-players", (player, profile, plugin) -> {
                        if (profile.canManage(player)) {
                                BannedPlayersGUI.open(player, profile, plugin);
                        } else {
                                player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                        }
                });
                registerFunction("profiles", (player, profile, plugin) ->
                                ProfileSelectorGUI.open(player, plugin));
                registerFunction("claim-tp", (player, profile, plugin) -> {
                        if (plugin.getCommandHandler() == null) {
                                plugin.getLogger().warning("Cannot execute main-menu function 'claim-tp': command handler is unavailable.");
                                return;
                        }
                        plugin.getCommandHandler().teleportToActiveProfileSpawnpoint(player, profile);
                });
                registerFunction("close", (player, profile, plugin) -> player.closeInventory());
        }

        public static void registerFunction(String id, MenuFunction function) {
                if (id == null || id.isBlank() || function == null) {
                        throw new IllegalArgumentException("Menu function ID and implementation are required.");
                }
                MENU_FUNCTIONS.put(id.toLowerCase(Locale.ROOT), function);
        }

        public static void open(Player player, ClaimProfile profile, LandClaimPlugin plugin) {
                if (!GuiHelper.checkMenuPermission(player, "main", plugin)) {
                        return;
                }
                FoliaScheduler.runForPlayer(plugin, player, () -> {
                        String ownerName = profile.getProfileId() != null
                                        ? profile.getColoredOwnerName()
                                        : "Unknown";
                        if (ownerName == null) {
                                ownerName = "Unknown";
                        }
                        String claimName = profile.getColoredName() != null
                                        ? profile.getColoredName()
                                        : "Unnamed Claim";

                        MainMenuConfig config = plugin.getConfigManager().getMainMenuConfig();
                        String[] structure = validateLayout(config, plugin);
                        if (structure == null) {
                                player.sendMessage(GuiHelper.MM.deserialize(
                                                "<red>Claim menu layout is invalid; check the plugin log."));
                                return;
                        }

                        String resolvedOwnerName = ownerName;
                        boolean canManageSettings = canManageSettings(player, profile);
                        boolean canManageMembers = canManageMembers(player, profile);
                        Map<String, SlotDefinition> ingredients = new HashMap<>();
                        ingredients.put("1", GuiHelper.buildSlot(config.filler1.material, config.filler1.name,
                                        config.filler1.lore, profile, player, ownerName, claimName,
                                        config.filler1.itemModel));
                        ingredients.put("2", GuiHelper.buildSlot(config.filler2.material, config.filler2.name,
                                        config.filler2.lore, profile, player, ownerName, claimName,
                                        config.filler2.itemModel));
                        ingredients.put("S", GuiHelper.buildSlot(config.settings.material, config.settings.name,
                                        config.settings.lore, profile, player, ownerName, claimName,
                                        config.settings.itemModel, (p, e) ->
                                                        ClaimSettingsGUI.open(p, profile, plugin)));
                        ingredients.put("T", GuiHelper.buildSlot(config.trusted.material, config.trusted.name,
                                        config.trusted.lore, profile, player, ownerName, claimName,
                                        config.trusted.itemModel, (p, e) -> {
                                                if (!canManageMembers) {
                                                        p.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                                                        return;
                                                }
                                                ManageGUI.open(p, profile, plugin);
                                        }));
                        ingredients.put("V", GuiHelper.buildSlot(config.visitors.material, config.visitors.name,
                                        config.visitors.lore, profile, player, ownerName, claimName,
                                        config.visitors.itemModel, (p, e) -> {
                                                if (!canManageSettings) {
                                                        p.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                                                        return;
                                                }
                                                VisitorSettingsGUI.open(p, profile, plugin, "visitor");
                                        }));
                        ingredients.put("C", GuiHelper.buildSlot(config.topClaims.material, config.topClaims.name,
                                        config.topClaims.lore, profile, player, ownerName, claimName,
                                        config.topClaims.itemModel, (p, e) ->
                                                        TopClaimsGUI.open(p, profile, plugin)));
                        ingredients.put("B", GuiHelper.buildSlot(config.bannedPlayers.material,
                                        config.bannedPlayers.name, config.bannedPlayers.lore,
                                        profile, player, ownerName, claimName, config.bannedPlayers.itemModel,
                                        (p, e) -> {
                                                if (!profile.canManage(p)) {
                                                        p.sendMessage(plugin.getConfigManager()
                                                                        .getMessage("no-permission"));
                                                        return;
                                                }
                                                BannedPlayersGUI.open(p, profile, plugin);
                                        }));
                        ingredients.put("P", GuiHelper.buildSlot(config.profiles.material,
                                        config.profiles.name, config.profiles.lore,
                                        profile, player, ownerName, claimName, config.profiles.itemModel,
                                        (p, e) -> ProfileSelectorGUI.open(p, plugin)));

                        Map<String, MainMenuConfig.ItemConfig> customButtons = new HashMap<>();
                        if (config.buttons != null) {
                                customButtons.putAll(config.buttons);
                        }
                        if (config.customItems != null) {
                                for (Map.Entry<String, MainMenuConfig.ItemConfig> entry : config.customItems.entrySet()) {
                                        customButtons.putIfAbsent(entry.getKey(), entry.getValue());
                                }
                        }
                        for (Map.Entry<String, MainMenuConfig.ItemConfig> entry : customButtons.entrySet()) {
                                MainMenuConfig.ItemConfig item = entry.getValue();
                                if (item == null) {
                                        continue;
                                }
                                ingredients.put(entry.getKey(), GuiHelper.buildSlot(item.material, item.name, item.lore,
                                                profile, player, ownerName, claimName, item.itemModel,
                                                (p, e) -> {
                                                        executeMenuFunction(item.function, p, profile, plugin);
                                                        executeCustomItemCommands(p, profile, resolvedOwnerName,
                                                                        claimName, item, plugin);
                                                }));
                        }

                        String windowTitle = config.title.replace("{claim_name}", claimName);
                        Component title = GuiHelper.deserialize(windowTitle);
                        CustomGui gui = new CustomGui(title, config.rows);
                        gui.fillFromStructure(structure, ingredients);
                        gui.open(player);
                });
        }

        private static boolean canManageSettings(Player player, ClaimProfile profile) {
                return profile.canManage(player)
                                || PermissionResolver.hasPermission(profile, player.getUniqueId(), "MANAGE_SETTINGS");
        }

        private static boolean canManageMembers(Player player, ClaimProfile profile) {
                return profile.canManage(player)
                                || PermissionResolver.hasPermission(profile, player.getUniqueId(), "MANAGE_MEMBERS");
        }

        private static void executeMenuFunction(String id, Player player, ClaimProfile profile,
                        LandClaimPlugin plugin) {
                if (id == null || id.isBlank()) {
                        return;
                }
                MenuFunction function = MENU_FUNCTIONS.get(id.toLowerCase(Locale.ROOT));
                if (function == null) {
                        plugin.getLogger().warning("Unknown main-menu function '" + id + "'.");
                        return;
                }
                function.execute(player, profile, plugin);
        }

        private static String[] validateLayout(MainMenuConfig config, LandClaimPlugin plugin) {
                if (config.rows < 1 || config.rows > 6 || config.layout == null
                                || config.layout.size() != config.rows) {
                        plugin.getLogger().severe("Invalid main menu layout: rows must be 1-6 and match the layout list.");
                        return null;
                }
                Map<String, MainMenuConfig.ItemConfig> buttons = new HashMap<>();
                if (config.buttons != null) {
                        buttons.putAll(config.buttons);
                }
                if (config.customItems != null) {
                        for (Map.Entry<String, MainMenuConfig.ItemConfig> entry : config.customItems.entrySet()) {
                                buttons.putIfAbsent(entry.getKey(), entry.getValue());
                        }
                }
                Set<String> allowedIds = new java.util.HashSet<>(RESERVED_IDS);
                for (String key : buttons.keySet()) {
                        if (key == null || key.isBlank() || RESERVED_IDS.contains(key) || key.contains("`")) {
                                plugin.getLogger().severe("Invalid main menu button ID '" + key
                                                + "': use a non-empty ID not reserved by the menu.");
                                return null;
                        }
                        allowedIds.add(key);
                }

                List<List<String>> parsedRows;
                try {
                        parsedRows = GuiLayoutParser.parseRows(config.layout.toArray(String[]::new));
                } catch (IllegalArgumentException e) {
                        plugin.getLogger().severe("Invalid main menu layout: " + e.getMessage());
                        return null;
                }
                for (int row = 0; row < parsedRows.size(); row++) {
                        List<String> slots = parsedRows.get(row);
                        if (slots.size() != 9) {
                                plugin.getLogger().severe("Invalid main menu layout row " + (row + 1)
                                                + ": each row must resolve to exactly nine slots.");
                                return null;
                        }
                        for (String slot : slots) {
                                if (!allowedIds.contains(slot)) {
                                        plugin.getLogger().severe("Invalid main menu layout slot '" + slot
                                                        + "' in row " + (row + 1) + ".");
                                        return null;
                                }
                        }
                }
                return config.layout.toArray(String[]::new);
        }

        private static void executeCustomItemCommands(Player player, ClaimProfile profile,
                        String ownerName, String claimName, MainMenuConfig.ItemConfig item,
                        LandClaimPlugin plugin) {
                if (item.playerCommands != null) {
                        for (String configuredCommand : item.playerCommands) {
                                String command = resolveCommandPlaceholders(configuredCommand, player, profile,
                                                ownerName, claimName);
                                if (!command.isBlank()) {
                                        Bukkit.dispatchCommand(player, stripCommandPrefix(command));
                                }
                        }
                }
                if (item.consoleCommands != null && !item.consoleCommands.isEmpty()) {
                        java.util.List<String> commands = item.consoleCommands.stream()
                                        .map(configuredCommand -> resolveCommandPlaceholders(configuredCommand,
                                                        player, profile, ownerName, claimName))
                                        .filter(command -> !command.isBlank())
                                        .map(MainMenuGUI::stripCommandPrefix)
                                        .toList();
                        FoliaScheduler.runTask(plugin, () -> {
                                for (String command : commands) {
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                                }
                        });
                }
        }

        private static String resolveCommandPlaceholders(String command, Player player,
                        ClaimProfile profile, String ownerName, String claimName) {
                return command.replace("%player%", player.getName())
                                .replace("%player_uuid%", player.getUniqueId().toString())
                                .replace("%claim_name%", profile.getName())
                                .replace("%owner%", profile.getDisplayOwnerName())
                                .replace("%world%", player.getWorld().getName())
                                .replace("%x%", String.valueOf(player.getLocation().getBlockX()))
                                .replace("%y%", String.valueOf(player.getLocation().getBlockY()))
                                .replace("%z%", String.valueOf(player.getLocation().getBlockZ()))
                                .replace("%display_claim_name%", claimName)
                                .replace("%display_owner_name%", ownerName);
        }

        private static String stripCommandPrefix(String command) {
                return command.startsWith("/") ? command.substring(1) : command;
        }
}
