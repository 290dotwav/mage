package mage.cards.g;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.Mode;
import mage.abilities.common.AsEntersBattlefieldAbility;
import mage.abilities.common.SpellCastOpponentTriggeredAbility;
import mage.abilities.effects.common.ChooseModeEffect;
import mage.abilities.effects.common.DrawCardSourceControllerEffect;
import mage.abilities.effects.common.GainLifeEffect;
import mage.abilities.effects.common.LoseLifeOpponentsEffect;
import mage.abilities.effects.common.counter.AddCountersSourceEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.ModeChoice;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.counters.CounterType;
import mage.filter.FilterSpell;
import mage.filter.predicate.ObjectSourcePlayer;
import mage.filter.predicate.ObjectSourcePlayerPredicate;
import mage.game.Game;
import mage.game.stack.StackObject;

import java.util.UUID;

/**
 * @author Claude
 */
public final class GollumRiddleMaster extends CardImpl {

    private static final FilterSpell filter = new FilterSpell("a spell with mana value of the chosen quality");

    static {
        filter.add(GollumRiddleMasterPredicate.instance);
    }

    public GollumRiddleMaster(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{1}{B}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HALFLING);
        this.subtype.add(SubType.HORROR);
        this.power = new MageInt(3);
        this.toughness = new MageInt(1);

        // As Gollum enters, choose odd or even. (Zero is even.)
        this.addAbility(new AsEntersBattlefieldAbility(
                new ChooseModeEffect(ModeChoice.ODD, ModeChoice.EVEN)
                        .setText("choose odd or even. <i>(Zero is even.)</i>")
        ));

        // Whenever an opponent casts a spell with mana value of the chosen quality, choose one that hasn't been chosen --
        // * Put a +1/+1 counter on Gollum.
        Ability ability = new SpellCastOpponentTriggeredAbility(
                new AddCountersSourceEffect(CounterType.P1P1.createInstance()), filter, false
        );
        ability.getModes().setLimitUsageByOnce(false);

        // * Each opponent loses 2 life and you gain 2 life.
        ability.addMode(new Mode(new LoseLifeOpponentsEffect(2))
                .addEffect(new GainLifeEffect(2).concatBy("and")));

        // * Draw a card.
        ability.addMode(new Mode(new DrawCardSourceControllerEffect(1)));
        this.addAbility(ability);
    }

    private GollumRiddleMaster(final GollumRiddleMaster card) {
        super(card);
    }

    @Override
    public GollumRiddleMaster copy() {
        return new GollumRiddleMaster(this);
    }
}

enum GollumRiddleMasterPredicate implements ObjectSourcePlayerPredicate<StackObject> {
    instance;

    @Override
    public boolean apply(ObjectSourcePlayer<StackObject> input, Game game) {
        int manaValue = input.getObject().getManaValue();
        if (ModeChoice.ODD.checkMode(game, input.getSource())) {
            return manaValue % 2 == 1;
        } else if (ModeChoice.EVEN.checkMode(game, input.getSource())) {
            return manaValue % 2 == 0;
        }
        return false;
    }
}
