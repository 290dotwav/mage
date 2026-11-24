package org.mage.test.cards.continuous;

import mage.abilities.effects.ChangeSources;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.game.permanent.Permanent;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Where each change of a permanent comes from (ClaudeMTG fork, {@link ChangeSources}): the
 * owner, « les modifications doivent avoir leur source à coté. Un +1 d'où il vient ? ». The
 * engine records, as it applies layers 6 and 7, what each source added to a permanent's size
 * and the abilities it gave or took away, for a game that asks for it (the web door's).
 */
public class ChangeSourcesTest extends CardTestPlayerBase {

    @Test
    public void anAnthemAndAnEquipmentEachSayWhatTheyGive() {
        addCard(Zone.BATTLEFIELD, playerA, "Glorious Anthem"); // Creatures you control get +1/+1.
        addCard(Zone.BATTLEFIELD, playerA, "Behemoth Sledge"); // Equipped creature gets +2/+2 and has trample and lifelink. Equip {3}
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears"); // 2/2
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 3);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Equip", "Grizzly Bears");

        currentGame.setRecordingChangeSources(true);
        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertPowerToughness(playerA, "Grizzly Bears", 5, 5);
        List<ChangeSources.Change> changes = changesOf("Grizzly Bears");
        Assert.assertEquals(changes.toString(), 2, changes.size());
        assertChange(changes, "Glorious Anthem", 1, 1, Collections.emptyList(), Collections.emptyList());
        assertChange(changes, "Behemoth Sledge", 2, 2, Arrays.asList("Trample", "Lifelink"), Collections.emptyList());
        // the Sledge itself is not a creature: nothing changed it
        Assert.assertNull(currentGame.getChangeSources().get(getPermanent("Behemoth Sledge").getId()));
    }

    @Test
    public void humilitySaysWhatItTookAway() {
        addCard(Zone.BATTLEFIELD, playerA, "Humility"); // All creatures lose all abilities and have base power and toughness 1/1.
        addCard(Zone.BATTLEFIELD, playerB, "Glorious Anthem");
        addCard(Zone.BATTLEFIELD, playerB, "Serra Angel"); // 4/4 flying, vigilance

        currentGame.setRecordingChangeSources(true);
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertPowerToughness(playerB, "Serra Angel", 2, 2);
        List<ChangeSources.Change> changes = changesOf("Serra Angel");
        Assert.assertEquals(changes.toString(), 2, changes.size());
        assertChange(changes, "Humility", -3, -3, Collections.emptyList(), Arrays.asList("Flying", "Vigilance"));
        assertChange(changes, "Glorious Anthem", 1, 1, Collections.emptyList(), Collections.emptyList());
    }

    @Test
    public void nothingIsRecordedForAGameThatDoesNotAsk() {
        addCard(Zone.BATTLEFIELD, playerA, "Glorious Anthem");
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");

        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertPowerToughness(playerA, "Grizzly Bears", 3, 3);
        Assert.assertTrue(currentGame.getChangeSources().isEmpty());
        // and a copy (what the AI simulates on) does not record, even of a game that does
        currentGame.setRecordingChangeSources(true);
        Assert.assertFalse(currentGame.copy().isRecordingChangeSources());
    }

    private List<ChangeSources.Change> changesOf(String name) {
        Permanent permanent = getPermanent(name);
        List<ChangeSources.Change> changes = currentGame.getChangeSources().get(permanent.getId());
        Assert.assertNotNull("no change recorded for " + name, changes);
        return changes;
    }

    private static void assertChange(List<ChangeSources.Change> changes, String from, int power, int toughness,
                                     List<String> gained, List<String> lost) {
        ChangeSources.Change change = changes.stream()
                .filter(c -> c.getFrom().equals(from))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no change from " + from + " in " + changes));
        Assert.assertEquals(from + " power", power, change.getPower());
        Assert.assertEquals(from + " toughness", toughness, change.getToughness());
        Assert.assertEquals(from + " gained", gained, change.getGained());
        Assert.assertEquals(from + " lost", lost, change.getLost());
    }
}
