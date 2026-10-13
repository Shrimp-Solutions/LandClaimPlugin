package org.ayosynk.landClaimPlugin.gui.framework;

import net.kyori.adventure.text.Component;
import org.ayosynk.landClaimPlugin.util.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core GUI runtime built on the Bukkit Inventory API.
 * <p>
 * Implements {@link InventoryHolder} for reliable identification in the
 * listener. Click dispatch, slot management, and lifecycle are all handled
 * internally — individual GUI classes only declare structure + items.
 * <p>
 * <b>Folia / Paper safety:</b> {@link #open(Player)} always schedules on the
 * appropriate thread via {@link FoliaScheduler#runForPlayer} — on Paper that is
 * the main thread, on Folia that is the player's region thread.
 */
public class CustomGui implements InventoryHolder {

    /** Plugin reference for scheduler access. */
    private static Plugin plugin;

    private final Inventory inventory;
    private Map<Integer, ClickAction> clickHandlers;

    /**
     * Must be called once during plugin enable, before any GUI is created.
     * Sets the plugin reference used for sync scheduling.
     */
    public static void setPlugin(Plugin plugin) {
        CustomGui.plugin = plugin;
    }

    /**
     * Creates a new GUI with the given title and row count.
     *
     * @param title MiniMessage-parsed title component
     * @param rows  number of inventory rows (1-6)
     */
    public CustomGui(Component title, int rows) {
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
        this.clickHandlers = new HashMap<>();
    }

    // ---- Slot management ----

    /**
     * Place an item with a click handler.
     */
    public void setItem(int slot, ItemStack item, ClickAction action) {
        inventory.setItem(slot, item);
        if (action != null) {
            clickHandlers.put(slot, action);
        }
    }

    /**
     * Place a decorative item (no click handler).
     */
    public void setItem(int slot, ItemStack item) {
        setItem(slot, item, null);
    }

    /**
     * Parse InvUI-style structure strings and fill the inventory.
     * <p>
     * Rows may use compact characters or space-separated IDs. Backticks delimit
     * multi-character IDs. Unknown IDs, {@code '.'}, and {@code '-'} are skipped.
     *
     * @param rows        structure rows, e.g. {@code "FFFFFFFFF"} or {@code "F F F F F F F F F"}
     * @param ingredients slot ID → item+action mappings (single-character IDs may use Character keys)
     */
    public void fillFromStructure(String[] rows, Map<?, SlotDefinition> ingredients) {
        List<List<String>> parsedRows = GuiLayoutParser.parseRows(rows);
        for (int rowIndex = 0; rowIndex < parsedRows.size(); rowIndex++) {
            List<String> row = parsedRows.get(rowIndex);
            for (int column = 0; column < row.size(); column++) {
                int slot = rowIndex * 9 + column;
                if (slot >= inventory.getSize()) {
                    return;
                }
                String id = row.get(column);
                if (id.equals(".") || id.equals("-")) {
                    continue;
                }
                SlotDefinition definition = ingredients.get(id);
                if (definition == null && id.length() == 1) {
                    definition = ingredients.get(id.charAt(0));
                }
                if (definition != null) {
                    setItem(slot, definition.item(), definition.action());
                }
            }
        }
    }

    // ---- Open / Close ----

    /**
     * Opens this GUI for the player.
     * <b>Always schedules on the appropriate thread</b> — safe to call from
     * async. On Paper, runs on the main thread; on Folia, on the player's
     * region thread.
     */
    public void open(Player player) {
        if (plugin == null) {
            throw new IllegalStateException("CustomGui.setPlugin() was not called during onEnable!");
        }
        if (FoliaScheduler.isFolia()) {
            // Folia: route through the player's region scheduler
            FoliaScheduler.runForPlayer(plugin, player, () -> openSync(player));
        } else if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> openSync(player));
        } else {
            openSync(player);
        }
    }

    private void openSync(Player player) {
        if (player.isOnline()) {
            player.openInventory(inventory);
        }
    }

    // ---- Click dispatch ----

    /**
     * Called by {@link GuiListener} on a valid click.
     *
     * @param slot   the raw slot index
     * @param player the clicking player
     * @param event  the raw click event (already cancelled)
     */
    public void handleClick(int slot, Player player, org.bukkit.event.inventory.InventoryClickEvent event) {
        if (clickHandlers == null)
            return;
        ClickAction action = clickHandlers.get(slot);
        if (action != null) {
            action.onClick(player, event);
        }
    }

    // ---- Lifecycle ----

    /**
     * Clears all references to prevent memory leaks.
     * Called by {@link GuiListener} on {@code InventoryCloseEvent}.
     */
    public void dispose() {
        if (clickHandlers != null) {
            clickHandlers.clear();
            clickHandlers = null;
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** Plugin reference for subclass scheduling needs. */
    protected static Plugin getPlugin() {
        return plugin;
    }
}
