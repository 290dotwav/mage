package mage.cards.t;

import mage.abilities.Ability;
import mage.abilities.common.MayPayAdditionalManaCostAbility;
import mage.abilities.effects.OneShotEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.game.Game;
import mage.players.Player;

import java.util.UUID;

/**
 * @author Claude
 */
public final class TasteOfParadise extends CardImpl {

    static final String COSTS_TAG = "TasteOfParadiseTimesPaid";

    public TasteOfParadise(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.SORCERY}, "{3}{G}");

        // As an additional cost to cast this spell, you may pay {1}{G} any number of times.
        this.addAbility(new MayPayAdditionalManaCostAbility("{1}{G}", true, COSTS_TAG));

        // You gain 3 life plus an additional 3 life for each additional {1}{G} you paid.
        this.getSpellAbility().addEffect(new TasteOfParadiseEffect());
    }

    private TasteOfParadise(final TasteOfParadise card) {
        super(card);
    }

    @Override
    public TasteOfParadise copy() {
        return new TasteOfParadise(this);
    }
}

class TasteOfParadiseEffect extends OneShotEffect {

    TasteOfParadiseEffect() {
        super(Outcome.GainLife);
        staticText = "you gain 3 life plus an additional 3 life for each additional {1}{G} you paid";
    }

    private TasteOfParadiseEffect(final TasteOfParadiseEffect effect) {
        super(effect);
    }

    @Override
    public TasteOfParadiseEffect copy() {
        return new TasteOfParadiseEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player player = game.getPlayer(source.getControllerId());
        if (player == null) {
            return false;
        }
        int times = MayPayAdditionalManaCostAbility.getTimesPaid(game, source, TasteOfParadise.COSTS_TAG);
        player.gainLife(3 + 3 * times, game, source);
        return true;
    }
}
