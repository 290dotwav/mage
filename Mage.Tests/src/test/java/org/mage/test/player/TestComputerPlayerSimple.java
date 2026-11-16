package org.mage.test.player;

import mage.constants.RangeOfInfluence;
import mage.game.Game;
import mage.player.ai.ComputerPlayerSimple;

/**
 * Ours: the "Computer - simple" bot in a test, seated as itself (TestPlayer with AIPlayer on): every
 * decision is the bot's, the scripted rival of the site's lessons included (RivalScript).
 */
public final class TestComputerPlayerSimple extends ComputerPlayerSimple {

    private TestPlayer testPlayerLink;

    public TestComputerPlayerSimple(String name, RangeOfInfluence range, int skill) {
        super(name, range, skill);
    }

    public void setTestPlayerLink(TestPlayer testPlayerLink) {
        this.testPlayerLink = testPlayerLink;
    }

    @Override
    public boolean flipCoinResult(Game game) {
        // same random results must be same in any mode
        return testPlayerLink.flipCoinResult(game);
    }

    @Override
    public int rollDieResult(int sides, Game game) {
        return testPlayerLink.rollDieResult(sides, game);
    }
}
