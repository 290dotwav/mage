package mage.server.web;

import mage.cards.decks.DeckCardLists;
import mage.cards.decks.importer.DckDeckImporter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * A deck arrives as the text of a .dck file ("1 [C13:149] Hua Tuo, Honored Physician",
 * "SB: 1 [C13:204] Prossh, Skyraider of Kher" for the commander). Their importer reads
 * files, so the text is written to a temporary file and read back with DckDeckImporter.
 */
final class DeckText {

    private DeckText() {
    }

    static final class Parsed {
        final DeckCardLists deck;
        final String warnings;

        Parsed(DeckCardLists deck, String warnings) {
            this.deck = deck;
            this.warnings = warnings;
        }
    }

    static Parsed parse(String dckText) throws IOException {
        File tmp = File.createTempFile("web-door-deck-", ".dck");
        try {
            Files.write(tmp.toPath(), dckText.getBytes(StandardCharsets.UTF_8));
            StringBuilder errors = new StringBuilder();
            DeckCardLists deck = new DckDeckImporter().importDeck(tmp.getAbsolutePath(), errors, false);
            if (deck == null || (deck.getCards().isEmpty() && deck.getSideboard().isEmpty())) {
                throw new IllegalArgumentException("deck text has no card the server knows" + (errors.length() > 0 ? ": " + errors : ""));
            }
            return new Parsed(deck, errors.length() > 0 ? errors.toString() : null);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
    }
}
