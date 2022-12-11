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

    public Bot() throws ConfigurationException {
        PropertiesConfiguration config = new PropertiesConfiguration();
        config.load("application.properties");
        this.actor = new GroupActor(config.getInt("groupId"), config.getString("token.access"));
        this.vk = new VkApiClient(new HttpTransportClient());
        this.random = new Random();
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
                        if (message.getText().equals("Начать")) {
                            sendMessage(START_MESSAGE, userId);
                            getArticlesForStart(userId);
                        } else if (message.getText().equals("Начать заново")) {
                            getArticlesForStart(userId);
                        } else {
                            sendMessage("Я тебя не понимаю", userId);
                            setKeyboardsButton("Начать заново", userId, RESTART);
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
        setKeyboardsButton("Случайная статья", userId, START_ARTICLE);
        String article1 = getArticle(vk);

        setKeyboardsButton("Случайная статья", userId, END_ARTICLE);
        String article2 = getArticle(vk);

        sendMessage("Начинаю поиск: " + article1 + " -> " + article2, userId);

        startGame(article1, article2, userId);
    }

    private void startGame(String start, String end, Integer userId) throws ClientException, ApiException {
        Separation separation;
        try {
            separation = new Separation(start, end);
        } catch (IOException e) {
            log.info("Unknown exception occurred.");
            return;
        }

        if (!separation.getPathExists()) {
            sendMessage("Невозможно найти путь от " + start + " к " + end, userId);
            log.info("Unable to find a path from " + start + " to " + end);
        } else {
            sendMessage("Понадобилось " + separation.getNumDegrees() + " переходов", userId);
            log.info("Degrees of Separation: " + separation.getNumDegrees());

            Stack<String> path = separation.getPath();
            Stack<String> embedded = separation.getEmbeddedPath();
            StringBuilder sb = new StringBuilder("Путь:\n");
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
            sendMessage(sb.toString(), userId);
            setKeyboardsButton("Начать заново", userId, RESTART);
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
                        if (msg.getText().equals("Случайная статья")) {
                            String article = Utilities.getRandomArticle();
                            vk.messages().send(actor).message("Случайная статья: " + article)
                                    .userId(msg.getFromId()).randomId(random.nextInt(10000)).execute();
                            return article;
                        } else {
                            String article = msg.getText();
                            vk.messages().send(actor).message("Ваша статья: " + article)
                                    .userId(msg.getFromId()).randomId(random.nextInt(10000)).execute();
                            return article;
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
