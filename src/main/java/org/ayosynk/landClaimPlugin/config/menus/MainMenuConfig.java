package org.ayosynk.landClaimPlugin.config.menus;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;

import java.util.List;
import java.util.Map;

@Header("MainMenu GUI Configuration")
@Header("Materials must be valid Bukkit Material enum names.")
@Header("Names and lore support MiniMessage formatting (e.g., <red>Test</red>, <gradient:blue:aqua>Gradient</gradient>)")
public class MainMenuConfig extends OkaeriConfig {

        public String title = "Claim: {claim_name}";

        @Comment("Inventory row count (1-6) and nine-slot-wide layout. Rows may be compact or space-separated.")
        public int rows = 3;

        @Comment("Layout IDs map to built-in tokens or buttons. Use '.' or '-' for empty slots and backticks for multi-character IDs.")
        public List<String> layout = List.of(
                        "112222211",
                        "1.PSTVCB1",
                        "222222222");

        @Comment("Filler 1")
        public ItemConfig filler1 = new ItemConfig("WHITE_STAINED_GLASS_PANE", " ", List.of());

        @Comment("Filler 2")
        public ItemConfig filler2 = new ItemConfig("GRAY_STAINED_GLASS_PANE", " ", List.of());

        @Comment("Claim Settings Button")
        public ItemConfig settings = new ItemConfig("EMERALD", "Settings", List.of());

        @Comment("Manage Residents, Trusted players, and Visitors")
        public ItemConfig trusted = new ItemConfig("COPPER_CHESTPLATE", "<gold>Manage", List.of());

        @Comment("Open profile flags (Visitor category first)")
        public ItemConfig visitors = new ItemConfig("SKULL_BANNER_PATTERN", "<yellow>Flags", List.of());

        @Comment("Open the top 10 profiles ranked by claimed chunks")
        public ItemConfig topClaims = new ItemConfig("NETHER_STAR", "<aqua>Top Claims", List.of());

        @Comment("Open the banned players list for this profile")
        public ItemConfig bannedPlayers = new ItemConfig("BARRIER", "<red>Banned Players", List.of());

        @Comment("Open the profile selector")
        public ItemConfig profiles = new ItemConfig("BOOK", "<aqua>Profiles", List.of());

        @Comment("Configurable layout buttons keyed by layout ID. Use backticks around multi-character IDs in layout rows.")
        public Map<String, ItemConfig> buttons = Map.of();

        @Comment("Legacy one-character layout items. Prefer 'buttons' for new menus.")
        public Map<String, ItemConfig> customItems = Map.of();

        public static class ItemConfig extends OkaeriConfig {
                public String material;
                public String name;
                public List<String> lore;
                @eu.okaeri.configs.annotation.CustomKey("item_model")
                public String itemModel;
                @eu.okaeri.configs.annotation.CustomKey("player_commands")
                public List<String> playerCommands = List.of();
                @eu.okaeri.configs.annotation.CustomKey("console_commands")
                public List<String> consoleCommands = List.of();
                @eu.okaeri.configs.annotation.CustomKey("function")
                public String function;

                public ItemConfig() {
                } // For Okaeri to instantiate

                public ItemConfig(String material, String name, List<String> lore) {
                        this.material = material;
                        this.name = name;
                        this.lore = lore;
                }
        }
}
