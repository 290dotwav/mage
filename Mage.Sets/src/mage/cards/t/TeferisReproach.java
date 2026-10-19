package mage.cards.t;

import mage.abilities.Ability;
import mage.abilities.effects.ContinuousEffectImpl;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.ExileSpellEffect;
import mage.abilities.effects.common.PhaseOutAllEffect;
import mage.abilities.keyword.ProtectionFromEverythingAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Layer;
import mage.constants.Outcome;
import mage.constants.SubLayer;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.common.TargetOpponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Claude
 */
public final class TeferisReproach extends CardImpl {

    public TeferisReproach(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.INSTANT}, "{2}{W}");

        // Choose target opponent. Until that player's next turn, they gain protection from everything and their life total can't change. All nonland permanents they control phase out.
        this.getSpellAbility().addEffect(new TeferisReproachEffect());
        this.getSpellAbility().addTarget(new TargetOpponent());

        // Exile Teferi's Reproach.
        this.getSpellAbility().addEffect(new ExileSpellEffect().concatBy("<br>"));
    }

    private TeferisReproach(final TeferisReproach card) {
        super(card);
    }

    @Override
    public TeferisReproach copy() {
        return new TeferisReproach(this);
    }
}

class TeferisReproachEffect extends OneShotEffect {

    TeferisReproachEffect() {
        super(Outcome.Detriment);
        staticText = "choose target opponent. Until that player's next turn, they gain protection from everything "
                + "and their life total can't change. All nonland permanents they control phase out. "
                + "<i>(While they're phased out, they're treated as though they don't exist. "
                + "They phase in before that player untaps during their next untap step.)</i>";
    }

    private TeferisReproachEffect(final TeferisReproachEffect effect) {
        super(effect);
    }

    @Override
    public TeferisReproachEffect copy() {
        return new TeferisReproachEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player opponent = game.getPlayer(getTargetPointer().getFirst(game, source));
        if (opponent == null) {
            return false;
        }
        game.addEffect(new TeferisReproachPlayerEffect(opponent.getId()), source);
        List<UUID> permIds = new ArrayList<>();
        for (Permanent permanent : game.getBattlefield().getActivePermanents(
                StaticFilters.FILTER_PERMANENTS_NON_LAND, opponent.getId(), source, game)) {
            if (permanent.isControlledBy(opponent.getId())) {
                permIds.add(permanent.getId());
            }
        }
        if (!permIds.isEmpty()) {
            new PhaseOutAllEffect(permIds).apply(game, source);
        }
        return true;
    }
}

/**
 * Protection from everything and a life total that can't change, until the chosen player's next turn
 */
class TeferisReproachPlayerEffect extends ContinuousEffectImpl {

    private final UUID playerId;
    private final Ability protection;

    TeferisReproachPlayerEffect(UUID playerId) {
        super(Duration.UntilYourNextTurn, Layer.PlayerEffects, SubLayer.NA, Outcome.Detriment);
        this.playerId = playerId;
        this.protection = new ProtectionFromEverythingAbility();
    }

    private TeferisReproachPlayerEffect(final TeferisReproachPlayerEffect effect) {
        super(effect);
        this.playerId = effect.playerId;
        this.protection = effect.protection.copy();
    }

    @Override
    public TeferisReproachPlayerEffect copy() {
        return new TeferisReproachPlayerEffect(this);
    }

    @Override
    public void init(Ability source, Game game, UUID activePlayerId) {
        super.init(source, game, activePlayerId);
        // the duration is "until that player's next turn", not the caster's
        setStartingControllerAndTurnNum(game, playerId, activePlayerId);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player player = game.getPlayer(playerId);
        if (player == null) {
            discard();
            return false;
        }
        player.addAbility(protection);
        player.setLifeTotalCanChange(false);
        return true;
    }
}
