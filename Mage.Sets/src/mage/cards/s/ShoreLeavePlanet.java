package mage.cards.s;

import mage.MageObject;
import mage.abilities.Ability;
import mage.abilities.common.EntersBattlefieldTappedAbility;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.mana.BlueManaAbility;
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
import mage.target.TargetPermanent;
import mage.util.functions.CopyApplier;

import java.util.UUID;

/**
 * @author Claude
 */
public final class ShoreLeavePlanet extends CardImpl {

    public ShoreLeavePlanet(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.LAND}, "");

        this.subtype.add(SubType.PLANET);

        // This land enters tapped.
        this.addAbility(new EntersBattlefieldTappedAbility());

        // {T}: Add {U}.
        this.addAbility(new BlueManaAbility());

        // {4}{U}: This land becomes a copy of target creature you control until end of turn, except it isn't legendary and it's a Robot artifact in addition to its other types.
        Ability ability = new SimpleActivatedAbility(new ShoreLeavePlanetEffect(), new ManaCostsImpl<>("{4}{U}"));
        ability.addTarget(new TargetPermanent(StaticFilters.FILTER_CONTROLLED_CREATURE));
        this.addAbility(ability);
    }

    private ShoreLeavePlanet(final ShoreLeavePlanet card) {
        super(card);
    }

    @Override
    public ShoreLeavePlanet copy() {
        return new ShoreLeavePlanet(this);
    }
}

class ShoreLeavePlanetEffect extends OneShotEffect {

    private static final CopyApplier applier = new CopyApplier() {
        @Override
        public boolean apply(Game game, MageObject blueprint, Ability source, UUID targetObjectId) {
            blueprint.removeSuperType(SuperType.LEGENDARY);
            blueprint.addCardType(CardType.ARTIFACT);
            blueprint.addSubType(SubType.ROBOT);
            return true;
        }
    };

    ShoreLeavePlanetEffect() {
        super(Outcome.Copy);
        staticText = "this land becomes a copy of target creature you control until end of turn, "
                + "except it isn't legendary and it's a Robot artifact in addition to its other types";
    }

    private ShoreLeavePlanetEffect(final ShoreLeavePlanetEffect effect) {
        super(effect);
    }

    @Override
    public ShoreLeavePlanetEffect copy() {
        return new ShoreLeavePlanetEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent sourcePermanent = source.getSourcePermanentIfItStillExists(game);
        Permanent copyFromPermanent = game.getPermanent(getTargetPointer().getFirst(game, source));
        if (sourcePermanent == null || copyFromPermanent == null) {
            return false;
        }
        game.copyPermanent(Duration.EndOfTurn, copyFromPermanent, sourcePermanent.getId(), source, applier);
        return true;
    }
}
