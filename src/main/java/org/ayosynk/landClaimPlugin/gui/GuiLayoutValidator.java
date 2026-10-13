package org.ayosynk.landClaimPlugin.gui;

import org.ayosynk.landClaimPlugin.LandClaimPlugin;
import org.ayosynk.landClaimPlugin.gui.framework.GuiLayoutParser;

import java.util.List;
import java.util.Set;

final class GuiLayoutValidator {

    private GuiLayoutValidator() {
    }

    static String[] validate(int rows, List<String> layout, Set<String> allowedSlots,
            String previousSlot, String nextSlot, String menuName, LandClaimPlugin plugin) {
        if (rows < 2 || rows > 6 || layout == null || layout.size() != rows) {
            plugin.getLogger().severe("Invalid " + menuName + " menu layout: rows must be 2-6 and match the layout list.");
            return null;
        }

        Set<String> supportedSlots = new java.util.HashSet<>(allowedSlots);
        supportedSlots.add(".");
        supportedSlots.add("-");
        int previousCount = 0;
        int nextCount = 0;
        int contentCount = 0;
        List<List<String>> parsedRows;
        try {
            parsedRows = GuiLayoutParser.parseRows(layout.toArray(String[]::new));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().severe("Invalid " + menuName + " menu layout: " + e.getMessage());
            return null;
        }
        for (int row = 0; row < parsedRows.size(); row++) {
            List<String> slots = parsedRows.get(row);
            if (slots.size() != 9) {
                plugin.getLogger().severe("Invalid " + menuName + " menu layout row " + (row + 1)
                        + ": each row must resolve to exactly nine slots.");
                return null;
            }
            for (String slot : slots) {
                if (!supportedSlots.contains(slot)) {
                    plugin.getLogger().severe("Invalid " + menuName + " menu layout slot '" + slot
                            + "' in row " + (row + 1) + ".");
                    return null;
                }
                if (previousSlot != null && slot.equals(previousSlot)) previousCount++;
                if (nextSlot != null && slot.equals(nextSlot)) nextCount++;
                if (slot.equals("x")) contentCount++;
            }
        }
        boolean paginated = previousSlot != null || nextSlot != null;
        if (paginated && (previousCount != 1 || nextCount != 1 || contentCount == 0)) {
            plugin.getLogger().severe("Invalid " + menuName + " menu layout: include one or more x content slots "
                    + "and exactly one " + previousSlot + " and one " + nextSlot + " navigation slot.");
            return null;
        }
        return layout.toArray(new String[0]);
    }

    static int findSlot(String[] layout, String target) {
        List<List<String>> parsedRows = GuiLayoutParser.parseRows(layout);
        for (int row = 0; row < parsedRows.size(); row++) {
            List<String> slots = parsedRows.get(row);
            for (int column = 0; column < slots.size(); column++) {
                if (slots.get(column).equals(target)) return row * 9 + column;
            }
        }
        throw new IllegalStateException("Validated GUI layout is missing slot " + target);
    }
}
