package org.ayosynk.landClaimPlugin.gui.framework;

import java.util.ArrayList;
import java.util.List;

/** Parses both compact character layouts and the legacy space-separated form. */
public final class GuiLayoutParser {

    private GuiLayoutParser() {
    }

    public static List<List<String>> parseRows(String[] rows) {
        List<List<String>> parsedRows = new ArrayList<>(rows.length);
        for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
            parsedRows.add(parseRow(rows[rowIndex], rowIndex + 1));
        }
        return parsedRows;
    }

    public static List<String> parseRow(String row, int rowNumber) {
        if (row == null) {
            throw new IllegalArgumentException("Layout row " + rowNumber + " is null.");
        }

        List<String> slots = new ArrayList<>();
        for (int index = 0; index < row.length();) {
            char current = row.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
                continue;
            }
            if (current != '`') {
                slots.add(String.valueOf(current));
                index++;
                continue;
            }

            int end = row.indexOf('`', index + 1);
            if (end < 0) {
                throw new IllegalArgumentException(
                        "Layout row " + rowNumber + " has an unclosed multi-character slot ID.");
            }
            String id = row.substring(index + 1, end);
            if (id.isBlank()) {
                throw new IllegalArgumentException(
                        "Layout row " + rowNumber + " has an empty multi-character slot ID.");
            }
            slots.add(id);
            index = end + 1;
        }
        return slots;
    }

    public static String encodeRow(List<String> slots) {
        StringBuilder encoded = new StringBuilder();
        for (String slot : slots) {
            if (slot.length() == 1 && !Character.isWhitespace(slot.charAt(0))
                    && slot.charAt(0) != '`') {
                encoded.append(slot);
            } else {
                encoded.append('`').append(slot).append('`');
            }
        }
        return encoded.toString();
    }
}
