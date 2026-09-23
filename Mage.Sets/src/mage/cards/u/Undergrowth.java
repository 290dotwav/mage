package mage.cards.u;

import mage.ObjectColor;
import mage.abilities.Ability;
import mage.abilities.common.MayPayAdditionalManaCostAbility;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.PreventAllDamageByAllPermanentsEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.filter.FilterPermanent;
import mage.filter.predicate.Predicates;
import mage.filter.predicate.mageobject.ColorPredicate;
import mage.game.Game;

import java.util.UUID;

/**
 * @author Claude
 */
public final class Undergrowth extends CardImpl {

    static final String COSTS_TAG = "UndergrowthPaid";

    public Undergrowth(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.INSTANT}, "{G}");

        // As an additional cost to cast this spell, you may pay {2}{R}.
        this.addAbility(new MayPayAdditionalManaCostAbility("{2}{R}", false, COSTS_TAG));

        // Prevent all combat damage that would be dealt this turn. If this spell's additional cost was paid, this effect doesn't affect combat damage that would be dealt by red creatures.
        this.getSpellAbility().addEffect(new UndergrowthEffect());
    }

    private Undergrowth(final Undergrowth card) {
        super(card);
    }

    @Override
    public Undergrowth copy() {
        return new Undergrowth(this);
    }
}

class UndergrowthEffect extends OneShotEffect {

    private static final FilterPermanent filter = new FilterPermanent("sources other than red creatures");

    static {
        filter.add(Predicates.not(Predicates.and(
                CardType.CREATURE.getPredicate(),
                new ColorPredicate(ObjectColor.RED)
        )));
    }

    UndergrowthEffect() {
        super(Outcome.PreventDamage);
        staticText = "prevent all combat damage that would be dealt this turn. If this spell's additional cost " +
                "was paid, this effect doesn't affect combat damage that would be dealt by red creatures";
    }

    private UndergrowthEffect(final UndergrowthEffect effect) {
        super(effect);
    }

    @Override
    public UndergrowthEffect copy() {
        return new UndergrowthEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        boolean paid = MayPayAdditionalManaCostAbility.getTimesPaid(game, source, Undergrowth.COSTS_TAG) > 0;
        game.addEffect(paid
                ? new PreventAllDamageByAllPermanentsEffect(filter, Duration.EndOfTurn, true)
                : new PreventAllDamageByAllPermanentsEffect(Duration.EndOfTurn, true), source);
        return true;
    }
}
