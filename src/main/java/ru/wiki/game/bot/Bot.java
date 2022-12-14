package ru.wiki.game.bot;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.messages.*;
import com.vk.api.sdk.queries.messages.MessagesGetLongPollHistoryQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import ru.wiki.game.separation.Separation;
import ru.wiki.game.util.URLFetch;
import ru.wiki.game.util.Utilities;


import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import static ru.wiki.game.bot.MessageConstants.END_ARTICLE;
import static ru.wiki.game.bot.MessageConstants.RESTART;
import static ru.wiki.game.bot.MessageConstants.START_MESSAGE;
import static ru.wiki.game.bot.MessageConstants.START_ARTICLE;

@Slf4j
public class Bot {

    private final GroupActor actor;
    private final VkApiClient vk;
    private final Random random;
    Cache<String, String> cache;

    public Bot() throws ConfigurationException {
        PropertiesConfiguration config = new PropertiesConfiguration();
        config.load("application.properties");
        this.actor = new GroupActor(config.getInt("groupId"), config.getString("token.access"));
        this.vk = new VkApiClient(new HttpTransportClient());
        this.random = new Random();

        CachingProvider provider = Caching.getCachingProvider();
        CacheManager cacheManager = provider.getCacheManager();
        MutableConfiguration<String, String> configuration =
                new MutableConfiguration<String, String>()
                        .setTypes(String.class, String.class)
                        .setStoreByValue(false)
                        .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.FIVE_MINUTES));
        cache = cacheManager.createCache("jCache", configuration);
//        cache.put(1L, "one");
//        String value = cache.get(1L);
    }

    public void start() throws ClientException, ApiException, InterruptedException {
        Integer ts = vk.messages().getLongPollServer(actor).execute().getTs();

        while (true) {
            MessagesGetLongPollHistoryQuery historyQuery = vk.messages().getLongPollHistory(actor).ts(ts);
            List<Message> messages = historyQuery.execute().getMessages().getItems();
            if (!messages.isEmpty()) {
                messages.forEach(message -> {
                    Integer userId = message.getFromId();
                    try {
                        if (message.getText().equals("????????????")) {
                            sendMessage(START_MESSAGE, userId);
                            getArticlesForStart(userId);
                        } else if (message.getText().equals("???????????? ????????????")) {
                            getArticlesForStart(userId);
                        } else {
                            sendMessage("?? ???????? ???? ??????????????", userId);
                            setKeyboardsButton("???????????? ????????????", userId, RESTART);
                        }
                    } catch (ApiException | ClientException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            ts = vk.messages().getLongPollServer(actor).execute().getTs();
            Thread.sleep(500);
        }
    }

    private void getArticlesForStart(Integer userId) throws ClientException, ApiException, InterruptedException {
        setKeyboardsButton("?????????????????? ????????????", userId, START_ARTICLE);
        String article1 = getArticle(vk);

        setKeyboardsButton("?????????????????? ????????????", userId, END_ARTICLE);
        String article2 = getArticle(vk);

        sendMessage("?????????????? ??????????: " + article1 + " -> " + article2, userId);

        startGame(article1, article2, userId);
    }

    private void startGame(String start, String end, Integer userId) throws ClientException, ApiException {
        Separation separation;
        if (cache.containsKey(start + " -> " + end)) {
            log.info("Getting from cache.");
            sendMessage(cache.get(start + " -> " + end), userId);
            setKeyboardsButton("???????????? ????????????", userId, RESTART);
            return;
        }
        try {
            separation = new Separation(start, end);
        } catch (IOException e) {
            log.info("Unknown exception occurred.");
            return;
        }

        if (!separation.getPathExists()) {
            sendMessage("???????????????????? ?????????? ???????? ???? " + start + " ?? " + end, userId);
            setKeyboardsButton("???????????? ????????????", userId, RESTART);
            log.info("Unable to find a path from " + start + " to " + end);
        } else {
            sendMessage("???????????????????????? " + separation.getNumDegrees() + " ??????????????????", userId);
            log.info("Degrees of Separation: " + separation.getNumDegrees());

            Stack<String> path = separation.getPath();
            Stack<String> embedded = separation.getEmbeddedPath();
            StringBuilder sb = new StringBuilder("????????:\n");
            for (int i = 0; i < path.size(); ++i) {
                String current = path.get(i);
                String currentEmbedded = (i > 0) ? embedded.get(i - 1) : current;

                sb.append(current).append(" ").append(URLFetch.getUrlFromTitle(current));
                if (!current.equalsIgnoreCase(currentEmbedded)) {
                    sb.append(" [").append(currentEmbedded).append("]");
                }

                if (i < path.size() - 1) {
                    sb.append(" -> \n");
                }
            }
            log.info(sb.toString());
            cache.put(start + " -> " + end, sb.toString());
            sendMessage(sb.toString(), userId);
            setKeyboardsButton("???????????? ????????????", userId, RESTART);
        }

    }

    private String getArticle(VkApiClient vk) throws ClientException, ApiException, InterruptedException {
        Random random = new Random();
        Integer ts = vk.messages().getLongPollServer(actor).execute().getTs();
        while (true) {
            MessagesGetLongPollHistoryQuery historyQuery = vk.messages().getLongPollHistory(actor).ts(ts);
            List<Message> messages = historyQuery.execute().getMessages().getItems();
            if (!messages.isEmpty()) {
                for (Message msg : messages) {
                    try {
                        String msgCurrent = msg.getText();
                        if (msgCurrent.equals("?????????????????? ????????????")) {
                            String article = Utilities.getRandomArticle();
                            vk.messages().send(actor).message("?????????????????? ????????????: " + article)
                                    .userId(msg.getFromId()).randomId(random.nextInt(10000)).execute();
                            return article;
                        } else {
                            vk.messages().send(actor).message("???????? ????????????: " + msgCurrent)
                                    .userId(msg.getFromId()).randomId(random.nextInt(10000)).execute();
                            return msgCurrent;
                        }
                    } catch (ApiException | ClientException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            ts = vk.messages().getLongPollServer(actor).execute().getTs();
            Thread.sleep(500);
        }
    }


    private void setKeyboardsButton(String button, Integer userId, String message) throws ClientException, ApiException {
        Keyboard keyboard = new Keyboard();
        List<List<KeyboardButton>> allKey = new ArrayList<>();
        List<KeyboardButton> line1 = new ArrayList<>();
        line1.add(new KeyboardButton().setAction(new KeyboardButtonAction().setLabel(button).setType(KeyboardButtonActionType.TEXT)).setColor(KeyboardButtonColor.POSITIVE));
        allKey.add(line1);
        keyboard.setButtons(allKey).setOneTime(true);
        vk.messages().send(actor).message(message)
                .userId(userId).randomId(random.nextInt(10000)).keyboard(keyboard).execute();
    }

    private void sendMessage(String message, Integer userId) throws ClientException, ApiException {
        vk.messages().send(actor).message(message)
                .userId(userId).randomId(random.nextInt(10000)).execute();
    }
}
