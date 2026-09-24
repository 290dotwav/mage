package mage.cards.k;

import mage.abilities.Ability;
import mage.abilities.common.EntersBattlefieldTriggeredAbility;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.condition.common.CastFromEverywhereSourceCondition;
import mage.abilities.costs.common.ExileSourceCost;
import mage.abilities.costs.common.TapSourceCost;
import mage.abilities.costs.mana.GenericManaCost;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.BecomesMonarchSourceEffect;
import mage.abilities.keyword.FlashAbility;
import mage.abilities.hint.common.MonarchHint;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.constants.CardType;
import mage.constants.ComparisonType;
import mage.constants.Outcome;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.filter.FilterPermanent;
import mage.filter.predicate.mageobject.ManaValuePredicate;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.game.permanent.PermanentToken;
import mage.players.Player;
import mage.target.TargetPermanent;
import mage.target.targetadjustment.ForEachPlayerTargetsAdjuster;
import mage.target.targetpointer.EachTargetPointer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author Claude
 */
public final class KingSolomonsFrogs extends CardImpl {

    private static final FilterPermanent filter = new FilterPermanent("permanent with mana value 3 or greater");

    static {
        filter.add(new ManaValuePredicate(ComparisonType.MORE_THAN, 2));
    }

    public KingSolomonsFrogs(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ARTIFACT}, "{3}{W}");

        this.supertype.add(SuperType.LEGENDARY);

        // Flash
        this.addAbility(FlashAbility.getInstance());

        // When King Solomon's Frogs enters, if you cast it, for each opponent, exile up to one target permanent that player controls with mana value 3 or greater. For each permanent exiled this way, its controller draws a card.
        Ability ability = new EntersBattlefieldTriggeredAbility(new KingSolomonsFrogsEffect())
                .withInterveningIf(CastFromEverywhereSourceCondition.instance);
        ability.addTarget(new TargetPermanent(0, 1, filter));
        ability.setTargetAdjuster(new ForEachPlayerTargetsAdjuster(false, true));
        this.addAbility(ability);

        // {3}, {T}, Exile King Solomon's Frogs: You become the monarch.
        Ability monarchAbility = new SimpleActivatedAbility(new BecomesMonarchSourceEffect(), new GenericManaCost(3));
        monarchAbility.addCost(new TapSourceCost());
        monarchAbility.addCost(new ExileSourceCost());
        monarchAbility.addHint(MonarchHint.instance);
        this.addAbility(monarchAbility);
    }

    private KingSolomonsFrogs(final KingSolomonsFrogs card) {
        super(card);
    }

    @Override
    public KingSolomonsFrogs copy() {
        return new KingSolomonsFrogs(this);
    }
}

class KingSolomonsFrogsEffect extends OneShotEffect {

    KingSolomonsFrogsEffect() {
        super(Outcome.Exile);
        this.setTargetPointer(new EachTargetPointer());
        staticText = "for each opponent, exile up to one target permanent that player controls with mana value 3 or greater. " +
                "For each permanent exiled this way, its controller draws a card";
    }

    private KingSolomonsFrogsEffect(final KingSolomonsFrogsEffect effect) {
        super(effect);
    }

    @Override
    public KingSolomonsFrogsEffect copy() {
        return new KingSolomonsFrogsEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        Map<UUID, UUID> controllerOf = new HashMap<>();
        Set<UUID> tokens = new HashSet<>();
        Cards cards = new CardsImpl();
        for (UUID targetId : getTargetPointer().getTargets(game, source)) {
            Permanent permanent = game.getPermanent(targetId);
            if (permanent != null) {
                controllerOf.put(permanent.getId(), permanent.getControllerId());
                if (permanent instanceof PermanentToken) {
                    tokens.add(permanent.getId());
                }
                cards.add(permanent);
            }
        }
        if (cards.isEmpty()) {
            return false;
        }
        controller.moveCards(cards, Zone.EXILED, source, game);
        Map<UUID, Integer> draws = new HashMap<>();
        for (Map.Entry<UUID, UUID> entry : controllerOf.entrySet()) {
            // a token that was exiled has ceased to exist; a card must actually be in exile (not, e.g., a commander moved to the command zone instead)
            boolean exiled = tokens.contains(entry.getKey())
                    ? game.getPermanent(entry.getKey()) == null
                    : game.getState().getZone(entry.getKey()) == Zone.EXILED;
            if (exiled) {
                draws.merge(entry.getValue(), 1, Integer::sum);
            }
        }
        for (Map.Entry<UUID, Integer> entry : draws.entrySet()) {
            Player player = game.getPlayer(entry.getKey());
            if (player != null) {
                player.drawCards(entry.getValue(), source, game);
            }
        }
        return true;
    }
}
