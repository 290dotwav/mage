package org.mage.test.cards.abilities.keywords;

import mage.abilities.Ability;
import mage.abilities.SpellAbility;
import mage.abilities.keyword.MayhemAbility;
import mage.abilities.keyword.SneakAbility;
import mage.abilities.keyword.WebSlingingAbility;
import mage.cards.Card;
import mage.cards.repository.CardCriteria;
import mage.cards.repository.CardInfo;
import mage.cards.repository.CardRepository;
import mage.cards.repository.CardScanner;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Sneak, web-slinging and mayhem are each a second SpellAbility made as a copy of the card's own
 * (702.190a, 702.188a, 702.187b: the same spell, cast for another cost). The copy is taken when the
 * keyword is added in the card's constructor: a card that adds its keyword before its spell's effects
 * and targets casts, by that cost, a spell that does nothing (Donatello's Technique drew no card,
 * Jennika's Technique dealt no damage). Every instant and sorcery with one of them must resolve the
 * card's effects by that cost too.
 */
public class AlternateCastCopiesSpellTest {

    @Test
    public void testEveryAlternateCastCarriesTheSpellsEffects() {
        CardScanner.scan();
        List<String> wrong = new ArrayList<>();
        for (CardInfo info : CardRepository.instance.findCards(new CardCriteria())) {
            String rules = String.join("\n", info.getRules());
            if (!rules.contains("Sneak") && !rules.contains("Web-slinging") && !rules.contains("Mayhem")) {
                continue;
            }
            Card card = info.createCard();
            if (card == null || !(card.isInstantOrSorcery())) {
                continue;
            }
            SpellAbility main = card.getSpellAbility();
            for (Ability ability : card.getAbilities()) {
                if (!(ability instanceof SneakAbility || ability instanceof WebSlingingAbility || ability instanceof MayhemAbility)) {
                    continue;
                }
                if (ability.getEffects().isEmpty() && !main.getEffects().isEmpty()) {
                    wrong.add(info.getName() + " (" + ability.getClass().getSimpleName() + ": no effect)");
                } else if (ability.getTargets().size() < main.getTargets().size()) {
                    wrong.add(info.getName() + " (" + ability.getClass().getSimpleName() + ": fewer targets)");
                } else if (ability.getModes().size() < main.getModes().size()) {
                    wrong.add(info.getName() + " (" + ability.getClass().getSimpleName() + ": fewer modes)");
                }
            }
        }
        Assert.assertTrue("cast by another cost, these do nothing: " + wrong, wrong.isEmpty());
    }
}
