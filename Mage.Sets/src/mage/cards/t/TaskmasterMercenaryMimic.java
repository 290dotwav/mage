package mage.cards.t;

import mage.MageInt;
import mage.MageObject;
import mage.abilities.Ability;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.CopyEffect;
import mage.abilities.triggers.BeginningOfFirstMainTriggeredAbility;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.game.permanent.PermanentCard;
import mage.target.common.TargetCardInGraveyardBattlefieldOrStack;
import mage.util.functions.CopyApplier;

import java.util.UUID;

/**
 * @author Claude
 */
public final class TaskmasterMercenaryMimic extends CardImpl {

    public TaskmasterMercenaryMimic(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{U}{B}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.MERCENARY);
        this.subtype.add(SubType.VILLAIN);
        this.power = new MageInt(3);
        this.toughness = new MageInt(5);

        // Photographic Reflexes -- At the beginning of your first main phase, until your next turn, Taskmaster becomes a copy of up to one target creature on the battlefield or creature card in a graveyard, except his name is Taskmaster, Mercenary Mimic and he's a legendary Human Mercenary Villain creature.
        Ability ability = new BeginningOfFirstMainTriggeredAbility(new TaskmasterMercenaryMimicEffect());
        ability.addTarget(new TargetCardInGraveyardBattlefieldOrStack(
                0, 1, StaticFilters.FILTER_CARD_CREATURE, StaticFilters.FILTER_PERMANENT_CREATURE,
                "creature on the battlefield or creature card in a graveyard"
        ));
        this.addAbility(ability.withFlavorWord("Photographic Reflexes"));
    }

    private TaskmasterMercenaryMimic(final TaskmasterMercenaryMimic card) {
        super(card);
    }

    @Override
    public TaskmasterMercenaryMimic copy() {
        return new TaskmasterMercenaryMimic(this);
    }
}

class TaskmasterMercenaryMimicEffect extends OneShotEffect {

    /**
     * "he's a legendary Human Mercenary Villain creature": the type line is set, not added to (205.1a)
     */
    private static final CopyApplier applier = new CopyApplier() {
        @Override
        public boolean apply(Game game, MageObject blueprint, Ability source, UUID copyToObjectId) {
            blueprint.setName("Taskmaster, Mercenary Mimic");
            blueprint.removeAllCardTypes();
            blueprint.addCardType(CardType.CREATURE);
            blueprint.getSubtype().clear();
            blueprint.setIsAllCreatureTypes(false);
            blueprint.addSubType(SubType.HUMAN, SubType.MERCENARY, SubType.VILLAIN);
            blueprint.removeAllSuperTypes();
            blueprint.addSuperType(SuperType.LEGENDARY);
            return true;
        }
    };

    TaskmasterMercenaryMimicEffect() {
        super(Outcome.Copy);
        staticText = "until your next turn, {this} becomes a copy of up to one target creature on the battlefield "
                + "or creature card in a graveyard, except his name is Taskmaster, Mercenary Mimic "
                + "and he's a legendary Human Mercenary Villain creature";
    }

    private TaskmasterMercenaryMimicEffect(final TaskmasterMercenaryMimicEffect effect) {
        super(effect);
    }

    @Override
    public TaskmasterMercenaryMimicEffect copy() {
        return new TaskmasterMercenaryMimicEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent taskmaster = source.getSourcePermanentIfItStillExists(game);
        if (taskmaster == null) {
            return false;
        }
        UUID targetId = getTargetPointer().getFirst(game, source);
        Permanent permanent = game.getPermanent(targetId);
        if (permanent != null) {
            game.copyPermanent(Duration.UntilYourNextTurn, permanent, taskmaster.getId(), source, applier);
            return true;
        }
        Card card = game.getCard(targetId);
        if (card == null) {
            return false;
        }
        // a card in a graveyard: copy its printed values, as Lazav, the Multifarious does
        Permanent blueprint = new PermanentCard(card, source.getControllerId(), game);
        blueprint.assignNewId();
        applier.apply(game, blueprint, source, taskmaster.getId());
        CopyEffect copyEffect = new CopyEffect(Duration.UntilYourNextTurn, blueprint, taskmaster.getId());
        copyEffect.setApplier(applier);
        Ability newAbility = source.copy();
        copyEffect.init(newAbility, game);
        game.addEffect(copyEffect, newAbility);
        return true;
    }
}
