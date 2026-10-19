package mage.cards.u;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.assignment.common.CardTypeAssignment;
import mage.abilities.common.EntersBattlefieldTriggeredAbility;
import mage.abilities.condition.common.CastFromEverywhereSourceCondition;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.keyword.FlyingAbility;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.filter.FilterCard;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.players.Player;
import mage.target.common.TargetCardInYourGraveyard;
import mage.util.CardUtil;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

/**
 * @author Claude
 */
public final class UldarosTheorix extends CardImpl {

    public UldarosTheorix(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{3}{U}{B}{B}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.ELDER);
        this.subtype.add(SubType.SPHINX);
        this.power = new MageInt(5);
        this.toughness = new MageInt(5);

        // Flying
        this.addAbility(FlyingAbility.getInstance());

        // When Uldaros Theorix enters, if you cast him, exile up to one target nonland card of each card type from your graveyard. Copy those cards. You may cast any number of spells with total mana value 6 or less from among the copies without paying their mana costs.
        Ability ability = new EntersBattlefieldTriggeredAbility(new UldarosTheorixEffect())
                .withInterveningIf(CastFromEverywhereSourceCondition.instance);
        ability.addTarget(new UldarosTheorixTarget());
        this.addAbility(ability);
    }

    private UldarosTheorix(final UldarosTheorix card) {
        super(card);
    }

    @Override
    public UldarosTheorix copy() {
        return new UldarosTheorix(this);
    }
}

class UldarosTheorixEffect extends OneShotEffect {

    UldarosTheorixEffect() {
        super(Outcome.PlayForFree);
        staticText = "exile up to one target nonland card of each card type from your graveyard. Copy those cards. "
                + "You may cast any number of spells with total mana value 6 or less from among the copies "
                + "without paying their mana costs. <i>(Permanent spells cast this way become tokens.)</i>";
    }

    private UldarosTheorixEffect(final UldarosTheorixEffect effect) {
        super(effect);
    }

    @Override
    public UldarosTheorixEffect copy() {
        return new UldarosTheorixEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        Cards cards = new CardsImpl(getTargetPointer().getTargets(game, source));
        if (cards.isEmpty()) {
            return false;
        }
        controller.moveCards(cards, Zone.EXILED, source, game);
        cards.retainZone(Zone.EXILED, game);
        Cards copies = new CardsImpl();
        for (Card card : cards.getCards(game)) {
            copies.add(game.copyCard(card, source, source.getControllerId()));
        }
        if (copies.isEmpty()) {
            return true;
        }
        CardUtil.castMultipleWithAttributeForFree(
                controller, source, game, copies, StaticFilters.FILTER_CARD,
                Integer.MAX_VALUE, new UldarosTheorixTracker()
        );
        return true;
    }
}

class UldarosTheorixTracker implements CardUtil.SpellCastTracker {

    private int totalManaValue = 0;

    @Override
    public boolean checkCard(Card card, Game game) {
        return card.getManaValue() + totalManaValue <= 6;
    }

    @Override
    public void addCard(Card card, Ability source, Game game) {
        totalManaValue += card.getManaValue();
    }
}

class UldarosTheorixTarget extends TargetCardInYourGraveyard {

    private static final FilterCard filter = new FilterCard("nonland card of each card type from your graveyard");

    static {
        filter.add(mage.filter.predicate.Predicates.not(CardType.LAND.getPredicate()));
    }

    private static final CardTypeAssignment cardTypeAssigner = new CardTypeAssignment(
            Arrays.stream(CardType.values()).filter(type -> type != CardType.LAND).toArray(CardType[]::new)
    );

    UldarosTheorixTarget() {
        super(0, Integer.MAX_VALUE, filter);
    }

    private UldarosTheorixTarget(final UldarosTheorixTarget target) {
        super(target);
    }

    @Override
    public UldarosTheorixTarget copy() {
        return new UldarosTheorixTarget(this);
    }

    @Override
    public Set<UUID> possibleTargets(UUID sourceControllerId, Ability source, Game game) {
        Set<UUID> possibleTargets = super.possibleTargets(sourceControllerId, source, game);
        // each chosen card must stand for a card type of its own
        Cards existingTargets = new CardsImpl(this.getTargets());
        possibleTargets.removeIf(id -> {
            Card card = game.getCard(id);
            if (card == null) {
                return true;
            }
            if (existingTargets.contains(id)) {
                return false;
            }
            Cards newTargets = existingTargets.copy();
            newTargets.add(card);
            return cardTypeAssigner.getRoleCount(newTargets, game) < newTargets.size();
        });
        return possibleTargets;
    }
}
