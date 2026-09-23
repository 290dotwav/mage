package mage.cards.n;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.AttacksAloneControlledTriggeredAbility;
import mage.abilities.effects.ContinuousEffect;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.DrawCardSourceControllerEffect;
import mage.abilities.effects.common.continuous.GainAbilityTargetEffect;
import mage.abilities.keyword.FirstStrikeAbility;
import mage.abilities.keyword.IndestructibleAbility;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.ComparisonType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.filter.FilterCard;
import mage.filter.common.FilterCreatureCard;
import mage.filter.predicate.mageobject.ManaValuePredicate;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.common.TargetCardInHand;
import mage.target.targetpointer.FixedTarget;
import mage.util.CardUtil;

import java.util.UUID;

/**
 * @author Claude
 */
public final class NickFurySpymaster extends CardImpl {

    public NickFurySpymaster(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{4}{W}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.SPY);
        this.subtype.add(SubType.HERO);
        this.power = new MageInt(4);
        this.toughness = new MageInt(4);

        // First strike
        this.addAbility(FirstStrikeAbility.getInstance());

        // Whenever a creature you control attacks alone, draw a card. Then you may put a creature card with mana value 3 or less from your hand onto the battlefield. It enters tapped and attacking and gains indestructible until end of turn.
        Ability ability = new AttacksAloneControlledTriggeredAbility(new DrawCardSourceControllerEffect(1));
        ability.addEffect(new NickFurySpymasterEffect());
        this.addAbility(ability);
    }

    private NickFurySpymaster(final NickFurySpymaster card) {
        super(card);
    }

    @Override
    public NickFurySpymaster copy() {
        return new NickFurySpymaster(this);
    }
}

class NickFurySpymasterEffect extends OneShotEffect {

    private static final FilterCard filter = new FilterCreatureCard("creature card with mana value 3 or less");

    static {
        filter.add(new ManaValuePredicate(ComparisonType.FEWER_THAN, 4));
    }

    NickFurySpymasterEffect() {
        super(Outcome.PutCreatureInPlay);
        staticText = "Then you may put a creature card with mana value 3 or less from your hand onto the battlefield. "
                + "It enters tapped and attacking and gains indestructible until end of turn";
    }

    private NickFurySpymasterEffect(final NickFurySpymasterEffect effect) {
        super(effect);
    }

    @Override
    public NickFurySpymasterEffect copy() {
        return new NickFurySpymasterEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player player = game.getPlayer(source.getControllerId());
        if (player == null || player.getHand().count(filter, player.getId(), source, game) == 0
                || !player.chooseUse(Outcome.PutCreatureInPlay, "Put a creature card with mana value 3 or less from your hand onto the battlefield tapped and attacking?", source, game)) {
            return false;
        }
        TargetCardInHand target = new TargetCardInHand(filter);
        player.choose(Outcome.PutCreatureInPlay, target, source, game);
        Card card = game.getCard(target.getFirstTarget());
        if (card == null || !player.moveCards(card, Zone.BATTLEFIELD, source, game, true, false, false, null)) {
            return false;
        }
        Permanent permanent = CardUtil.getPermanentFromCardPutToBattlefield(card, game);
        if (permanent == null) {
            return false;
        }
        game.getCombat().addAttackingCreature(permanent.getId(), game);
        ContinuousEffect effect = new GainAbilityTargetEffect(IndestructibleAbility.getInstance(), Duration.EndOfTurn);
        effect.setTargetPointer(new FixedTarget(permanent, game));
        game.addEffect(effect, source);
        return true;
    }
}
