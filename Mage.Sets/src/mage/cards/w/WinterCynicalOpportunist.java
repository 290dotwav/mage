package mage.cards.w;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.AttacksTriggeredAbility;
import mage.abilities.dynamicvalue.common.CardTypesInGraveyardCount;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.MillCardsControllerEffect;
import mage.abilities.hint.HintUtils;
import mage.abilities.keyword.DeathtouchAbility;
import mage.abilities.triggers.BeginningOfEndStepTriggeredAbility;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.constants.*;
import mage.counters.CounterType;
import mage.counters.Counters;
import mage.filter.FilterCard;
import mage.filter.common.FilterPermanentCard;
import mage.game.Game;
import mage.players.Player;
import mage.target.TargetCard;
import mage.target.common.TargetCardInYourGraveyard;
import mage.util.CardUtil;

import java.awt.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ClaudeMTG
 */
public final class WinterCynicalOpportunist extends CardImpl {

    public WinterCynicalOpportunist(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{B}{G}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.WARLOCK);
        this.power = new MageInt(2);
        this.toughness = new MageInt(5);

        // Deathtouch
        this.addAbility(DeathtouchAbility.getInstance());

        // Whenever Winter attacks, mill three cards.
        this.addAbility(new AttacksTriggeredAbility(new MillCardsControllerEffect(3)));

        // Delirium -- At the beginning of your end step, you may exile any number of cards from your graveyard
        // with four or more card types among them. If you do, put a permanent card from among them onto the
        // battlefield with a finality counter on it.
        this.addAbility(new BeginningOfEndStepTriggeredAbility(
                TargetController.YOU, new WinterCynicalOpportunistEffect(), false
        ).setAbilityWord(AbilityWord.DELIRIUM).addHint(CardTypesInGraveyardCount.YOU.getHint()));
    }

    private WinterCynicalOpportunist(final WinterCynicalOpportunist card) {
        super(card);
    }

    @Override
    public WinterCynicalOpportunist copy() {
        return new WinterCynicalOpportunist(this);
    }
}

class WinterCynicalOpportunistEffect extends OneShotEffect {

    private static final FilterCard filter = new FilterPermanentCard("permanent card to put onto the battlefield");

    WinterCynicalOpportunistEffect() {
        super(Outcome.PutCardInPlay);
        staticText = "you may exile any number of cards from your graveyard with four or more card types "
                + "among them. If you do, put a permanent card from among them onto the battlefield "
                + "with a finality counter on it";
    }

    private WinterCynicalOpportunistEffect(final WinterCynicalOpportunistEffect effect) {
        super(effect);
    }

    @Override
    public WinterCynicalOpportunistEffect copy() {
        return new WinterCynicalOpportunistEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player player = game.getPlayer(source.getControllerId());
        if (player == null) {
            return false;
        }
        WinterCynicalOpportunistTarget target = new WinterCynicalOpportunistTarget();
        if (!target.canChoose(player.getId(), source, game)
                || !player.chooseUse(outcome, "Exile cards from your graveyard with four or more card types among them?", source, game)) {
            return false;
        }
        player.choose(outcome, target, source, game);
        Cards toExile = new CardsImpl(target.getTargets());
        if (toExile.isEmpty()) {
            return false;
        }
        player.moveCards(toExile, Zone.EXILED, source, game);

        Cards exiled = new CardsImpl();
        toExile.getCards(game)
                .stream()
                .filter(card -> Zone.EXILED == game.getState().getZone(card.getId()))
                .forEach(exiled::add);
        if (exiled.count(filter, source.getControllerId(), source, game) < 1) {
            return true;
        }
        TargetCard targetCard = new TargetCard(Zone.EXILED, filter);
        targetCard.withNotTarget(true);
        player.choose(Outcome.PutCardInPlay, exiled, targetCard, source, game);
        Card card = game.getCard(targetCard.getFirstTarget());
        if (card == null) {
            return true;
        }
        game.setEnterWithCounters(card.getId(), new Counters().addCounter(CounterType.FINALITY.createInstance()));
        player.moveCards(card, Zone.BATTLEFIELD, source, game);
        return true;
    }
}

class WinterCynicalOpportunistTarget extends TargetCardInYourGraveyard {

    private static final FilterCard filter
            = new FilterCard("cards from your graveyard with four or more card types among them");

    WinterCynicalOpportunistTarget() {
        super(1, Integer.MAX_VALUE, filter, true);
    }

    private WinterCynicalOpportunistTarget(final WinterCynicalOpportunistTarget target) {
        super(target);
    }

    @Override
    public WinterCynicalOpportunistTarget copy() {
        return new WinterCynicalOpportunistTarget(this);
    }

    @Override
    public boolean isChosen(Game game) {
        return super.isChosen(game) && metCondition(this.getTargets(), game);
    }

    @Override
    public String getMessage(Game game) {
        String text = "Select " + CardUtil.addArticle(targetName);
        Set<CardType> types = typesAmongSelection(this.getTargets(), game);
        text += " (selected " + this.getTargets().size() + " cards; card types: ";
        text += HintUtils.prepareText(
                types.size() + " of 4",
                types.size() >= 4 ? Color.GREEN : Color.RED
        );
        String info = types.stream().map(CardType::toString).collect(Collectors.joining(", "));
        if (!info.isEmpty()) {
            text += " [" + info + "]";
        }
        text += ")";
        return text;
    }

    @Override
    public boolean canChoose(UUID sourceControllerId, Ability source, Game game) {
        if (!super.canChoose(sourceControllerId, source, game)) {
            return false;
        }
        // check that exiling all the possible cards would have >= 4 different card types
        Set<UUID> idsToCheck = new HashSet<>();
        idsToCheck.addAll(this.getTargets());
        idsToCheck.addAll(this.possibleTargets(sourceControllerId, source, game));
        return metCondition(idsToCheck, game);
    }

    private static Set<CardType> typesAmongSelection(Collection<UUID> cardsIds, Game game) {
        return cardsIds
                .stream()
                .map(game::getCard)
                .filter(Objects::nonNull)
                .flatMap(c -> c.getCardType(game).stream())
                .collect(Collectors.toSet());
    }

    private static boolean metCondition(Collection<UUID> cardsIds, Game game) {
        return typesAmongSelection(cardsIds, game).size() >= 4;
    }
}
