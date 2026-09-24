package mage.cards.e;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.TriggeredAbilityImpl;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.common.DrawCardSourceControllerEffect;
import mage.abilities.effects.common.ExileReturnBattlefieldNextEndStepTargetEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.filter.FilterPermanent;
import mage.filter.common.FilterControlledPermanent;
import mage.filter.predicate.Predicates;
import mage.filter.predicate.mageobject.AnotherPredicate;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;
import mage.game.stack.StackAbility;
import mage.game.stack.StackObject;
import mage.target.TargetPermanent;

import java.util.UUID;

/**
 * @author Claude
 */
public final class ElrondMoonReader extends CardImpl {

    private static final FilterPermanent filter = new FilterControlledPermanent("other target nonland permanents you control");

    static {
        filter.add(AnotherPredicate.instance);
        filter.add(Predicates.not(CardType.LAND.getPredicate()));
    }

    public ElrondMoonReader(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{U}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.ELF);
        this.subtype.add(SubType.NOBLE);
        this.power = new MageInt(3);
        this.toughness = new MageInt(3);

        // Whenever you activate an ability of a creature, draw a card. This ability triggers only once each turn.
        this.addAbility(new ElrondMoonReaderTriggeredAbility());

        // {5}{U}{U}: Exile up to two other target nonland permanents you control. Return those cards to the battlefield under their owner's control at the beginning of the next end step.
        Ability ability = new SimpleActivatedAbility(
                new ExileReturnBattlefieldNextEndStepTargetEffect()
                        .setText("exile up to two other target nonland permanents you control. Return those cards " +
                                "to the battlefield under their owner's control at the beginning of the next end step"),
                new ManaCostsImpl<>("{5}{U}{U}")
        );
        ability.addTarget(new TargetPermanent(0, 2, filter));
        this.addAbility(ability);
    }

    private ElrondMoonReader(final ElrondMoonReader card) {
        super(card);
    }

    @Override
    public ElrondMoonReader copy() {
        return new ElrondMoonReader(this);
    }
}

/**
 * Mana abilities of creatures count too (see the card's rulings). They don't use the stack and
 * don't fire ACTIVATED_ABILITY, so they are caught when they resolve (RESOLVING_ABILITY),
 * which for a mana ability happens right as it is activated.
 */
class ElrondMoonReaderTriggeredAbility extends TriggeredAbilityImpl {

    ElrondMoonReaderTriggeredAbility() {
        super(Zone.BATTLEFIELD, new DrawCardSourceControllerEffect(1));
        setTriggerPhrase("Whenever you activate an ability of a creature, ");
        setTriggersLimitEachTurn(1);
    }

    private ElrondMoonReaderTriggeredAbility(final ElrondMoonReaderTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public ElrondMoonReaderTriggeredAbility copy() {
        return new ElrondMoonReaderTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.ACTIVATED_ABILITY
                || event.getType() == GameEvent.EventType.RESOLVING_ABILITY;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        if (!isControlledBy(event.getPlayerId())) {
            return false;
        }
        Permanent permanent = game.getPermanentOrLKIBattlefield(event.getSourceId());
        if (permanent == null || !permanent.isCreature(game)) {
            return false;
        }
        if (event.getType() == GameEvent.EventType.ACTIVATED_ABILITY) {
            StackObject stackObject = game.getStack().getStackObject(event.getTargetId());
            return stackObject instanceof StackAbility
                    && ((StackAbility) stackObject).getStackAbility().isActivatedAbility();
        }
        // RESOLVING_ABILITY: only activated mana abilities (everything else was counted when activated)
        for (Ability ability : permanent.getAbilities(game)) {
            if ((ability.getOriginalId().equals(event.getTargetId()) || ability.getId().equals(event.getTargetId()))
                    && ability.isManaActivatedAbility()) {
                return true;
            }
        }
        return false;
    }
}
