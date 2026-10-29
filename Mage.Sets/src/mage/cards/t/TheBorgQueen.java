package mage.cards.t;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.EntersBattlefieldOrAttacksSourceTriggeredAbility;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.AssimilateTargetEffect;
import mage.abilities.effects.common.continuous.BoostControlledEffect;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.players.Player;
import mage.target.common.TargetOpponent;

import java.util.UUID;

/**
 * @author Claude
 */
public final class TheBorgQueen extends CardImpl {

    public TheBorgQueen(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ARTIFACT, CardType.CREATURE}, "{4}{W}{U}{B}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.BORG);
        this.subtype.add(SubType.NOBLE);
        this.power = new MageInt(4);
        this.toughness = new MageInt(4);

        // Artifact creatures you control get +2/+2.
        this.addAbility(new SimpleStaticAbility(new BoostControlledEffect(
                2, 2, Duration.WhileOnBattlefield, StaticFilters.FILTER_PERMANENTS_ARTIFACT_CREATURE
        )));

        // Whenever The Borg Queen enters or attacks, target opponent exiles cards from the top of their library until they exile a creature card. Assimilate that card. They put the rest on the bottom in a random order.
        Ability ability = new EntersBattlefieldOrAttacksSourceTriggeredAbility(new TheBorgQueenEffect());
        ability.addTarget(new TargetOpponent());
        this.addAbility(ability);
    }

    private TheBorgQueen(final TheBorgQueen card) {
        super(card);
    }

    @Override
    public TheBorgQueen copy() {
        return new TheBorgQueen(this);
    }
}

class TheBorgQueenEffect extends OneShotEffect {

    TheBorgQueenEffect() {
        super(Outcome.PutCreatureInPlay);
        staticText = "target opponent exiles cards from the top of their library until they exile a creature card. "
                + "Assimilate that card. They put the rest on the bottom in a random order. "
                + AssimilateTargetEffect.CARD_REMINDER;
    }

    private TheBorgQueenEffect(final TheBorgQueenEffect effect) {
        super(effect);
    }

    @Override
    public TheBorgQueenEffect copy() {
        return new TheBorgQueenEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        Player opponent = game.getPlayer(getTargetPointer().getFirst(game, source));
        if (controller == null || opponent == null) {
            return false;
        }
        Cards exiled = new CardsImpl();
        Card creature = null;
        for (Card card : opponent.getLibrary().getCards(game)) {
            exiled.add(card);
            opponent.moveCards(card, Zone.EXILED, source, game);
            if (card.isCreature(game)) {
                creature = card;
                break;
            }
        }
        if (creature != null && game.getState().getZone(creature.getId()) == Zone.EXILED) {
            exiled.remove(creature);
            AssimilateTargetEffect.assimilateCard(creature, controller, source, game);
        }
        exiled.retainZone(Zone.EXILED, game);
        opponent.putCardsOnBottomOfLibrary(exiled, game, source, false);
        return true;
    }
}
