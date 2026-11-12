package mage.server;

import mage.interfaces.callback.ClientCallback;
import mage.interfaces.callback.ClientCallbackMethod;
import mage.server.managers.ManagerFactory;
import mage.server.managers.ThreadExecutor;
import mage.server.managers.UserManager;
import mage.view.ChatMessage;
import mage.view.ChatMessage.MessageColor;
import mage.view.ChatMessage.MessageType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Each Claude chair says its traits in one line of the game's chat the moment the game is over, and every
 * screen draws them on its end of the game. The game's chat went the instant the game ended, so the lines that
 * came after that were dropped: the owner, on a game with three chairs, « Je peux voir qu'un seul Claude la.. ».
 * A game's chat now stays open a moment after the end, for those lines and nothing else.
 */
public class GameChatAfterTheEndTest {

    private ScheduledThreadPoolExecutor timer;
    private ChatManagerImpl chats;
    private final List<String> heard = new ArrayList<>();
    private User screen;

    @Before
    public void setUp() {
        timer = new ScheduledThreadPoolExecutor(1);
        Executor now = Runnable::run;
        ManagerFactory[] factory = new ManagerFactory[1];
        ThreadExecutor threads = (ThreadExecutor) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ThreadExecutor.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getTimeoutExecutor":
                            return timer;
                        case "getCallExecutor":
                            return new java.util.concurrent.AbstractExecutorService() {
                                public void shutdown() {}
                                public List<Runnable> shutdownNow() { return new ArrayList<>(); }
                                public boolean isShutdown() { return false; }
                                public boolean isTerminated() { return false; }
                                public boolean awaitTermination(long t, java.util.concurrent.TimeUnit u) { return true; }
                                public void execute(Runnable r) { now.execute(r); }
                            };
                        default:
                            return null;
                    }
                });
        screen = new User(null, "fgmuller", "127.0.0.1", null) {
            @Override
            public void addCallback(ClientCallback call) {
                if (call.getMethod() == ClientCallbackMethod.CHATMESSAGE) {
                    call.decompressData();
                    heard.add(((ChatMessage) call.getData()).getMessage());
                }
            }

            @Override
            public void flushCallbacksQueue() {
            }
        };
        UserManager users = (UserManager) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{UserManager.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getUser":
                            return screen.getId().equals(args[0]) ? Optional.of(screen) : Optional.empty();
                        case "getUserByName":
                            return Optional.empty();
                        default:
                            return null;
                    }
                });
        factory[0] = (ManagerFactory) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ManagerFactory.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "threadExecutor":
                            return threads;
                        case "userManager":
                            return users;
                        default:
                            return null;
                    }
                });
        chats = new ChatManagerImpl(factory[0]);
    }

    @After
    public void tearDown() {
        timer.shutdownNow();
    }

    private void say(UUID chatId, String name, String line) {
        chats.broadcast(chatId, name, line, MessageColor.BLUE, true, null, MessageType.TALK, null);
    }

    @Test
    public void everyChairsTraitsLineIsHeardAfterTheEnd() {
        UUID chatId = chats.createRoomChatSession(UUID.randomUUID());
        chats.joinChat(chatId, screen.getId());
        heard.clear();

        // the game is over: GameController.cleanUp
        chats.closeGameChatSession(chatId);

        // the three chairs of that game, each a moment after the end, in any order
        say(chatId, "CounterSpelled", "🎭 CounterSpelled instincts: proud 0.58, vulture 0.74 (drawn); quirks: roleplayer, fatalist, dry");
        say(chatId, "ProxyKing", "🎭 ProxyKing instincts: vulture 0.71, bluffer 0.49 (drawn); quirks: roleplayer, salty, dry");
        say(chatId, "Mulligan7", "🎭 Mulligan7 instincts: swayed 0.57, cautious 0.57 (drawn); quirks: nostalgic");
        Assert.assertEquals("each chair's line reaches the screen", 3, heard.size());
        Assert.assertTrue(heard.get(1).contains("ProxyKing instincts"));
        Assert.assertTrue(heard.get(2).contains("Mulligan7 instincts"));
    }

    @Test
    public void nothingElseIsHeardAfterTheEnd() {
        UUID chatId = chats.createRoomChatSession(UUID.randomUUID());
        chats.joinChat(chatId, screen.getId());
        heard.clear();
        chats.closeGameChatSession(chatId);

        // as when the chat was gone at once
        say(chatId, "fgmuller", "gg");
        chats.broadcast(chatId, "", "Mulligan7 has left", MessageColor.BLUE, true, null, MessageType.STATUS, null);
        Assert.assertEquals(new ArrayList<String>(), heard);
    }

    @Test
    public void theChatGoesHalfAMinuteLater() {
        UUID chatId = chats.createRoomChatSession(UUID.randomUUID());
        chats.closeGameChatSession(chatId);
        Assert.assertEquals("still open just after the end", 1, chats.getChatSessions().size());
        ScheduledExecutorService scheduled = timer;
        Assert.assertEquals("its close is waiting", 1, ((ScheduledThreadPoolExecutor) scheduled).getQueue().size());
        Assert.assertEquals(30, ChatManagerImpl.GAME_CHAT_OPEN_AFTER_END_SECS);
        chats.destroyChatSession(chatId);
        Assert.assertEquals(0, chats.getChatSessions().size());
    }
}
