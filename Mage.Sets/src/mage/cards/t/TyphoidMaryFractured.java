package mage.cards.t;

import mage.MageInt;
import mage.abilities.Mode;
import mage.abilities.common.AttacksTriggeredAbility;
import mage.abilities.effects.common.CreateTokenEffect;
import mage.abilities.effects.common.DrawCardSourceControllerEffect;
import mage.abilities.effects.common.GainLifeEffect;
import mage.abilities.effects.common.LoseLifeOpponentsEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.permanent.token.TreasureToken;
import mage.watchers.common.DiscardedCardWatcher;

import java.util.UUID;

/**
 * @author Claude
 */
public final class TyphoidMaryFractured extends CardImpl {

    public TyphoidMaryFractured(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{1}{B}{R}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.MUTANT);
        this.subtype.add(SubType.VILLAIN);
        this.power = new MageInt(3);
        this.toughness = new MageInt(3);

        // Whenever Typhoid Mary attacks, choose one at random. If you discarded a card this turn, you choose one instead.
        // * Mary -- Create a Treasure token.
        // * Typhoid Mary -- Draw a card.
        // * Bloody Mary -- Each opponent loses 2 life and you gain 2 life.
        this.addAbility(new TyphoidMaryFracturedTriggeredAbility(), new DiscardedCardWatcher());
    }

    private TyphoidMaryFractured(final TyphoidMaryFractured card) {
        super(card);
    }

    @Override
    public TyphoidMaryFractured copy() {
        return new TyphoidMaryFractured(this);
    }
}

class TyphoidMaryFracturedTriggeredAbility extends AttacksTriggeredAbility {

    TyphoidMaryFracturedTriggeredAbility() {
        super(new CreateTokenEffect(new TreasureToken()));
        this.withFirstModeFlavorWord("Mary");
        this.addMode(new Mode(new DrawCardSourceControllerEffect(1)).withFlavorWord("Typhoid Mary"));
        Mode mode = new Mode(new LoseLifeOpponentsEffect(2));
        mode.addEffect(new GainLifeEffect(2).concatBy("and"));
        this.addMode(mode.withFlavorWord("Bloody Mary"));
        this.getModes().setChooseText("choose one at random. If you discarded a card this turn, you choose one instead.");
        this.getModes().setRandom(true);
    }

    private TyphoidMaryFracturedTriggeredAbility(final TyphoidMaryFracturedTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public TyphoidMaryFracturedTriggeredAbility copy() {
        return new TyphoidMaryFracturedTriggeredAbility(this);
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        if (!super.checkTrigger(event, game)) {
            return false;
        }
        // the modes are chosen as the ability is put on the stack (700.2b): random unless you discarded a card this turn
        this.getModes().setRandom(!DiscardedCardWatcher.checkPlayerDiscarded(getControllerId(), game));
        return true;
    }
}
