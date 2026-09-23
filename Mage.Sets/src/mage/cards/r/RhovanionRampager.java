package mage.cards.r;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.AttacksTriggeredAbility;
import mage.abilities.common.DiesSourceTriggeredAbility;
import mage.abilities.dynamicvalue.common.SourcePermanentPowerValue;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.keyword.AmassEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.counters.CounterType;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.common.TargetSacrifice;

import java.util.UUID;

/**
 * @author Claude
 */
public final class RhovanionRampager extends CardImpl {

    public RhovanionRampager(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{B}");

        this.subtype.add(SubType.WOLF);
        this.power = new MageInt(3);
        this.toughness = new MageInt(2);

        // Whenever this creature attacks, you may sacrifice another creature. If you do, put a number of +1/+1 counters on this creature equal to the sacrificed creature's power.
        this.addAbility(new AttacksTriggeredAbility(new RhovanionRampagerEffect()));

        // When this creature dies, amass Goblins X, where X is this creature's power.
        this.addAbility(new DiesSourceTriggeredAbility(
                new AmassEffect(SourcePermanentPowerValue.NOT_NEGATIVE, SubType.GOBLIN, false)
                        .setText("amass Goblins X, where X is {this}'s power. <i>(Put X +1/+1 counters " +
                                "on an Army you control. It's also a Goblin. If you don't control an Army, " +
                                "create a 0/0 black Goblin Army creature token first.)</i>")
        ));
    }

    private RhovanionRampager(final RhovanionRampager card) {
        super(card);
    }

    @Override
    public RhovanionRampager copy() {
        return new RhovanionRampager(this);
    }
}

class RhovanionRampagerEffect extends OneShotEffect {

    RhovanionRampagerEffect() {
        super(Outcome.BoostCreature);
        staticText = "you may sacrifice another creature. If you do, put a number of +1/+1 counters " +
                "on {this} equal to the sacrificed creature's power";
    }

    private RhovanionRampagerEffect(final RhovanionRampagerEffect effect) {
        super(effect);
    }

    @Override
    public RhovanionRampagerEffect copy() {
        return new RhovanionRampagerEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player player = game.getPlayer(source.getControllerId());
        if (player == null) {
            return false;
        }
        TargetSacrifice target = new TargetSacrifice(StaticFilters.FILTER_CONTROLLED_ANOTHER_CREATURE);
        if (!target.canChoose(player.getId(), source, game)
                || !player.chooseUse(outcome, "Sacrifice another creature?", source, game)) {
            return false;
        }
        player.choose(Outcome.Sacrifice, target, source, game);
        Permanent permanent = game.getPermanent(target.getFirstTarget());
        if (permanent == null) {
            return false;
        }
        int power = permanent.getPower().getValue();
        if (!permanent.sacrifice(source, game)) {
            return false;
        }
        Permanent sourcePermanent = source.getSourcePermanentIfItStillExists(game);
        if (sourcePermanent != null && power > 0) {
            sourcePermanent.addCounters(CounterType.P1P1.createInstance(power), source.getControllerId(), source, game);
        }
        return true;
    }
}
