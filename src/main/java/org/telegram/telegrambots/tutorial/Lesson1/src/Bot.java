package org.telegram.telegrambots.tutorial.Lesson1.src;

import com.google.gson.Gson;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Bot implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private final API api = new API();
    private final Gson gson = new Gson();
    private final Map<Long, UserSession> sessions = new ConcurrentHashMap<>();

    public Bot(String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {

            long chatId = update.getMessage().getChatId();
            UserSession user = sessions.computeIfAbsent(chatId, k -> new UserSession(chatId));
            user.firstName = update.getMessage().getFrom().getFirstName();
            user.username = update.getMessage().getFrom().getUserName();
            String messsaggio = update.getMessage().getText().trim();

            StringBuilder sout = new StringBuilder();
            sout.append("\u001B[35m[" + user.username + "]\u001B[0m" + " " + messsaggio);
            System.out.println(sout);

            try {
                if (user.attesa && !messsaggio.startsWith("/")) {

                    switch (user.tipoAttesa) {
                        case "INFO":
                            String callsignInput = messsaggio.toUpperCase().trim();
                            if (!callsignInput.matches("^[A-Z0-9]{3,8}$")) {
                                sendText(chatId, "❌ Codice volo non valido!");
                                return;
                            }
                            user.attesa = false;
                            sendText(chatId, api.cercaDettagliVolo(callsignInput));
                            break;

                        case "LUOGO":
                            float[] coords = api.ottieniCoordinate(messsaggio);
                            if (coords != null) {
                                user.attesa = false; // Luogo trovato, resetto l'attesa
                                user.luogo = messsaggio;
                                user.latitudine = coords[0];
                                user.longitudine = coords[1];
                                sendText(chatId, "✅ Luogo aggiornato: [" + Format.primaLetteraMaiuscola(user.luogo) + "](https://www.google.com/maps?q=" + coords[0] + "," + coords[1] + ")\n\n");
                            } else {
                                sendText(chatId, "❌ Non ho trovato questo posto. Riprova usando un altro nome!");
                            }
                            break;

                        case "RAGGIO":
                            try {
                                int r = Integer.parseInt(messsaggio);
                                if (r <= 0) {
                                    sendText(chatId, "⚠️ Inserisci un numero positivo!");
                                    return;
                                } else if (r > 150) {
                                    sendText(chatId, "⚠️ Raggio troppo grande!");
                                    return;
                                }
//SLEEEEEEEEEEEEEEEEEPPPPPPPPPPPPYYYYYYYYYYYYYYYYHHHHHHHHHHHHHHHHEEEEEEEEEEEEEEAAAAAAAAAAAAAAAAAADDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD
                                user.attesa = false; // Raggio valido, resetto l'attesa
                                user.raggio = r;
                                sendText(chatId, "✅ Raggio aggiornato a " + r + " km.");
                            } catch (NumberFormatException e) {
                                sendText(chatId, "❌ Inserisci un numero valido!");
                            }
                            break;
                    }
                    return;
                }

                if (messsaggio.equals("/start")) {
                    sendText(chatId, "👋 Ehilà "+ user.firstName + ", benvenuto su Flightrack! Per iniziare, scrivi il nome della città o del luogo che vuoi monitorare.");
                    return;
                }

                if (user.luogo.isEmpty()) {
                    float[] coords = api.ottieniCoordinate(messsaggio);
                    if (coords != null) {
                        if(messsaggio.startsWith("/")) {
                            sendText(chatId, "⚠️ Non hai ancora configurato il bot! Usa il comando /start per iniziare.");
                            return;
                        }
                        user.luogo = messsaggio;
                        user.latitudine = coords[0];
                        user.longitudine = coords[1];

                        try {
                            SendMessage message = SendMessage.builder()
                                    .chatId(chatId)
                                    .text("📍 Perfetto! Ho impostato [" + Format.primaLetteraMaiuscola(user.luogo) + "](https://www.google.com/maps?q=" + user.latitudine + "," + user.longitudine + ") come luogo da monitorare.\n\n"
                                            + "📏 Ora dimmi, quanti chilometri di raggio vuoi coprire?\n"
                                            + "(Scrivi solo il numero, es: 50)")
                                    .parseMode("Markdown")
                                    .disableWebPagePreview(true)
                                    .build();

                            telegramClient.execute(message);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    } else {
                        sendText(chatId, "❌ Non ho trovato questo posto. Riprova usando un altro nome!");
                    }
                    return;
                }

                if (user.raggio <= 0) {
                    try {
                        int raggioUtente = Integer.parseInt(messsaggio);
                        if (raggioUtente < 0) {
                            sendText(chatId, "❌ Inserisci un raggio valido!");
                            return;
                        }
                        else if (raggioUtente > 150){
                            sendText(chatId, "❌ Raggio troppo grande! Riprova con uno più piccolo.");
                            return;
                        }
                        user.raggio = raggioUtente;
                        sendText(chatId, "*CONFIGURAZIONE COMPLETATA*\n" + "🗺️ Luogo: " + Format.primaLetteraMaiuscola(user.luogo) + "\n" + "📏 Raggio: " + user.raggio + " km\n\n" + "Usa il comando /voli per tracciare gli aerei nella zona che hai selezionato! Per cambiare i parametri impostati puoi usare i comandi /luogo e /raggio, oppure /start per eseguire la configurazione da capo. Buon divertimento!");
                        return;
                    } catch (NumberFormatException e) {
                        sendText(chatId, "❌ Inserisci un numero valido per il raggio!");
                        return;
                    }
                }

                if (messsaggio.startsWith("/voli")) {
                    if (user.luogo.isEmpty() || user.raggio == 0) {
                        sendText(chatId, "⚠️ Non hai ancora configurato il bot! Digita /start per iniziare.");
                        return;
                    }

                    String adsbUrl = "https://api.adsb.lol/v2/lat/" + user.latitudine + "/lon/" + user.longitudine + "/dist/" + user.raggio;
                    String responseBody = api.chiamataAPI(adsbUrl);
                    Flightlist data = gson.fromJson(responseBody, Flightlist.class);

                    if (data.ac == null || data.ac.isEmpty()) {
                        sendText(chatId, "❌ Nessun volo trovato nel raggio di " + user.raggio + "km da " + Format.primaLetteraMaiuscola(user.luogo));
                        return;
                    }

                    StringBuilder sb = new StringBuilder("*✈️ VOLI INTORNO A " + user.luogo.toUpperCase() + "*\n");

                    for (Flight f : data.ac) {
                        String callsign = (f.flight != null) ? f.flight.trim() : "N/A";
                        String route = api.ottieniVoli(callsign);
                        sb.append("• `").append(callsign).append("`: ").append(route).append("\n");
                    }
                    sendText(chatId, sb.toString());
                    return;
                }

                if (messsaggio.startsWith("/info")) {
                    String[] parti = messsaggio.split("\\s+");
                    if (parti.length == 1) {
                        user.attesa = true;
                        user.tipoAttesa = "INFO";
                        sendText(chatId, "✈️ Di quale volo vuoi conoscere i dettagli? Inviami il callsign!");
                    } else {
                        String callsign = parti[1].toUpperCase();
                        sendText(chatId, api.cercaDettagliVolo(callsign));
                    }
                    return;
                }

                if (messsaggio.startsWith("/luogo")) {
                    String[] parti = messsaggio.split("\\s+", 2);
                    if (parti.length == 1) {
                        user.attesa = true;
                        user.tipoAttesa = "LUOGO";
                        sendText(chatId, "🗺️ Quale luogo vorresti monitorare?");
                        return;
                    }

                    String nuovoLuogo = parti[1].trim();
                    float[] coords = api.ottieniCoordinate(nuovoLuogo);
                    if (coords != null) {
                        user.luogo = nuovoLuogo;
                        user.latitudine = coords[0];
                        user.longitudine = coords[1];
                        try {
                            SendMessage message = SendMessage.builder()
                                    .chatId(chatId)
                                    .text("✅ Luogo aggiornato: [" + Format.primaLetteraMaiuscola(user.luogo) + "](https://www.google.com/maps?q=" + coords[0] + "," + coords[1] + ")\n\n")
                                    .parseMode("Markdown")
                                    .disableWebPagePreview(true)
                                    .build();

                            telegramClient.execute(message);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    } else {
                        sendText(chatId, "❌ Non ho trovato questo posto. Riprova usando un altro nome!");
                    }
                    return;
                }

                if (messsaggio.startsWith("/raggio")) {
                    String[] parti = messsaggio.split("\\s+", 2);
                    if (parti.length == 1) {
                        user.attesa = true;
                        user.tipoAttesa = "RAGGIO";
                        sendText(chatId, "📏 Che raggio di copertura vorresti usare?");
                        return;
                    }
                    try {
                        int nuovoRaggio = Integer.parseInt(parti[1].trim());
                        if (nuovoRaggio <= 0) {
                            sendText(chatId, "⚠️ Inserisci un numero positivo!");
                            return;
                        }
                        else if (nuovoRaggio > 150) {
                            sendText(chatId, "⚠️ Raggio troppo grande!");
                            return;
                        }
                        user.raggio = nuovoRaggio;
                        sendText(chatId, "✅ Raggio aggiornato a " + user.raggio + " km.");
                    } catch (NumberFormatException e) {
                        sendText(chatId, "❌ Inserisci un numero valido!");
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendText(chatId, "⚠️ Si è verificato un errore durante il recupero dei dati.");
            }
        }
    }

    private void sendText(long chatId, String text) {
        try {
            SendMessage message = SendMessage.builder().chatId(chatId).text(text).parseMode("Markdown").build();
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}