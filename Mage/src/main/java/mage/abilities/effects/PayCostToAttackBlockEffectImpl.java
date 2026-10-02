
package mage.abilities.effects;

import mage.abilities.Ability;
import mage.abilities.costs.Cost;
import mage.abilities.costs.mana.GenericManaCost;
import mage.abilities.costs.mana.ManaCost;
import mage.abilities.costs.mana.ManaCosts;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.events.GameEvent.EventType;
import mage.players.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author LevelX2
 */
public abstract class PayCostToAttackBlockEffectImpl extends ReplacementEffectImpl implements PayCostToAttackBlockEffect {

    public enum RestrictType {

        ATTACK("attack"),
        ATTACK_AND_BLOCK("attack or block"),
        BLOCK("block");

        private final String text;

        RestrictType(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    protected final Cost cost;
    protected final ManaCosts manaCosts;
    protected final RestrictType restrictType;

    public PayCostToAttackBlockEffectImpl(Duration duration, Outcome outcome, RestrictType restrictType) {
        super(duration, outcome, false);
        this.restrictType = restrictType;
        this.cost = null;
        this.manaCosts = null;
    }

    public PayCostToAttackBlockEffectImpl(Duration duration, Outcome outcome, RestrictType restrictType, Cost cost) {
        super(duration, outcome, false);
        this.restrictType = restrictType;
        if (cost instanceof ManaCosts) {
            this.cost = null;
            this.manaCosts = (ManaCosts) cost;
        } else {
            this.cost = cost;
            this.manaCosts = null;
        }
    }

    protected PayCostToAttackBlockEffectImpl(final PayCostToAttackBlockEffectImpl effect) {
        super(effect);
        if (effect.cost != null) {
            this.cost = effect.cost.copy();
        } else {
            this.cost = null;
        }
        if (effect.manaCosts != null) {
            this.manaCosts = effect.manaCosts.copy();
        } else {
            this.manaCosts = null;
        }
        this.restrictType = effect.restrictType;
    }

    @Override
    public boolean checksEventType(GameEvent event, Game game) {
        switch (restrictType) {
            case ATTACK:
                return event.getType() == GameEvent.EventType.DECLARE_ATTACKER;
            case BLOCK:
                return event.getType() == GameEvent.EventType.DECLARE_BLOCKER;
            case ATTACK_AND_BLOCK:
                return event.getType() == GameEvent.EventType.DECLARE_ATTACKER || event.getType() == GameEvent.EventType.DECLARE_BLOCKER;
        }
        return false;
    }

    @Override
    public boolean replaceEvent(GameEvent event, Ability source, Game game) {
        // ClaudeMTG fork: CR 508.1g-h / 509.1d-e - the taxes on one declaration are
        // totalled and paid at once. Asked one by one, a first {2} was paid, a second
        // could not be, the creature did not attack and the first {2} was lost.
        Map<PayCostToAttackBlockEffect, Ability> others = game.getContinuousEffects().getApplicablePayCostToAttackBlockEffects(event, game);
        others.entrySet().removeIf(entry -> entry.getKey().getId().equals(this.getId())
                || entry.getKey().isCostless(event, entry.getValue(), game));
        if (!others.isEmpty()) {
            return handleAllCosts(others, event, source, game);
        }
        ManaCosts attackBlockManaTax = getManaCostToPay(event, source, game);
        if (attackBlockManaTax != null) {
            return handleManaCosts(attackBlockManaTax.copy(), event, source, game);
        }
        Cost attackBlockOtherTax = getOtherCostToPay(event, source, game);
        if (attackBlockOtherTax != null) {
            return handleOtherCosts(attackBlockOtherTax.copy(), event, source, game);
        }
        return false;
    }

    private boolean handleManaCosts(ManaCosts attackBlockManaTax, GameEvent event, Ability source, Game game) {
        Player player = game.getPlayer(event.getPlayerId());
        if (player != null) {
            String chooseText;
            if (event.getType() == GameEvent.EventType.DECLARE_ATTACKER) {
                chooseText = "Pay " + attackBlockManaTax.getText() + " to attack?";
            } else {
                chooseText = "Pay " + attackBlockManaTax.getText() + " to block?";
            }
            attackBlockManaTax.clearPaid();
            if (attackBlockManaTax.canPay(source, source, player.getId(), game)
                    && player.chooseUse(Outcome.Neutral, chooseText, source, game)) {
                if (attackBlockManaTax instanceof ManaCostsImpl) {
                    if (attackBlockManaTax.payOrRollback(source, game, source, event.getPlayerId())) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    private boolean handleOtherCosts(Cost attackBlockOtherTax, GameEvent event, Ability source, Game game) {
        Player player = game.getPlayer(event.getPlayerId());
        if (player != null) {
            attackBlockOtherTax.clearPaid();
            if (attackBlockOtherTax.canPay(source, source, event.getPlayerId(), game)
                    && player.chooseUse(Outcome.Neutral,
                    attackBlockOtherTax.getText() + " to " + (event.getType() == GameEvent.EventType.DECLARE_ATTACKER ? "attack?" : "block?"), source, game)) {
                if (attackBlockOtherTax.pay(source, game, source, event.getPlayerId(), false, null)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * ClaudeMTG fork: this tax and every other one on the same declaration, asked once
     * and paid together - all of it, or nothing taken.
     */
    private boolean handleAllCosts(Map<PayCostToAttackBlockEffect, Ability> others, GameEvent event, Ability source, Game game) {
        Player player = game.getPlayer(event.getPlayerId());
        if (player == null) {
            return false;
        }
        Map<PayCostToAttackBlockEffect, Ability> all = new LinkedHashMap<>();
        all.put(this, source);
        all.putAll(others);
        // the others are paid here: replaceEvent must not apply them again
        for (PayCostToAttackBlockEffect effect : others.keySet()) {
            event.getAppliedEffects().add(effect.getId());
        }

        int generic = 0;
        ManaCosts<ManaCost> colored = new ManaCostsImpl<>();
        List<Cost> otherCosts = new ArrayList<>();
        List<Ability> otherSources = new ArrayList<>();
        for (Map.Entry<PayCostToAttackBlockEffect, Ability> entry : all.entrySet()) {
            ManaCosts<ManaCost> manaTax = entry.getKey().getManaCostToPay(event, entry.getValue(), game);
            if (manaTax != null) {
                for (ManaCost part : manaTax) {
                    if (part instanceof GenericManaCost) {
                        generic += part.manaValue();
                    } else {
                        colored.add(part.copy());
                    }
                }
                continue;
            }
            Cost otherTax = entry.getKey().getOtherCostToPay(event, entry.getValue(), game);
            if (otherTax != null) {
                otherTax = otherTax.copy();
                otherTax.clearPaid();
                otherCosts.add(otherTax);
                otherSources.add(entry.getValue());
            }
        }
        ManaCosts<ManaCost> total = new ManaCostsImpl<>();
        if (generic > 0) {
            total.add(new GenericManaCost(generic));
        }
        total.add(colored);
        total.clearPaid();

        if (!total.canPay(source, source, player.getId(), game)) {
            return true;
        }
        for (int i = 0; i < otherCosts.size(); i++) {
            if (!otherCosts.get(i).canPay(otherSources.get(i), otherSources.get(i), player.getId(), game)) {
                return true;
            }
        }
        List<String> parts = new ArrayList<>();
        if (!total.isEmpty()) {
            parts.add("Pay " + total.getText());
        }
        for (Cost otherTax : otherCosts) {
            String text = otherTax.getText();
            parts.add(parts.isEmpty() || text.isEmpty() ? text : Character.toLowerCase(text.charAt(0)) + text.substring(1));
        }
        String chooseText = String.join(" and ", parts)
                + (event.getType() == GameEvent.EventType.DECLARE_ATTACKER ? " to attack?" : " to block?");
        if (!player.chooseUse(Outcome.Neutral, chooseText, source, game)) {
            return true;
        }
        if (otherCosts.isEmpty()) {
            // payOrRollback: a payment left unfinished gives back every mana already taken
            return total.isEmpty() || !total.payOrRollback(source, game, source, player.getId());
        }
        int bookmark = game.bookmarkState();
        boolean paid = total.isEmpty() || total.payOrRollback(source, game, source, player.getId());
        for (int i = 0; paid && i < otherCosts.size(); i++) {
            paid = otherCosts.get(i).pay(otherSources.get(i), game, otherSources.get(i), player.getId(), false, null);
        }
        if (paid) {
            game.removeBookmark(bookmark);
            return false;
        }
        player.restoreState(bookmark, "attack or block tax", game);
        return true;
    }

    @Override
    public Cost getOtherCostToPay(GameEvent event, Ability source, Game game) {
        return cost;
    }

    @Override
    public ManaCosts getManaCostToPay(GameEvent event, Ability source, Game game) {
        return manaCosts;
    }

    @Override
    public boolean isCostless(GameEvent event, Ability source, Game game) {
        ManaCosts currentManaCosts = getManaCostToPay(event, source, game);
        if (currentManaCosts != null && currentManaCosts.manaValue() > 0) {
            return false;
        }
        return getOtherCostToPay(event, source, game) == null;
    }

}
