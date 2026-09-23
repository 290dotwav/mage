package mage.cards.d;

import mage.MageIdentifier;
import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.costs.Cost;
import mage.abilities.costs.Costs;
import mage.abilities.costs.CostsImpl;
import mage.abilities.costs.common.DiscardCardCost;
import mage.abilities.dynamicvalue.DynamicValue;
import mage.abilities.effects.AsThoughEffectImpl;
import mage.abilities.effects.Effect;
import mage.abilities.effects.common.continuous.SetBasePowerSourceEffect;
import mage.abilities.keyword.FlyingAbility;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.AsThoughEffectType;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.filter.FilterPermanent;
import mage.filter.StaticFilters;
import mage.filter.common.FilterControlledPermanent;
import mage.filter.predicate.Predicates;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;

import java.util.UUID;

/**
 * @author Claude
 */
public final class DragonManReformedRobot extends CardImpl {

    public DragonManReformedRobot(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ARTIFACT, CardType.CREATURE}, "{2}{W}{U}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.DRAGON);
        this.subtype.add(SubType.ROBOT);
        this.power = new MageInt(0);
        this.toughness = new MageInt(5);

        // Flying
        this.addAbility(FlyingAbility.getInstance());

        // Dragon Man's power is equal to the greatest mana value among noncreature permanents you control and noncreature cards in your graveyard.
        this.addAbility(new SimpleStaticAbility(Zone.ALL, new SetBasePowerSourceEffect(DragonManReformedRobotValue.instance)
                .setText("{this}'s power is equal to the greatest mana value among noncreature permanents you control and noncreature cards in your graveyard")));

        // You may cast this card from your graveyard by discarding a card in addition to paying its other costs.
        this.addAbility(new SimpleStaticAbility(Zone.GRAVEYARD, new DragonManReformedRobotCastEffect())
                .setIdentifier(MageIdentifier.DragonManReformedRobotAlternateCast));
    }

    private DragonManReformedRobot(final DragonManReformedRobot card) {
        super(card);
    }

    @Override
    public DragonManReformedRobot copy() {
        return new DragonManReformedRobot(this);
    }
}

enum DragonManReformedRobotValue implements DynamicValue {
    instance;

    private static final FilterPermanent filter = new FilterControlledPermanent("noncreature permanent you control");

    static {
        filter.add(Predicates.not(CardType.CREATURE.getPredicate()));
    }

    @Override
    public int calculate(Game game, Ability sourceAbility, Effect effect) {
        int max = 0;
        for (Permanent permanent : game.getBattlefield().getActivePermanents(
                filter, sourceAbility.getControllerId(), sourceAbility, game)) {
            max = Math.max(max, permanent.getManaValue());
        }
        Player player = game.getPlayer(sourceAbility.getControllerId());
        if (player != null) {
            for (Card card : player.getGraveyard().getCards(StaticFilters.FILTER_CARD_NON_CREATURE, player.getId(), sourceAbility, game)) {
                max = Math.max(max, card.getManaValue());
            }
        }
        return max;
    }

    @Override
    public DragonManReformedRobotValue copy() {
        return this;
    }

    @Override
    public String getMessage() {
        return "the greatest mana value among noncreature permanents you control and noncreature cards in your graveyard";
    }

    @Override
    public String toString() {
        return "X";
    }
}

class DragonManReformedRobotCastEffect extends AsThoughEffectImpl {

    DragonManReformedRobotCastEffect() {
        super(AsThoughEffectType.CAST_FROM_NOT_OWN_HAND_ZONE, Duration.EndOfGame, Outcome.PutCreatureInPlay);
        this.staticText = "you may cast this card from your graveyard by discarding a card in addition to paying its other costs";
    }

    private DragonManReformedRobotCastEffect(final DragonManReformedRobotCastEffect effect) {
        super(effect);
    }

    @Override
    public DragonManReformedRobotCastEffect copy() {
        return new DragonManReformedRobotCastEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        return true;
    }

    @Override
    public boolean applies(UUID objectId, Ability source, UUID affectedControllerId, Game game) {
        if (!objectId.equals(source.getSourceId())
                || !source.isControlledBy(affectedControllerId)
                || game.getState().getZone(objectId) != Zone.GRAVEYARD) {
            return false;
        }
        Player controller = game.getPlayer(affectedControllerId);
        Card card = game.getCard(objectId);
        // no card to discard: the additional cost can't be paid, so it can't be cast this way (601.2f-h)
        if (controller == null || card == null || controller.getHand().isEmpty()) {
            return false;
        }
        Costs<Cost> costs = new CostsImpl<>();
        costs.add(new DiscardCardCost());
        controller.setCastSourceIdWithAlternateMana(
                objectId, card.getManaCost().copy(), costs,
                MageIdentifier.DragonManReformedRobotAlternateCast
        );
        return true;
    }
}
