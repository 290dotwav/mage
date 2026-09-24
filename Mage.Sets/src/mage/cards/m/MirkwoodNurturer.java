package mage.cards.m;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.EntersBattlefieldTriggeredAbility;
import mage.abilities.effects.OneShotEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.counters.CounterType;
import mage.filter.FilterPermanent;
import mage.filter.common.FilterControlledPermanent;
import mage.filter.predicate.mageobject.AnotherPredicate;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.TargetPermanent;

import java.util.UUID;

/**
 * @author Claude
 */
public final class MirkwoodNurturer extends CardImpl {

    private static final FilterPermanent filter = new FilterControlledPermanent("other target permanent you control");

    static {
        filter.add(AnotherPredicate.instance);
    }

    public MirkwoodNurturer(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{G/U}");

        this.subtype.add(SubType.ELF);
        this.subtype.add(SubType.RANGER);
        this.power = new MageInt(3);
        this.toughness = new MageInt(2);

        // When this creature enters, return up to one other target permanent you control to its owner's hand. If you do, put a +1/+1 counter on this creature.
        Ability ability = new EntersBattlefieldTriggeredAbility(new MirkwoodNurturerEffect());
        ability.addTarget(new TargetPermanent(0, 1, filter));
        this.addAbility(ability);
    }

    private MirkwoodNurturer(final MirkwoodNurturer card) {
        super(card);
    }

    @Override
    public MirkwoodNurturer copy() {
        return new MirkwoodNurturer(this);
    }
}

class MirkwoodNurturerEffect extends OneShotEffect {

    MirkwoodNurturerEffect() {
        super(Outcome.ReturnToHand);
        staticText = "return up to one other target permanent you control to its owner's hand. " +
                "If you do, put a +1/+1 counter on {this}";
    }

    private MirkwoodNurturerEffect(final MirkwoodNurturerEffect effect) {
        super(effect);
    }

    @Override
    public MirkwoodNurturerEffect copy() {
        return new MirkwoodNurturerEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player player = game.getPlayer(source.getControllerId());
        Permanent permanent = game.getPermanent(getTargetPointer().getFirst(game, source));
        if (player == null || permanent == null) {
            return false;
        }
        player.moveCards(permanent, Zone.HAND, source, game);
        if (game.getState().getZone(permanent.getId()) != Zone.HAND) {
            // a token ceases to exist in its owner's hand, but it was still returned
            if (!permanent.isToken() || game.getPermanent(permanent.getId()) != null) {
                return false;
            }
        }
        Permanent sourcePermanent = source.getSourcePermanentIfItStillExists(game);
        if (sourcePermanent != null) {
            sourcePermanent.addCounters(CounterType.P1P1.createInstance(), source.getControllerId(), source, game);
        }
        return true;
    }
}
