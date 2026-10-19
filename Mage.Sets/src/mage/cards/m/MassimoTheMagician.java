package mage.cards.m;

import mage.MageInt;
import mage.MageObjectReference;
import mage.abilities.Ability;
import mage.abilities.common.DealsCombatDamageToAPlayerTriggeredAbility;
import mage.abilities.common.EntersBattlefieldThisOrAnotherTriggeredAbility;
import mage.abilities.effects.ContinuousEffect;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.continuous.GainAbilityTargetEffect;
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
import mage.filter.StaticFilters;
import mage.filter.common.FilterInstantOrSorceryCard;
import mage.filter.predicate.mageobject.ManaValuePredicate;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.common.TargetCardInYourGraveyard;
import mage.target.targetpointer.FixedTarget;
import mage.util.CardUtil;

import java.util.UUID;

/**
 * @author Claude
 */
public final class MassimoTheMagician extends CardImpl {

    private static final FilterCard filter = new FilterInstantOrSorceryCard("instant or sorcery card with mana value 1 from your graveyard");

    static {
        filter.add(new ManaValuePredicate(ComparisonType.EQUAL_TO, 1));
    }

    public MassimoTheMagician(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{U}{R}{W}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.CAT);
        this.subtype.add(SubType.WIZARD);
        this.power = new MageInt(3);
        this.toughness = new MageInt(2);

        // Whenever Massimo or another creature you control enters, exile up to one target instant or sorcery card with mana value 1 from your graveyard. If a card is exiled this way, that creature gains "Whenever this creature deals combat damage to a player, copy the exiled card. You may cast the copy without paying its mana cost."
        Ability ability = new EntersBattlefieldThisOrAnotherTriggeredAbility(
                new MassimoTheMagicianEffect(), StaticFilters.FILTER_PERMANENT_CREATURE, false, true
        );
        ability.addTarget(new TargetCardInYourGraveyard(0, 1, filter));
        this.addAbility(ability);
    }

    private MassimoTheMagician(final MassimoTheMagician card) {
        super(card);
    }

    @Override
    public MassimoTheMagician copy() {
        return new MassimoTheMagician(this);
    }
}

class MassimoTheMagicianEffect extends OneShotEffect {

    MassimoTheMagicianEffect() {
        super(Outcome.Benefit);
        staticText = "exile up to one target instant or sorcery card with mana value 1 from your graveyard. "
                + "If a card is exiled this way, that creature gains \"Whenever this creature deals combat damage "
                + "to a player, copy the exiled card. You may cast the copy without paying its mana cost.\"";
    }

    private MassimoTheMagicianEffect(final MassimoTheMagicianEffect effect) {
        super(effect);
    }

    @Override
    public MassimoTheMagicianEffect copy() {
        return new MassimoTheMagicianEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        Card card = game.getCard(getTargetPointer().getFirst(game, source));
        if (controller == null || card == null) {
            return false;
        }
        controller.moveCardsToExile(card, source, game, true, CardUtil.getExileZoneId(game, source),
                CardUtil.getSourceName(game, source));
        if (game.getState().getZone(card.getId()) != Zone.EXILED) {
            return false;
        }
        Permanent creature = (Permanent) getValue("permanentEnteringBattlefield");
        if (creature == null) {
            return true;
        }
        Ability gained = new DealsCombatDamageToAPlayerTriggeredAbility(
                new MassimoTheMagicianCopyEffect(new MageObjectReference(card, game)), false
        );
        ContinuousEffect effect = new GainAbilityTargetEffect(gained, Duration.Custom);
        effect.setTargetPointer(new FixedTarget(creature.getId(), game));
        game.addEffect(effect, source);
        return true;
    }
}

class MassimoTheMagicianCopyEffect extends OneShotEffect {

    private final MageObjectReference exiledCard;

    MassimoTheMagicianCopyEffect(MageObjectReference exiledCard) {
        super(Outcome.PlayForFree);
        this.exiledCard = exiledCard;
        staticText = "copy the exiled card. You may cast the copy without paying its mana cost";
    }

    private MassimoTheMagicianCopyEffect(final MassimoTheMagicianCopyEffect effect) {
        super(effect);
        this.exiledCard = effect.exiledCard;
    }

    @Override
    public MassimoTheMagicianCopyEffect copy() {
        return new MassimoTheMagicianCopyEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        Card card = exiledCard.getCard(game);
        if (controller == null || card == null || game.getState().getZone(card.getId()) != Zone.EXILED) {
            return false;
        }
        Card copiedCard = game.copyCard(card, source, controller.getId());
        CardUtil.castSpellWithAttributesForFree(controller, source, game, copiedCard);
        return true;
    }
}
