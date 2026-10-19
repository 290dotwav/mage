package mage.cards.e;

import mage.abilities.Ability;
import mage.abilities.effects.OneShotEffect;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.Zone;
import mage.filter.FilterCard;
import mage.game.Game;
import mage.players.Player;
import mage.target.Target;
import mage.target.TargetCard;
import mage.target.common.TargetOpponent;
import mage.util.CardUtil;

import java.util.Set;
import java.util.UUID;

/**
 * @author Claude
 */
public final class ExtrapolateTheImpossible extends CardImpl {

    public ExtrapolateTheImpossible(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.SORCERY}, "{1}{B}");

        // You may reveal exactly two cards you own with different names from outside the game. An opponent chooses one of them. You put that card into your hand.
        this.getSpellAbility().addEffect(new ExtrapolateTheImpossibleEffect());
    }

    private ExtrapolateTheImpossible(final ExtrapolateTheImpossible card) {
        super(card);
    }

    @Override
    public ExtrapolateTheImpossible copy() {
        return new ExtrapolateTheImpossible(this);
    }
}

class ExtrapolateTheImpossibleEffect extends OneShotEffect {

    ExtrapolateTheImpossibleEffect() {
        super(Outcome.DrawCard);
        staticText = "you may reveal exactly two cards you own with different names from outside the game. "
                + "An opponent chooses one of them. You put that card into your hand";
    }

    private ExtrapolateTheImpossibleEffect(final ExtrapolateTheImpossibleEffect effect) {
        super(effect);
    }

    @Override
    public ExtrapolateTheImpossibleEffect copy() {
        return new ExtrapolateTheImpossibleEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        Cards outside = new CardsImpl(controller.getSideboard());
        // exactly two cards with different names are needed
        Set<String> names = new java.util.HashSet<>();
        for (Card card : outside.getCards(game)) {
            names.add(card.getName());
        }
        if (names.size() < 2) {
            game.informPlayer(controller, "You don't have two cards with different names outside the game.");
            return true;
        }
        if (!controller.chooseUse(Outcome.DrawCard, "Reveal two cards with different names from outside the game?", source, game)) {
            return true;
        }
        TargetCard targetFirst = new TargetCard(Zone.ALL, new FilterCard("first card to reveal"));
        targetFirst.withNotTarget(true);
        controller.choose(Outcome.DrawCard, outside, targetFirst, source, game);
        Card first = game.getCard(targetFirst.getFirstTarget());
        if (first == null) {
            return true;
        }
        Cards rest = new CardsImpl();
        for (Card card : outside.getCards(game)) {
            if (!CardUtil.haveSameNames(card, first)) {
                rest.add(card);
            }
        }
        TargetCard targetSecond = new TargetCard(Zone.ALL, new FilterCard("second card to reveal, with a different name"));
        targetSecond.withNotTarget(true);
        controller.choose(Outcome.DrawCard, rest, targetSecond, source, game);
        Card second = game.getCard(targetSecond.getFirstTarget());
        if (second == null) {
            return true;
        }
        Cards revealed = new CardsImpl();
        revealed.add(first);
        revealed.add(second);
        controller.revealCards(source, revealed, game);

        Player opponent = chooseOpponent(controller, game, source);
        Card chosen = first;
        if (opponent != null) {
            TargetCard targetChosen = new TargetCard(Zone.ALL, new FilterCard("card to put into " + controller.getName() + "'s hand"));
            targetChosen.withNotTarget(true);
            targetChosen.withChooseHint("the other card stays outside the game");
            opponent.choose(Outcome.Detriment, revealed, targetChosen, source, game);
            Card picked = game.getCard(targetChosen.getFirstTarget());
            if (picked != null && revealed.contains(picked.getId())) {
                chosen = picked;
            }
            game.informPlayers(opponent.getLogName() + " chose " + chosen.getLogName());
        }
        controller.moveCards(chosen, Zone.HAND, source, game);
        return true;
    }

    private static Player chooseOpponent(Player controller, Game game, Ability source) {
        Set<UUID> opponents = game.getOpponents(source.getControllerId());
        if (opponents.isEmpty()) {
            return null;
        }
        if (opponents.size() == 1) {
            return game.getPlayer(opponents.iterator().next());
        }
        Target targetOpponent = new TargetOpponent(true);
        controller.chooseTarget(Outcome.Neutral, targetOpponent, source, game);
        Player opponent = game.getPlayer(targetOpponent.getFirstTarget());
        if (opponent == null) {
            opponent = game.getPlayer(opponents.iterator().next());
        }
        return opponent;
    }
}
