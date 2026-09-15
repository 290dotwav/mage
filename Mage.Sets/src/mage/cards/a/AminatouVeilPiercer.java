package mage.cards.a;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.costs.mana.ManaCost;
import mage.abilities.costs.mana.ManaCosts;
import mage.abilities.effects.ContinuousEffectImpl;
import mage.abilities.effects.keyword.SurveilEffect;
import mage.abilities.keyword.MiracleAbility;
import mage.abilities.triggers.BeginningOfUpkeepTriggeredAbility;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.*;
import mage.game.Game;
import mage.players.Player;
import mage.util.CardUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author ClaudeMTG
 */
public final class AminatouVeilPiercer extends CardImpl {

    public AminatouVeilPiercer(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{1}{W}{U}{B}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.WIZARD);
        this.power = new MageInt(2);
        this.toughness = new MageInt(4);

        // At the beginning of your upkeep, surveil 2.
        this.addAbility(new BeginningOfUpkeepTriggeredAbility(new SurveilEffect(2)));

        // Each enchantment card in your hand has miracle. Its miracle cost is equal to its mana cost reduced by {4}.
        // TODO: MiracleWatcher looks at the drawn card's abilities in the DREW_CARD event handler, which
        //  runs before the continuous effects are applied again, so a granted miracle is not offered on
        //  the draw yet. It needs a game.applyEffects() call before MiracleWatcher.checkMiracleAbility.
        this.addAbility(new SimpleStaticAbility(new AminatouVeilPiercerEffect()));
    }

    private AminatouVeilPiercer(final AminatouVeilPiercer card) {
        super(card);
    }

    @Override
    public AminatouVeilPiercer copy() {
        return new AminatouVeilPiercer(this);
    }
}

class AminatouVeilPiercerEffect extends ContinuousEffectImpl {

    private final Map<UUID, MiracleAbility> miracleAbilities = new HashMap<>();

    AminatouVeilPiercerEffect() {
        super(Duration.WhileOnBattlefield, Layer.AbilityAddingRemovingEffects_6, SubLayer.NA, Outcome.AddAbility);
        staticText = "each enchantment card in your hand has miracle. "
                + "Its miracle cost is equal to its mana cost reduced by {4}. "
                + "<i>(You may cast a card for its miracle cost when you draw it "
                + "if it's the first card you drew this turn.)</i>";
    }

    private AminatouVeilPiercerEffect(final AminatouVeilPiercerEffect effect) {
        super(effect);
        this.miracleAbilities.putAll(effect.miracleAbilities);
    }

    @Override
    public AminatouVeilPiercerEffect copy() {
        return new AminatouVeilPiercerEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        for (Card card : controller.getHand().getCards(game)) {
            if (!card.isEnchantment(game)) {
                continue;
            }
            MiracleAbility ability = miracleAbilities.computeIfAbsent(
                    card.getId(), k -> new MiracleAbility(getMiracleCost(card, game))
            );
            game.getState().addOtherAbility(card, ability, false);
        }
        return true;
    }

    private static String getMiracleCost(Card card, Game game) {
        ManaCosts<ManaCost> costs = card.getSpellAbility() == null
                ? card.getManaCost()
                : card.getSpellAbility().getManaCosts();
        return CardUtil.reduceCost(costs.copy(), 4).getText();
    }
}
