package mage.cards.i;

import mage.MageInt;
import mage.MageObject;
import mage.abilities.Ability;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.keyword.FlyingAbility;
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
public final class ImpossibleMan extends CardImpl {

    public ImpossibleMan(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{U}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.ALIEN);
        this.subtype.add(SubType.SHAPESHIFTER);
        this.power = new MageInt(1);
        this.toughness = new MageInt(4);

        // Flying
        this.addAbility(FlyingAbility.getInstance());

        // {2}{U}: Impossible Man becomes a copy of another target permanent until end of turn, except his name is Impossible Man.
        Ability ability = new SimpleActivatedAbility(new ImpossibleManEffect(), new ManaCostsImpl<>("{2}{U}"));
        ability.addTarget(new TargetPermanent(StaticFilters.FILTER_ANOTHER_PERMANENT));
        this.addAbility(ability);
    }

    private ImpossibleMan(final ImpossibleMan card) {
        super(card);
    }

    @Override
    public ImpossibleMan copy() {
        return new ImpossibleMan(this);
    }
}

class ImpossibleManEffect extends OneShotEffect {

    private static final CopyApplier applier = new CopyApplier() {
        @Override
        public boolean apply(Game game, MageObject blueprint, Ability source, UUID copyToObjectId) {
            blueprint.setName("Impossible Man");
            return true;
        }
    };

    ImpossibleManEffect() {
        super(Outcome.Copy);
        staticText = "{this} becomes a copy of another target permanent until end of turn, except his name is Impossible Man";
    }

    private ImpossibleManEffect(final ImpossibleManEffect effect) {
        super(effect);
    }

    @Override
    public ImpossibleManEffect copy() {
        return new ImpossibleManEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent permanent = source.getSourcePermanentIfItStillExists(game);
        Permanent target = game.getPermanent(getTargetPointer().getFirst(game, source));
        if (permanent == null || target == null) {
            return false;
        }
        game.copyPermanent(Duration.EndOfTurn, target, permanent.getId(), source, applier);
        return true;
    }
}
