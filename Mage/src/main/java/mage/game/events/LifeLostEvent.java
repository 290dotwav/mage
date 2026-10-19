package mage.game.events;

import mage.abilities.Ability;

import java.util.UUID;

/**
 * @author jimga150
 */
public class LifeLostEvent extends GameEvent {

    private final boolean fromDamage;

    public LifeLostEvent(UUID playerId, Ability source, int amount, boolean atCombat){
        this(playerId, source, amount, atCombat, false);
    }

    public LifeLostEvent(UUID playerId, Ability source, int amount, boolean atCombat, boolean fromDamage){
        super(GameEvent.EventType.LOST_LIFE, playerId, source, playerId, amount, atCombat);
        this.fromDamage = fromDamage;
    }

    /**
     * 120.3a: the life was lost because damage was dealt to the player, not by a "lose life" instruction
     */
    public boolean isFromDamage() {
        return fromDamage;
    }

}
