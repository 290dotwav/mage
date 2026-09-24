package mage.cards.r;

import mage.abilities.Ability;
import mage.abilities.DelayedTriggeredAbility;
import mage.abilities.common.SagaAbility;
import mage.abilities.common.delayed.WhenYouAttackDelayedTriggeredAbility;
import mage.abilities.dynamicvalue.DynamicValue;
import mage.abilities.dynamicvalue.common.PermanentsOnBattlefieldCount;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.CreateDelayedTriggeredAbilityEffect;
import mage.abilities.effects.common.GainLifeEffect;
import mage.abilities.effects.common.continuous.BoostTargetEffect;
import mage.abilities.hint.ValueHint;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SagaChapter;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.filter.FilterCard;
import mage.filter.StaticFilters;
import mage.filter.common.FilterBasicCard;
import mage.filter.common.FilterControlledPermanent;
import mage.game.ExileZone;
import mage.game.Game;
import mage.players.Player;
import mage.target.TargetCard;
import mage.target.common.TargetCardInExile;
import mage.target.common.TargetCardInLibrary;
import mage.target.common.TargetControlledCreaturePermanent;
import mage.util.CardUtil;

import java.util.UUID;

/**
 * @author Claude
 */
public final class RoadsGoEverEverOn extends CardImpl {

    private static final DynamicValue xValue = new PermanentsOnBattlefieldCount(
            new FilterControlledPermanent(SubType.PLAINS, "Plains you control")
    );

    public RoadsGoEverEverOn(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ENCHANTMENT}, "{1}{W}");

        this.subtype.add(SubType.SAGA);

        // (As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)
        SagaAbility sagaAbility = new SagaAbility(this, SagaChapter.CHAPTER_IV);

        // I -- Search your library for up to two basic Plains cards, exile them, then shuffle. You gain 2 life.
        sagaAbility.addChapterEffect(
                this, SagaChapter.CHAPTER_I,
                new RoadsGoEverEverOnSearchEffect(), new GainLifeEffect(2)
        );

        // II, III -- Put a card exiled with this Saga into its owner's hand.
        sagaAbility.addChapterEffect(
                this, SagaChapter.CHAPTER_II, SagaChapter.CHAPTER_III,
                new RoadsGoEverEverOnReturnEffect()
        );

        // IV -- Whenever you attack this turn, target creature you control gets +1/+1 until end of turn for each Plains you control.
        DelayedTriggeredAbility delayed = new WhenYouAttackDelayedTriggeredAbility(
                new BoostTargetEffect(xValue, xValue)
        );
        delayed.addTarget(new TargetControlledCreaturePermanent());
        sagaAbility.addChapterEffect(
                this, SagaChapter.CHAPTER_IV,
                new CreateDelayedTriggeredAbilityEffect(delayed)
                        .setText("whenever you attack this turn, target creature you control " +
                                "gets +1/+1 until end of turn for each Plains you control")
        );
        this.addAbility(sagaAbility.addHint(new ValueHint("Plains you control", xValue)));
    }

    private RoadsGoEverEverOn(final RoadsGoEverEverOn card) {
        super(card);
    }

    @Override
    public RoadsGoEverEverOn copy() {
        return new RoadsGoEverEverOn(this);
    }
}

class RoadsGoEverEverOnSearchEffect extends OneShotEffect {

    private static final FilterCard filter = new FilterBasicCard(SubType.PLAINS, "basic Plains cards");

    RoadsGoEverEverOnSearchEffect() {
        super(Outcome.Neutral);
        staticText = "search your library for up to two basic Plains cards, exile them, then shuffle";
    }

    private RoadsGoEverEverOnSearchEffect(final RoadsGoEverEverOnSearchEffect effect) {
        super(effect);
    }

    @Override
    public RoadsGoEverEverOnSearchEffect copy() {
        return new RoadsGoEverEverOnSearchEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player player = game.getPlayer(source.getControllerId());
        if (player == null) {
            return false;
        }
        TargetCardInLibrary target = new TargetCardInLibrary(0, 2, filter);
        player.searchLibrary(target, source, game);
        Cards cards = new CardsImpl();
        for (UUID cardId : target.getTargets()) {
            Card card = player.getLibrary().getCard(cardId, game);
            if (card != null) {
                cards.add(card);
            }
        }
        if (!cards.isEmpty()) {
            player.moveCardsToExile(
                    cards.getCards(game), source, game, true,
                    CardUtil.getExileZoneId(game, source),
                    CardUtil.getSourceName(game, source)
            );
        }
        player.shuffleLibrary(source, game);
        return true;
    }
}

class RoadsGoEverEverOnReturnEffect extends OneShotEffect {

    RoadsGoEverEverOnReturnEffect() {
        super(Outcome.ReturnToHand);
        staticText = "put a card exiled with {this} into its owner's hand";
    }

    private RoadsGoEverEverOnReturnEffect(final RoadsGoEverEverOnReturnEffect effect) {
        super(effect);
    }

    @Override
    public RoadsGoEverEverOnReturnEffect copy() {
        return new RoadsGoEverEverOnReturnEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player player = game.getPlayer(source.getControllerId());
        ExileZone exileZone = game.getExile().getExileZone(CardUtil.getExileZoneId(game, source));
        if (player == null || exileZone == null || exileZone.isEmpty()) {
            return false;
        }
        Card card;
        if (exileZone.size() == 1) {
            card = exileZone.getRandom(game);
        } else {
            TargetCard target = new TargetCardInExile(StaticFilters.FILTER_CARD, exileZone.getId());
            target.withNotTarget(true);
            target.withChooseHint("to put into its owner's hand");
            player.choose(outcome, exileZone, target, source, game);
            card = game.getCard(target.getFirstTarget());
        }
        if (card == null) {
            return false;
        }
        Player owner = game.getPlayer(card.getOwnerId());
        return owner != null && owner.moveCards(card, Zone.HAND, source, game);
    }
}
