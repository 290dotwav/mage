package mage.cards.m;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.ActivatedAbilityImpl;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.OneShotEffect;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.game.Game;
import mage.players.Player;

import java.util.UUID;

/**
 * @author Claude
 */
public final class MisterImmortal extends CardImpl {

    public MisterImmortal(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{1}{G}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.MUTANT);
        this.subtype.add(SubType.HERO);
        this.power = new MageInt(2);
        this.toughness = new MageInt(1);

        // {2}{G}: Return this card from your graveyard or from exile to the battlefield tapped.
        this.addAbility(new MisterImmortalAbility());
    }

    private MisterImmortal(final MisterImmortal card) {
        super(card);
    }

    @Override
    public MisterImmortal copy() {
        return new MisterImmortal(this);
    }
}

/**
 * One ability that functions both in the graveyard and in exile (113.6), and nowhere else.
 */
class MisterImmortalAbility extends ActivatedAbilityImpl {

    MisterImmortalAbility() {
        super(Zone.ALL, new MisterImmortalEffect(), new ManaCostsImpl<>("{2}{G}"));
    }

    private MisterImmortalAbility(final MisterImmortalAbility ability) {
        super(ability);
    }

    @Override
    public MisterImmortalAbility copy() {
        return new MisterImmortalAbility(this);
    }

    @Override
    public ActivationStatus canActivate(UUID playerId, Game game) {
        Zone zone = game.getState().getZone(getSourceId());
        if (zone != Zone.GRAVEYARD && zone != Zone.EXILED) {
            return ActivationStatus.getFalse();
        }
        Card card = game.getCard(getSourceId());
        // a face-down card in exile has no abilities (708.2)
        if (card == null || card.isFaceDown(game) || !card.isOwnedBy(playerId)) {
            return ActivationStatus.getFalse();
        }
        return super.canActivate(playerId, game);
    }
}

class MisterImmortalEffect extends OneShotEffect {

    MisterImmortalEffect() {
        super(Outcome.PutCreatureInPlay);
        staticText = "return this card from your graveyard or from exile to the battlefield tapped";
    }

    private MisterImmortalEffect(final MisterImmortalEffect effect) {
        super(effect);
    }

    @Override
    public MisterImmortalEffect copy() {
        return new MisterImmortalEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player player = game.getPlayer(source.getControllerId());
        Card card = game.getCard(source.getSourceId());
        if (player == null || card == null
                || card.getZoneChangeCounter(game) != source.getStackMomentSourceZCC()) {
            return false;
        }
        Zone zone = game.getState().getZone(card.getId());
        if (zone != Zone.GRAVEYARD && zone != Zone.EXILED) {
            return false;
        }
        return player.moveCards(card, Zone.BATTLEFIELD, source, game, true, false, false, null);
    }
}
