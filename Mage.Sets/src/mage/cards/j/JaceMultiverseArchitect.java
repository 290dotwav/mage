package mage.cards.j;

import mage.abilities.Ability;
import mage.abilities.LoyaltyAbility;
import mage.abilities.common.CanBeYourCommanderAbility;
import mage.abilities.costs.Cost;
import mage.abilities.costs.mana.GenericManaCost;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.RestrictionEffect;
import mage.abilities.triggers.BeginningOfCombatTriggeredAbility;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.TargetController;
import mage.constants.Zone;
import mage.filter.common.FilterControlledPermanent;
import mage.filter.predicate.Predicates;
import mage.filter.predicate.mageobject.AnotherPredicate;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.TargetPermanent;
import mage.target.common.TargetCardInHand;

import java.util.UUID;

/**
 * @author Claude
 */
public final class JaceMultiverseArchitect extends CardImpl {

    private static final FilterControlledPermanent filter
            = new FilterControlledPermanent("another target planeswalker or creature you control");

    static {
        filter.add(AnotherPredicate.instance);
        filter.add(Predicates.or(
                CardType.PLANESWALKER.getPredicate(),
                CardType.CREATURE.getPredicate()
        ));
    }

    public JaceMultiverseArchitect(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.PLANESWALKER}, "{1}{W}{U}{B}{R}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.JACE);
        this.setStartingLoyalty(4);

        // At the beginning of combat on each opponent's turn, they may pay {2}. If they don't, creatures they control can't attack Jaces you control this turn.
        this.addAbility(new BeginningOfCombatTriggeredAbility(
                TargetController.OPPONENT, new JaceMultiverseArchitectPayEffect(), false
        ));

        // +1: Draw two cards, then put a card from your hand on the bottom of your library.
        this.addAbility(new LoyaltyAbility(new JaceMultiverseArchitectDrawEffect(), 1));

        // -3: Exile another target planeswalker or creature you control. Reveal cards from the top of your library until you reveal a creature or planeswalker card. Put that card onto the battlefield and the rest on the bottom of your library in a random order.
        Ability ability = new LoyaltyAbility(new JaceMultiverseArchitectExileEffect(), -3);
        ability.addTarget(new TargetPermanent(filter));
        this.addAbility(ability);

        // Jace, Multiverse Architect can be your commander.
        this.addAbility(CanBeYourCommanderAbility.getInstance());
    }

    private JaceMultiverseArchitect(final JaceMultiverseArchitect card) {
        super(card);
    }

    @Override
    public JaceMultiverseArchitect copy() {
        return new JaceMultiverseArchitect(this);
    }
}

class JaceMultiverseArchitectPayEffect extends OneShotEffect {

    JaceMultiverseArchitectPayEffect() {
        super(Outcome.Detriment);
        staticText = "they may pay {2}. If they don't, creatures they control can't attack Jaces you control this turn";
    }

    private JaceMultiverseArchitectPayEffect(final JaceMultiverseArchitectPayEffect effect) {
        super(effect);
    }

    @Override
    public JaceMultiverseArchitectPayEffect copy() {
        return new JaceMultiverseArchitectPayEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player player = game.getPlayer(getTargetPointer().getFirst(game, source));
        if (player == null) {
            return false;
        }
        Cost cost = new GenericManaCost(2);
        if (cost.canPay(source, source, player.getId(), game)
                && player.chooseUse(Outcome.Benefit, "Pay {2}? If you don't, your creatures can't attack Jaces "
                + "controlled by " + game.getPlayer(source.getControllerId()).getName() + " this turn.", source, game)
                && cost.pay(source, game, source, player.getId(), false, null)) {
            game.informPlayers(player.getLogName() + " pays {2}");
            return true;
        }
        game.informPlayers(player.getLogName() + " doesn't pay {2}: their creatures can't attack Jaces this turn");
        game.addEffect(new JaceMultiverseArchitectRestrictionEffect(player.getId()), source);
        return true;
    }
}

class JaceMultiverseArchitectRestrictionEffect extends RestrictionEffect {

    private final UUID attackingPlayerId;

    JaceMultiverseArchitectRestrictionEffect(UUID attackingPlayerId) {
        super(Duration.EndOfTurn);
        this.attackingPlayerId = attackingPlayerId;
    }

    private JaceMultiverseArchitectRestrictionEffect(final JaceMultiverseArchitectRestrictionEffect effect) {
        super(effect);
        this.attackingPlayerId = effect.attackingPlayerId;
    }

    @Override
    public boolean applies(Permanent permanent, Ability source, Game game) {
        return permanent.isControlledBy(attackingPlayerId);
    }

    @Override
    public boolean canAttack(Permanent attacker, UUID defenderId, Ability source, Game game, boolean canUseChooseDialogs) {
        if (defenderId == null) {
            return true;
        }
        Permanent defender = game.getPermanent(defenderId);
        return defender == null
                || !defender.isControlledBy(source.getControllerId())
                || !defender.isPlaneswalker(game)
                || !defender.hasSubtype(SubType.JACE, game);
    }

    @Override
    public JaceMultiverseArchitectRestrictionEffect copy() {
        return new JaceMultiverseArchitectRestrictionEffect(this);
    }
}

class JaceMultiverseArchitectDrawEffect extends OneShotEffect {

    JaceMultiverseArchitectDrawEffect() {
        super(Outcome.DrawCard);
        staticText = "draw two cards, then put a card from your hand on the bottom of your library";
    }

    private JaceMultiverseArchitectDrawEffect(final JaceMultiverseArchitectDrawEffect effect) {
        super(effect);
    }

    @Override
    public JaceMultiverseArchitectDrawEffect copy() {
        return new JaceMultiverseArchitectDrawEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        controller.drawCards(2, source, game);
        if (controller.getHand().isEmpty()) {
            return true;
        }
        TargetCardInHand target = new TargetCardInHand();
        target.withChooseHint("to put on the bottom of your library");
        controller.choose(Outcome.Discard, controller.getHand(), target, source, game);
        Card card = game.getCard(target.getFirstTarget());
        if (card != null) {
            controller.putCardsOnBottomOfLibrary(card, game, source);
        }
        return true;
    }
}

class JaceMultiverseArchitectExileEffect extends OneShotEffect {

    JaceMultiverseArchitectExileEffect() {
        super(Outcome.PutCardInPlay);
        staticText = "exile another target planeswalker or creature you control. Reveal cards from the top of your "
                + "library until you reveal a creature or planeswalker card. Put that card onto the battlefield "
                + "and the rest on the bottom of your library in a random order";
    }

    private JaceMultiverseArchitectExileEffect(final JaceMultiverseArchitectExileEffect effect) {
        super(effect);
    }

    @Override
    public JaceMultiverseArchitectExileEffect copy() {
        return new JaceMultiverseArchitectExileEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        Permanent permanent = game.getPermanent(getTargetPointer().getFirst(game, source));
        if (permanent != null) {
            controller.moveCards(permanent, Zone.EXILED, source, game);
        }
        Cards revealed = new CardsImpl();
        Card found = null;
        for (Card card : controller.getLibrary().getCards(game)) {
            revealed.add(card);
            if (card.isCreature(game) || card.isPlaneswalker(game)) {
                found = card;
                break;
            }
        }
        if (revealed.isEmpty()) {
            return true;
        }
        controller.revealCards(source, revealed, game);
        if (found != null) {
            controller.moveCards(found, Zone.BATTLEFIELD, source, game);
        }
        revealed.retainZone(Zone.LIBRARY, game);
        controller.putCardsOnBottomOfLibrary(revealed, game, source, false);
        return true;
    }
}
