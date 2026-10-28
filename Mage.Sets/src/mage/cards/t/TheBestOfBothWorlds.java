package mage.cards.t;

import mage.MageObjectReference;
import mage.abilities.Ability;
import mage.abilities.common.SagaAbility;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.AssimilateTargetEffect;
import mage.abilities.effects.common.continuous.BoostControlledEffect;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SagaChapter;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.filter.StaticFilters;
import mage.filter.common.FilterCreaturePermanent;
import mage.filter.predicate.Predicates;
import mage.constants.TargetController;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.TargetPermanent;
import mage.util.CardUtil;

import java.util.UUID;

/**
 * @author Claude
 */
public final class TheBestOfBothWorlds extends CardImpl {

    private static final FilterCreaturePermanent filter = new FilterCreaturePermanent("nonartifact creature an opponent controls");

    static {
        filter.add(Predicates.not(CardType.ARTIFACT.getPredicate()));
        filter.add(TargetController.OPPONENT.getControllerPredicate());
    }

    public TheBestOfBothWorlds(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ENCHANTMENT}, "{4}{W}{U}");

        this.subtype.add(SubType.SAGA);

        // (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
        SagaAbility sagaAbility = new SagaAbility(this);

        // I -- Assimilate target nonartifact creature an opponent controls.
        sagaAbility.addChapterEffect(
                this, SagaChapter.CHAPTER_I,
                new TheBestOfBothWorldsAssimilateEffect(), new TargetPermanent(filter)
        );

        // II -- Artifact creatures you control get +2/+2 until end of turn.
        sagaAbility.addChapterEffect(
                this, SagaChapter.CHAPTER_II,
                new BoostControlledEffect(2, 2, Duration.EndOfTurn, StaticFilters.FILTER_PERMANENTS_ARTIFACT_CREATURE)
        );

        // III -- Exile the Borg assimilated with this Saga, then return it to the battlefield under its owner's control.
        sagaAbility.addChapterEffect(this, SagaChapter.CHAPTER_III, new TheBestOfBothWorldsReturnEffect());
        this.addAbility(sagaAbility);
    }

    private TheBestOfBothWorlds(final TheBestOfBothWorlds card) {
        super(card);
    }

    @Override
    public TheBestOfBothWorlds copy() {
        return new TheBestOfBothWorlds(this);
    }

    /** Where chapter I keeps the Borg it assimilated, for chapter III: per Saga, per time on the battlefield. */
    static String key(Game game, Ability source) {
        return CardUtil.getObjectZoneString("assimilatedBorg", source.getSourceId(), game,
                source.getStackMomentSourceZCC(), false);
    }
}

class TheBestOfBothWorldsAssimilateEffect extends OneShotEffect {

    TheBestOfBothWorldsAssimilateEffect() {
        super(Outcome.GainControl);
        staticText = "assimilate target nonartifact creature an opponent controls. " + AssimilateTargetEffect.PERMANENT_REMINDER;
    }

    private TheBestOfBothWorldsAssimilateEffect(final TheBestOfBothWorldsAssimilateEffect effect) {
        super(effect);
    }

    @Override
    public TheBestOfBothWorldsAssimilateEffect copy() {
        return new TheBestOfBothWorldsAssimilateEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        Permanent permanent = game.getPermanent(getTargetPointer().getFirst(game, source));
        if (controller == null || permanent == null) {
            return false;
        }
        AssimilateTargetEffect.assimilatePermanent(permanent, controller, source, game);
        game.getState().setValue(TheBestOfBothWorlds.key(game, source), new MageObjectReference(permanent, game));
        return true;
    }
}

class TheBestOfBothWorldsReturnEffect extends OneShotEffect {

    TheBestOfBothWorldsReturnEffect() {
        super(Outcome.Neutral);
        staticText = "exile the Borg assimilated with this Saga, then return it to the battlefield under its owner's control";
    }

    private TheBestOfBothWorldsReturnEffect(final TheBestOfBothWorldsReturnEffect effect) {
        super(effect);
    }

    @Override
    public TheBestOfBothWorldsReturnEffect copy() {
        return new TheBestOfBothWorldsReturnEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        Object value = game.getState().getValue(TheBestOfBothWorlds.key(game, source));
        if (controller == null || !(value instanceof MageObjectReference)) {
            return false;
        }
        Permanent permanent = ((MageObjectReference) value).getPermanent(game);
        if (permanent == null) {
            return false;
        }
        Card card = permanent.getMainCard();
        controller.moveCards(permanent, Zone.EXILED, source, game);
        if (card == null || game.getState().getZone(card.getId()) != Zone.EXILED) {
            return true;
        }
        Player owner = game.getPlayer(card.getOwnerId());
        if (owner != null) {
            owner.moveCards(card, Zone.BATTLEFIELD, source, game);
        }
        return true;
    }
}
