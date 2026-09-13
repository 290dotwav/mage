package mage.server.web;

import mage.cards.decks.DeckCardLists;
import mage.cards.decks.importer.DckDeckImporter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A deck arrives as the text of a .dck file ("1 [C13:149] Hua Tuo, Honored Physician",
 * "SB: 1 [C13:204] Prossh, Skyraider of Kher" - in Commander the whole SB: block is the
 * command zone). Their importer reads files, so the text goes through a temporary file
 * and DckDeckImporter.
 * <p>
 * Our site sends "[???:k]" as set code (a decklist only has names): the importer then
 * picks a printing by name and leaves two non-fatal messages per line ("can't find card by
 * number, will try to replace", "replaced to [SET:n] Name"). Those are counted, not shown.
 * A deck is refused only on an "ERROR" message ("ERROR, can't find card [Name]"); any other
 * message is passed on as a warning.
 */
final class DeckText {

    private static final Pattern ERROR_LINE = Pattern.compile("^(Line \\d+: )?ERROR");
    private static final Pattern REPLACED_LINE = Pattern.compile("^(Line \\d+: )?((can't find card by number|found outdated card number or name), will try to replace|replaced to \\[)");

    private DeckText() {
    }

    static final class Parsed {
        final DeckCardLists deck;
        final List<String> warnings;
        final int replaced;

        Parsed(DeckCardLists deck, List<String> warnings, int replaced) {
            this.deck = deck;
            this.warnings = warnings;
            this.replaced = replaced;
        }
    }

    static Parsed parse(String dckText) throws IOException {
        File tmp = File.createTempFile("web-door-deck-", ".dck");
        try {
            Files.write(tmp.toPath(), dckText.getBytes(StandardCharsets.UTF_8));
            StringBuilder messages = new StringBuilder();
            DeckCardLists deck = new DckDeckImporter().importDeck(tmp.getAbsolutePath(), messages, false);

            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            int replaced = 0;
            for (String line : messages.toString().split("\n")) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (ERROR_LINE.matcher(line).find()) {
                    errors.add(line);
                } else if (REPLACED_LINE.matcher(line).find()) {
                    replaced++;
                } else {
                    warnings.add(line);
                }
            }
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException("deck refused: " + String.join("; ", errors));
            }
            if (deck == null || (deck.getCards().isEmpty() && deck.getSideboard().isEmpty())) {
                throw new IllegalArgumentException("deck refused: no card found in the text");
            }
            return new Parsed(deck, warnings, replaced);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
    }
}
