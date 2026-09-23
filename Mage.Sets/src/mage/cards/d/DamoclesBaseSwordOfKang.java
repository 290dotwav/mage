package mage.cards.d;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.DealsCombatDamageToAPlayerTriggeredAbility;
import mage.abilities.costs.common.SacrificeTargetCost;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.keyword.CrewAbility;
import mage.abilities.keyword.DeathtouchAbility;
import mage.abilities.keyword.FlyingAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.choices.FaceVillainousChoice;
import mage.choices.VillainousChoice;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.players.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Claude
 */
public final class DamoclesBaseSwordOfKang extends CardImpl {

    public DamoclesBaseSwordOfKang(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ARTIFACT}, "{4}{B}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.VEHICLE);
        this.power = new MageInt(5);
        this.toughness = new MageInt(5);

        // Flying
        this.addAbility(FlyingAbility.getInstance());

        // Deathtouch
        this.addAbility(DeathtouchAbility.getInstance());

        // Whenever Damocles Base deals combat damage to a player, that player faces a villainous choice -- They sacrifice a nontoken creature of their choice, or they lose 2 life and you draw two cards.
        this.addAbility(new DealsCombatDamageToAPlayerTriggeredAbility(
                new DamoclesBaseSwordOfKangEffect(), false, true
        ));

        // Crew 3
        this.addAbility(new CrewAbility(3));
    }

    private DamoclesBaseSwordOfKang(final DamoclesBaseSwordOfKang card) {
        super(card);
    }

    @Override
    public DamoclesBaseSwordOfKang copy() {
        return new DamoclesBaseSwordOfKang(this);
    }
}

class DamoclesBaseSwordOfKangEffect extends OneShotEffect {

    private static final FaceVillainousChoice choice = new FaceVillainousChoice(
            Outcome.Sacrifice,
            new DamoclesBaseSwordOfKangFirstChoice(),
            new DamoclesBaseSwordOfKangSecondChoice()
    );

    DamoclesBaseSwordOfKangEffect() {
        super(Outcome.Benefit);
        staticText = "that player " + choice.generateRule();
    }

    private DamoclesBaseSwordOfKangEffect(final DamoclesBaseSwordOfKangEffect effect) {
        super(effect);
    }

    @Override
    public DamoclesBaseSwordOfKangEffect copy() {
        return new DamoclesBaseSwordOfKangEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Optional.ofNullable(getTargetPointer().getFirst(game, source))
                .map(game::getPlayer)
                .ifPresent(player -> choice.faceChoice(player, game, source));
        return true;
    }
}

class DamoclesBaseSwordOfKangFirstChoice extends VillainousChoice {

    DamoclesBaseSwordOfKangFirstChoice() {
        super("They sacrifice a nontoken creature of their choice", "Sacrifice a nontoken creature");
    }

    @Override
    public boolean doChoice(Player player, Game game, Ability source) {
        return new SacrificeTargetCost(StaticFilters.FILTER_CREATURE_NON_TOKEN)
                .pay(source, game, source, player.getId(), true);
    }
}

class DamoclesBaseSwordOfKangSecondChoice extends VillainousChoice {

    DamoclesBaseSwordOfKangSecondChoice() {
        super("they lose 2 life and you draw two cards", "You lose 2 life and {controller} draws two cards");
    }

    @Override
    public boolean doChoice(Player player, Game game, Ability source) {
        player.loseLife(2, game, source, false);
        Player controller = game.getPlayer(source.getControllerId());
        if (controller != null) {
            controller.drawCards(2, source, game);
        }
        return true;
    }
}
