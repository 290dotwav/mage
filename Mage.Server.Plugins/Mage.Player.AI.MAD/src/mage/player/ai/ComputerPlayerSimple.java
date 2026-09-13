package mage.player.ai;

import mage.abilities.ActivatedAbility;
import mage.abilities.PlayLandAbility;
import mage.abilities.SpellAbility;
import mage.constants.PhaseStep;
import mage.constants.RangeOfInfluence;
import mage.game.Game;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * AI: the cheapest bot - it never simulates the game, so it answers at once, even on a small machine.
 * <p>
 * "Computer - mad" ({@link ComputerPlayer7}) runs game-tree simulations on both main phases and on the
 * declare attackers/blockers steps of every player's turn (skill * 3 seconds each unless
 * {@link #setMaxThinkTimeSecs(int)} caps it): three mad seats saturate a 2-vCPU server and every click of
 * the human waits for them. This bot keeps everything of {@link ComputerPlayer6} but its priority - the
 * static combat heuristics (declare attackers, declare blockers: no simulation) and every choose dialog
 * of {@link ComputerPlayer} (targets, modes, X, mulligan...) - and replaces the simulated priority by a
 * greedy one: on its own main phases, with an empty stack, it plays a land, then casts the most
 * expensive spell it can pay for, one action per priority; it passes everywhere else, never casts on
 * another player's turn and never activates a non-mana ability. The skill is accepted and ignored.
 * <p>
 * Registered in config.xml as "Computer - simple" ({@link mage.players.PlayerType#COMPUTER_SIMPLE}).
 */
public class ComputerPlayerSimple extends ComputerPlayer6 {

    private static final Logger logger = Logger.getLogger(ComputerPlayerSimple.class);

    // a spell that comes back to hand for free would otherwise be cast for ever
    private static final int MAX_ACTIONS_PER_STEP = 30;

    private int actionsTurn = 0;
    private PhaseStep actionsStep = null;
    private int actionsInStep = 0;

    public ComputerPlayerSimple(String name, RangeOfInfluence range, int skill) {
        super(name, range, skill);
    }

    public ComputerPlayerSimple(final ComputerPlayerSimple player) {
        super(player);
        this.actionsTurn = player.actionsTurn;
        this.actionsStep = player.actionsStep;
        this.actionsInStep = player.actionsInStep;
    }

    @Override
    public ComputerPlayerSimple copy() {
        return new ComputerPlayerSimple(this);
    }

    @Override
    public boolean priority(Game game) {
        game.getState().setPriorityPlayerId(playerId);
        game.firePriorityEvent(playerId);
        PhaseStep step = game.getTurnStepType();
        if ((step == PhaseStep.PRECOMBAT_MAIN || step == PhaseStep.POSTCOMBAT_MAIN)
                && game.isActivePlayer(playerId)
                && game.getStack().isEmpty()
                && playOneAction(game, step)) {
            return true; // priority again on the new state: the next land or spell, or a pass
        }
        pass(game);
        return false;
    }

    /**
     * Plays a land if one can be played, else casts the most expensive spell whose activation
     * succeeds (targets, extra costs and mana are chosen by ComputerPlayer's dialogs).
     *
     * @return true when something was played
     */
    private boolean playOneAction(Game game, PhaseStep step) {
        if (game.getTurnNum() != actionsTurn || step != actionsStep) {
            actionsTurn = game.getTurnNum();
            actionsStep = step;
            actionsInStep = 0;
        }
        if (actionsInStep >= MAX_ACTIONS_PER_STEP) {
            return false;
        }
        List<ActivatedAbility> spells = new ArrayList<>();
        for (ActivatedAbility ability : getPlayable(game, true)) {
            if (ability instanceof PlayLandAbility) {
                if (activateAbility(ability, game)) {
                    actionsInStep++;
                    return true;
                }
            } else if (ability instanceof SpellAbility) {
                spells.add(ability);
            }
        }
        spells.sort(Comparator.comparingInt((ActivatedAbility a) -> a.getManaCosts().manaValue()).reversed());
        for (ActivatedAbility spell : spells) {
            if (activateAbility(spell, game)) {
                actionsInStep++;
                if (logger.isDebugEnabled()) {
                    logger.debug(getName() + " casts " + spell.getRule());
                }
                return true;
            }
        }
        return false;
    }
}
