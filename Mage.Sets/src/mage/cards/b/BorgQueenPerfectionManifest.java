package mage.cards.b;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.EntersBattlefieldTriggeredAbility;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.effects.ContinuousEffect;
import mage.abilities.effects.ContinuousEffectImpl;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.continuous.BoostControlledEffect;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Layer;
import mage.constants.Outcome;
import mage.constants.SubLayer;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.TargetController;
import mage.constants.Zone;
import mage.counters.CounterType;
import mage.filter.StaticFilters;
import mage.filter.common.FilterCreatureCard;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.common.TargetCardInOpponentsGraveyard;
import mage.target.targetpointer.FixedTarget;

import java.util.UUID;

/**
 * @author Claude
 */
public final class BorgQueenPerfectionManifest extends CardImpl {

    private static final FilterCreatureCard filter = new FilterCreatureCard("creature card from an opponent's graveyard");

    public BorgQueenPerfectionManifest(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ARTIFACT, CardType.CREATURE}, "{4}{B}{B}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.BORG);
        this.subtype.add(SubType.NOBLE);
        this.power = new MageInt(1);
        this.toughness = new MageInt(4);

        // Artifact creatures you control get +2/+0.
        this.addAbility(new SimpleStaticAbility(new BoostControlledEffect(
                2, 0, Duration.WhileOnBattlefield, StaticFilters.FILTER_PERMANENTS_ARTIFACT_CREATURE
        )));

        // When Borg Queen enters, assimilate target creature card from an opponent's graveyard.
        Ability ability = new EntersBattlefieldTriggeredAbility(new BorgQueenAssimilateEffect());
        ability.addTarget(new TargetCardInOpponentsGraveyard(filter));
        this.addAbility(ability);
    }

    private BorgQueenPerfectionManifest(final BorgQueenPerfectionManifest card) {
        super(card);
    }

    @Override
    public BorgQueenPerfectionManifest copy() {
        return new BorgQueenPerfectionManifest(this);
    }
}

class BorgQueenAssimilateEffect extends OneShotEffect {

    BorgQueenAssimilateEffect() {
        super(Outcome.PutCreatureInPlay);
        staticText = "assimilate target creature card from an opponent's graveyard. <i>(Put it onto the battlefield "
                + "under your control with a +1/+1 counter. It's a Borg artifact creature and loses all other "
                + "creature types.)</i>";
    }

    private BorgQueenAssimilateEffect(final BorgQueenAssimilateEffect effect) {
        super(effect);
    }

    @Override
    public BorgQueenAssimilateEffect copy() {
        return new BorgQueenAssimilateEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        Card card = game.getCard(getTargetPointer().getFirst(game, source));
        if (controller == null || card == null) {
            return false;
        }
        game.setEnterWithCounters(card.getId(), new mage.counters.Counters().addCounter(CounterType.P1P1.createInstance()));
        controller.moveCards(card, Zone.BATTLEFIELD, source, game);
        Permanent permanent = game.getPermanent(card.getId());
        if (permanent == null) {
            return true;
        }
        ContinuousEffect effect = new BorgQueenBorgEffect();
        effect.setTargetPointer(new FixedTarget(permanent, game));
        game.addEffect(effect, source);
        return true;
    }
}

class BorgQueenBorgEffect extends ContinuousEffectImpl {

    BorgQueenBorgEffect() {
        super(Duration.Custom, Layer.TypeChangingEffects_4, SubLayer.NA, Outcome.Benefit);
    }

    private BorgQueenBorgEffect(final BorgQueenBorgEffect effect) {
        super(effect);
    }

    @Override
    public BorgQueenBorgEffect copy() {
        return new BorgQueenBorgEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent permanent = game.getPermanent(getTargetPointer().getFirst(game, source));
        if (permanent == null) {
            discard();
            return false;
        }
        permanent.addCardType(game, CardType.ARTIFACT);
        permanent.removeAllCreatureTypes(game);
        permanent.addSubType(game, SubType.BORG);
        return true;
    }
}
