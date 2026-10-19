package mage.cards.v;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.Mode;
import mage.abilities.common.EntersBattlefieldTriggeredAbility;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.CopyTargetStackObjectEffect;
import mage.abilities.effects.common.CreateTokenCopyTargetEffect;
import mage.abilities.keyword.FlashAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.TargetController;
import mage.filter.FilterPermanent;
import mage.filter.FilterSpell;
import mage.filter.predicate.Predicates;
import mage.game.Game;
import mage.target.TargetPermanent;
import mage.target.TargetSpell;

import java.util.UUID;

/**
 * @author Claude
 */
public final class VenserFerventForger extends CardImpl {

    private static final FilterSpell filterSpell = new FilterSpell("instant or sorcery spell an opponent controls");
    private static final FilterPermanent filterPermanent = new FilterPermanent("permanent an opponent controls");

    static {
        filterSpell.add(Predicates.or(
                CardType.INSTANT.getPredicate(),
                CardType.SORCERY.getPredicate()
        ));
        filterSpell.add(TargetController.OPPONENT.getControllerPredicate());
        filterPermanent.add(TargetController.OPPONENT.getControllerPredicate());
    }

    public VenserFerventForger(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{4}{R}{R}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.SORCERER);
        this.power = new MageInt(5);
        this.toughness = new MageInt(3);

        // Flash
        this.addAbility(FlashAbility.getInstance());

        // When Venser enters, choose one --
        // * Copy target instant or sorcery spell an opponent controls twice. You may choose new targets for the copies.
        Ability ability = new EntersBattlefieldTriggeredAbility(
                new CopyTargetStackObjectEffect(false, false, true, 2, null)
                        .setText("copy target instant or sorcery spell an opponent controls twice. "
                                + "You may choose new targets for the copies")
        );
        ability.addTarget(new TargetSpell(filterSpell));

        // * Create two tokens that are copies of target permanent an opponent controls. They gain haste. At the beginning of the next end step, sacrifice them.
        Mode mode = new Mode(new VenserFerventForgerTokenEffect());
        mode.addTarget(new TargetPermanent(filterPermanent));
        ability.addMode(mode);
        this.addAbility(ability);
    }

    private VenserFerventForger(final VenserFerventForger card) {
        super(card);
    }

    @Override
    public VenserFerventForger copy() {
        return new VenserFerventForger(this);
    }
}

class VenserFerventForgerTokenEffect extends OneShotEffect {

    VenserFerventForgerTokenEffect() {
        super(Outcome.PutCreatureInPlay);
        staticText = "create two tokens that are copies of target permanent an opponent controls. "
                + "They gain haste. At the beginning of the next end step, sacrifice them";
    }

    private VenserFerventForgerTokenEffect(final VenserFerventForgerTokenEffect effect) {
        super(effect);
    }

    @Override
    public VenserFerventForgerTokenEffect copy() {
        return new VenserFerventForgerTokenEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        CreateTokenCopyTargetEffect effect = new CreateTokenCopyTargetEffect(
                null, null, true, 2
        );
        effect.setTargetPointer(getTargetPointer().copy());
        if (!effect.apply(game, source)) {
            return false;
        }
        effect.sacrificeTokensCreatedAtNextEndStep(game, source);
        return true;
    }
}
