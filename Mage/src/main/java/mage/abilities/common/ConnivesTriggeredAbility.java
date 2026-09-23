package mage.abilities.common;

import mage.abilities.TriggeredAbilityImpl;
import mage.abilities.effects.Effect;
import mage.constants.SetTargetPointer;
import mage.constants.Zone;
import mage.filter.FilterPermanent;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;
import mage.target.targetpointer.FixedTarget;
import mage.util.CardUtil;

/**
 * "Whenever [a creature you control] connives, ..."
 * <p>
 * 701.50a A permanent connives after its controller draws a card, then discards a card
 * (and a +1/+1 counter is put on it if a nonland card was discarded). The trigger uses
 * the conniving permanent's last known information if it left the battlefield (701.50b).
 *
 * @author Claude
 */
public class ConnivesTriggeredAbility extends TriggeredAbilityImpl {

    private final FilterPermanent filter;
    private final SetTargetPointer setTargetPointer;

    /**
     * "Whenever a creature you control connives, ..."
     */
    public ConnivesTriggeredAbility(Effect effect, boolean optional) {
        this(effect, optional, StaticFilters.FILTER_CONTROLLED_A_CREATURE, SetTargetPointer.PERMANENT);
    }

    public ConnivesTriggeredAbility(Effect effect, boolean optional, FilterPermanent filter, SetTargetPointer setTargetPointer) {
        super(Zone.BATTLEFIELD, effect, optional);
        this.filter = filter;
        this.setTargetPointer = setTargetPointer;
        setTriggerPhrase("Whenever " + CardUtil.addArticle(filter.getMessage()) + " connives, ");
    }

    protected ConnivesTriggeredAbility(final ConnivesTriggeredAbility ability) {
        super(ability);
        this.filter = ability.filter;
        this.setTargetPointer = ability.setTargetPointer;
    }

    @Override
    public ConnivesTriggeredAbility copy() {
        return new ConnivesTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.CONNIVED;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        Permanent permanent = game.getPermanentOrLKIBattlefield(event.getTargetId());
        if (permanent == null || !filter.match(permanent, getControllerId(), this, game)) {
            return false;
        }
        if (setTargetPointer == SetTargetPointer.PERMANENT) {
            getEffects().setTargetPointer(new FixedTarget(permanent, game));
        }
        return true;
    }
}
