package mage.cards.s;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.EntersBattlefieldTriggeredAbility;
import mage.abilities.common.LandfallAbility;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.DoIfCostPaid;
import mage.abilities.effects.common.ReturnSourceFromGraveyardToHandEffect;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.game.Game;
import mage.players.Player;

import java.util.UUID;

/**
 * @author Claude
 */
public final class SilvanReveler extends CardImpl {

    public SilvanReveler(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{G}{U}");

        this.subtype.add(SubType.ELF);
        this.subtype.add(SubType.CITIZEN);
        this.power = new MageInt(3);
        this.toughness = new MageInt(2);

        // When this creature enters, draw a card, then discard a card. If you discard a land card this way, put it from your graveyard onto the battlefield tapped.
        this.addAbility(new EntersBattlefieldTriggeredAbility(new SilvanRevelerEffect()));

        // Landfall -- Whenever a land you control enters, you may pay {1}{G}{U}. If you do, return this card from your graveyard to your hand.
        this.addAbility(new LandfallAbility(Zone.GRAVEYARD, new DoIfCostPaid(
                new ReturnSourceFromGraveyardToHandEffect(), new ManaCostsImpl<>("{1}{G}{U}")
        ), false));
    }

    private SilvanReveler(final SilvanReveler card) {
        super(card);
    }

    @Override
    public SilvanReveler copy() {
        return new SilvanReveler(this);
    }
}

class SilvanRevelerEffect extends OneShotEffect {

    SilvanRevelerEffect() {
        super(Outcome.DrawCard);
        staticText = "draw a card, then discard a card. If you discard a land card this way, " +
                "put it from your graveyard onto the battlefield tapped";
    }

    private SilvanRevelerEffect(final SilvanRevelerEffect effect) {
        super(effect);
    }

    @Override
    public SilvanRevelerEffect copy() {
        return new SilvanRevelerEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player player = game.getPlayer(source.getControllerId());
        if (player == null) {
            return false;
        }
        player.drawCards(1, source, game);
        Card card = player.discardOne(false, false, source, game);
        if (card == null || !card.isLand(game) || game.getState().getZone(card.getId()) != Zone.GRAVEYARD) {
            return true;
        }
        player.moveCards(card, Zone.BATTLEFIELD, source, game, true, false, false, null);
        return true;
    }
}
