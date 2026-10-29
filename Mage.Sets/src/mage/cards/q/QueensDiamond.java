package mage.cards.q;

import mage.abilities.Ability;
import mage.abilities.costs.common.TapSourceCost;
import mage.abilities.dynamicvalue.common.CountersSourceCount;
import mage.abilities.keyword.FlyingAbility;
import mage.abilities.keyword.HasteAbility;
import mage.abilities.keyword.StationAbility;
import mage.abilities.keyword.StationLevelAbility;
import mage.abilities.keyword.VigilanceAbility;
import mage.abilities.mana.ConditionalAnyColorManaAbility;
import mage.abilities.mana.conditional.ConditionalSpellManaBuilder;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.TargetController;
import mage.counters.CounterType;
import mage.filter.FilterSpell;
import mage.filter.predicate.mageobject.CommanderPredicate;

import java.util.UUID;

/**
 * @author Claude
 */
public final class QueensDiamond extends CardImpl {

    private static final FilterSpell filter = new FilterSpell("your commander");

    static {
        filter.add(CommanderPredicate.instance);
        filter.add(TargetController.YOU.getOwnerPredicate());
    }

    private static final CountersSourceCount xValue = new CountersSourceCount(CounterType.CHARGE);

    public QueensDiamond(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ARTIFACT}, "{4}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.SPACECRAFT);

        // {T}: Choose a color. Add one mana of that color for each charge counter on Queen's Diamond. Spend this mana only to cast your commander.
        Ability ability = new ConditionalAnyColorManaAbility(
                new TapSourceCost(), xValue, xValue, new ConditionalSpellManaBuilder(filter), true
        );
        ability.getEffects().get(0).setText("choose a color. Add one mana of that color for each charge counter "
                + "on {this}. Spend this mana only to cast your commander");
        this.addAbility(ability);

        // Station
        this.addAbility(new StationAbility());

        // STATION 8+
        // Flying, vigilance, haste
        // 5/5
        this.addAbility(new StationLevelAbility(8)
                .withLevelAbility(FlyingAbility.getInstance())
                .withLevelAbility(VigilanceAbility.getInstance())
                .withLevelAbility(HasteAbility.getInstance())
                .withPT(5, 5));
    }

    private QueensDiamond(final QueensDiamond card) {
        super(card);
    }

    @Override
    public QueensDiamond copy() {
        return new QueensDiamond(this);
    }
}
