package mage.cards.f;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.effects.ReplacementEffectImpl;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.choices.Choice;
import mage.choices.ChoiceImpl;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.counters.CounterType;
import mage.counters.Counters;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.events.ZoneChangeEvent;
import mage.players.Player;
import mage.target.TargetCard;
import mage.target.common.TargetCardInYourGraveyard;
import mage.util.CardUtil;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.UUID;

/**
 * @author Claude
 */
public final class FrankensteinsMonster extends CardImpl {

    public FrankensteinsMonster(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{X}{B}{B}");

        this.subtype.add(SubType.ZOMBIE);
        this.power = new MageInt(0);
        this.toughness = new MageInt(1);

        // As this creature enters, exile X creature cards from your graveyard. If you can't, put this creature into its owner's graveyard instead of onto the battlefield. For each creature card exiled this way, this creature enters with a +2/+0, +1/+1, or +0/+2 counter on it.
        this.addAbility(new SimpleStaticAbility(Zone.ALL, new FrankensteinsMonsterEffect()));
    }

    private FrankensteinsMonster(final FrankensteinsMonster card) {
        super(card);
    }

    @Override
    public FrankensteinsMonster copy() {
        return new FrankensteinsMonster(this);
    }
}

class FrankensteinsMonsterEffect extends ReplacementEffectImpl {

    private static final String P2P0 = "+2/+0";
    private static final String P1P1 = "+1/+1";
    private static final String P0P2 = "+0/+2";

    FrankensteinsMonsterEffect() {
        super(Duration.WhileOnBattlefield, Outcome.Benefit);
        staticText = "as {this} enters, exile X creature cards from your graveyard. If you can't, put {this} " +
                "into its owner's graveyard instead of onto the battlefield. For each creature card exiled this way, " +
                "{this} enters with a +2/+0, +1/+1, or +0/+2 counter on it";
    }

    private FrankensteinsMonsterEffect(final FrankensteinsMonsterEffect effect) {
        super(effect);
    }

    @Override
    public FrankensteinsMonsterEffect copy() {
        return new FrankensteinsMonsterEffect(this);
    }

    @Override
    public boolean checksEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.ZONE_CHANGE;
    }

    @Override
    public boolean applies(GameEvent event, Ability source, Game game) {
        return event.getTargetId().equals(source.getSourceId())
                && ((ZoneChangeEvent) event).getToZone() == Zone.BATTLEFIELD;
    }

    @Override
    public boolean replaceEvent(GameEvent event, Ability source, Game game) {
        Player controller = game.getPlayer(source.getControllerId());
        Card card = game.getCard(source.getSourceId());
        if (controller == null || card == null) {
            return false;
        }
        // X is only set when it's cast
        int xValue = ((ZoneChangeEvent) event).getFromZone() == Zone.STACK
                ? CardUtil.getSourceCostsTag(game, source, "X", 0) : 0;
        if (xValue < 1) {
            return false;
        }
        if (controller.getGraveyard().count(StaticFilters.FILTER_CARD_CREATURE, game) < xValue) {
            // can't exile X creature cards: put it into its owner's graveyard instead
            Player owner = game.getPlayer(card.getOwnerId());
            if (owner != null) {
                owner.moveCards(card, Zone.GRAVEYARD, source, game);
            }
            return true;
        }
        TargetCard target = new TargetCardInYourGraveyard(xValue, xValue, StaticFilters.FILTER_CARD_CREATURES);
        target.withNotTarget(true);
        controller.choose(Outcome.Exile, controller.getGraveyard(), target, source, game);
        Cards cards = new CardsImpl(target.getTargets());
        controller.moveCards(cards, Zone.EXILED, source, game);
        cards.retainZone(Zone.EXILED, game);
        Counters counters = new Counters();
        for (int i = 0; i < cards.size(); i++) {
            Choice choice = new ChoiceImpl(true);
            choice.setMessage("Choose a counter for " + card.getName() + " (" + (i + 1) + " of " + cards.size() + ")");
            choice.setChoices(new LinkedHashSet<>(Arrays.asList(P2P0, P1P1, P0P2)));
            controller.choose(Outcome.BoostCreature, choice, game);
            String chosen = choice.getChoice();
            if (P2P0.equals(chosen)) {
                counters.addCounter(CounterType.P2P0.createInstance());
            } else if (P0P2.equals(chosen)) {
                counters.addCounter(CounterType.P0P2.createInstance());
            } else {
                counters.addCounter(CounterType.P1P1.createInstance());
            }
        }
        if (!counters.isEmpty()) {
            game.setEnterWithCounters(card.getId(), counters);
        }
        return false;
    }
}
