package mage.cards.f;

import mage.ObjectColor;
import mage.abilities.Ability;
import mage.abilities.dynamicvalue.DynamicValue;
import mage.abilities.dynamicvalue.common.ColorsAmongControlledPermanentsCount;
import mage.abilities.effects.Effect;
import mage.abilities.effects.common.DrawCardSourceControllerEffect;
import mage.abilities.effects.common.GainLifeEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.game.Game;
import mage.game.stack.Spell;
import mage.watchers.common.SpellsCastWatcher;

import java.util.UUID;

/**
 * @author Claude
 */
public final class FirstFamily extends CardImpl {

    public FirstFamily(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.INSTANT}, "{2}{G}{U}");

        // You draw X cards and gain X life, where X is the number of colors among permanents you control and spells you've cast this turn.
        this.getSpellAbility().addEffect(new DrawCardSourceControllerEffect(FirstFamilyValue.instance, true)
                .setText("you draw X cards"));
        this.getSpellAbility().addEffect(new GainLifeEffect(FirstFamilyValue.instance)
                .setText("and gain X life, where X is the number of colors among permanents you control and spells you've cast this turn"));
    }

    private FirstFamily(final FirstFamily card) {
        super(card);
    }

    @Override
    public FirstFamily copy() {
        return new FirstFamily(this);
    }
}

enum FirstFamilyValue implements DynamicValue {
    instance;

    @Override
    public int calculate(Game game, Ability sourceAbility, Effect effect) {
        ObjectColor color = ColorsAmongControlledPermanentsCount.ALL_PERMANENTS.getAllControlledColors(game, sourceAbility);
        SpellsCastWatcher watcher = game.getState().getWatcher(SpellsCastWatcher.class);
        if (watcher != null) {
            for (Spell spell : watcher.getSpellsCastThisTurn(sourceAbility.getControllerId())) {
                color = color.union(spell.getColor(game));
            }
        }
        return color.getColorCount();
    }

    @Override
    public FirstFamilyValue copy() {
        return this;
    }

    @Override
    public String getMessage() {
        return "the number of colors among permanents you control and spells you've cast this turn";
    }

    @Override
    public String toString() {
        return "X";
    }
}
