package mage.cards.c;

import mage.MageObject;
import mage.ObjectColor;
import mage.abilities.Ability;
import mage.abilities.common.AsEntersBattlefieldAbility;
import mage.abilities.common.EntersBattlefieldAbility;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.costs.CostAdjuster;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.PreventionEffectImpl;
import mage.abilities.effects.common.AttachEffect;
import mage.abilities.effects.common.ChooseColorEffect;
import mage.abilities.effects.common.InfoEffect;
import mage.abilities.effects.common.counter.AddCountersSourceEffect;
import mage.abilities.keyword.EnchantAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.counters.CounterType;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;
import mage.target.TargetPermanent;
import mage.target.common.TargetCreaturePermanent;

import java.util.UUID;

/**
 * @author Claude
 */
public final class ChromaticArmor extends CardImpl {

    public ChromaticArmor(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ENCHANTMENT}, "{1}{W}{U}");

        this.subtype.add(SubType.AURA);

        // Enchant creature
        TargetPermanent auraTarget = new TargetCreaturePermanent();
        this.getSpellAbility().addTarget(auraTarget);
        this.getSpellAbility().addEffect(new AttachEffect(Outcome.BoostCreature));
        this.addAbility(new EnchantAbility(auraTarget));

        // As this Aura enters, choose a color.
        this.addAbility(new AsEntersBattlefieldAbility(new ChooseColorEffect(Outcome.Benefit)));

        // This Aura enters with a sleight counter on it.
        this.addAbility(new EntersBattlefieldAbility(
                new AddCountersSourceEffect(CounterType.SLEIGHT.createInstance()),
                "with a sleight counter on it"
        ));

        // Prevent all damage that would be dealt to enchanted creature by sources of the last chosen color.
        this.addAbility(new SimpleStaticAbility(new ChromaticArmorPreventDamageEffect()));

        // {X}: Put a sleight counter on this Aura and choose a color. X is the number of sleight counters on this Aura.
        Ability ability = new SimpleActivatedAbility(
                new AddCountersSourceEffect(CounterType.SLEIGHT.createInstance()), new ManaCostsImpl<>("{X}")
        );
        ability.addEffect(new ChooseColorEffect(Outcome.Benefit).concatBy("and"));
        ability.addEffect(new InfoEffect("X is the number of sleight counters on {this}"));
        ability.setCostAdjuster(ChromaticArmorCostAdjuster.instance);
        this.addAbility(ability);
    }

    private ChromaticArmor(final ChromaticArmor card) {
        super(card);
    }

    @Override
    public ChromaticArmor copy() {
        return new ChromaticArmor(this);
    }
}

enum ChromaticArmorCostAdjuster implements CostAdjuster {
    instance;

    @Override
    public void prepareX(Ability ability, Game game) {
        Permanent permanent = ability.getSourcePermanentIfItStillExists(game);
        int counters = permanent == null ? 0 : permanent.getCounters(game).getCount(CounterType.SLEIGHT);
        if (game.inCheckPlayableState()) {
            ability.setVariableCostsMinMax(counters, counters);
        } else {
            ability.setVariableCostsValue(counters);
        }
    }
}

class ChromaticArmorPreventDamageEffect extends PreventionEffectImpl {

    ChromaticArmorPreventDamageEffect() {
        super(Duration.WhileOnBattlefield, Integer.MAX_VALUE, false);
        staticText = "prevent all damage that would be dealt to enchanted creature by sources of the last chosen color";
    }

    private ChromaticArmorPreventDamageEffect(final ChromaticArmorPreventDamageEffect effect) {
        super(effect);
    }

    @Override
    public ChromaticArmorPreventDamageEffect copy() {
        return new ChromaticArmorPreventDamageEffect(this);
    }

    @Override
    public boolean applies(GameEvent event, Ability source, Game game) {
        if (!super.applies(event, source, game)) {
            return false;
        }
        Permanent aura = game.getPermanent(source.getSourceId());
        if (aura == null || aura.getAttachedTo() == null || !event.getTargetId().equals(aura.getAttachedTo())) {
            return false;
        }
        ObjectColor color = (ObjectColor) game.getState().getValue(aura.getId() + "_color");
        if (color == null) {
            return false;
        }
        MageObject sourceObject = game.getObject(event.getSourceId());
        return sourceObject != null && sourceObject.getColor(game).shares(color);
    }
}
