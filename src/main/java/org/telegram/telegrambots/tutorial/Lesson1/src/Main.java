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
        TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
        botsApplication.registerBot("8265336333:AAHjrFTDfbZRln0b0m1NFKvPGGuimgswXFs", new MyAmazingBot("8265336333:AAHjrFTDfbZRln0b0m1NFKvPGGuimgswXFs"));
        System.out.println("Il bot è attivo!");
    }
}
