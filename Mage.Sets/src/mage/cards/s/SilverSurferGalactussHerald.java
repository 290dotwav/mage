package mage.cards.s;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.DealsCombatDamageToAPlayerTriggeredAbility;
import mage.abilities.common.EntersBattlefieldTriggeredAbility;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.RequirementEffect;
import mage.abilities.effects.common.search.SearchLibraryPutInHandEffect;
import mage.abilities.keyword.FlyingAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.filter.FilterCard;
import mage.filter.predicate.mageobject.NamePredicate;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.target.common.TargetCardInLibrary;
import mage.target.common.TargetCreaturePermanent;
import mage.target.targetpointer.FixedTarget;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * @author Claude
 */
public final class SilverSurferGalactussHerald extends CardImpl {

    private static final FilterCard filter = new FilterCard("card named Galactus, Devourer of Worlds");

    static {
        filter.add(new NamePredicate("Galactus, Devourer of Worlds"));
    }

    public SilverSurferGalactussHerald(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{5}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.ALIEN);
        this.subtype.add(SubType.HERO);
        this.power = new MageInt(4);
        this.toughness = new MageInt(5);

        // Flying
        this.addAbility(FlyingAbility.getInstance());

        // When Silver Surfer enters, you may search your library for a card named Galactus, Devourer of Worlds, reveal it, put it into your hand, then shuffle.
        this.addAbility(new EntersBattlefieldTriggeredAbility(
                new SearchLibraryPutInHandEffect(new TargetCardInLibrary(filter), true), true
        ));

        // Whenever Silver Surfer deals combat damage to a player, until the end of your next turn, target creature attacks that player each combat if able.
        Ability ability = new DealsCombatDamageToAPlayerTriggeredAbility(new SilverSurferGalactussHeraldEffect(), false, true);
        ability.addTarget(new TargetCreaturePermanent());
        this.addAbility(ability);
    }

    private SilverSurferGalactussHerald(final SilverSurferGalactussHerald card) {
        super(card);
    }

    @Override
    public SilverSurferGalactussHerald copy() {
        return new SilverSurferGalactussHerald(this);
    }
}

class SilverSurferGalactussHeraldEffect extends OneShotEffect {

    SilverSurferGalactussHeraldEffect() {
        super(Outcome.Benefit);
        staticText = "until the end of your next turn, target creature attacks that player each combat if able";
    }

    private SilverSurferGalactussHeraldEffect(final SilverSurferGalactussHeraldEffect effect) {
        super(effect);
    }

    @Override
    public SilverSurferGalactussHeraldEffect copy() {
        return new SilverSurferGalactussHeraldEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        // the trigger's target pointer holds the damaged player; the ability's own target is the creature
        UUID playerId = getTargetPointer().getFirst(game, source);
        Permanent creature = game.getPermanent(source.getFirstTarget());
        if (playerId == null || game.getPlayer(playerId) == null || creature == null) {
            return false;
        }
        RequirementEffect effect = new SilverSurferGalactussHeraldRequirementEffect(playerId);
        effect.setTargetPointer(new FixedTarget(creature, game));
        game.addEffect(effect, source);
        return true;
    }
}

class SilverSurferGalactussHeraldRequirementEffect extends RequirementEffect {

    private final UUID defenderId;

    SilverSurferGalactussHeraldRequirementEffect(UUID defenderId) {
        super(Duration.UntilEndOfYourNextTurn);
        this.defenderId = defenderId;
    }

    private SilverSurferGalactussHeraldRequirementEffect(final SilverSurferGalactussHeraldRequirementEffect effect) {
        super(effect);
        this.defenderId = effect.defenderId;
    }

    @Override
    public SilverSurferGalactussHeraldRequirementEffect copy() {
        return new SilverSurferGalactussHeraldRequirementEffect(this);
    }

    @Override
    public boolean applies(Permanent permanent, Ability source, Game game) {
        return permanent.getId().equals(getTargetPointer().getFirst(game, source));
    }

    @Override
    public boolean mustAttack(Game game) {
        return true;
    }

    @Override
    public boolean mustBlock(Game game) {
        return false;
    }

    /**
     * "attacks that player each combat if able": when it can't attack that player, nothing forces it to attack
     * anyone else (RequirementEffect.mustAttackDefenders, from cards-batch-B)
     */
    @Override
    public Set<UUID> mustAttackDefenders(Ability source, Game game) {
        return Collections.singleton(defenderId);
    }
}
