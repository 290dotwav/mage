package org.mage.test.serverside;

import mage.abilities.icon.system.PlayableCountIcon;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.game.permanent.Permanent;
import mage.players.PlayableObjectStats;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

import java.util.List;

/**
 * The playable records of an object (PlayableObjectStats) are what a web client offers as the ways to play it. They
 * were cut at 50 characters with "...", which a client completed from the card's printed text; an ability no card
 * prints had nothing to complete it from: reconfigure's unattach (702.151a) read "{2}: Unattach this permanent.
 * Activate only if th...". The records are whole now; the card icons popup cuts its own lines.
 */
public class PlayableWaysWholeTest extends CardTestPlayerBase {

    @Test
    public void testReconfigureUnattachWhole() {
        // Lizard Blades {1}{R} 1/1 double strike, reconfigure {2}
        addCard(Zone.BATTLEFIELD, playerA, "Lizard Blades");
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 4);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Reconfigure", "Grizzly Bears");

        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        Permanent blades = getPermanent("Lizard Blades", playerA);
        Assert.assertNotNull("attached", blades.getAttachedTo());
        PlayableObjectStats stats = playerA.getPlayableObjects(currentGame, Zone.ALL).getStats(blades.getId());
        List<String> names = stats.getPlayableAbilityNames();
        Assert.assertTrue("whole: " + names, names.contains("{2}: Unattach this permanent. Activate only if this permanent is attached to a creature and only as a sorcery."));
        String popup = new PlayableCountIcon(stats).getHint();
        Assert.assertTrue("the popup still cuts: " + popup, popup.contains("{2}: Unattach this permanent. Activate only if th..."));
    }
}
