package mage.cards.l;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.costs.common.DiscardCardCost;
import mage.abilities.effects.ContinuousEffectImpl;
import mage.abilities.effects.common.DoIfCostPaid;
import mage.abilities.effects.common.DrawCardSourceControllerEffect;
import mage.abilities.keyword.FlyingAbility;
import mage.abilities.keyword.HasteAbility;
import mage.abilities.keyword.MiracleAbility;
import mage.abilities.triggers.BeginningOfUpkeepTriggeredAbility;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Layer;
import mage.constants.Outcome;
import mage.constants.SubLayer;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.TargetController;
import mage.game.Game;
import mage.players.Player;
import mage.watchers.common.MiracleWatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Claude
 */
public final class LoreholdTheHistorian extends CardImpl {

    public LoreholdTheHistorian(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{3}{R}{W}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.ELDER);
        this.subtype.add(SubType.DRAGON);
        this.power = new MageInt(5);
        this.toughness = new MageInt(5);

        // Flying
        this.addAbility(FlyingAbility.getInstance());

        // Haste
        this.addAbility(HasteAbility.getInstance());

        // Each instant and sorcery card in your hand has miracle {2}.
        // (the watcher is needed even if no card had miracle when the game began)
        this.addAbility(new SimpleStaticAbility(new LoreholdTheHistorianEffect()), new MiracleWatcher());

        // At the beginning of each opponent's upkeep, you may discard a card. If you do, draw a card.
        this.addAbility(new BeginningOfUpkeepTriggeredAbility(
                TargetController.OPPONENT,
                new DoIfCostPaid(new DrawCardSourceControllerEffect(1), new DiscardCardCost()),
                false
        ));
    }

    private LoreholdTheHistorian(final LoreholdTheHistorian card) {
        super(card);
    }

    @Override
    public LoreholdTheHistorian copy() {
        return new LoreholdTheHistorian(this);
    }
}

class LoreholdTheHistorianEffect extends ContinuousEffectImpl {

    private final Map<UUID, MiracleAbility> miracleAbilities = new HashMap<>();

    LoreholdTheHistorianEffect() {
        super(Duration.WhileOnBattlefield, Layer.AbilityAddingRemovingEffects_6, SubLayer.NA, Outcome.AddAbility);
        staticText = "each instant and sorcery card in your hand has miracle {2}. "
                + "<i>(You may cast a card for its miracle cost when you draw it "
                + "if it's the first card you drew this turn.)</i>";
    }

    private LoreholdTheHistorianEffect(final LoreholdTheHistorianEffect effect) {
        super(effect);
        this.miracleAbilities.putAll(effect.miracleAbilities);
    }

    @Override
    public LoreholdTheHistorianEffect copy() {
        return new LoreholdTheHistorianEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        for (Card card : controller.getHand().getCards(game)) {
            if (!card.isInstantOrSorcery(game)) {
                continue;
            }
            MiracleAbility ability = miracleAbilities.computeIfAbsent(
                    card.getId(), k -> new MiracleAbility("{2}")
            );
            game.getState().addOtherAbility(card, ability, false);
        }
        return true;
    }
}
