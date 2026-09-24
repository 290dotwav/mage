package mage.cards.m;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.effects.ContinuousEffectImpl;
import mage.abilities.keyword.MiracleAbility;
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
import mage.game.Game;
import mage.players.Player;
import mage.watchers.common.MiracleWatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Claude
 */
public final class MoleculeMan extends CardImpl {

    public MoleculeMan(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{6}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.VILLAIN);
        this.power = new MageInt(5);
        this.toughness = new MageInt(5);

        // Nonland cards in your hand have miracle {0}.
        // (the watcher is needed even if no card had miracle when the game began)
        this.addAbility(new SimpleStaticAbility(new MoleculeManEffect()), new MiracleWatcher());
    }

    private MoleculeMan(final MoleculeMan card) {
        super(card);
    }

    @Override
    public MoleculeMan copy() {
        return new MoleculeMan(this);
    }
}

class MoleculeManEffect extends ContinuousEffectImpl {

    private final Map<UUID, MiracleAbility> miracleAbilities = new HashMap<>();

    MoleculeManEffect() {
        super(Duration.WhileOnBattlefield, Layer.AbilityAddingRemovingEffects_6, SubLayer.NA, Outcome.AddAbility);
        staticText = "nonland cards in your hand have miracle {0}. "
                + "<i>(You may cast a card for its miracle cost when you draw it "
                + "if it's the first card you drew this turn.)</i>";
    }

    private MoleculeManEffect(final MoleculeManEffect effect) {
        super(effect);
        this.miracleAbilities.putAll(effect.miracleAbilities);
    }

    @Override
    public MoleculeManEffect copy() {
        return new MoleculeManEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        for (Card card : controller.getHand().getCards(game)) {
            if (card.isLand(game)) {
                continue;
            }
            MiracleAbility ability = miracleAbilities.computeIfAbsent(
                    card.getId(), k -> new MiracleAbility("{0}")
            );
            game.getState().addOtherAbility(card, ability, false);
        }
        return true;
    }
}
