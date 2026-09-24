package mage.cards.m;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.DrawNthCardTriggeredAbility;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.dynamicvalue.DynamicValue;
import mage.abilities.dynamicvalue.common.StaticValue;
import mage.abilities.effects.Effect;
import mage.abilities.effects.common.MillCardsTargetEffect;
import mage.abilities.effects.common.continuous.BoostSourceEffect;
import mage.abilities.hint.ValueHint;
import mage.abilities.keyword.VigilanceAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.SubType;
import mage.game.Game;
import mage.players.Player;
import mage.target.TargetPlayer;

import java.util.UUID;

/**
 * @author Claude
 */
public final class MastersCouncillors extends CardImpl {

    public MastersCouncillors(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{1}{U}");

        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.ADVISOR);
        this.power = new MageInt(1);
        this.toughness = new MageInt(3);

        // Vigilance
        this.addAbility(VigilanceAbility.getInstance());

        // This creature gets +2/+0 for each graveyard with seven or more cards in it.
        this.addAbility(new SimpleStaticAbility(new BoostSourceEffect(
                MastersCouncillorsValue.instance, StaticValue.get(0), Duration.WhileOnBattlefield
        ).setText("{this} gets +2/+0 for each graveyard with seven or more cards in it"))
                .addHint(new ValueHint("Graveyards with seven or more cards", MastersCouncillorsCount.instance)));

        // Whenever you draw your second card each turn, target player mills three cards.
        Ability ability = new DrawNthCardTriggeredAbility(new MillCardsTargetEffect(3));
        ability.addTarget(new TargetPlayer());
        this.addAbility(ability);
    }

    private MastersCouncillors(final MastersCouncillors card) {
        super(card);
    }

    @Override
    public MastersCouncillors copy() {
        return new MastersCouncillors(this);
    }
}

enum MastersCouncillorsCount implements DynamicValue {
    instance;

    @Override
    public int calculate(Game game, Ability sourceAbility, Effect effect) {
        int count = 0;
        for (UUID playerId : game.getState().getPlayersInRange(sourceAbility.getControllerId(), game)) {
            Player player = game.getPlayer(playerId);
            if (player != null && player.getGraveyard().size() >= 7) {
                count++;
            }
        }
        return count;
    }

    @Override
    public MastersCouncillorsCount copy() {
        return this;
    }

    @Override
    public String getMessage() {
        return "graveyard with seven or more cards in it";
    }

    @Override
    public String toString() {
        return "1";
    }
}

enum MastersCouncillorsValue implements DynamicValue {
    instance;

    @Override
    public int calculate(Game game, Ability sourceAbility, Effect effect) {
        return 2 * MastersCouncillorsCount.instance.calculate(game, sourceAbility, effect);
    }

    @Override
    public MastersCouncillorsValue copy() {
        return this;
    }

    @Override
    public String getMessage() {
        return "graveyard with seven or more cards in it";
    }

    @Override
    public String toString() {
        return "2";
    }
}
