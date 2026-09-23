package mage.cards.p;

import mage.abilities.Ability;
import mage.abilities.common.AttacksCreatureYouControlTriggeredAbility;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.condition.common.RaidCondition;
import mage.abilities.effects.AsThoughEffectImpl;
import mage.abilities.effects.common.ExileCardsFromTopOfLibraryControllerEffect;
import mage.abilities.hint.common.RaidHint;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.AsThoughEffectType;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.Zone;
import mage.filter.common.FilterControlledCreaturePermanent;
import mage.filter.predicate.permanent.EnteredThisTurnPredicate;
import mage.game.ExileZone;
import mage.game.Game;
import mage.util.CardUtil;
import mage.watchers.common.PlayerAttackedWatcher;

import java.util.UUID;

/**
 * @author Claude
 */
public final class PickUpThePace extends CardImpl {

    private static final FilterControlledCreaturePermanent filter
            = new FilterControlledCreaturePermanent("a creature you control that entered this turn");

    static {
        filter.add(EnteredThisTurnPredicate.instance);
    }

    public PickUpThePace(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ENCHANTMENT}, "{1}{R}");

        // Whenever a creature you control that entered this turn attacks, exile the top card of your library.
        this.addAbility(new AttacksCreatureYouControlTriggeredAbility(
                new ExileCardsFromTopOfLibraryControllerEffect(1, true), false, filter
        ));

        // You may play cards exiled with this enchantment as long as you attacked this turn.
        this.addAbility(new SimpleStaticAbility(new PickUpThePaceEffect()).addHint(RaidHint.instance), new PlayerAttackedWatcher());
    }

    private PickUpThePace(final PickUpThePace card) {
        super(card);
    }

    @Override
    public PickUpThePace copy() {
        return new PickUpThePace(this);
    }
}

class PickUpThePaceEffect extends AsThoughEffectImpl {

    PickUpThePaceEffect() {
        super(AsThoughEffectType.PLAY_FROM_NOT_OWN_HAND_ZONE, Duration.WhileOnBattlefield, Outcome.Benefit);
        staticText = "you may play cards exiled with this enchantment as long as you attacked this turn";
    }

    private PickUpThePaceEffect(final PickUpThePaceEffect effect) {
        super(effect);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        return true;
    }

    @Override
    public PickUpThePaceEffect copy() {
        return new PickUpThePaceEffect(this);
    }

    @Override
    public boolean applies(UUID objectId, Ability source, UUID affectedControllerId, Game game) {
        Card card = game.getCard(objectId);
        if (card == null
                || !source.isControlledBy(affectedControllerId)
                || !RaidCondition.instance.apply(game, source)) {
            return false;
        }
        UUID mainId = card.getMainCard().getId(); // split cards and mdfc
        if (game.getState().getZone(mainId) != Zone.EXILED) {
            return false;
        }
        ExileZone exileZone = game.getExile().getExileZone(CardUtil.getCardExileZoneId(game, source));
        return exileZone != null && exileZone.contains(mainId);
    }
}
