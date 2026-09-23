package mage.cards.a;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.EntersBattlefieldTriggeredAbility;
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
public final class AzogMoriasRuin extends CardImpl {

    public AzogMoriasRuin(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{B}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.GOBLIN);
        this.subtype.add(SubType.SOLDIER);
        this.power = new MageInt(1);
        this.toughness = new MageInt(3);

        // When Azog enters, destroy up to one other target creature. Its controller amasses Goblins X, where X is that creature's power. If you controlled that creature, draw a card.
        Ability ability = new EntersBattlefieldTriggeredAbility(new AzogMoriasRuinEffect());
        ability.addTarget(new TargetPermanent(0, 1, StaticFilters.FILTER_ANOTHER_TARGET_CREATURE));
        this.addAbility(ability);
    }

    private AzogMoriasRuin(final AzogMoriasRuin card) {
        super(card);
    }

    @Override
    public AzogMoriasRuin copy() {
        return new AzogMoriasRuin(this);
    }
}

class AzogMoriasRuinEffect extends OneShotEffect {

    AzogMoriasRuinEffect() {
        super(Outcome.DestroyPermanent);
        staticText = "destroy up to one other target creature. Its controller amasses Goblins X, " +
                "where X is that creature's power. If you controlled that creature, draw a card. " +
                "<i>(To amass Goblins X, that player puts X +1/+1 counters on an Army they control. " +
                "It's also a Goblin. If they don't control an Army, they create a 0/0 black Goblin Army " +
                "creature token first.)</i>";
    }

    private AzogMoriasRuinEffect(final AzogMoriasRuinEffect effect) {
        super(effect);
    }

    @Override
    public AzogMoriasRuinEffect copy() {
        return new AzogMoriasRuinEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent permanent = game.getPermanent(getTargetPointer().getFirst(game, source));
        if (permanent == null) {
            return false;
        }
        UUID controllerId = permanent.getControllerId();
        permanent.destroy(source, game, false);
        game.processAction();
        // power as it last existed on the battlefield
        Permanent lki = game.getPermanentOrLKIBattlefield(permanent.getId());
        int power = Math.max(0, (lki != null ? lki : permanent).getPower().getValue());
        AmassEffect.doAmass(power, SubType.GOBLIN, game, source, controllerId);
        if (source.isControlledBy(controllerId)) {
            Player you = game.getPlayer(source.getControllerId());
            if (you != null) {
                you.drawCards(1, source, game);
            }
        }
        return true;
    }
}
