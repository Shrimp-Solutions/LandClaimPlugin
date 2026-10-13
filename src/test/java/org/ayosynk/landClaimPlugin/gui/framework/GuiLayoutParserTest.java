package org.ayosynk.landClaimPlugin.gui.framework;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuiLayoutParserTest {

    @Test
    void parsesCompactAndSpaceSeparatedRowsTheSameWay() {
        List<String> compact = GuiLayoutParser.parseRow("123456789", 1);
        List<String> spaced = GuiLayoutParser.parseRow("1 2 3 4 5 6 7 8 9", 1);

        assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9"), compact);
        assertEquals(compact, spaced);
    }

    @Test
    void preservesEmptySlotsAndMultiCharacterIds() {
        assertEquals(List.of("1", "resident", "-", ".", ".", ".", ".", ".", "."),
                GuiLayoutParser.parseRow("1`resident`-......", 1));
    }

    @Test
    void rejectsMalformedMultiCharacterIds() {
        assertThrows(IllegalArgumentException.class, () -> GuiLayoutParser.parseRow("1`resident", 2));
        assertThrows(IllegalArgumentException.class, () -> GuiLayoutParser.parseRow("1``", 2));
    }
}
