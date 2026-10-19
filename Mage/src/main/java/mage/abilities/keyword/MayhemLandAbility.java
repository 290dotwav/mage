package mage.abilities.keyword;

import mage.abilities.Ability;
import mage.abilities.StaticAbility;
import mage.abilities.effects.AsThoughEffect;
import mage.abilities.effects.AsThoughEffectImpl;
import mage.constants.*;
import mage.game.Game;
import mage.util.CardUtil;

import java.util.UUID;

public  class MayhemLandAbility extends StaticAbility {

        private final String rule;

        public MayhemLandAbility() {
            super(AbilityType.STATIC, Zone.GRAVEYARD);
            this.newId();
            this.addEffect(new MayhemPlayEffect());
            this.addWatcher(new MayhemWatcher());
            this.setRuleAtTheTop(true);
            this.rule = "Mayhem " +
                    " <i>(You may play this card from your graveyard if you discarded it this turn. " +
                    "Timing rules still apply.)</i>";
        }

        protected MayhemLandAbility(final MayhemLandAbility ability) {
            super(ability);
            this.rule = ability.rule;
        }

        @Override
        public MayhemLandAbility copy() {
            return new MayhemLandAbility(this);
        }

        @Override
        public String getRule() {
            return rule;
        }
}

class MayhemPlayEffect extends AsThoughEffectImpl {

    public MayhemPlayEffect() {
        super(AsThoughEffectType.PLAY_FROM_NOT_OWN_HAND_ZONE, Duration.WhileInGraveyard, Outcome.Neutral);
    }

    public MayhemPlayEffect(final MayhemPlayEffect effect) {
        super(effect);
    }

    @Override
    public boolean applies(UUID objectId, Ability source, UUID affectedControllerId, Game game) {
        // The land itself, by its own owner: the first argument is the object being played, not the source, so
        // without these two checks any Oscorp Industries in any graveyard let any player play any card discarded
        // this turn (a Grizzly Bears discarded beside it was castable from the graveyard, and the opponent's
        // Oscorp offered itself as a second "permitting object" for this one).
        return CardUtil.getMainCardId(game, objectId).equals(source.getSourceId())
                && affectedControllerId.equals(source.getControllerId())
                && Zone.GRAVEYARD.match(game.getState().getZone(objectId))
                && MayhemWatcher.checkCard(objectId, game);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        return true;
    }

    @Override
    public AsThoughEffect copy() {
        return new MayhemPlayEffect(this);
    }
}