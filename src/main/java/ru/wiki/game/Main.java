package ru.wiki.game;

import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import org.apache.commons.configuration.ConfigurationException;
import ru.wiki.game.bot.Bot;


public class Main {

    public static void main(String[] args)  {
        Bot bot;
        try {
            bot = new Bot();
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
        try {
            bot.start();
        } catch (ClientException | ApiException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
