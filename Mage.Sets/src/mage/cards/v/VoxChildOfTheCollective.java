package mage.cards.v;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.EntersBattlefieldTriggeredAbility;
import mage.abilities.effects.common.continuous.NextSpellCastHasAbilityEffect;
import mage.abilities.keyword.UndauntedAbility;
import mage.abilities.meta.OrTriggeredAbility;
import mage.abilities.triggers.BeginningOfFirstMainTriggeredAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.filter.FilterCard;
import mage.filter.common.FilterArtifactCard;

import java.util.UUID;

/**
 * @author Claude
 */
public final class VoxChildOfTheCollective extends CardImpl {

    private static final FilterCard artifactCreature = new FilterArtifactCard("artifact creature card");
    private static final FilterCard spacecraft = new FilterCard("Spacecraft card");

    static {
        artifactCreature.add(CardType.CREATURE.getPredicate());
        spacecraft.add(SubType.SPACECRAFT.getPredicate());
    }

    public VoxChildOfTheCollective(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ARTIFACT, CardType.CREATURE}, "{4}{U}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.BORG);
        this.power = new MageInt(3);
        this.toughness = new MageInt(3);

        // When Vox enters and at the beginning of your first main phase, the next artifact creature spell and the next Spacecraft spell you cast this turn have undaunted.
        Ability ability = new OrTriggeredAbility(Zone.BATTLEFIELD,
                new NextSpellCastHasAbilityEffect(new UndauntedAbility(), artifactCreature)
                        .setText("the next artifact creature spell and the next Spacecraft spell you cast this turn "
                                + "have undaunted. <i>(They cost {1} less to cast for each opponent.)</i>"),
                false, "When {this} enters and at the beginning of your first main phase, ",
                new EntersBattlefieldTriggeredAbility(null),
                new BeginningOfFirstMainTriggeredAbility(null)
        );
        ability.addEffect(new NextSpellCastHasAbilityEffect(new UndauntedAbility(), spacecraft).setText(""));
        this.addAbility(ability);
    }

    private VoxChildOfTheCollective(final VoxChildOfTheCollective card) {
        super(card);
    }

    @Override
    public VoxChildOfTheCollective copy() {
        return new VoxChildOfTheCollective(this);
    }
}
