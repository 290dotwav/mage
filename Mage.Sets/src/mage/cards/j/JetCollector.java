package mage.cards.j;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.ActivateAsSorceryActivatedAbility;
import mage.abilities.condition.common.CardsInControllerGraveyardCondition;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.common.ConjureCardEffect;
import mage.abilities.effects.common.ReturnFromGraveyardToBattlefieldWithCounterTargetEffect;
import mage.abilities.triggers.BeginningOfSecondMainTriggeredAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.SubType;
import mage.counters.CounterType;
import mage.filter.common.FilterCreatureCard;
import mage.target.common.TargetCardInYourGraveyard;
import mage.target.targetadjustment.XManaValueTargetAdjuster;

import java.util.UUID;

/**
 * @author Claude
 */
public final class JetCollector extends CardImpl {

    private static final FilterCreatureCard filter = new FilterCreatureCard("creature card with mana value X from your graveyard");

    public JetCollector(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{1}{G}");

        this.subtype.add(SubType.MERFOLK);
        this.subtype.add(SubType.SCOUT);
        this.power = new MageInt(2);
        this.toughness = new MageInt(2);

        // At the beginning of your second main phase, if there are four or more cards in your graveyard, conjure a card named Mox Jet into your hand. This ability triggers only once.
        this.addAbility(new BeginningOfSecondMainTriggeredAbility(new ConjureCardEffect("Mox Jet"), false)
                .withInterveningIf(new CardsInControllerGraveyardCondition(4))
                .setTriggersLimitEachGame(1));

        // {X}{B}: Return target creature card with mana value X from your graveyard to the battlefield with a finality counter on it. Activate only as a sorcery.
        Ability ability = new ActivateAsSorceryActivatedAbility(
                new ReturnFromGraveyardToBattlefieldWithCounterTargetEffect(CounterType.FINALITY.createInstance()),
                new ManaCostsImpl<>("{X}{B}")
        );
        ability.addTarget(new TargetCardInYourGraveyard(filter));
        ability.setTargetAdjuster(new XManaValueTargetAdjuster());
        this.addAbility(ability);
    }

    private JetCollector(final JetCollector card) {
        super(card);
    }

    @Override
    public JetCollector copy() {
        return new JetCollector(this);
    }
}
