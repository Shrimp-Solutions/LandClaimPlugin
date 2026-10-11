package org.ayosynk.landClaimPlugin.config.menus;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import eu.okaeri.configs.annotation.Header;

import java.util.List;

@Header("Top Claims GUI Configuration")
@Header("Entries are informational; the layout must contain exactly ten x slots.")
public class TopClaimsConfig extends OkaeriConfig {

    public String title = "Top Claims";
    public int rows = 6;

    @Comment("Use x for the ten ranked entries, V for the viewer panel, B for back, F for filler, and . for empty.")
    public List<String> layout = List.of(
            "F F F F F F F F F",
            "F x x x x x F F F",
            "F x x x x x F F F",
            "F F F F F F F F F",
            "F F F F V F F F F",
            "F F F B F F F F F");

    public EntryConfig entry = new EntryConfig("<gold>#{rank} <white>{claim_name}",
            List.of("<gray>Owner: <white>{owner}", "<gray>Chunks: <white>{chunks}"));
    public ItemConfig viewer = new ItemConfig("PAPER", "<aqua>Your ranking",
            List.of("<gray>Profile: <white>{claim_name}", "<gray>Rank: <white>#{rank}",
                    "<gray>Chunks: <white>{chunks}"));
    public ItemConfig filler = new ItemConfig("GRAY_STAINED_GLASS_PANE", " ", List.of());
    public ItemConfig back = new ItemConfig("ARROW", "<yellow>Back", List.of());

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

    public static class EntryConfig extends OkaeriConfig {
        public String name;
        public List<String> lore;
        @CustomKey("item_model")
        public String itemModel;

        public EntryConfig() {
        }

        public EntryConfig(String name, List<String> lore) {
            this.name = name;
            this.lore = lore;
        }
    }
}
