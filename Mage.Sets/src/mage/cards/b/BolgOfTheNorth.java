package mage.cards.b;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.EntersBattlefieldTriggeredAbility;
import mage.abilities.common.delayed.ReflexiveTriggeredAbility;
import mage.abilities.costs.common.SacrificeTargetCost;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.keyword.AmassEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.TargetPermanent;

import java.util.UUID;

/**
 * @author Claude
 */
public final class BolgOfTheNorth extends CardImpl {

    public BolgOfTheNorth(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{3}{B}{R}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.GOBLIN);
        this.subtype.add(SubType.SOLDIER);
        this.power = new MageInt(5);
        this.toughness = new MageInt(5);

        // When Bolg enters, you may sacrifice another creature. When you do, Bolg deals damage equal to that creature's power to another target creature. If excess damage was dealt this way, amass Goblins X, where X is that excess damage.
        this.addAbility(new EntersBattlefieldTriggeredAbility(new BolgOfTheNorthEffect()));
    }

    private BolgOfTheNorth(final BolgOfTheNorth card) {
        super(card);
    }

    @Override
    public BolgOfTheNorth copy() {
        return new BolgOfTheNorth(this);
    }
}

class BolgOfTheNorthEffect extends OneShotEffect {

    BolgOfTheNorthEffect() {
        super(Outcome.Damage);
        staticText = "you may sacrifice another creature. When you do, {this} deals damage equal to that " +
                "creature's power to another target creature. If excess damage was dealt this way, " +
                "amass Goblins X, where X is that excess damage. <i>(Put X +1/+1 counters on an Army you control. " +
                "It's also a Goblin. If you don't control an Army, create a 0/0 black Goblin Army creature token first.)</i>";
    }

    private BolgOfTheNorthEffect(final BolgOfTheNorthEffect effect) {
        super(effect);
    }

    @Override
    public BolgOfTheNorthEffect copy() {
        return new BolgOfTheNorthEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        SacrificeTargetCost cost = new SacrificeTargetCost(StaticFilters.FILTER_CONTROLLED_ANOTHER_CREATURE);
        if (!cost.canPay(source, source, source.getControllerId(), game)
                || !controller.chooseUse(outcome, "Sacrifice another creature?", source, game)
                || !cost.pay(source, game, source, source.getControllerId(), false)) {
            return false;
        }
        int power = cost
                .getPermanents()
                .stream()
                .mapToInt(permanent -> permanent.getPower().getValue())
                .sum();
        ReflexiveTriggeredAbility trigger = new ReflexiveTriggeredAbility(
                new BolgOfTheNorthDamageEffect(power), false,
                "{this} deals damage equal to that creature's power to another target creature. " +
                        "If excess damage was dealt this way, amass Goblins X, where X is that excess damage"
        );
        trigger.addTarget(new TargetPermanent(StaticFilters.FILTER_ANOTHER_CREATURE));
        game.fireReflexiveTriggeredAbility(trigger, source);
        return true;
    }
}

class BolgOfTheNorthDamageEffect extends OneShotEffect {

    private final int amount;

    BolgOfTheNorthDamageEffect(int amount) {
        super(Outcome.Damage);
        this.amount = amount;
    }

    private BolgOfTheNorthDamageEffect(final BolgOfTheNorthDamageEffect effect) {
        super(effect);
        this.amount = effect.amount;
    }

    @Override
    public BolgOfTheNorthDamageEffect copy() {
        return new BolgOfTheNorthDamageEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent permanent = game.getPermanent(getTargetPointer().getFirst(game, source));
        if (permanent == null || amount < 1) {
            return false;
        }
        int excess = permanent.damageWithExcess(amount, source, game);
        if (excess > 0) {
            AmassEffect.doAmass(excess, SubType.GOBLIN, game, source);
        }
        return true;
    }
}
