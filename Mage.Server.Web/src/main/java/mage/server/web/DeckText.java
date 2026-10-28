package mage.server.web;

import mage.cards.decks.CardNameUtil;
import mage.cards.decks.DeckCardLists;
import mage.cards.decks.importer.DckDeckImporter;
import mage.cards.repository.CardRepository;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
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
 * <p>
 * {@link #check} reads a deck the same way and answers instead of refusing: the names the
 * card database does not know, so the site can say so while the list is being entered, not
 * when the table is opened ({ kind: "server", op: "check", deck } in WebSession).
 * <p>
 * A name spelled with a spaced ellipsis, as Scryfall and Moxfield write it ("Set Phasers to
 * . . ."), is not the database's spelling ("Set Phasers to...", "I'm a Doctor, Not a ...", the
 * space kept or not as the card was entered): such a line is given the database's spelling
 * before the importer reads it ({@link #repairNames}).
 */
final class DeckText {

    private static final Pattern ERROR_LINE = Pattern.compile("^(Line \\d+: )?ERROR");
    private static final Pattern REPLACED_LINE = Pattern.compile("^(Line \\d+: )?replaced to \\[");
    private static final Pattern WILL_REPLACE_LINE = Pattern.compile("^(Line \\d+: )?(can't find card by number|found outdated card number or name), will try to replace");
    /** "Line 59: ERROR, can't find card [Conquest Mission]": the name between the brackets. */
    private static final Pattern UNKNOWN_LINE = Pattern.compile("ERROR, can't find card \\[(.*)\\]\\s*$");
    /** DckDeckImporter's own line pattern: (SB:)? count [set:number] name. */
    private static final Pattern DCK_LINE = Pattern.compile("^((?:SB:)?\\s*\\d*\\s*\\[[^]:]+:[^]:]+\\]\\s*)(.*?)\\s*$");
    /** Three dots, spaced or not: ". . ." or "...". */
    private static final Pattern ELLIPSIS = Pattern.compile("\\s*\\.\\s*\\.\\s*\\.");

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

    /** What {@link #check} answers: the names not in the card database, any other refusal, the cards read. */
    static final class Check {
        final List<String> unknown;
        final List<String> errors;
        final int cards;

        Check(List<String> unknown, List<String> errors, int cards) {
            this.unknown = unknown;
            this.errors = errors;
            this.cards = cards;
        }
    }

    /** The importer's verdict on a text: the deck it made and every message it left. */
    private static final class Read {
        final DeckCardLists deck;
        final String messages;

        Read(DeckCardLists deck, String messages) {
            this.deck = deck;
            this.messages = messages;
        }
    }

    private static Read read(String dckText) throws IOException {
        File tmp = File.createTempFile("web-door-deck-", ".dck");
        try {
            Files.write(tmp.toPath(), repairNames(dckText, DeckText::known).getBytes(StandardCharsets.UTF_8));
            StringBuilder messages = new StringBuilder();
            DeckCardLists deck = new DckDeckImporter().importDeck(tmp.getAbsolutePath(), messages, false);
            return new Read(deck, messages.toString());
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
    }

    private static boolean known(String name) {
        return !CardRepository.instance.findCards(name, 1).isEmpty();
    }

    /**
     * The deck read without refusing it: which names the card database lacks (each once, in
     * list order), the importer's other ERROR lines, and how many cards it did read. Read-only.
     */
    static Check check(String dckText) throws IOException {
        Read read = read(dckText);
        List<String> errors = new ArrayList<>();
        for (String line : read.messages.split("\n")) {
            line = line.trim();
            if (ERROR_LINE.matcher(line).find() && unknownIn(line) == null) {
                errors.add(line);
            }
        }
        int cards = read.deck == null ? 0 : read.deck.getCards().size() + read.deck.getSideboard().size();
        return new Check(unknownNames(read.messages), errors, cards);
    }

    /** The name an importer message says it cannot find, or null for any other message. */
    static String unknownIn(String message) {
        Matcher m = UNKNOWN_LINE.matcher(message.trim());
        return m.find() ? m.group(1) : null;
    }

    /** Every name the importer's messages say it cannot find, each once, in their order. */
    static List<String> unknownNames(String messages) {
        Set<String> names = new LinkedHashSet<>();
        for (String line : messages.split("\n")) {
            String name = unknownIn(line);
            if (name != null && !name.isEmpty()) {
                names.add(name);
            }
        }
        return new ArrayList<>(names);
    }

    /**
     * Each line whose name has three dots the database does not know as written, given the
     * spelling it does know: the dots closed up ("to . . ." -> "to...") or after a space
     * ("a . . ." -> "a ..."). A name {@code known} already, or none of whose spellings is known,
     * is left as it came (the importer then says "can't find card" as before).
     */
    static String repairNames(String dckText, Predicate<String> known) {
        StringBuilder out = new StringBuilder();
        for (String line : dckText.split("\n", -1)) {
            Matcher m = DCK_LINE.matcher(line);
            if (m.matches() && ELLIPSIS.matcher(m.group(2)).find()) {
                String name = CardNameUtil.normalizeCardName(m.group(2));
                if (!known.test(name)) {
                    for (String spelling : new String[]{ELLIPSIS.matcher(name).replaceAll("..."), ELLIPSIS.matcher(name).replaceAll(" ...")}) {
                        if (known.test(spelling)) {
                            line = m.group(1) + spelling;
                            break;
                        }
                    }
                }
            }
            out.append(line).append('\n');
        }
        return out.substring(0, out.length() - 1);
    }

    static Parsed parse(String dckText) throws IOException {
        Read read = read(dckText);
        DeckCardLists deck = read.deck;
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int replaced = 0;
        for (String line : read.messages.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (ERROR_LINE.matcher(line).find()) {
                errors.add(line);
            } else if (REPLACED_LINE.matcher(line).find()) {
                replaced++; // one per replaced line
            } else if (WILL_REPLACE_LINE.matcher(line).find()) {
                // the announcement before "replaced to", nothing to report
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
    }
}
