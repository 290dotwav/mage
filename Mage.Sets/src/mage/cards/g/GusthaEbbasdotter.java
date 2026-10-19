package mage.cards.g;

import mage.MageInt;
import mage.ObjectColor;
import mage.abilities.common.FirstSpellOpponentsTurnTriggeredAbility;
import mage.abilities.effects.common.BrainstormEffect;
import mage.abilities.keyword.ProtectionAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.SubType;
import mage.constants.SuperType;

import java.util.UUID;

/**
 * @author Claude
 */
public final class GusthaEbbasdotter extends CardImpl {

    public GusthaEbbasdotter(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{3}{U}{U}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.WIZARD);
        this.power = new MageInt(3);
        this.toughness = new MageInt(3);

        // Protection from red
        this.addAbility(ProtectionAbility.from(ObjectColor.RED));

        // Whenever you cast your first spell during each opponent's turn, draw three cards, then put two cards from your hand on top of your library in any order.
        this.addAbility(new FirstSpellOpponentsTurnTriggeredAbility(new BrainstormEffect(3, 2), false));
    }

    private GusthaEbbasdotter(final GusthaEbbasdotter card) {
        super(card);
    }

    @Override
    public GusthaEbbasdotter copy() {
        return new GusthaEbbasdotter(this);
    }
}
