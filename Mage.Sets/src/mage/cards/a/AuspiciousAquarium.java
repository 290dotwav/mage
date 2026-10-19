package mage.cards.a;

import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.condition.Condition;
import mage.abilities.condition.common.PermanentsOnTheBattlefieldCondition;
import mage.abilities.effects.common.WinGameSourceControllerEffect;
import mage.abilities.effects.common.continuous.BoostControlledEffect;
import mage.abilities.triggers.BeginningOfUpkeepTriggeredAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.ComparisonType;
import mage.constants.Duration;
import mage.constants.SubType;
import mage.filter.common.FilterControlledCreaturePermanent;
import mage.filter.common.FilterCreaturePermanent;
import mage.filter.predicate.Predicate;
import mage.filter.predicate.Predicates;
import mage.game.Game;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Claude
 */
public final class AuspiciousAquarium extends CardImpl {

    private static final List<SubType> aquatic = Arrays.asList(
            SubType.CLAMFOLK, SubType.CRAB, SubType.CROCODILE, SubType.FISH, SubType.HOMARID, SubType.JELLYFISH,
            SubType.KRAKEN, SubType.LEVIATHAN, SubType.LOBSTER, SubType.MERFOLK, SubType.NAUTILUS, SubType.OCTOPUS,
            SubType.OTTER, SubType.OYSTER, SubType.SEAL, SubType.SERPENT, SubType.SHARK, SubType.SHRIMP,
            SubType.SPONGE, SubType.SQUID, SubType.STARFISH, SubType.TRILOBITE, SubType.TURTLE, SubType.WALRUS,
            SubType.WHALE
    );

    private static final FilterCreaturePermanent filter = new FilterCreaturePermanent(
            "creature you control that's a Clamfolk, Crab, Crocodile, Fish, Homarid, Jellyfish, Kraken, Leviathan, "
                    + "Lobster, Merfolk, Nautilus, Octopus, Otter, Oyster, Seal, Serpent, Shark, Shrimp, Sponge, Squid, "
                    + "Starfish, Trilobite, Turtle, Walrus, or Whale");
    private static final FilterControlledCreaturePermanent filterControlled = new FilterControlledCreaturePermanent();
    private static final FilterControlledCreaturePermanent filterBrushwagg = new FilterControlledCreaturePermanent(SubType.BRUSHWAGG, "Brushwagg");

    static {
        List<Predicate<mage.MageObject>> types = aquatic.stream()
                .map(SubType::getPredicate)
                .collect(Collectors.toList());
        filter.add(Predicates.or(types));
        filterControlled.add(Predicates.or(types));
    }

    private static final Condition condition = new AuspiciousAquariumCondition(
            new PermanentsOnTheBattlefieldCondition(filterControlled, ComparisonType.OR_GREATER, 12, true),
            new PermanentsOnTheBattlefieldCondition(filterBrushwagg, ComparisonType.EQUAL_TO, 0, true)
    );

    public AuspiciousAquarium(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ARTIFACT}, "{5}");

        // Each creature you control that's a Clamfolk, Crab, Crocodile, Fish, Homarid, Jellyfish, Kraken, Leviathan, Lobster, Merfolk, Nautilus, Octopus, Otter, Oyster, Seal, Serpent, Shark, Shrimp, Sponge, Squid, Starfish, Trilobite, Turtle, Walrus, or Whale gets +2/+2.
        this.addAbility(new SimpleStaticAbility(new BoostControlledEffect(2, 2, Duration.WhileOnBattlefield, filter)
                .setText("each " + filter.getMessage() + " gets +2/+2")));

        // At the beginning of your upkeep, if you control twelve different creatures from the above list and don't control a Brushwagg, you win the game.
        this.addAbility(new BeginningOfUpkeepTriggeredAbility(new WinGameSourceControllerEffect())
                .withInterveningIf(condition));
    }

    private AuspiciousAquarium(final AuspiciousAquarium card) {
        super(card);
    }

    @Override
    public AuspiciousAquarium copy() {
        return new AuspiciousAquarium(this);
    }
}

class AuspiciousAquariumCondition implements Condition {

    private final Condition twelve;
    private final Condition noBrushwagg;

    AuspiciousAquariumCondition(Condition twelve, Condition noBrushwagg) {
        this.twelve = twelve;
        this.noBrushwagg = noBrushwagg;
    }

    @Override
    public boolean apply(Game game, mage.abilities.Ability source) {
        return twelve.apply(game, source) && noBrushwagg.apply(game, source);
    }

    @Override
    public String toString() {
        return "you control twelve different creatures from the above list and don't control a Brushwagg";
    }
}
