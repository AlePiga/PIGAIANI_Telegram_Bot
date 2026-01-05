package org.telegram.telegrambots.tutorial.Lesson1.src;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Main {
    public static void main(String[] args) throws TelegramApiException {
        String botToken = "8265336333:AAHjrFTDfbZRln0b0m1NFKvPGGuimgswXFs";
        TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
        botsApplication.registerBot(botToken, new MyAmazingBot(botToken));
        System.out.println("Il bot è attivo!");
    }
}
