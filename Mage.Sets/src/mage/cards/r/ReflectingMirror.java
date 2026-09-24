package mage.cards.r;

import mage.MageItem;
import mage.abilities.Ability;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.costs.common.TapSourceCost;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.dynamicvalue.DynamicValue;
import mage.abilities.effects.Effect;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.InfoEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.ComparisonType;
import mage.constants.Outcome;
import mage.filter.FilterSpell;
import mage.filter.predicate.ObjectSourcePlayer;
import mage.filter.predicate.Predicate;
import mage.filter.predicate.ObjectSourcePlayerPredicate;
import mage.filter.predicate.other.NumberOfTargetsPredicate;
import mage.game.Game;
import mage.game.stack.Spell;
import mage.game.stack.StackObject;
import mage.players.Player;
import mage.target.Target;
import mage.target.TargetSpell;
import mage.target.targetadjustment.ManaValueTargetAdjuster;
import mage.util.CardUtil;

import java.util.UUID;

/**
 * @author Claude
 */
public final class ReflectingMirror extends CardImpl {

    private static final FilterSpell filter = new FilterSpell("spell with a single target if that target is you");

    static {
        filter.add(new NumberOfTargetsPredicate(1));
        filter.add(ReflectingMirrorPredicate.instance);
    }

    public ReflectingMirror(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ARTIFACT}, "{4}");

        // {X}, {T}: Change the target of target spell with a single target if that target is you. The new target must be a player. X is twice the mana value of that spell.
        Ability ability = new SimpleActivatedAbility(new ReflectingMirrorEffect(), new ManaCostsImpl<>("{X}"));
        ability.addCost(new TapSourceCost());
        ability.addEffect(new InfoEffect("X is twice the mana value of that spell"));
        ability.addTarget(new TargetSpell(filter));
        ability.setTargetAdjuster(new ManaValueTargetAdjuster(ReflectingMirrorValue.instance, ComparisonType.EQUAL_TO));
        this.addAbility(ability);
    }

    private ReflectingMirror(final ReflectingMirror card) {
        super(card);
    }

    @Override
    public ReflectingMirror copy() {
        return new ReflectingMirror(this);
    }
}

enum ReflectingMirrorPredicate implements ObjectSourcePlayerPredicate<StackObject> {
    instance;

    @Override
    public boolean apply(ObjectSourcePlayer<StackObject> input, Game game) {
        for (UUID modeId : input.getObject().getStackAbility().getModes().getSelectedModes()) {
            for (Target target : input.getObject().getStackAbility().getModes().get(modeId).getTargets()) {
                if (target.getTargets().contains(input.getPlayerId())) {
                    return true;
                }
            }
        }
        return false;
    }
}

/**
 * The spell's mana value must be X / 2 (an odd X matches nothing)
 */
enum ReflectingMirrorValue implements DynamicValue {
    instance;

    @Override
    public int calculate(Game game, Ability sourceAbility, Effect effect) {
        int xValue = CardUtil.getSourceCostsTagX(game, sourceAbility, 0);
        return xValue % 2 == 0 ? xValue / 2 : -1;
    }

    @Override
    public ReflectingMirrorValue copy() {
        return this;
    }

    @Override
    public String getMessage() {
        return "half X";
    }

    @Override
    public String toString() {
        return "X/2";
    }
}

class ReflectingMirrorEffect extends OneShotEffect {

    ReflectingMirrorEffect() {
        super(Outcome.Neutral);
        staticText = "change the target of target spell with a single target if that target is you. " +
                "The new target must be a player";
    }

    private ReflectingMirrorEffect(final ReflectingMirrorEffect effect) {
        super(effect);
    }

    @Override
    public ReflectingMirrorEffect copy() {
        return new ReflectingMirrorEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Spell spell = game.getStack().getSpell(getTargetPointer().getFirst(game, source));
        Player controller = game.getPlayer(source.getControllerId());
        if (spell == null || controller == null) {
            return false;
        }
        return spell.chooseNewTargets(game, controller.getId(), true, true, ReflectingMirrorPlayerPredicate.instance);
    }
}

enum ReflectingMirrorPlayerPredicate implements Predicate<MageItem> {
    instance;

    @Override
    public boolean apply(MageItem input, Game game) {
        return input instanceof Player;
    }
}
