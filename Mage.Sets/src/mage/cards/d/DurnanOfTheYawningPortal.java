package mage.cards.d;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.SpellAbility;
import mage.abilities.common.AttacksTriggeredAbility;
import mage.abilities.common.ChooseABackgroundAbility;
import mage.abilities.dynamicvalue.common.OpponentsCount;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.asthought.PlayFromNotOwnHandZoneTargetEffect;
import mage.abilities.effects.common.cost.CostModificationEffectImpl;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.constants.*;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.players.Player;
import mage.target.TargetCard;
import mage.util.CardUtil;

import java.util.UUID;

/**
 * @author ClaudeMTG
 */
public final class DurnanOfTheYawningPortal extends CardImpl {

    public DurnanOfTheYawningPortal(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{3}{G}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.WARRIOR);
        this.power = new MageInt(3);
        this.toughness = new MageInt(3);

        // Whenever Durnan attacks, look at the top four cards of your library. You may exile a creature card
        // from among them. Put the rest on the bottom of your library in any order. For as long as that card
        // remains exiled, you may cast it. That spell has undaunted.
        this.addAbility(new AttacksTriggeredAbility(new DurnanOfTheYawningPortalEffect()));

        // Choose a Background
        this.addAbility(ChooseABackgroundAbility.getInstance());
    }

    private DurnanOfTheYawningPortal(final DurnanOfTheYawningPortal card) {
        super(card);
    }

    @Override
    public DurnanOfTheYawningPortal copy() {
        return new DurnanOfTheYawningPortal(this);
    }
}

class DurnanOfTheYawningPortalEffect extends OneShotEffect {

    DurnanOfTheYawningPortalEffect() {
        super(Outcome.Benefit);
        staticText = "look at the top four cards of your library. You may exile a creature card from among them. "
                + "Put the rest on the bottom of your library in any order. For as long as that card remains "
                + "exiled, you may cast it. That spell has undaunted. <i>(It costs {1} less to cast for each opponent.)</i>";
    }

    private DurnanOfTheYawningPortalEffect(final DurnanOfTheYawningPortalEffect effect) {
        super(effect);
    }

    @Override
    public DurnanOfTheYawningPortalEffect copy() {
        return new DurnanOfTheYawningPortalEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        Cards cards = new CardsImpl(controller.getLibrary().getTopCards(game, 4));
        if (cards.isEmpty()) {
            return true;
        }
        controller.lookAtCards(source, null, cards, game);
        TargetCard target = new TargetCard(
                0, 1, Zone.LIBRARY, StaticFilters.FILTER_CARD_CREATURE_A
        );
        target.withNotTarget(true);
        controller.choose(Outcome.Exile, cards, target, source, game);
        Card card = game.getCard(target.getFirstTarget());
        if (card != null) {
            cards.remove(card);
            PlayFromNotOwnHandZoneTargetEffect.exileAndPlayFromExile(
                    game, source, card, TargetController.YOU,
                    Duration.EndOfGame, false, false, true
            );
            game.addEffect(new DurnanOfTheYawningPortalCostEffect(card.getId()), source);
        }
        controller.putCardsOnBottomOfLibrary(cards, game, source, true);
        return true;
    }
}

/**
 * Gives undaunted to the spell cast from exile. The ability can't simply be added to the card,
 * because a card loses its added abilities when it changes zone (exile to stack).
 */
class DurnanOfTheYawningPortalCostEffect extends CostModificationEffectImpl {

    private final UUID cardId;

    DurnanOfTheYawningPortalCostEffect(UUID cardId) {
        super(Duration.EndOfGame, Outcome.Benefit, CostModificationType.REDUCE_COST);
        this.cardId = cardId;
        this.staticText = "";
    }

    private DurnanOfTheYawningPortalCostEffect(final DurnanOfTheYawningPortalCostEffect effect) {
        super(effect);
        this.cardId = effect.cardId;
    }

    @Override
    public boolean apply(Game game, Ability source, Ability abilityToModify) {
        CardUtil.reduceCost(abilityToModify, OpponentsCount.instance.calculate(game, abilityToModify, this));
        return true;
    }

    @Override
    public boolean applies(Ability abilityToModify, Ability source, Game game) {
        if (!(abilityToModify instanceof SpellAbility)
                || !cardId.equals(abilityToModify.getSourceId())) {
            return false;
        }
        switch (game.getState().getZone(cardId)) {
            case EXILED:
            case STACK:
                return true;
            default:
                // the card left exile on its own, the permission and the undaunted are gone
                this.discard();
                return false;
        }
    }

    @Override
    public DurnanOfTheYawningPortalCostEffect copy() {
        return new DurnanOfTheYawningPortalCostEffect(this);
    }
}
