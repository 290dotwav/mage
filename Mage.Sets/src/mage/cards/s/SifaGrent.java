package mage.cards.s;

import mage.abilities.Ability;
import mage.abilities.LoyaltyAbility;
import mage.abilities.common.CanBeYourCommanderAbility;
import mage.abilities.common.DiesCreatureTriggeredAbility;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.GainLifeEffect;
import mage.abilities.effects.common.LoseLifeTargetEffect;
import mage.abilities.effects.common.asthought.PlayFromNotOwnHandZoneTargetEffect;
import mage.abilities.effects.common.combat.GoadTargetEffect;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.TargetController;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.players.Player;
import mage.target.common.TargetCreaturePermanent;
import mage.target.common.TargetOpponent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author Claude
 */
public final class SifaGrent extends CardImpl {

    public SifaGrent(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.PLANESWALKER}, "{3}{B}{R}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.SIFA);
        this.setStartingLoyalty(4);

        // Whenever a creature dies, target opponent loses 1 life and you gain 1 life.
        Ability ability = new DiesCreatureTriggeredAbility(
                new LoseLifeTargetEffect(1), false, StaticFilters.FILTER_PERMANENT_A_CREATURE
        );
        ability.addEffect(new GainLifeEffect(1).concatBy("and"));
        ability.addTarget(new TargetOpponent());
        this.addAbility(ability);

        // +1: Goad up to two target creatures.
        ability = new LoyaltyAbility(new GoadTargetEffect(), 1);
        ability.addTarget(new TargetCreaturePermanent(0, 2));
        this.addAbility(ability);

        // -6: Each opponent exiles the top two cards of their library. Until end of turn, you may play those cards without paying their mana costs.
        this.addAbility(new LoyaltyAbility(new SifaGrentEffect(), -6));

        // Sifa Grent can be your commander.
        this.addAbility(CanBeYourCommanderAbility.getInstance());
    }

    private SifaGrent(final SifaGrent card) {
        super(card);
    }

    @Override
    public SifaGrent copy() {
        return new SifaGrent(this);
    }
}

class SifaGrentEffect extends OneShotEffect {

    SifaGrentEffect() {
        super(Outcome.PlayForFree);
        staticText = "each opponent exiles the top two cards of their library. Until end of turn, "
                + "you may play those cards without paying their mana costs";
    }

    private SifaGrentEffect(final SifaGrentEffect effect) {
        super(effect);
    }

    @Override
    public SifaGrentEffect copy() {
        return new SifaGrentEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        Set<Card> cards = new HashSet<>();
        for (UUID opponentId : game.getOpponents(controller.getId())) {
            Player opponent = game.getPlayer(opponentId);
            if (opponent != null) {
                cards.addAll(opponent.getLibrary().getTopCards(game, 2));
            }
        }
        return PlayFromNotOwnHandZoneTargetEffect.exileAndPlayFromExile(
                game, source, cards, TargetController.YOU, Duration.EndOfTurn, true, false, false
        );
    }
}
