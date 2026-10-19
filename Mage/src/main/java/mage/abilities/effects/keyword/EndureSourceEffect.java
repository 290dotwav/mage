package mage.abilities.effects.keyword;

import mage.abilities.Ability;
import mage.abilities.dynamicvalue.DynamicValue;
import mage.abilities.dynamicvalue.common.StaticValue;
import mage.abilities.effects.OneShotEffect;
import mage.constants.Outcome;
import mage.counters.CounterType;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.game.permanent.token.SpiritXXToken;
import mage.game.permanent.token.NoFlyingSpiritWhite11Token;
import mage.game.permanent.token.NoFlyingSpiritWhite22Token;
import mage.game.permanent.token.NoFlyingSpiritWhite33Token;
import mage.players.Player;
import mage.util.CardUtil;

/**
 * @author TheElk801
 */
public class EndureSourceEffect extends OneShotEffect {

    private final DynamicValue amount;

    public EndureSourceEffect(int amount) {
        this(amount, "it");
    }

    public EndureSourceEffect(int amount, String selfText) {
        this(StaticValue.get(amount), selfText);
    }

    public EndureSourceEffect(DynamicValue amount, String selfText) {
        super(Outcome.Benefit);
        staticText = selfText + " endures " + amount;
        this.amount = amount;
    }

    private EndureSourceEffect(final EndureSourceEffect effect) {
        super(effect);
        this.amount = effect.amount;
    }

    @Override
    public EndureSourceEffect copy() {
        return new EndureSourceEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        return doEndure(
                source.getSourcePermanentOrLKI(game),
                amount.calculate(game, source, this),
                game, source
        );
    }

    /**
     * The endure question (701.63a): "Put two +1/+1 counters on X or create a 2/2 Spirit token?".
     * It read "a 2 2/2": the article was put before the number, then the size after it.
     */
    static String choiceText(String name, int amount) {
        String size = amount + "/" + amount;
        boolean an = size.startsWith("8") || size.startsWith("11/") || size.startsWith("18/");
        return "Put " + CardUtil.numberToText(amount, "a") + " +1/+1 counter" + (amount > 1 ? "s" : "")
                + " on " + name + " or create " + (an ? "an " : "a ") + size + " Spirit token?";
    }

    public static boolean doEndure(Permanent permanent, int amount, Game game, Ability source) {
        if (permanent == null || amount < 1) {
            return false;
        }
        Player controller = game.getPlayer(permanent.getControllerId());
        if (controller == null) {
            return false;
        }
        if (permanent.getZoneChangeCounter(game) == game.getState().getZoneChangeCounter(permanent.getId())
                && controller.chooseUse(
                Outcome.BoostCreature, choiceText(permanent.getName(), amount),
                null, "Add counters", "Create token", source, game
        )) {
            return permanent.addCounters(CounterType.P1P1.createInstance(amount), source, game);
        }

        switch (amount) {
            case 1:
                return new NoFlyingSpiritWhite11Token().putOntoBattlefield(1, game, source);
            case 2:
                return new NoFlyingSpiritWhite22Token().putOntoBattlefield(1, game, source);
            case 3:
                return new NoFlyingSpiritWhite33Token().putOntoBattlefield(1, game, source);
            default:
                return new SpiritXXToken(amount).putOntoBattlefield(1, game, source);
        }
    }
}
