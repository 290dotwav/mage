package mage.view;

import mage.game.Game;
import mage.util.CardUtil;

import java.io.Serializable;
import java.util.Date;

/**
 * @author BetaSteward_at_googlemail.com
 */
public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private Date time;
    private String turnInfo;
    private String message;
    private MessageColor color;
    private SoundToPlay soundToPlay;
    private MessageType messageType;
    // web door: the game log's own number for a line of it (Game.getLogLinesSaid), null for anything
    // else; a rollback takes lines back by these numbers (RollbackPoints.takenBack)
    private Integer gameLine;

    public enum MessageColor {
        BLACK, RED, GREEN, BLUE, ORANGE, YELLOW
    }

    public enum MessageType {
        USER_INFO, // system messages
        STATUS, // system messages
        GAME, // game logs
        TALK, // public chat
        WHISPER_FROM, // private chat income
        WHISPER_TO // private chat outcome
    }

    public enum SoundToPlay {
        PlayerLeft, PlayerQuitTournament, PlayerSubmittedDeck, PlayerWhispered
    }

    public ChatMessage(String username, String message, Date time, Game game, MessageColor color) {
        this(username, message, time, game, color, null);
    }

    public ChatMessage(String username, String message, Date time, Game game, MessageColor color, SoundToPlay soundToPlay) {
        this(username, message, time, null, color, MessageType.TALK, soundToPlay);
    }

    public ChatMessage(String username, String message, Date time, Game game, MessageColor color, MessageType messageType, SoundToPlay soundToPlay) {
        this.username = username;
        this.message = message;
        this.time = time;
        this.turnInfo = CardUtil.getTurnInfo(game);
        this.color = color;
        this.messageType = messageType;
        this.soundToPlay = soundToPlay;
    }

    public ChatMessage(String username, String message, Date time, Game game, MessageColor color, MessageType messageType, SoundToPlay soundToPlay, int gameLine) {
        this(username, message, time, game, color, messageType, soundToPlay);
        this.gameLine = gameLine > 0 ? gameLine : null;
    }

    public String getMessage() {
        return message;
    }

    public MessageColor getColor() {
        return color;
    }

    public boolean isUserMessage() {
        return color != null && (color == MessageColor.BLUE || color == MessageColor.YELLOW);
    }

    public boolean isStatusMessage() {
        return color != null && color == MessageColor.ORANGE;
    }

    public String getUsername() {
        return username;
    }

    public Date getTime() {
        return time;
    }

    public String getTurnInfo() {
        return turnInfo;
    }

    public SoundToPlay getSoundToPlay() {
        return soundToPlay;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    /** The game log's number for this line, null when it is not a numbered line of a game's log. */
    public Integer getGameLine() {
        return gameLine;
    }

}
