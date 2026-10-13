package org.ayosynk.landClaimPlugin.gui;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuiHelperTest {

    @Test
    void deserializesLegacyFormattingInsideMiniMessageText() {
        String mixedFormatting = "<red>Ban <gold>Hizzie_Senpai</gold><red> §cfrom your claim?";

        assertEquals("Ban Hizzie_Senpai from your claim?",
                PlainTextComponentSerializer.plainText().serialize(GuiHelper.deserialize(mixedFormatting)));
    }

    @Test
    void convertsLegacyHexFormatting() {
        String legacyHex = "\u00a7x\u00a7f\u00a7f\u00a70\u00a70\u00a7a\u00a7aCustom color";

        assertEquals("Custom color",
                PlainTextComponentSerializer.plainText().serialize(GuiHelper.deserialize(legacyHex)));
    }
}
