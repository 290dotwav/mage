package mage.cards.s;

import mage.MageInt;
import mage.abilities.TriggeredAbilityImpl;
import mage.abilities.common.ActivateAsSorceryActivatedAbility;
import mage.abilities.costs.mana.GenericManaCost;
import mage.abilities.effects.common.combat.CantBeBlockedTargetEffect;
import mage.abilities.effects.keyword.ManifestEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.TargetController;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;
import mage.target.targetpointer.FixedTarget;

import java.util.UUID;

/**
 * @author Claude
 */
public final class SashAndWaistcoatUnmen extends CardImpl {

    public SashAndWaistcoatUnmen(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{1}{U}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.INSECT);
        this.subtype.add(SubType.ROGUE);
        this.power = new MageInt(2);
        this.toughness = new MageInt(2);

        // Whenever a face-down creature attacks one of your opponents, it can't be blocked this combat.
        this.addAbility(new SashAndWaistcoatUnmenTriggeredAbility());

        // {3}: Manifest the top card of your library. Any player may activate this ability but only as a sorcery.
        this.addAbility(new SashAndWaistcoatUnmenManifestAbility());
    }

    private SashAndWaistcoatUnmen(final SashAndWaistcoatUnmen card) {
        super(card);
    }

    @Override
    public SashAndWaistcoatUnmen copy() {
        return new SashAndWaistcoatUnmen(this);
    }
}

class SashAndWaistcoatUnmenTriggeredAbility extends TriggeredAbilityImpl {

    SashAndWaistcoatUnmenTriggeredAbility() {
        super(Zone.BATTLEFIELD, new CantBeBlockedTargetEffect(Duration.EndOfCombat)
                .setText("it can't be blocked this combat"));
        setTriggerPhrase("Whenever a face-down creature attacks one of your opponents, ");
    }

    private SashAndWaistcoatUnmenTriggeredAbility(final SashAndWaistcoatUnmenTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public SashAndWaistcoatUnmenTriggeredAbility copy() {
        return new SashAndWaistcoatUnmenTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.ATTACKER_DECLARED;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        Permanent attacker = game.getPermanent(event.getSourceId());
        if (attacker == null || !attacker.isFaceDown(game)) {
            return false;
        }
        UUID defenderId = event.getTargetId();
        if (defenderId == null || game.getPlayer(defenderId) == null
                || !game.getOpponents(getControllerId()).contains(defenderId)) {
            return false;
        }
        getEffects().setTargetPointer(new FixedTarget(attacker, game));
        return true;
    }
}

class SashAndWaistcoatUnmenManifestAbility extends ActivateAsSorceryActivatedAbility {

    SashAndWaistcoatUnmenManifestAbility() {
        super(new ManifestEffect(1).setText("manifest the top card of your library"), new GenericManaCost(3));
        setMayActivate(TargetController.ANY);
    }

    private SashAndWaistcoatUnmenManifestAbility(final SashAndWaistcoatUnmenManifestAbility ability) {
        super(ability);
    }

    @Override
    public SashAndWaistcoatUnmenManifestAbility copy() {
        return new SashAndWaistcoatUnmenManifestAbility(this);
    }

    @Override
    public String getRule() {
        return "{3}: Manifest the top card of your library. Any player may activate this ability but only as a sorcery. "
                + "<i>(To manifest a card, put it onto the battlefield face down as a 2/2 creature. "
                + "Turn it face up any time for its mana cost if it's a creature card.)</i>";
    }
}
