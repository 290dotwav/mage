package mage.cards.k;

import mage.MageInt;
import mage.abilities.common.DealsCombatDamageToAPlayerTriggeredAbility;
import mage.abilities.common.PlayCardTriggeredAbility;
import mage.abilities.effects.common.ExileFaceDownTopNLibraryYouMayPlayAsLongAsExiledTargetEffect;
import mage.abilities.effects.common.continuous.GainAbilitySourceEffect;
import mage.abilities.keyword.DeathtouchAbility;
import mage.abilities.keyword.IndestructibleAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.CastManaAdjustment;
import mage.constants.Duration;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.TargetController;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.events.GameEvent;

import java.util.UUID;

/**
 * @author Claude
 */
public final class KlawMasterOfSound extends CardImpl {

    public KlawMasterOfSound(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{B}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.ELEMENTAL);
        this.subtype.add(SubType.ROGUE);
        this.subtype.add(SubType.VILLAIN);
        this.power = new MageInt(3);
        this.toughness = new MageInt(3);

        // Deathtouch
        this.addAbility(DeathtouchAbility.getInstance());

        // Whenever you play a card from exile, Klaw gains indestructible until end of turn.
        this.addAbility(new KlawMasterOfSoundTriggeredAbility());

        // Whenever Klaw deals combat damage to a player, look at the top card of that player's library, then exile it face down. You may play it for as long as it remains exiled. Mana of any type can be spent to cast a spell this way.
        this.addAbility(new DealsCombatDamageToAPlayerTriggeredAbility(
                new ExileFaceDownTopNLibraryYouMayPlayAsLongAsExiledTargetEffect(false, CastManaAdjustment.AS_THOUGH_ANY_MANA_TYPE)
                        .setText("look at the top card of that player's library, then exile it face down. "
                                + "You may play it for as long as it remains exiled. "
                                + "Mana of any type can be spent to cast a spell this way"),
                false, true
        ));
    }

    private KlawMasterOfSound(final KlawMasterOfSound card) {
        super(card);
    }

    @Override
    public KlawMasterOfSound copy() {
        return new KlawMasterOfSound(this);
    }
}

class KlawMasterOfSoundTriggeredAbility extends PlayCardTriggeredAbility {

    KlawMasterOfSoundTriggeredAbility() {
        super(TargetController.YOU, Zone.BATTLEFIELD, new GainAbilitySourceEffect(IndestructibleAbility.getInstance(), Duration.EndOfTurn));
        setTriggerPhrase("Whenever you play a card from exile, ");
    }

    private KlawMasterOfSoundTriggeredAbility(final KlawMasterOfSoundTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        return super.checkTrigger(event, game) && event.getZone() == Zone.EXILED;
    }

    @Override
    public KlawMasterOfSoundTriggeredAbility copy() {
        return new KlawMasterOfSoundTriggeredAbility(this);
    }
}
