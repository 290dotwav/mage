package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.game.permanent.Permanent;
import mage.game.permanent.PermanentToken;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class LokiTheDeceiverTest extends CardTestPlayerBase {

    private static final String loki = "Loki, the Deceiver";
    private static final String knight = "Knight of Wundagore"; // 2/1 trample Villain

    @Test
    public void test_TokenCopyAttacks_DrawOnce_SacrificedAtEnd() {
        addCard(Zone.BATTLEFIELD, playerA, loki);
        addCard(Zone.BATTLEFIELD, playerA, knight);

        attack(1, playerA, loki);
        addTarget(playerA, knight);

        checkPermanentCount("token attacking", 1, PhaseStep.DECLARE_BLOCKERS, playerA, knight, 2);
        runCode("token is a nonlegendary Illusion copy", 1, PhaseStep.DECLARE_BLOCKERS, playerA, (info, player, game) -> {
            Permanent token = game.getBattlefield().getAllActivePermanents().stream()
                    .filter(p -> p instanceof PermanentToken && p.getName().equals(knight))
                    .findFirst().orElse(null);
            Assert.assertNotNull(info, token);
            Assert.assertTrue(info, token.hasSubtype(SubType.ILLUSION, game));
            Assert.assertTrue(info, token.hasSubtype(SubType.VILLAIN, game));
            Assert.assertFalse(info, token.isLegendary(game));
            Assert.assertTrue(info, token.isTapped());
            Assert.assertTrue(info, token.isAttacking());
        });

        setStopAt(2, PhaseStep.UPKEEP);
        setStrictChooseMode(true);
        execute();

        assertLife(playerB, 20 - 4 - 2);
        assertHandCount(playerA, 1); // one trigger for one damaged player
        assertPermanentCount(playerA, knight, 1);
        assertTapped(knight, false);
    }
}
