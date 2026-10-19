package mage.cards.d;

import mage.MageInt;
import mage.MageObject;
import mage.abilities.Ability;
import mage.abilities.common.DealsCombatDamageToAPlayerTriggeredAbility;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.condition.Condition;
import mage.abilities.costs.AlternativeCostSourceAbility;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.ContinuousEffectImpl;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.asthought.PlayFromNotOwnHandZoneTargetEffect;
import mage.abilities.keyword.CrewAbility;
import mage.abilities.keyword.FlyingAbility;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.ModalDoubleFacedCardHalf;
import mage.cards.SpellOptionCard;
import mage.cards.SplitCardHalf;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Layer;
import mage.constants.Outcome;
import mage.constants.SubLayer;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.TargetController;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.game.stack.Spell;
import mage.players.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author Claude
 */
public final class DragonsSmile extends CardImpl {

    public DragonsSmile(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ARTIFACT}, "{5}{R}{R}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.VEHICLE);
        this.power = new MageInt(4);
        this.toughness = new MageInt(5);

        // Flying
        this.addAbility(FlyingAbility.getInstance());

        // Once during each of your turns, you may pay {0} rather than pay the mana cost for a spell you cast from exile.
        this.addAbility(new SimpleStaticAbility(new DragonsSmileAddAltCostEffect()));

        // Whenever Dragon's Smile deals combat damage to a player, that player exiles that many cards from the top of their library. You may play those cards this turn.
        this.addAbility(new DealsCombatDamageToAPlayerTriggeredAbility(new DragonsSmileExileEffect(), false, true));

        // Crew 3
        this.addAbility(new CrewAbility(3));
    }

    private DragonsSmile(final DragonsSmile card) {
        super(card);
    }

    @Override
    public DragonsSmile copy() {
        return new DragonsSmile(this);
    }
}

// based on Warped Space
enum DragonsSmileExiledSpellCondition implements Condition {
    instance;

    @Override
    public boolean apply(Game game, Ability source) {
        MageObject object = game.getObject(source);
        if (object instanceof SplitCardHalf || object instanceof SpellOptionCard || object instanceof ModalDoubleFacedCardHalf) {
            UUID mainCardId = ((Card) object).getMainCard().getId();
            object = game.getObject(mainCardId);
        }
        if (object instanceof Spell) {
            return Zone.EXILED.equals(((Spell) object).getFromZone());
        }
        if (object instanceof Card) {
            return game.getExile().getCard(((Card) object).getId(), game) != null;
        }
        return false;
    }
}

class DragonsSmileAlternativeCost extends AlternativeCostSourceAbility {

    DragonsSmileAlternativeCost() {
        super(new ManaCostsImpl<>("{0}"), DragonsSmileExiledSpellCondition.instance);
    }

    private DragonsSmileAlternativeCost(final DragonsSmileAlternativeCost ability) {
        super(ability);
    }

    @Override
    public DragonsSmileAlternativeCost copy() {
        return new DragonsSmileAlternativeCost(this);
    }

    @Override
    public boolean activateAlternativeCosts(Ability ability, Game game) {
        if (!super.activateAlternativeCosts(ability, game)) {
            return false;
        }
        Permanent provider = game.getPermanent(getSourceId());
        if (provider != null) {
            game.getState().setValue(DragonsSmileAddAltCostEffect.usedKey(provider, game), true);
        }
        return true;
    }
}

class DragonsSmileAddAltCostEffect extends ContinuousEffectImpl {

    DragonsSmileAddAltCostEffect() {
        super(Duration.WhileOnBattlefield, Outcome.Benefit);
        staticText = "once during each of your turns, you may pay {0} rather than pay the mana cost "
                + "for a spell you cast from exile";
    }

    private DragonsSmileAddAltCostEffect(final DragonsSmileAddAltCostEffect effect) {
        super(effect);
    }

    @Override
    public DragonsSmileAddAltCostEffect copy() {
        return new DragonsSmileAddAltCostEffect(this);
    }

    static String usedKey(Permanent provider, Game game) {
        return "DragonsSmile" + provider.getId() + provider.getZoneChangeCounter(game) + "-" + game.getTurnNum();
    }

    @Override
    public boolean apply(Layer layer, SubLayer sublayer, Ability source, Game game) {
        Player controller = game.getPlayer(source.getControllerId());
        Permanent sourcePermanent = game.getPermanent(source.getSourceId());
        if (controller == null || sourcePermanent == null) {
            return false;
        }
        if (!game.isActivePlayer(controller.getId())) {
            return true;
        }
        if (game.getState().getValue(usedKey(sourcePermanent, game)) == null) {
            DragonsSmileAlternativeCost alternateCostAbility = new DragonsSmileAlternativeCost();
            alternateCostAbility.setSourceId(source.getSourceId());
            controller.getAlternativeSourceCosts().add(alternateCostAbility);
        }
        return true;
    }

    @Override
    public boolean apply(Game game, Ability source) {
        return false;
    }

    @Override
    public boolean hasLayer(Layer layer) {
        return layer == Layer.RulesEffects;
    }
}

class DragonsSmileExileEffect extends OneShotEffect {

    DragonsSmileExileEffect() {
        super(Outcome.PlayForFree);
        staticText = "that player exiles that many cards from the top of their library. You may play those cards this turn";
    }

    private DragonsSmileExileEffect(final DragonsSmileExileEffect effect) {
        super(effect);
    }

    @Override
    public DragonsSmileExileEffect copy() {
        return new DragonsSmileExileEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player player = game.getPlayer(getTargetPointer().getFirst(game, source));
        Integer damage = (Integer) getValue("damage");
        if (player == null || damage == null || damage < 1) {
            return false;
        }
        Set<Card> cards = new HashSet<>(player.getLibrary().getTopCards(game, damage));
        return PlayFromNotOwnHandZoneTargetEffect.exileAndPlayFromExile(
                game, source, cards, TargetController.YOU, Duration.EndOfTurn, false, false, false
        );
    }
}
