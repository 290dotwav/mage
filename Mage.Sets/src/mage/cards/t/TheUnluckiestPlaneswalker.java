package mage.cards.t;

import mage.abilities.Ability;
import mage.abilities.LoyaltyAbility;
import mage.abilities.common.CanBeYourCommanderAbility;
import mage.abilities.common.delayed.ManaSpentDelayedTriggeredAbility;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.CreateDelayedTriggeredAbilityEffect;
import mage.abilities.effects.common.CreateTokenEffect;
import mage.abilities.effects.mana.AddManaOfAnyColorEffect;
import mage.abilities.keyword.RulebreakerAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.choices.Choice;
import mage.choices.ChoiceImpl;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.filter.FilterCard;
import mage.filter.FilterSpell;
import mage.filter.common.FilterControlledPermanent;
import mage.filter.predicate.Predicates;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.game.permanent.token.Gremlin11Token;
import mage.players.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Claude
 */
public final class TheUnluckiestPlaneswalker extends CardImpl {

    private static final FilterCard cardFilter = new FilterCard("can have Aura cards of any color identity and any basic land cards");
    private static final FilterSpell auraSpell = new FilterSpell("an Aura spell");
    static final FilterControlledPermanent aurasYouControl = new FilterControlledPermanent("Auras you control");

    static {
        aurasYouControl.add(SubType.AURA.getPredicate());
        cardFilter.add(Predicates.or(
                SubType.AURA.getPredicate(),
                SuperType.BASIC.getPredicate()
        ));
        auraSpell.add(SubType.AURA.getPredicate());
    }

    public TheUnluckiestPlaneswalker(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.PLANESWALKER}, "{2}{R}");

        this.supertype.add(SuperType.LEGENDARY);
        this.setStartingLoyalty(4);

        // Rulebreaker -- A deck with this commander can have Aura cards of any color identity and any basic land cards.
        this.addAbility(new RulebreakerAbility(cardFilter));

        // +1: Add one mana of any color. When you spend this mana to cast an Aura spell this turn, create a 1/1 red Gremlin creature token.
        Ability ability = new LoyaltyAbility(new AddManaOfAnyColorEffect(), 1);
        ability.addEffect(new CreateDelayedTriggeredAbilityEffect(
                new ManaSpentDelayedTriggeredAbility(new CreateTokenEffect(new Gremlin11Token()), auraSpell)
        ).setText("When you spend this mana to cast an Aura spell this turn, create a 1/1 red Gremlin creature token"));
        this.addAbility(ability);

        // -3: Discard your hand, then draw cards equal to twice the number of Auras you control. For each Aura attached to you, attach it to an opponent of your choice.
        this.addAbility(new LoyaltyAbility(new TheUnluckiestPlaneswalkerEffect(), -3));

        // The Unluckiest Planeswalker can be your commander.
        this.addAbility(CanBeYourCommanderAbility.getInstance());
    }

    private TheUnluckiestPlaneswalker(final TheUnluckiestPlaneswalker card) {
        super(card);
    }

    @Override
    public TheUnluckiestPlaneswalker copy() {
        return new TheUnluckiestPlaneswalker(this);
    }
}

class TheUnluckiestPlaneswalkerEffect extends OneShotEffect {

    TheUnluckiestPlaneswalkerEffect() {
        super(Outcome.DrawCard);
        staticText = "discard your hand, then draw cards equal to twice the number of Auras you control. "
                + "For each Aura attached to you, attach it to an opponent of your choice";
    }

    private TheUnluckiestPlaneswalkerEffect(final TheUnluckiestPlaneswalkerEffect effect) {
        super(effect);
    }

    @Override
    public TheUnluckiestPlaneswalkerEffect copy() {
        return new TheUnluckiestPlaneswalkerEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        controller.discard(controller.getHand(), false, source, game);
        int auras = game.getBattlefield().count(TheUnluckiestPlaneswalker.aurasYouControl, controller.getId(), source, game);
        if (auras > 0) {
            controller.drawCards(2 * auras, source, game);
        }
        List<UUID> opponents = new ArrayList<>();
        for (UUID opponentId : game.getOpponents(controller.getId())) {
            Player opponent = game.getPlayer(opponentId);
            if (opponent != null && opponent.isInGame()) {
                opponents.add(opponentId);
            }
        }
        if (opponents.isEmpty()) {
            return true;
        }
        for (UUID attachmentId : new ArrayList<>(controller.getAttachments())) {
            Permanent aura = game.getPermanent(attachmentId);
            if (aura == null || !aura.hasSubtype(SubType.AURA, game)) {
                continue;
            }
            UUID newPlayerId = opponents.get(0);
            if (opponents.size() > 1) {
                Map<String, UUID> byName = new LinkedHashMap<>();
                for (UUID id : opponents) {
                    byName.put(game.getPlayer(id).getName(), id);
                }
                Choice choice = new ChoiceImpl(true);
                choice.setMessage("Choose an opponent to attach " + aura.getLogName() + " to");
                choice.setChoices(new LinkedHashSet<>(byName.keySet()));
                controller.choose(Outcome.Detriment, choice, game);
                newPlayerId = byName.getOrDefault(choice.getChoice(), newPlayerId);
            }
            Player newPlayer = game.getPlayer(newPlayerId);
            if (newPlayer != null && controller.removeAttachment(aura, source, game)) {
                newPlayer.addAttachment(aura.getId(), source, game);
            }
        }
        return true;
    }
}
