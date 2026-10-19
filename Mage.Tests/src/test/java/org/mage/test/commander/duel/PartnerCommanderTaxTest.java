package org.mage.test.commander.duel;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.GameException;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestCommanderDuelBase;

import java.io.FileNotFoundException;

/**
 * 903.8: each partner carries its own tax. Thrasios ({G}{U}) cast, bounced to the command zone, cast again: {G}{U}{2},
 * four lands; its partner never cast, nothing of its own added.
 */
public class PartnerCommanderTaxTest extends CardTestCommanderDuelBase {

    @Override
    protected Game createNewGameAndPlayers() throws GameException, FileNotFoundException {
        setDecknamePlayerA("CommanderDuel_Partners.dck"); // Ishai, Ojutai Dragonspeaker and Thrasios, Triton Hero
        return super.createNewGameAndPlayers();
    }

    @Test
    public void testEachPartnerCarriesItsOwnTax() {
        addCard(Zone.BATTLEFIELD, playerA, "Tropical Island", 8);
        addCard(Zone.BATTLEFIELD, playerB, "Island", 1);
        addCard(Zone.HAND, playerB, "Unsummon");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Thrasios, Triton Hero");
        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Unsummon", "Thrasios, Triton Hero");
        setChoice(playerA, true); // to the command zone
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Thrasios, Triton Hero");

        setStopAt(3, PhaseStep.BEGIN_COMBAT);
        execute();

        assertPermanentCount(playerA, "Thrasios, Triton Hero", 1);
        assertTappedCount("Tropical Island", true, 4);
    }

    @Test
    public void testCastFromTheHandPaysNoTax() {
        // 903.8: the tax is for a cast from the command zone; left in the hand after a bounce, it is cast at its own cost
        addCard(Zone.BATTLEFIELD, playerA, "Tropical Island", 8);
        addCard(Zone.BATTLEFIELD, playerB, "Island", 1);
        addCard(Zone.HAND, playerB, "Unsummon");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Thrasios, Triton Hero");
        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Unsummon", "Thrasios, Triton Hero");
        setChoice(playerA, false); // stays in the hand
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Thrasios, Triton Hero");

        setStopAt(3, PhaseStep.BEGIN_COMBAT);
        execute();

        assertPermanentCount(playerA, "Thrasios, Triton Hero", 1);
        assertTappedCount("Tropical Island", true, 2);
    }
}
