package mage.game.turn;

import mage.constants.PhaseStep;
import mage.constants.TurnPhase;
import mage.util.Copyable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.UUID;

/**
 * Turn, phase and step modification for extra/skip (use it for one time mod only)
 *
 * @author BetaSteward_at_googlemail.com
 */
public class TurnMods extends ArrayList<TurnMod> implements Serializable, Copyable<TurnMods> {

    public TurnMods() {
    }

    private TurnMods(final TurnMods mods) {
        for (TurnMod mod : mods) {
            this.add(mod.copy());
        }
    }

    public TurnMods copy() {
        return new TurnMods(this);
    }

    @Override
    public boolean add(TurnMod turnMod) {
        if (!turnMod.isLocked()) {
            throw new IllegalStateException("Wrong code usage: you must prepare turn mode with modification");
        }
        return super.add(turnMod);
    }

    public TurnMod useNextExtraTurn() {
        ListIterator<TurnMod> it = this.listIterator(this.size());
        while (it.hasPrevious()) {
            TurnMod turnMod = it.previous();
            if (turnMod.isExtraTurn()) {
                it.remove();
                return turnMod;
            }
        }
        return null;
    }

    public TurnMod useNextSkipTurn(UUID playerId) {
        ListIterator<TurnMod> it = this.listIterator(this.size());
        while (it.hasPrevious()) {
            TurnMod turnMod = it.previous();
            if (turnMod.isSkipTurn() && turnMod.getPlayerId().equals(playerId)) {
                it.remove();
                return turnMod;
            }
        }
        return null;
    }

    public TurnMod useNextNewController(UUID playerId) {
        // 720.1a
        // Multiple player-controlling effects that affect the same player overwrite each other.
        // The last one to be created is the one that works.
        //
        // 720.1b
        // If a turn is skipped, any pending player-controlling effects wait until the player
        // who would be affected actually takes a turn.

        // web door fork: the LAST created mod works (723.1a). The search used to walk from the end
        // and keep overwriting its pick, so the earliest one won: a second Mindslaver on the same
        // player gave his turn to the first one's controller. Every one of them is used up here, and
        // what each one adds after that turn (Emrakul, the Promised End's extra turn) still comes.
        TurnMod lastNewControllerMod = null;
        java.util.List<TurnMod> subsequent = new ArrayList<>();
        ListIterator<TurnMod> it = this.listIterator();
        while (it.hasNext()) {
            TurnMod turnMod = it.next();
            if (turnMod.getNewControllerId() != null && turnMod.getPlayerId().equals(playerId)) {
                lastNewControllerMod = turnMod;
                if (turnMod.getSubsequentTurnMod() != null) {
                    subsequent.add(turnMod.getSubsequentTurnMod());
                }
                it.remove();
            }
        }

        // add subsequent turn mods to execute after current
        for (TurnMod mod : subsequent) {
            this.add(mod);
        }

        return lastNewControllerMod;
    }

    public TurnMod useNextExtraStep(UUID playerId, PhaseStep afterStep) {
        ListIterator<TurnMod> it = this.listIterator(this.size());
        while (it.hasPrevious()) {
            TurnMod turnMod = it.previous();
            if (turnMod.getExtraStep() != null && turnMod.getPlayerId().equals(playerId) && (turnMod.getAfterStep() == null || turnMod.getAfterStep() == afterStep)) {
                it.remove();
                return turnMod;
            }
        }
        return null;
    }

    public TurnMod useNextSkipStep(UUID playerId, PhaseStep step) {
        if (step != null) {
            ListIterator<TurnMod> it = this.listIterator(this.size());
            while (it.hasPrevious()) {
                TurnMod turnMod = it.previous();
                if (turnMod.getSkipStep() != null) {
                    if (turnMod.getPlayerId() != null && turnMod.getPlayerId().equals(playerId)) {
                        if (turnMod.getSkipStep() == step) {
                            it.remove();
                            return turnMod;
                        }
                    }
                }
            }
        }
        return null;
    }

    public TurnMod useNextExtraPhase(UUID playerId, TurnPhase afterPhase) {
        ListIterator<TurnMod> it = this.listIterator(this.size());
        while (it.hasPrevious()) {
            TurnMod turnMod = it.previous();
            if (turnMod.getExtraPhase() != null
                    && turnMod.getPlayerId().equals(playerId)
                    && (turnMod.getAfterPhase() == null || turnMod.getAfterPhase() == afterPhase)) {
                it.remove();
                return turnMod;
            }
        }
        return null;
    }

    public TurnMod useNextSkipPhase(UUID playerId, TurnPhase phase) {
        ListIterator<TurnMod> it = this.listIterator(this.size());
        while (it.hasPrevious()) {
            TurnMod turnMod = it.previous();
            if (turnMod.getSkipPhase() != null && turnMod.getPlayerId().equals(playerId) && turnMod.getSkipPhase() == phase) {
                it.remove();
                return turnMod;
            }
        }
        return null;
    }
}
