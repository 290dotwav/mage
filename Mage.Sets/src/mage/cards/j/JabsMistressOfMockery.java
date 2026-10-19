package mage.cards.j;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.effects.ReplacementEffectImpl;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.events.NumberOfTriggersEvent;
import mage.game.permanent.Permanent;

import java.util.UUID;

/**
 * @author Claude
 */
public final class JabsMistressOfMockery extends CardImpl {

    public JabsMistressOfMockery(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{R}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.ROGUE);
        this.power = new MageInt(2);
        this.toughness = new MageInt(3);

        // If a triggered ability of a permanent you control but don't own triggers, that ability triggers an additional time.
        this.addAbility(new SimpleStaticAbility(new JabsMistressOfMockeryEffect()));
    }

    private JabsMistressOfMockery(final JabsMistressOfMockery card) {
        super(card);
    }

    @Override
    public JabsMistressOfMockery copy() {
        return new JabsMistressOfMockery(this);
    }
}

class JabsMistressOfMockeryEffect extends ReplacementEffectImpl {

    JabsMistressOfMockeryEffect() {
        super(Duration.WhileOnBattlefield, Outcome.Benefit);
        staticText = "if a triggered ability of a permanent you control but don't own triggers, "
                + "that ability triggers an additional time";
    }

    private JabsMistressOfMockeryEffect(final JabsMistressOfMockeryEffect effect) {
        super(effect);
    }

    @Override
    public JabsMistressOfMockeryEffect copy() {
        return new JabsMistressOfMockeryEffect(this);
    }

    @Override
    public boolean checksEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.NUMBER_OF_TRIGGERS;
    }

    @Override
    public boolean applies(GameEvent event, Ability source, Game game) {
        NumberOfTriggersEvent triggersEvent = (NumberOfTriggersEvent) event;
        Permanent permanent = game.getPermanentOrLKIBattlefield(triggersEvent.getSourceId());
        return permanent != null
                && permanent.isControlledBy(source.getControllerId())
                && !permanent.isOwnedBy(source.getControllerId());
    }

    @Override
    public boolean replaceEvent(GameEvent event, Ability source, Game game) {
        event.setAmount(event.getAmount() + 1);
        return false;
    }
}
