package mage.cards.a;

import mage.abilities.effects.common.AssimilateTargetEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.filter.FilterCard;
import mage.filter.common.FilterCreatureCard;
import mage.filter.predicate.card.PutIntoGraveFromBattlefieldThisTurnPredicate;
import mage.target.common.TargetCardInGraveyard;
import mage.target.targetadjustment.ForEachPlayerTargetsAdjuster;
import mage.target.targetpointer.EachTargetPointer;
import mage.watchers.common.CardsPutIntoGraveyardWatcher;

import java.util.UUID;

/**
 * @author Claude
 */
public final class AssimilateTheWounded extends CardImpl {

    private static final FilterCard filter = new FilterCreatureCard(
            "creature card from that player's graveyard that was put there from the battlefield this turn"
    );

    static {
        filter.add(PutIntoGraveFromBattlefieldThisTurnPredicate.instance);
    }

    public AssimilateTheWounded(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.INSTANT}, "{2}{B}");

        // For each opponent, assimilate up to one target creature card from that player's graveyard that was put there from the battlefield this turn.
        this.getSpellAbility().addEffect(new AssimilateTargetEffect()
                .setTargetPointer(new EachTargetPointer())
                .setText("for each opponent, assimilate up to one target creature card from that player's graveyard "
                        + "that was put there from the battlefield this turn. " + AssimilateTargetEffect.CARD_REMINDER));
        this.getSpellAbility().addTarget(new TargetCardInGraveyard(0, 1, filter));
        this.getSpellAbility().setTargetAdjuster(new ForEachPlayerTargetsAdjuster(true, true));
        this.getSpellAbility().addWatcher(new CardsPutIntoGraveyardWatcher());
    }

    private AssimilateTheWounded(final AssimilateTheWounded card) {
        super(card);
    }

    @Override
    public AssimilateTheWounded copy() {
        return new AssimilateTheWounded(this);
    }
}
