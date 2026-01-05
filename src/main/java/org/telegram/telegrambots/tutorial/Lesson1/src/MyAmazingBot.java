package org.telegram.telegrambots.tutorial.Lesson1.src;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MyAmazingBot implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public String luogo = "";
    public int raggio = 0;

    public float latitudine = 0;
    public float longitudine = 0;

    public MyAmazingBot(String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText().trim();
            String nomeUtente = update.getMessage().getFrom().getFirstName();

            try {
                if (text.equals("/start")) {
                    sendText(chatId, "👋 Ehilà "+ nomeUtente + ", benvenuto su Flightrack! Per iniziare, scrivi il nome della città o del luogo che vuoi monitorare.");
                    return;
                }

                if (luogo.isEmpty()) {
                    float[] coords = coordinate(text);
                    if (coords != null) {
                        if(text.equals("/voli")) {
                            sendText(chatId, "⚠️ Non hai ancora configurato il bot! Digita /start per iniziare.");
                            return;
                        }
                        luogo = text;
                        sendText(chatId, "📍 Perfetto! Ho impostato " + primaLetteraMaiuscola(luogo) + " (" + coords[0] + ", " + coords[1] + ") come luogo da monitorare.\n\nOra dimmi, quanti chilometri di raggio vuoi coprire? (Scrivi solo il numero, es: 50)");
                    } else {
                        sendText(chatId, "❌ Non ho trovato questo posto. Riprova usando un altro nome!");
                    }
                    return;
                }

                if (raggio <= 0) {
                    int raggioUtente = Integer.parseInt(text);
                    if (raggioUtente < 0) {
                        sendText(chatId, "❌ Inserisci un raggio valido!");
                        return;
                    }
                    raggio = raggioUtente;
                    sendText(chatId, "✅ Configurazione completata!\n\n" + "Luogo: " + primaLetteraMaiuscola(luogo) + "\n" + "Raggio: " + raggio + " km\n\n" + "Ora puoi usare il comando /voli per tracciare gli aerei che passano sopra di te! Per cambiare i parametri impostati puoi usare /start per eseguire la configurazione da capo, oppure /luogo <luogo> e /raggio <raggio>. Buon divertimento!");
                    return;
                }

                if (text.startsWith("/voli")) {
                    if (luogo.isEmpty() || raggio == 0) {
                        sendText(chatId, "⚠️ Non hai ancora configurato il bot! Digita /start per iniziare.");
                        return;
                    }

                    String adsbUrl = "https://api.adsb.lol/v2/lat/" + latitudine + "/lon/" + longitudine + "/dist/" + raggio;
                    String responseBody = chiamataAPI(adsbUrl);
                    Flightlist data = gson.fromJson(responseBody, Flightlist.class);

                    if (data.ac == null || data.ac.isEmpty()) {
                        sendText(chatId, "Nessun volo trovato nel raggio di " + raggio + "km da " + primaLetteraMaiuscola(luogo));
                        return;
                    }

                    StringBuilder sb = new StringBuilder("*✈️ VOLI INTORNO A " + luogo.toUpperCase() + "*\n");

                    for (Flight f : data.ac) {
                        String callsign = (f.flight != null) ? f.flight.trim() : "N/A";
                        String route = getFlightRouteSummary(callsign);
                        sb.append("•`").append(callsign).append("`: ").append(route).append("\n");
                    }
                    sendText(chatId, sb.toString());
                }

                if (text.startsWith("/info")) {
                    String[] parti = text.split("\\s+"); // Stringa divisa per spazi

                    if (parti.length == 1) {
                        sendText(chatId, "✈️ Di quale volo vuoi conoscere i dettagli? Scrivi il comando nel formato /info <volo> (es: /info NOS6508)");
                    } else {
                        String callsign = parti[1].toUpperCase();
                        cercaDettagliVolo(chatId, callsign);
                    }
                }

                if (text.startsWith("/luogo")) {
                    String[] parti = text.split("\\s+", 2);

                    if (parti.length == 1) {
                        sendText(chatId, "🗺️ Quale luogo vorresti monitorare? Scrivi il comando nel formato /luogo <luogo> (es: /luogo Roma)");
                        return;
                    }

                    String nuovoLuogo = parti[1].trim();
                    float[] coords = coordinate(nuovoLuogo);

                    if (coords != null) {
                        luogo = nuovoLuogo;
                        latitudine = coords[0];
                        longitudine = coords[1];
                        sendText(chatId, "✅ Luogo aggiornato: " + primaLetteraMaiuscola(luogo) + " (" + coords[0] + ", " + coords[1] + ")");
                    } else {
                        sendText(chatId, "❌ Non ho trovato questo posto. Riprova usando un altro nome!");
                    }
                    return;
                }

                if (text.startsWith("/raggio")) {
                    String[] parti = text.split("\\s+", 2);

                    if (parti.length == 1) {
                        sendText(chatId, "📏 Che raggio di copertura vorresti usare? Scrivi il comando nel formato /raggio <raggio> (es: /raggio 70)");
                        return;
                    }

                    try {
                        int nuovoRaggio = Integer.parseInt(parti[1].trim());

                        if (nuovoRaggio <= 0) {
                            sendText(chatId, "⚠️ Inserisci un numero positivo.");
                            return;
                        }

                        raggio = nuovoRaggio;
                        sendText(chatId, "✅ Raggio aggiornato a " + raggio + " km.");
                    } catch (NumberFormatException e) {
                        sendText(chatId, "⚠️ Il raggio deve essere un numero (es: 50).");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendText(chatId, "⚠️ Si è verificato un errore durante il recupero dei dati.");
            }
        }
    }

    private void cercaDettagliVolo(long chatId, String callsignInput) {
        callsignInput = callsignInput.trim().toUpperCase();

        if (!callsignInput.matches("^[A-Z0-9]{3,8}$")) {
            sendText(chatId, "⚠️ Callsign non valido!");
            return;
        }

        try {
            String radarUrl = "https://api.adsb.lol/v2/callsign/" + callsignInput;
            String rispostaRadar = chiamataAPI(radarUrl);
            JsonObject radarJson = gson.fromJson(rispostaRadar, JsonObject.class);

            String hex = null;
            JsonObject liveData = null;

            // Estraiamo il primo aereo dall'array "ac"
//            if (radarJson.has("ac") && radarJson.get("ac").isJsonArray()) {
                JsonArray acArray = radarJson.getAsJsonArray("ac");
                if (!acArray.isEmpty()) {
                    liveData = acArray.get(0).getAsJsonObject();
                    hex = liveData.get("hex").getAsString();
                }
//            }

            // --- 2) Recupero dati DATABASE (Fallback o arricchimento) ---
            String urlDb = (hex != null)
                    ? "https://api.adsbdb.com/v0/aircraft/" + hex + "?callsign=" + callsignInput
                    : "https://api.adsbdb.com/v0/callsign/" + callsignInput;

            JsonObject rootDb = gson.fromJson(chiamataAPI(urlDb), JsonObject.class);

            StringBuilder sb = new StringBuilder();
            String photoUrl = null;

            // ===========================
            //   SEZIONE AEROMOBILE (da ADSBDB)
            // ===========================
            if (rootDb.has("response") && !rootDb.get("response").isJsonNull()) {
                JsonObject resp = rootDb.getAsJsonObject("response");

                if (resp.has("aircraft") && !resp.get("aircraft").isJsonNull()) {
                    JsonObject ac = resp.getAsJsonObject("aircraft");
                    sb.append("🛩 *DETTAGLI AEROMOBILE*\n");
                    sb.append("• Modello: ").append(ac.get("manufacturer").getAsString()).append(" ")
                            .append(ac.get("type").getAsString()).append(" (")
                            .append(ac.get("icao_type").getAsString()).append(")\n");

                    sb.append("• Registrazione: `").append(ac.get("registration").getAsString()).append("` ")
                            .append(emoji(ac.get("registered_owner_country_iso_name").getAsString())).append("\n");

                    sb.append("• Proprietario: ").append(ac.get("registered_owner").getAsString()).append("\n");
                    sb.append("• Mode-S (HEX): `").append(ac.get("mode_s").getAsString()).append("`\n\n");

                    if (ac.has("url_photo") && !ac.get("url_photo").isJsonNull()) {
                        photoUrl = ac.get("url_photo").getAsString();
                    }
                }

                if (resp.has("flightroute") && !resp.get("flightroute").isJsonNull()) {
                    JsonObject fr = resp.getAsJsonObject("flightroute");
                    sb.append("ℹ️ *INFORMAZIONI VOLO*\n");

                    if (fr.has("airline") && !fr.get("airline").isJsonNull()) {
                        sb.append("• Compagnia: ").append(fr.getAsJsonObject("airline").get("name").getAsString()).append("\n");
                    }
                    if (fr.has("callsign_iata") && !fr.get("callsign_iata").isJsonNull()) {
                        sb.append("• Callsign IATA: ").append(fr.get("callsign_iata").getAsString()).append("\n");
                    }
                    if (fr.has("origin") && !fr.get("origin").isJsonNull()) {
                        JsonObject o = fr.getAsJsonObject("origin");
                        sb.append("• Partenza: ").append(o.get("municipality").getAsString())
                                .append(" (").append(o.get("iata_code").getAsString()).append(") ")
                                .append(o.get("name").getAsString()).append("\n");
                    }
                    if (fr.has("destination") && !fr.get("destination").isJsonNull()) {
                        JsonObject d = fr.getAsJsonObject("destination");
                        sb.append("• Arrivo: ").append(d.get("municipality").getAsString())
                                .append(" (").append(d.get("iata_code").getAsString()).append(") ")
                                .append(d.get("name").getAsString()).append("\n");
                    }
                    sb.append("\n");
                }
            }

            if (liveData != null) {
                sb.append("📡 *ALTRI DATI*\n");
                sb.append("• Modello: ").append(liveData.has("t") ? liveData.get("t").getAsString() : "N/D").append("\n");
                sb.append("• Latitudine: ").append(liveData.get("lat").getAsFloat()).append("\n");
                sb.append("• Longitudine: ").append(liveData.get("lon").getAsFloat()).append("\n");

                // L'altitudine può essere "ground", quindi la gestiamo come stringa
                String alt = liveData.get("alt_baro").isJsonPrimitive() ? liveData.get("alt_baro").getAsString() : "N/D";
                sb.append("• Altitudine: ").append(alt).append(" ft\n");

                sb.append("• Velocità rispetto al suolo: ").append(liveData.has("gs") ? liveData.get("gs").getAsFloat() : 0).append(" kt\n");

                String emergency = liveData.has("emergency") ? liveData.get("emergency").getAsString() : "none";
                sb.append("• Stato emergenza: ").append(emergency.equals("none") ? "Nessuna" : "⚠️ " + emergency).append("\n");
            } else if (sb.length() == 0) {
                sendText(chatId, "❌ Dati non trovati per " + callsignInput);
                return;
            }

            if (photoUrl != null && !photoUrl.isEmpty()) {
                sb.append("\n*🖼 FOTO DELL'AEREO* [\u200E](").append(photoUrl).append(")"); // Link associato ad un carattere vuoto
            }

            sendText(chatId, sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId, "⚠️ Errore durante il recupero dei dettagli.");
        }
    }


    private String chiamataAPI(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    private String getFlightRouteSummary(String callsign) {
        try {
            String url = "https://api.adsbdb.com/v0/callsign/" + URLEncoder.encode(callsign, StandardCharsets.UTF_8);
            JsonObject root = gson.fromJson(chiamataAPI(url), JsonObject.class);
            if (root.has("response") && !root.get("response").isJsonNull()) {
                JsonObject route = root.getAsJsonObject("response").getAsJsonObject("flightroute");
                if (route.has("origin") && !route.get("origin").isJsonNull()) {
                    JsonObject origin = route.getAsJsonObject("origin");
                    JsonObject dest = route.getAsJsonObject("destination");
                    String from = origin.get("iata_code").getAsString() + emoji(origin.get("country_iso_name").getAsString());
                    String to = dest.get("iata_code").getAsString() + emoji(dest.get("country_iso_name").getAsString());
                    return from + " ➜ " + to;
                }
            }
        } catch (Exception e) { return "Rotta non disponibile"; }
        return "Rotta non disponibile";
    }

    private String emoji(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) return "🌐"; // Se non è stato trovato il country code torna un emoji di default
        int firstChar = Character.codePointAt(countryCode.toUpperCase(), 0) - 0x41 + 0x1F1E6;
        int secondChar = Character.codePointAt(countryCode.toUpperCase(), 1) - 0x41 + 0x1F1E6;
        return new String(Character.toChars(firstChar)) + new String(Character.toChars(secondChar));
    }

    private void sendText(long chatId, String text) {
        try {
            SendMessage message = SendMessage.builder().chatId(chatId).text(text).parseMode("Markdown").build();
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private float[] coordinate(String location) {
        try {
            String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?q=" + encodedLocation + "&format=json&limit=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonArray jsonArray = gson.fromJson(response.body(), JsonArray.class);

            if (jsonArray != null && jsonArray.size() > 0) {
                JsonObject firstResult = jsonArray.get(0).getAsJsonObject();
                latitudine = firstResult.get("lat").getAsFloat();
                longitudine = firstResult.get("lon").getAsFloat();

                return new float[]{latitudine, longitudine};
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String primaLetteraMaiuscola(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}