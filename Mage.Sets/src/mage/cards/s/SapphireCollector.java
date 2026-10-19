package mage.cards.s;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.TriggeredAbilityImpl;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.common.ConjureCardEffect;
import mage.abilities.effects.common.continuous.GainFlashbackTargetEffect;
import mage.abilities.keyword.ProwessAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.stack.Spell;
import mage.target.common.TargetCardInYourGraveyard;
import mage.watchers.common.SpellsCastWatcher;

import java.util.UUID;

/**
 * @author Claude
 */
public final class SapphireCollector extends CardImpl {

    public SapphireCollector(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{R}");

        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.MERCENARY);
        this.power = new MageInt(3);
        this.toughness = new MageInt(3);

        // Prowess
        this.addAbility(new ProwessAbility());

        // When you cast your second noncreature spell in a turn, conjure a card named Mox Sapphire into your hand. This ability triggers only once.
        this.addAbility(new SapphireCollectorTriggeredAbility().setTriggersLimitEachGame(1));

        // {2}{U}: Target instant or sorcery card in your graveyard gains flashback until end of turn. The flashback cost is equal to its mana cost.
        Ability ability = new SimpleActivatedAbility(new GainFlashbackTargetEffect(), new ManaCostsImpl<>("{2}{U}"));
        ability.addTarget(new TargetCardInYourGraveyard(StaticFilters.FILTER_CARD_INSTANT_OR_SORCERY_FROM_YOUR_GRAVEYARD));
        this.addAbility(ability);
    }

    private SapphireCollector(final SapphireCollector card) {
        super(card);
    }

    @Override
    public SapphireCollector copy() {
        return new SapphireCollector(this);
    }
}

class SapphireCollectorTriggeredAbility extends TriggeredAbilityImpl {

    SapphireCollectorTriggeredAbility() {
        super(Zone.BATTLEFIELD, new ConjureCardEffect("Mox Sapphire"));
        setTriggerPhrase("When you cast your second noncreature spell in a turn, ");
        this.addWatcher(new SpellsCastWatcher());
    }

    private SapphireCollectorTriggeredAbility(final SapphireCollectorTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public SapphireCollectorTriggeredAbility copy() {
        return new SapphireCollectorTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.SPELL_CAST;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        if (!isControlledBy(event.getPlayerId())) {
            return false;
        }
        Spell spell = game.getSpell(event.getTargetId());
        if (spell == null || spell.isCreature(game)) {
            return false;
        }
        SpellsCastWatcher watcher = game.getState().getWatcher(SpellsCastWatcher.class);
        if (watcher == null) {
            return false;
        }
        long noncreature = watcher.getSpellsCastThisTurn(getControllerId())
                .stream()
                .filter(s -> !s.isCreature(game))
                .count();
        return noncreature == 2;
    }
}
