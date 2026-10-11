package org.ayosynk.landClaimPlugin.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.ayosynk.landClaimPlugin.LandClaimPlugin;
import org.ayosynk.landClaimPlugin.util.FoliaScheduler;
import org.ayosynk.landClaimPlugin.util.GeyserFormHelper;
import org.ayosynk.landClaimPlugin.gui.framework.CustomGui;
import org.ayosynk.landClaimPlugin.gui.framework.SlotDefinition;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reusable confirmation dialog with an inventory fallback and optional Geyser
 * ModalForm presentation.
 */
public class ConfirmationGUI {

    public static void open(Player player, String title, Runnable onConfirm, Runnable onCancel) {
        open(player, title, title, onConfirm, onCancel);
    }

    public static void open(Player player, String title, String content, Runnable onConfirm, Runnable onCancel) {
        LandClaimPlugin plugin = LandClaimPlugin.getInstance();
        if (plugin != null && plugin.getConfigManager().getPluginConfig().geyserForms
                && GeyserFormHelper.shouldUseForms(player)) {
            FoliaScheduler.runForPlayer(plugin, player, () -> {
                player.closeInventory();
                String plainTitle = toPlainText(title);
                String plainContent = toPlainText(content);
                boolean sent = GeyserFormHelper.sendModalForm(player, plainTitle, plainContent, "Confirm", "Cancel", confirmed ->
                        FoliaScheduler.runForPlayer(plugin, player, () -> {
                            if (confirmed) {
                                if (onConfirm != null) onConfirm.run();
                            } else if (onCancel != null) {
                                onCancel.run();
                            }
                        }));
                if (!sent) {
                    openInventory(player, title, content, onConfirm, onCancel);
                }
            });
            return;
        }
        openInventory(player, title, content, onConfirm, onCancel);
    }

    private static void openInventory(Player player, String title, String content,
            Runnable onConfirm, Runnable onCancel) {
        String[] structure = {
                "F F F Y I N F F F"
        };

        Map<Character, SlotDefinition> ingredients = new HashMap<>();
        ingredients.put('F', GuiHelper.buildSlot("GRAY_STAINED_GLASS_PANE", " ", List.of()));
        ingredients.put('I', GuiHelper.buildSlot("PAPER", "<yellow>Details",
                List.of(content == null ? "" : content)));
        ingredients.put('Y', GuiHelper.buildSlot("LIME_WOOL", "<green>Confirm",
                List.of("<gray>Click to confirm."), (p, e) -> {
                    p.closeInventory();
                    if (onConfirm != null)
                        onConfirm.run();
                }));
        ingredients.put('N', GuiHelper.buildSlot("RED_WOOL", "<red>Cancel",
                List.of("<gray>Click to cancel."), (p, e) -> {
                    p.closeInventory();
                    if (onCancel != null)
                        onCancel.run();
                }));

        Component guiTitle = GuiHelper.MM.deserialize(title);
        CustomGui gui = new CustomGui(guiTitle, 1);
        gui.fillFromStructure(structure, ingredients);
        gui.open(player);
    }

    private static String toPlainText(String text) {
        Component component = text != null && text.contains("§")
                ? LegacyComponentSerializer.legacySection().deserialize(text)
                : GuiHelper.MM.deserialize(text == null ? "" : text);
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
