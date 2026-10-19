package mage.game.permanent.token;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.keyword.FlyingAbility;
import mage.abilities.keyword.TrampleAbility;
import mage.abilities.triggers.BeginningOfUpkeepTriggeredAbility;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.filter.common.FilterControlledCreaturePermanent;
import mage.filter.predicate.mageobject.AnotherPredicate;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.Target;
import mage.target.common.TargetSacrifice;

/**
 * Lord of the Pit token (Thomil, the Destroyer)
 *
 * @author Claude
 */
public final class LordOfThePitToken extends TokenImpl {

    public LordOfThePitToken() {
        super("Lord of the Pit", "Lord of the Pit token");
        manaCost = new ManaCostsImpl<>("{4}{B}{B}{B}");
        cardType.add(CardType.CREATURE);
        color.setBlack(true);
        subtype.add(SubType.DEMON);
        power = new MageInt(7);
        toughness = new MageInt(7);

        addAbility(FlyingAbility.getInstance());
        addAbility(TrampleAbility.getInstance());
        // At the beginning of your upkeep, sacrifice another creature. If you can't, this token deals 7 damage to you.
        addAbility(new BeginningOfUpkeepTriggeredAbility(new LordOfThePitTokenEffect()));
    }

    private LordOfThePitToken(final LordOfThePitToken token) {
        super(token);
    }

    @Override
    public LordOfThePitToken copy() {
        return new LordOfThePitToken(this);
    }
}

class LordOfThePitTokenEffect extends OneShotEffect {

    private static final FilterControlledCreaturePermanent filter = new FilterControlledCreaturePermanent("another creature");

    static {
        filter.add(AnotherPredicate.instance);
    }

    LordOfThePitTokenEffect() {
        super(Outcome.Damage);
        this.staticText = "sacrifice another creature. If you can't, this token deals 7 damage to you";
    }

    private LordOfThePitTokenEffect(final LordOfThePitTokenEffect effect) {
        super(effect);
    }

    @Override
    public LordOfThePitTokenEffect copy() {
        return new LordOfThePitTokenEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player player = game.getPlayer(source.getControllerId());
        if (player == null) {
            return false;
        }
        Target target = new TargetSacrifice(filter);
        if (target.canChoose(player.getId(), source, game)) {
            player.choose(Outcome.Sacrifice, target, source, game);
            Permanent permanent = game.getPermanent(target.getFirstTarget());
            if (permanent != null) {
                permanent.sacrifice(source, game);
                return true;
            }
            return false;
        }
        player.damage(7, source.getSourceId(), source, game);
        return true;
    }
}
