package org.telegram.telegrambots.tutorial.Lesson1.src;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws TelegramApiException {
//        String botToken = "";
//        System.out.println("Inserisci il token del bot: ");
//        Scanner sc = new Scanner(System.in);
//        botToken = sc.next(); // Momentaneamente...
//        sc.close();
        final ConfigLoader cl = new ConfigLoader();
        TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
        botsApplication.registerBot(cl.getProperty("BOT_TOKEN"), new MyAmazingBot(cl.getProperty("BOT_TOKEN")));
        System.out.println("Il bot è attivo!");
    }
}
