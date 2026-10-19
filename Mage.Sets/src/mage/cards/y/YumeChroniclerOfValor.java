package mage.cards.y;

import mage.MageInt;
import mage.MageObject;
import mage.abilities.Ability;
import mage.abilities.TriggeredAbilityImpl;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.continuous.GainAbilityControlledSpellsEffect;
import mage.abilities.keyword.AssistAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.filter.common.FilterNonlandCard;
import mage.filter.predicate.ObjectSourcePlayer;
import mage.filter.predicate.ObjectSourcePlayerPredicate;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.stack.Spell;
import mage.players.Player;
import mage.watchers.common.SpellsCastWatcher;

import java.util.List;
import java.util.UUID;

/**
 * @author Claude
 */
public final class YumeChroniclerOfValor extends CardImpl {

    private static final FilterNonlandCard filter = new FilterNonlandCard("the first spell you cast during each of your turns");

    static {
        filter.add(YumeChroniclerOfValorPredicate.instance);
    }

    public YumeChroniclerOfValor(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{1}{W}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.DRUID);
        this.power = new MageInt(1);
        this.toughness = new MageInt(2);

        // The first spell you cast during each of your turns has assist.
        Ability ability = new SimpleStaticAbility(new GainAbilityControlledSpellsEffect(new AssistAbility(), filter)
                .setText("the first spell you cast during each of your turns has assist"));
        ability.addWatcher(new SpellsCastWatcher());
        this.addAbility(ability);

        // When you cast that spell, if another player spent mana to cast it, you and that player each draw a card.
        this.addAbility(new YumeChroniclerOfValorTriggeredAbility());
    }

    private YumeChroniclerOfValor(final YumeChroniclerOfValor card) {
        super(card);
    }

    @Override
    public YumeChroniclerOfValor copy() {
        return new YumeChroniclerOfValor(this);
    }

    static String assistKey(UUID sourceId, Game game) {
        return sourceId.toString() + game.getState().getZoneChangeCounter(sourceId) + "_assisted";
    }
}

/**
 * A card that would be the first spell cast this turn by its player on their own turn, or that spell on the stack
 */
enum YumeChroniclerOfValorPredicate implements ObjectSourcePlayerPredicate<MageObject> {
    instance;

    @Override
    public boolean apply(ObjectSourcePlayer<MageObject> input, Game game) {
        UUID playerId = input.getPlayerId();
        if (!game.isActivePlayer(playerId)) {
            return false;
        }
        SpellsCastWatcher watcher = game.getState().getWatcher(SpellsCastWatcher.class);
        if (watcher == null) {
            return false;
        }
        List<Spell> cast = watcher.getSpellsCastThisTurn(playerId);
        if (cast.isEmpty()) {
            return true;
        }
        MageObject object = input.getObject();
        return object instanceof Spell && cast.get(0).getId().equals(object.getId());
    }
}

class YumeChroniclerOfValorTriggeredAbility extends TriggeredAbilityImpl {

    YumeChroniclerOfValorTriggeredAbility() {
        super(Zone.BATTLEFIELD, new YumeChroniclerOfValorEffect());
        setTriggerPhrase("When you cast that spell, if another player spent mana to cast it, ");
    }

    private YumeChroniclerOfValorTriggeredAbility(final YumeChroniclerOfValorTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public YumeChroniclerOfValorTriggeredAbility copy() {
        return new YumeChroniclerOfValorTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.SPELL_CAST;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        if (!isControlledBy(event.getPlayerId()) || !game.isActivePlayer(getControllerId())) {
            return false;
        }
        SpellsCastWatcher watcher = game.getState().getWatcher(SpellsCastWatcher.class);
        Spell spell = game.getSpell(event.getTargetId());
        if (watcher == null || spell == null) {
            return false;
        }
        List<Spell> cast = watcher.getSpellsCastThisTurn(getControllerId());
        if (cast.size() != 1 || !cast.get(0).getId().equals(spell.getId())) {
            return false;
        }
        Object helper = game.getState().getValue(YumeChroniclerOfValor.assistKey(spell.getSourceId(), game));
        if (!(helper instanceof UUID) || helper.equals(getControllerId())) {
            return false;
        }
        getEffects().setValue("helper", helper);
        return true;
    }
}

class YumeChroniclerOfValorEffect extends OneShotEffect {

    YumeChroniclerOfValorEffect() {
        super(Outcome.DrawCard);
        staticText = "you and that player each draw a card";
    }

    private YumeChroniclerOfValorEffect(final YumeChroniclerOfValorEffect effect) {
        super(effect);
    }

    @Override
    public YumeChroniclerOfValorEffect copy() {
        return new YumeChroniclerOfValorEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        Player helper = game.getPlayer((UUID) getValue("helper"));
        if (controller != null) {
            controller.drawCards(1, source, game);
        }
        if (helper != null) {
            helper.drawCards(1, source, game);
        }
        return true;
    }
}
