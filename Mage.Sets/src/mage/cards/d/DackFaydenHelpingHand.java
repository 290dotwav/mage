package mage.cards.d;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.EntersBattlefieldTriggeredAbility;
import mage.abilities.effects.ContinuousEffect;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.combat.GoadTargetEffect;
import mage.abilities.effects.common.continuous.GainControlTargetEffect;
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
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.targetpointer.FixedTarget;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Claude
 */
public final class DackFaydenHelpingHand extends CardImpl {

    public DackFaydenHelpingHand(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{4}{W}{W}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.ADVISOR);
        this.power = new MageInt(4);
        this.toughness = new MageInt(6);

        // When Dack Fayden enters, reveal cards from the top of your library until you reveal X creature cards, where X is the number of opponents you have. Put those creature cards onto the battlefield, then shuffle. They're goaded for the rest of the game. For each of those permanents, choose a different opponent. Each opponent gains control of the permanent for which they were chosen.
        this.addAbility(new EntersBattlefieldTriggeredAbility(new DackFaydenHelpingHandEffect()));
    }

    private DackFaydenHelpingHand(final DackFaydenHelpingHand card) {
        super(card);
    }

    @Override
    public DackFaydenHelpingHand copy() {
        return new DackFaydenHelpingHand(this);
    }
}

class DackFaydenHelpingHandEffect extends OneShotEffect {

    DackFaydenHelpingHandEffect() {
        super(Outcome.Benefit);
        staticText = "reveal cards from the top of your library until you reveal X creature cards, where X is the "
                + "number of opponents you have. Put those creature cards onto the battlefield, then shuffle. "
                + "They're goaded for the rest of the game. For each of those permanents, choose a different opponent. "
                + "Each opponent gains control of the permanent for which they were chosen";
    }

    private DackFaydenHelpingHandEffect(final DackFaydenHelpingHandEffect effect) {
        super(effect);
    }

    @Override
    public DackFaydenHelpingHandEffect copy() {
        return new DackFaydenHelpingHandEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        List<UUID> opponents = new ArrayList<>(game.getOpponents(controller.getId()));
        int x = opponents.size();
        if (x < 1) {
            return true;
        }
        Cards revealed = new CardsImpl();
        Cards creatures = new CardsImpl();
        for (Card card : controller.getLibrary().getCards(game)) {
            revealed.add(card);
            if (card.isCreature(game)) {
                creatures.add(card);
                if (creatures.size() >= x) {
                    break;
                }
            }
        }
        if (revealed.isEmpty()) {
            return true;
        }
        controller.revealCards(source, revealed, game);
        controller.moveCards(creatures.getCards(game), Zone.BATTLEFIELD, source, game);
        controller.shuffleLibrary(source, game);

        List<Permanent> permanents = new ArrayList<>();
        for (UUID cardId : creatures) {
            Permanent permanent = game.getPermanent(cardId);
            if (permanent != null) {
                permanents.add(permanent);
            }
        }
        if (permanents.isEmpty()) {
            return true;
        }
        for (Permanent permanent : permanents) {
            ContinuousEffect goad = new GoadTargetEffect(Duration.EndOfGame);
            goad.setTargetPointer(new FixedTarget(permanent, game));
            game.addEffect(goad, source);
        }
        // for each of those permanents, a different opponent
        List<UUID> remaining = new ArrayList<>();
        for (UUID opponentId : opponents) {
            Player opponent = game.getPlayer(opponentId);
            if (opponent != null && opponent.isInGame()) {
                remaining.add(opponentId);
            }
        }
        for (Permanent permanent : permanents) {
            if (remaining.isEmpty()) {
                break;
            }
            UUID chosenId;
            if (remaining.size() == 1) {
                chosenId = remaining.get(0);
            } else {
                Map<String, UUID> byName = new LinkedHashMap<>();
                for (UUID opponentId : remaining) {
                    byName.put(game.getPlayer(opponentId).getName(), opponentId);
                }
                Choice choice = new ChoiceImpl(true);
                choice.setMessage("Choose the opponent who gains control of " + permanent.getLogName());
                choice.setChoices(new java.util.LinkedHashSet<>(byName.keySet()));
                controller.choose(Outcome.Neutral, choice, game);
                chosenId = byName.getOrDefault(choice.getChoice(), remaining.get(0));
            }
            remaining.remove(chosenId);
            Player chosen = game.getPlayer(chosenId);
            game.informPlayers(controller.getLogName() + " chooses " + chosen.getLogName()
                    + " for " + permanent.getLogName());
            ContinuousEffect control = new GainControlTargetEffect(Duration.Custom, true, chosenId);
            control.setTargetPointer(new FixedTarget(permanent, game));
            game.addEffect(control, source);
        }
        return true;
    }
}
