package mage.cards.t;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.AttacksWithCreaturesTriggeredAbility;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.effects.Effect;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.cost.SpellsCostReductionControllerEffect;
import mage.abilities.keyword.FlyingAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.constants.AbilityWord;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.filter.FilterCard;
import mage.filter.StaticFilters;
import mage.filter.common.FilterControlledCreaturePermanent;
import mage.filter.predicate.mageobject.AnotherPredicate;
import mage.game.Game;
import mage.players.Player;
import mage.util.CardUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Claude
 */
public final class TheUrSphinx extends CardImpl {

    private static final FilterCard filter = new FilterCard("Sphinx spells");
    private static final FilterControlledCreaturePermanent filterSphinx
            = new FilterControlledCreaturePermanent(SubType.SPHINX, "Sphinxes you control");

    static {
        filter.add(SubType.SPHINX.getPredicate());
        filter.add(AnotherPredicate.instance);
    }

    public TheUrSphinx(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{6}{W}{U}{B}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.SPHINX);
        this.subtype.add(SubType.AVATAR);
        this.power = new MageInt(10);
        this.toughness = new MageInt(10);

        // Eminence -- As long as The Ur-Sphinx is in the command zone or on the battlefield, other Sphinx spells you cast cost {1} less to cast.
        Effect effect = new SpellsCostReductionControllerEffect(filter, 1);
        effect.setText("As long as {this} is in the command zone or on the battlefield, other Sphinx spells you cast cost {1} less to cast");
        Ability ability = new SimpleStaticAbility(effect);
        ability.setAbilityWord(AbilityWord.EMINENCE);
        this.addAbility(ability);
        effect = new SpellsCostReductionControllerEffect(filter, 1);
        effect.setText("");
        this.addAbility(new SimpleStaticAbility(Zone.COMMAND, effect));

        // Flying
        this.addAbility(FlyingAbility.getInstance());

        // Whenever one or more Sphinxes you control attack, each player mills that many cards. For each player, you may cast a card that player milled this way without paying its mana cost.
        this.addAbility(new AttacksWithCreaturesTriggeredAbility(new TheUrSphinxEffect(), 1, filterSphinx));
    }

    private TheUrSphinx(final TheUrSphinx card) {
        super(card);
    }

    @Override
    public TheUrSphinx copy() {
        return new TheUrSphinx(this);
    }
}

class TheUrSphinxEffect extends OneShotEffect {

    TheUrSphinxEffect() {
        super(Outcome.PlayForFree);
        staticText = "each player mills that many cards. For each player, you may cast a card that player "
                + "milled this way without paying its mana cost";
    }

    private TheUrSphinxEffect(final TheUrSphinxEffect effect) {
        super(effect);
    }

    @Override
    public TheUrSphinxEffect copy() {
        return new TheUrSphinxEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        Integer amount = (Integer) getValue(AttacksWithCreaturesTriggeredAbility.VALUEKEY_NUMBER_ATTACKERS);
        if (amount == null || amount < 1) {
            return false;
        }
        Map<UUID, Cards> milledByPlayer = new LinkedHashMap<>();
        for (UUID playerId : game.getState().getPlayersInRange(controller.getId(), game)) {
            Player player = game.getPlayer(playerId);
            if (player == null) {
                continue;
            }
            milledByPlayer.put(playerId, new CardsImpl(player.millCards(amount, source, game)));
        }
        for (Map.Entry<UUID, Cards> entry : milledByPlayer.entrySet()) {
            Cards milled = entry.getValue();
            milled.retainZone(Zone.GRAVEYARD, game);
            if (milled.isEmpty()) {
                continue;
            }
            CardUtil.castSpellWithAttributesForFree(controller, source, game, milled, StaticFilters.FILTER_CARD);
        }
        return true;
    }
}
