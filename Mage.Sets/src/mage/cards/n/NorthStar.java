package mage.cards.n;

import mage.MageObjectReference;
import mage.abilities.Ability;
import mage.abilities.SpellAbility;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.costs.common.TapSourceCost;
import mage.Mana;
import mage.abilities.costs.mana.ManaCost;
import mage.abilities.costs.mana.VariableManaCost;
import mage.abilities.costs.mana.GenericManaCost;
import mage.abilities.effects.AsThoughEffectImpl;
import mage.abilities.effects.AsThoughManaEffect;
import mage.abilities.effects.common.AddContinuousEffectToGame;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.AsThoughEffectType;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.ManaType;
import mage.constants.Outcome;
import mage.game.Game;
import mage.players.ManaPoolItem;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author Claude
 */
public final class NorthStar extends CardImpl {

    public NorthStar(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ARTIFACT}, "{4}");

        // {4}, {T}: For one spell this turn, you may spend mana as though it were mana of any type to pay that spell's mana cost.
        Ability ability = new SimpleActivatedAbility(
                new AddContinuousEffectToGame(new NorthStarEffect()), new GenericManaCost(4)
        );
        ability.addCost(new TapSourceCost());
        this.addAbility(ability);
    }

    private NorthStar(final NorthStar card) {
        super(card);
    }

    @Override
    public NorthStar copy() {
        return new NorthStar(this);
    }
}

/**
 * The effect is tied to the first spell for which it's actually needed (mana of another type is spent as the
 * needed type): from then on it only works for that spell.
 */
class NorthStarEffect extends AsThoughEffectImpl implements AsThoughManaEffect {

    // the spell being paid, set by applies() right before getAsThoughManaType() is asked
    private UUID checkedSpellId = null;
    // the mana types of the symbols that spell still has to pay (other requests are generic payment attempts)
    private final Set<ManaType> checkedSpellNeededTypes = EnumSet.noneOf(ManaType.class);

    NorthStarEffect() {
        super(AsThoughEffectType.SPEND_OTHER_MANA, Duration.EndOfTurn, Outcome.Benefit);
        staticText = "for one spell this turn, you may spend mana as though it were mana of any type " +
                "to pay that spell's mana cost. <i>(Additional costs are still paid normally.)</i>";
    }

    private NorthStarEffect(final NorthStarEffect effect) {
        super(effect);
        this.checkedSpellId = effect.checkedSpellId;
        this.checkedSpellNeededTypes.addAll(effect.checkedSpellNeededTypes);
    }

    @Override
    public NorthStarEffect copy() {
        return new NorthStarEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        return true;
    }

    private String getKey() {
        return "NorthStarSpell_" + getId();
    }

    @Override
    public boolean applies(UUID objectId, Ability source, UUID affectedControllerId, Game game) {
        return false;
    }

    @Override
    public boolean applies(UUID objectId, Ability affectedAbility, Ability source, Game game, UUID playerId) {
        if (!(affectedAbility instanceof SpellAbility) || !source.isControlledBy(playerId)) {
            return false;
        }
        MageObjectReference used = (MageObjectReference) game.getState().getValue(getKey());
        if (used != null && !used.refersTo(affectedAbility.getSourceId(), game)) {
            return false;
        }
        checkedSpellId = affectedAbility.getSourceId();
        checkedSpellNeededTypes.clear();
        for (ManaCost cost : affectedAbility.getManaCostsToPay()) {
            if (cost.isPaid() || cost instanceof GenericManaCost || cost instanceof VariableManaCost) {
                continue;
            }
            for (Mana option : cost.getManaOptions()) {
                for (ManaType type : ManaType.values()) {
                    if (type != ManaType.GENERIC && option.get(type) > 0) {
                        checkedSpellNeededTypes.add(type);
                    }
                }
            }
        }
        return true;
    }

    @Override
    public ManaType getAsThoughManaType(ManaType manaType, ManaPoolItem mana, UUID affectedControllerId, Ability source, Game game) {
        if (mana.get(manaType) > 0) {
            return manaType;
        }
        if (!checkedSpellNeededTypes.contains(manaType)) {
            // generic mana is paid by trying each type in turn: nothing to convert
            return manaType;
        }
        ManaType other = mana.getFirstAvailable();
        if (other != null && checkedSpellId != null && !game.inCheckPlayableState()
                && game.getState().getValue(getKey()) == null) {
            // from now on, only for this spell
            game.getState().setValue(getKey(), new MageObjectReference(checkedSpellId, game));
        }
        return other;
    }
}
