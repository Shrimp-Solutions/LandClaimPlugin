package org.ayosynk.landClaimPlugin.config.menus;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import eu.okaeri.configs.annotation.Header;

import java.util.List;

@Header("Banned Players GUI Configuration")
@Header("Materials must be valid Bukkit Material enum names.")
@Header("Names and lore support MiniMessage formatting.")
public class BannedPlayersConfig extends OkaeriConfig {

    public String title = "<dark_red>Banned Players";
    public int rows = 4;
    public List<String> layout = List.of(
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "P F F F B F F F N");

    @Comment("Decoration and navigation background")
    public ItemConfig filler = new ItemConfig("GRAY_STAINED_GLASS_PANE", " ", List.of());

    @Comment("Banned player entry; {player} is replaced with the account name.")
    public ItemConfig bannedPlayer = new ItemConfig("PLAYER_HEAD", "<red>{player}",
            List.of("<gray>Banned from this profile.", "", "<yellow>Click to unban."));

    @Comment("Shown when this profile has no banned players.")
    public ItemConfig empty = new ItemConfig("BARRIER", "<green>No Banned Players", List.of());

    @Comment("Return to the main claim menu")
    public ItemConfig back = new ItemConfig("SPECTRAL_ARROW", "<yellow>Back",
            List.of("<gray>Return to the main menu."));

    public ItemConfig previousPage = new ItemConfig("ARROW", "<green>Previous Page", List.of());
    public ItemConfig nextPage = new ItemConfig("ARROW", "<green>Next Page", List.of());

    public static class ItemConfig extends OkaeriConfig {
        public String material;
        public String name;
        public List<String> lore;
        @CustomKey("item_model")
        public String itemModel;

        public ItemConfig() {
        }

        public ItemConfig(String material, String name, List<String> lore) {
            this.material = material;
            this.name = name;
            this.lore = lore;
        }
    }
}
