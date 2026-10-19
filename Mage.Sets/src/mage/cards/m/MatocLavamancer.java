package mage.cards.m;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.BatchTriggeredAbility;
import mage.abilities.TriggeredAbilityImpl;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.costs.common.ExileFromGraveCost;
import mage.abilities.costs.common.TapSourceCost;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.common.DamageTargetEffect;
import mage.abilities.effects.common.ExileTopXMayPlayUntilEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.filter.StaticFilters;
import mage.filter.common.FilterAnyTarget;
import mage.filter.common.FilterPermanentOrPlayer;
import mage.filter.predicate.mageobject.AnotherPredicate;
import mage.game.Game;
import mage.game.events.DamagedBatchBySourceEvent;
import mage.game.events.DamagedEvent;
import mage.game.events.GameEvent;
import mage.target.common.TargetCardInYourGraveyard;
import mage.target.common.TargetPermanentOrPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Claude
 */
public final class MatocLavamancer extends CardImpl {

    private static final FilterPermanentOrPlayer filter = new FilterAnyTarget("any other target");

    static {
        filter.getPermanentFilter().add(AnotherPredicate.instance);
    }

    public MatocLavamancer(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{3}{R}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.BARBARIAN);
        this.subtype.add(SubType.WIZARD);
        this.power = new MageInt(2);
        this.toughness = new MageInt(3);

        // Whenever a source you control deals damage, if at least one permanent or player was dealt exactly 2 of that damage, exile the top card of your library. You may play it until the end of your next turn.
        this.addAbility(new MatocLavamancerTriggeredAbility());

        // {R}, {T}, Exile two cards from your graveyard: Matoc deals 2 damage to any other target.
        Ability ability = new SimpleActivatedAbility(new DamageTargetEffect(2), new ManaCostsImpl<>("{R}"));
        ability.addCost(new TapSourceCost());
        ability.addCost(new ExileFromGraveCost(new TargetCardInYourGraveyard(2, StaticFilters.FILTER_CARDS_FROM_YOUR_GRAVEYARD)));
        ability.addTarget(new TargetPermanentOrPlayer(filter));
        this.addAbility(ability);
    }

    private MatocLavamancer(final MatocLavamancer card) {
        super(card);
    }

    @Override
    public MatocLavamancer copy() {
        return new MatocLavamancer(this);
    }
}

class MatocLavamancerTriggeredAbility extends TriggeredAbilityImpl implements BatchTriggeredAbility<DamagedEvent> {

    MatocLavamancerTriggeredAbility() {
        super(Zone.BATTLEFIELD, new ExileTopXMayPlayUntilEffect(1, Duration.UntilEndOfYourNextTurn));
        setTriggerPhrase("Whenever a source you control deals damage, if at least one permanent or player "
                + "was dealt exactly 2 of that damage, ");
    }

    private MatocLavamancerTriggeredAbility(final MatocLavamancerTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public MatocLavamancerTriggeredAbility copy() {
        return new MatocLavamancerTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.DAMAGED_BATCH_BY_SOURCE;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        if (!isControlledBy(game.getControllerId(event.getSourceId()))) {
            return false;
        }
        Map<UUID, Integer> damageByRecipient = new HashMap<>();
        for (DamagedEvent damagedEvent : ((DamagedBatchBySourceEvent) event).getEvents()) {
            damageByRecipient.merge(damagedEvent.getTargetId(), damagedEvent.getAmount(), Integer::sum);
        }
        return damageByRecipient.containsValue(2);
    }
}
