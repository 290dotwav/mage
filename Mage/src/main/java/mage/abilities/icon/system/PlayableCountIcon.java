package mage.abilities.icon.system;

import mage.abilities.icon.CardIconImpl;
import mage.abilities.icon.CardIconType;
import mage.players.PlayableObjectStats;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GUI: playable icon, shown if card have an improtant playable abilities
 *
 * @author JayDi85
 */
public final class PlayableCountIcon extends CardIconImpl {

    public PlayableCountIcon(PlayableObjectStats objectStats) {
        // show full stats, so users can see normal and important abilities
        super(CardIconType.PLAYABLE_COUNT, getHint(objectStats), getAmount(objectStats));
    }

    private static String getAmount(PlayableObjectStats objectStats) {
        return String.valueOf(objectStats.getPlayableAmount());
    }

    private static String getHint(PlayableObjectStats objectStats) {
        String res = "Playable abilities: " + objectStats.getPlayableAmount();
        // abilities list already sorted
        List<String> list = objectStats.getPlayableAbilityNames();
        final int[] counter = {0};
        if (!list.isEmpty()) {
            res += "<br>" + list
                    .stream()
                    .map(s -> {
                        counter[0]++;
                        // the popup's lines are short; the records themselves are whole (PlayableObjectStats)
                        return " " + counter[0] + ". " + (s.length() > 50 ? s.substring(0, 50 - 1) + "..." : s);
                    })
                    .collect(Collectors.joining("<br>")) + "";
        }
        return res;
    }
}
