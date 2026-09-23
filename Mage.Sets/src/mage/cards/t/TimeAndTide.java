package mage.cards.t;

import mage.abilities.Ability;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.keyword.PhasingAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.game.Game;
import mage.game.permanent.Permanent;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Claude
 */
public final class TimeAndTide extends CardImpl {

    public TimeAndTide(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.INSTANT}, "{U}{U}");

        // Simultaneously, all phased-out creatures phase in and all creatures with phasing phase out.
        this.getSpellAbility().addEffect(new TimeAndTideEffect());
    }

    private TimeAndTide(final TimeAndTide card) {
        super(card);
    }

    @Override
    public TimeAndTide copy() {
        return new TimeAndTide(this);
    }
}

class TimeAndTideEffect extends OneShotEffect {

    TimeAndTideEffect() {
        super(Outcome.Benefit);
        staticText = "simultaneously, all phased-out creatures phase in and all creatures with phasing phase out";
    }

    private TimeAndTideEffect(final TimeAndTideEffect effect) {
        super(effect);
    }

    @Override
    public TimeAndTideEffect copy() {
        return new TimeAndTideEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        // both groups are decided first, so a creature that phases in doesn't phase out again
        List<Permanent> toPhaseIn = game.getBattlefield().getAllPermanents()
                .stream()
                .filter(permanent -> !permanent.isPhasedIn()
                        && !permanent.isPhasedOutIndirectly()
                        && permanent.isCreature(game))
                .collect(Collectors.toList());
        List<Permanent> toPhaseOut = game.getBattlefield().getAllActivePermanents()
                .stream()
                .filter(permanent -> permanent.isCreature(game)
                        && permanent.hasAbility(PhasingAbility.getInstance(), game))
                .collect(Collectors.toList());
        for (Permanent permanent : toPhaseIn) {
            permanent.phaseIn(game);
        }
        for (Permanent permanent : toPhaseOut) {
            permanent.phaseOut(game);
        }
        return true;
    }
}
