package mage.player.ai;

import mage.abilities.Ability;
import mage.abilities.ActivatedAbility;
import mage.abilities.PlayLandAbility;
import mage.abilities.SpellAbility;
import mage.cards.Card;
import mage.constants.PhaseStep;
import mage.constants.RangeOfInfluence;
import mage.game.Game;
import mage.game.combat.CombatGroup;
import mage.game.lesson.RivalScript;
import mage.game.permanent.Permanent;
import mage.game.stack.StackObject;
import mage.players.Player;
import mage.target.Target;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

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
 * <p>
 * It can be taken under control like "Computer - mad" (723: Emrakul, the Promised End, Mindslaver):
 * it is a {@link ComputerPlayerControllableProxy}, so while another player controls its turn every
 * decision, its priority included, goes to that player. Without it a human who cast Emrakul on a
 * simple bot was sent the bot's priorities ("as Bot") while the bot went on playing its own turn.
 * Under its own control nothing changes: the proxy hands every dialog to {@link ComputerPlayer6}
 * (through {@link ComputerPlayer7}, which adds nothing but the simulated priority this class replaces).
 * <p>
 * Ours, the site's lessons: in a game whose options carry a {@link RivalScript} for this player, it
 * plays the script and nothing else (priority, attackers, blockers); a free script with
 * {@code noMultiBlock} keeps its own play but never puts two blockers on one attacker.
 */
public class ComputerPlayerSimple extends ComputerPlayerControllableProxy {

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
        if (!isUnderMe(game)) {
            return super.priority(game); // the proxy gives it to the player who controls this turn
        }
        game.getState().setPriorityPlayerId(playerId);
        game.firePriorityEvent(playerId);
        RivalScript script = scriptOf(game);
        if (script != null && !script.isFree()) {
            if (scriptedCast(script, game)) {
                return true; // priority again on the new state, as after a play of its own
            }
            pass(game);
            return false;
        }
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

    // --- the site's lessons: the scripted rival (RivalScript) ---

    /** This player's script in that game, or null. */
    private RivalScript scriptOf(Game game) {
        if (game == null || game.isSimulation() || game.getOptions() == null) {
            return null;
        }
        RivalScript script = game.getOptions().rivalScript;
        return script != null && playerId.equals(script.getPlayerId()) ? script : null;
    }

    /** Still a game to play for this player: a line waiting for the site stops waiting otherwise. */
    private boolean playing(Game game) {
        if (game.hasEnded() || game.getState().isGameOver() || !canRespond()) {
            return false;
        }
        for (UUID opponentId : game.getOpponents(playerId)) {
            Player opponent = game.getPlayer(opponentId);
            if (opponent != null && opponent.canRespond()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The script's cast of this moment, if it has one: after the spell of the cast before it has
     * left the stack, and after the site's step it waits for.
     *
     * @return true when a spell was cast
     */
    private boolean scriptedCast(RivalScript script, Game game) {
        RivalScript.Line line = script.current(game.getTurnNum(), game.getTurnStepType());
        if (line == null || !line.isCast() || line.getTurn() != game.getTurnNum() || line.getStep() != game.getTurnStepType()) {
            return false;
        }
        UUID last = script.getLastSpellId();
        if (last != null && onStack(last, game)) {
            return false; // the cast before it has not had its effect yet
        }
        if (!script.awaitStep(line.getWaitFor(), () -> playing(game))) {
            return false;
        }
        Card card = null;
        for (Card inHand : getHand().getCards(game)) {
            if (inHand.getName().equals(line.getCard())) {
                card = inHand;
                break;
            }
        }
        SpellAbility spell = null;
        if (card != null) {
            for (ActivatedAbility ability : getPlayable(game, true)) {
                if (ability instanceof SpellAbility && ability.getSourceId().equals(card.getId())) {
                    spell = (SpellAbility) ability;
                    break;
                }
            }
        }
        if (spell == null) {
            if (card != null && !game.getStack().isEmpty()) {
                return false; // a sorcery, or a creature: when the stack is empty again
            }
            logger.warn(getName() + " cannot play the line of its script \"" + line + "\" (" + (card == null ? "not in hand" : "not castable") + "): given up");
            script.done(line);
            return false;
        }
        SpellAbility cast = spell.copy();
        if (line.getAction() == RivalScript.Action.CAST && line.getTarget() != null && !cast.getTargets().isEmpty()) {
            Target target = cast.getTargets().get(0);
            UUID targetId = scriptedTarget(line.getTarget(), target, cast, game);
            if (targetId == null) {
                logger.warn(getName() + " has no target \"" + line.getTarget() + "\" for the line of its script \"" + line + "\": given up");
                script.done(line);
                return false;
            }
            target.clearChosen();
            target.addTarget(targetId, cast, game);
        }
        script.done(line);
        if (activateAbility(cast, game)) {
            script.setLastSpellId(card.getId());
            logger.info(getName() + " plays the line of its script \"" + line + "\"");
            return true;
        }
        logger.warn(getName() + " could not cast the line of its script \"" + line + "\"");
        return false;
    }

    private static boolean onStack(UUID cardId, Game game) {
        for (StackObject object : game.getStack()) {
            if (cardId.equals(object.getSourceId())) {
                return true;
            }
        }
        return false;
    }

    /** student: the other player; self: this one; else a permanent of that name, the opponent's first. */
    private UUID scriptedTarget(String written, Target target, Ability source, Game game) {
        List<UUID> candidates = new ArrayList<>();
        if (RivalScript.STUDENT.equals(written)) {
            candidates.addAll(game.getOpponents(playerId));
        } else if (RivalScript.SELF.equals(written)) {
            candidates.add(playerId);
        } else {
            for (Permanent permanent : game.getBattlefield().getAllActivePermanents()) {
                if (permanent.getName().equals(written)) {
                    if (permanent.isControlledBy(playerId)) {
                        candidates.add(permanent.getId());
                    } else {
                        candidates.add(0, permanent.getId());
                    }
                }
            }
        }
        for (UUID id : candidates) {
            if (target.canTarget(playerId, id, source, game)) {
                return id;
            }
        }
        return null;
    }

    @Override
    public void selectAttackers(Game game, UUID attackingPlayerId) {
        RivalScript script = scriptOf(game);
        if (script == null || script.isFree() || !isUnderMe(game)) {
            super.selectAttackers(game, attackingPlayerId);
            return;
        }
        UUID defenderId = null;
        for (UUID opponentId : game.getOpponents(playerId)) {
            if (game.getCombat().getDefenders().contains(opponentId)) {
                defenderId = opponentId;
                break;
            }
        }
        // every attack line of this turn, in their order: one declaration
        RivalScript.Line line;
        while ((line = script.current(game.getTurnNum(), game.getTurnStepType())) != null
                && line.getTurn() == game.getTurnNum() && line.getAction() == RivalScript.Action.ATTACK) {
            if (!script.awaitStep(line.getWaitFor(), () -> playing(game))) {
                return;
            }
            script.done(line);
            Permanent attacker = null;
            for (Permanent permanent : game.getBattlefield().getAllActivePermanents(playerId)) {
                if (permanent.isCreature(game) && permanent.getName().equals(line.getCard()) && !permanent.isAttacking()
                        && defenderId != null && permanent.canAttack(defenderId, game)) {
                    attacker = permanent;
                    break;
                }
            }
            if (attacker == null) {
                logger.info(getName() + ": no " + line.getCard() + " to attack with (\"" + line + "\")");
                continue;
            }
            declareAttacker(attacker.getId(), defenderId, game, false);
            logger.info(getName() + " plays the line of its script \"" + line + "\"");
        }
    }

    @Override
    public void selectBlockers(Ability source, Game game, UUID defendingPlayerId) {
        RivalScript script = scriptOf(game);
        if (script == null || script.isFree() || !isUnderMe(game)
                || (defendingPlayerId != null && !defendingPlayerId.equals(playerId))) {
            super.selectBlockers(source, game, defendingPlayerId);
            return;
        }
        RivalScript.Line line;
        while ((line = script.current(game.getTurnNum(), game.getTurnStepType())) != null
                && line.getTurn() == game.getTurnNum() && line.isBlock()) {
            if (!script.awaitStep(line.getWaitFor(), () -> playing(game))) {
                return;
            }
            script.done(line);
            if (line.getAction() == RivalScript.Action.NO_BLOCK) {
                continue;
            }
            Permanent attacker = null;
            for (UUID attackerId : game.getCombat().getAttackers()) {
                Permanent permanent = game.getPermanent(attackerId);
                if (permanent != null && permanent.getName().equals(line.getAttacker())) {
                    attacker = permanent;
                    break;
                }
            }
            Permanent blocker = null;
            if (attacker != null) {
                for (Permanent permanent : game.getBattlefield().getAllActivePermanents(playerId)) {
                    if (permanent.isCreature(game) && permanent.getName().equals(line.getBlocker()) && !permanent.isTapped()
                            && game.getCombat().findGroupOfBlocker(permanent.getId()) == null && permanent.canBlock(attacker.getId(), game)) {
                        blocker = permanent;
                        break;
                    }
                }
            }
            if (blocker == null) {
                logger.info(getName() + ": no block \"" + line + "\" (" + (attacker == null ? "no such attacker" : "no such blocker") + ")");
                continue;
            }
            declareBlocker(playerId, blocker.getId(), attacker.getId(), game);
            logger.info(getName() + " plays the line of its script \"" + line + "\"");
        }
    }

    @Override
    public void declareBlocker(UUID defenderId, UUID blockerId, UUID attackerId, Game game, boolean allowUndo) {
        RivalScript script = scriptOf(game);
        if (script != null && script.isNoMultiBlock()) {
            Permanent attacker = game.getPermanent(attackerId);
            if (attacker != null && attacker.getMinBlockedBy() > 1) {
                return; // menace and the like: one blocker cannot block it, and two are never put
            }
            CombatGroup group = game.getCombat().findGroup(attackerId);
            if (group != null) {
                for (UUID already : group.getBlockers()) {
                    Permanent other = game.getPermanent(already);
                    if (other != null && other.isControlledBy(playerId) && !already.equals(blockerId)) {
                        return; // one blocker of ours on an attacker at most
                    }
                }
            }
        }
        super.declareBlocker(defenderId, blockerId, attackerId, game, allowUndo);
    }
}
