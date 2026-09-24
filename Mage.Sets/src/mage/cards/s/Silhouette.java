package mage.cards.s;

import mage.abilities.Ability;
import mage.abilities.effects.PreventionEffectImpl;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.stack.StackObject;
import mage.target.Target;
import mage.target.common.TargetCreaturePermanent;

import java.util.UUID;

/**
 * @author Claude
 */
public final class Silhouette extends CardImpl {

    public Silhouette(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.INSTANT}, "{1}{U}");

        // Choose target creature. If a spell or ability that targets that creature would cause a source to deal damage to that creature this turn, prevent that damage.
        this.getSpellAbility().addEffect(new SilhouetteEffect());
        this.getSpellAbility().addTarget(new TargetCreaturePermanent());
    }

    private Silhouette(final Silhouette card) {
        super(card);
    }

    @Override
    public Silhouette copy() {
        return new Silhouette(this);
    }
}

/**
 * The spell or ability that causes the damage is the one resolving, at the top of the stack.
 */
class SilhouetteEffect extends PreventionEffectImpl {

    SilhouetteEffect() {
        super(Duration.EndOfTurn, Integer.MAX_VALUE, false);
        staticText = "choose target creature. If a spell or ability that targets that creature would cause " +
                "a source to deal damage to that creature this turn, prevent that damage";
    }

    private SilhouetteEffect(final SilhouetteEffect effect) {
        super(effect);
    }

    @Override
    public SilhouetteEffect copy() {
        return new SilhouetteEffect(this);
    }

    @Override
    public boolean applies(GameEvent event, Ability source, Game game) {
        if (!super.applies(event, source, game)
                || event.getType() != GameEvent.EventType.DAMAGE_PERMANENT
                || !event.getTargetId().equals(getTargetPointer().getFirst(game, source))) {
            return false;
        }
        StackObject resolving = game.getStack().getFirstOrNull();
        if (resolving == null) {
            return false;
        }
        Ability ability = resolving.getStackAbility();
        for (UUID modeId : ability.getModes().getSelectedModes()) {
            for (Target target : ability.getModes().get(modeId).getTargets()) {
                if (target.getTargets().contains(event.getTargetId())) {
                    return true;
                }
            }
        }
        return false;
    }
}
