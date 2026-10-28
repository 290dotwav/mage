package mage.cards.a;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.AttacksTriggeredAbility;
import mage.abilities.common.EntersBattlefieldTriggeredAbility;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.AssimilateTargetEffect;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.filter.StaticFilters;
import mage.game.ExileZone;
import mage.game.Game;
import mage.players.Player;
import mage.target.TargetCard;
import mage.target.common.TargetOpponent;
import mage.util.CardUtil;

import java.util.UUID;

/**
 * @author Claude
 */
public final class AssimilationAssessor extends CardImpl {

    public AssimilationAssessor(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ARTIFACT, CardType.CREATURE}, "{4}{B}");

        this.subtype.add(SubType.BORG);
        this.power = new MageInt(5);
        this.toughness = new MageInt(5);

        // When this creature enters, exile target opponent's graveyard.
        Ability ability = new EntersBattlefieldTriggeredAbility(new AssimilationAssessorExileEffect());
        ability.addTarget(new TargetOpponent());
        this.addAbility(ability);

        // Whenever this creature attacks, assimilate a creature card exiled with it.
        this.addAbility(new AttacksTriggeredAbility(new AssimilationAssessorAssimilateEffect()));
    }

    private AssimilationAssessor(final AssimilationAssessor card) {
        super(card);
    }

    @Override
    public AssimilationAssessor copy() {
        return new AssimilationAssessor(this);
    }
}

class AssimilationAssessorExileEffect extends OneShotEffect {

    AssimilationAssessorExileEffect() {
        super(Outcome.Exile);
        staticText = "exile target opponent's graveyard";
    }

    private AssimilationAssessorExileEffect(final AssimilationAssessorExileEffect effect) {
        super(effect);
    }

    @Override
    public AssimilationAssessorExileEffect copy() {
        return new AssimilationAssessorExileEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        Player opponent = game.getPlayer(getTargetPointer().getFirst(game, source));
        if (controller == null || opponent == null) {
            return false;
        }
        Cards cards = new CardsImpl(opponent.getGraveyard());
        if (cards.isEmpty()) {
            return true;
        }
        return controller.moveCardsToExile(
                cards.getCards(game), source, game, true,
                CardUtil.getExileZoneId(game, source), CardUtil.getSourceName(game, source)
        );
    }
}

class AssimilationAssessorAssimilateEffect extends OneShotEffect {

    AssimilationAssessorAssimilateEffect() {
        super(Outcome.PutCreatureInPlay);
        staticText = "assimilate a creature card exiled with it. " + AssimilateTargetEffect.CARD_REMINDER;
    }

    private AssimilationAssessorAssimilateEffect(final AssimilationAssessorAssimilateEffect effect) {
        super(effect);
    }

    @Override
    public AssimilationAssessorAssimilateEffect copy() {
        return new AssimilationAssessorAssimilateEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        ExileZone exileZone = game.getExile().getExileZone(CardUtil.getExileZoneId(game, source));
        if (controller == null || exileZone == null) {
            return false;
        }
        Cards creatures = new CardsImpl();
        for (Card card : exileZone.getCards(game)) {
            if (card.isCreature(game)) {
                creatures.add(card);
            }
        }
        if (creatures.isEmpty()) {
            return false;
        }
        TargetCard target = new TargetCard(Zone.EXILED, StaticFilters.FILTER_CARD_CREATURE);
        target.withNotTarget(true);
        target.withChooseHint("to assimilate");
        controller.choose(Outcome.PutCreatureInPlay, creatures, target, source, game);
        Card card = game.getCard(target.getFirstTarget());
        if (card == null) {
            return false;
        }
        return AssimilateTargetEffect.assimilateCard(card, controller, source, game) != null;
    }
}
