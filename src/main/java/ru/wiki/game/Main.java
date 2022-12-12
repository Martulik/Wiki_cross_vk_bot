package ru.wiki.game;

import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ApiMessagesDenySendException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration.ConfigurationException;
import ru.wiki.game.bot.Bot;

@Slf4j
public class Main {

    public static void main(String[] args) {
        start();
    }

    private static void start() {
        Bot bot;
        try {
            bot = new Bot();
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
        try {
            bot.start();
        } catch (ClientException | ApiException | InterruptedException e) {
            log.info("For some reason, the bot is trying to reply to its own message");
            e.printStackTrace();
            log.info("Restart");
            start();
        }
    }
}
