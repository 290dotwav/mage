package mage.cards.v;

import mage.MageInt;
import mage.MageObject;
import mage.abilities.common.SpellCastControllerTriggeredAbility;
import mage.abilities.effects.common.CreateTokenEffect;
import mage.abilities.keyword.DefenderAbility;
import mage.abilities.keyword.TeamworkAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.SubType;
import mage.filter.FilterSpell;
import mage.filter.predicate.Predicate;
import mage.game.Game;
import mage.game.permanent.token.Robot11HeroToken;
import mage.game.stack.Spell;

import java.util.Map;
import java.util.UUID;

/**
 * @author Claude
 */
public final class VirtualAssistant extends CardImpl {

    private static final FilterSpell filter = new FilterSpell("a spell using teamwork");

    static {
        filter.add(VirtualAssistantPredicate.instance);
    }

    public VirtualAssistant(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ARTIFACT, CardType.CREATURE}, "{1}{U}");

        this.subtype.add(SubType.ILLUSION);
        this.subtype.add(SubType.ADVISOR);
        this.power = new MageInt(3);
        this.toughness = new MageInt(3);

        // Defender
        this.addAbility(DefenderAbility.getInstance());

        // Whenever you cast a spell using teamwork, create a 1/1 colorless Robot Hero artifact creature token with flying.
        this.addAbility(new SpellCastControllerTriggeredAbility(
                new CreateTokenEffect(new Robot11HeroToken()), filter, false
        ));
    }

    private VirtualAssistant(final VirtualAssistant card) {
        super(card);
    }

    @Override
    public VirtualAssistant copy() {
        return new VirtualAssistant(this);
    }
}

/**
 * Must be used for SPELL_CAST events only: the teamwork tag lives on the spell's own costs.
 */
enum VirtualAssistantPredicate implements Predicate<MageObject> {
    instance;

    @Override
    public boolean apply(MageObject input, Game game) {
        if (!(input instanceof Spell)) {
            return false;
        }
        Map<String, Object> tags = ((Spell) input).getSpellAbility().getCostsTagMap();
        return tags != null && tags.containsKey(TeamworkAbility.TEAMWORK_ACTIVATION_VALUE_KEY);
    }
}
