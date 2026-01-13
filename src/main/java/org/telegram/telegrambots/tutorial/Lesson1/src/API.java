package org.telegram.telegrambots.tutorial.Lesson1.src;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class API {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public String chiamataAPI(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "FlightTrackBot/1.0") // Buona norma per Nominatim
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    public float[] ottieniCoordinate(String location) {
        try {
            String encoded = URLEncoder.encode(location, StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?q=" + encoded + "&format=json&limit=1";
            JsonArray jsonArray = gson.fromJson(chiamataAPI(url), JsonArray.class);
            if (!jsonArray.isEmpty()) {
                JsonObject obj = jsonArray.get(0).getAsJsonObject();
                return new float[]{obj.get("lat").getAsFloat(), obj.get("lon").getAsFloat()};
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    String cercaDettagliVolo(String callsignInput) {
        callsignInput = callsignInput.trim().toUpperCase();

        if (!callsignInput.matches("^[A-Z0-9]{3,8}$")) {
            return  "⚠️ Callsign non valido!";
        }

        try {
            String radarUrl = "https://api.adsb.lol/v2/callsign/" + callsignInput;
            String rispostaRadar = chiamataAPI(radarUrl);
            Flightlist radarJson = gson.fromJson(rispostaRadar, Flightlist.class);

            String hex = null;
            Flight liveData = null;

            if (radarJson != null && radarJson.ac != null && !radarJson.ac.isEmpty()) {
                liveData = radarJson.ac.get(0);
                hex = liveData.hex;
            }

            String urlDb = (hex != null)
                    ? "https://api.adsbdb.com/v0/aircraft/" + hex + "?callsign=" + callsignInput
                    : "https://api.adsbdb.com/v0/callsign/" + callsignInput;

            String rispostaDb = chiamataAPI(urlDb);
            JsonObject rootDb = gson.fromJson(rispostaDb, JsonObject.class); // Risposta dell'API in json invece che usare classe

            StringBuilder mess = new StringBuilder();
            String photoUrl = "";

            if (valido(rootDb, "response")) {
                JsonObject resp = rootDb.getAsJsonObject("response");

                if (resp.has("aircraft") && !resp.get("aircraft").isJsonNull()) {
                    JsonObject ac = resp.getAsJsonObject("aircraft");
                    mess.append("🛩 *DETTAGLI AEROMOBILE*\n");

                    String man = ac.has("manufacturer") ? ac.get("manufacturer").getAsString() : "N/D";
                    String type = ac.has("type") ? ac.get("type").getAsString() : "";
                    mess.append("• Modello: ").append(man).append(" ").append(type).append("\n");

                    mess.append("• Registrazione: `").append(ac.get("registration").getAsString()).append("`\n");
                    mess.append("• Codice HEX: `").append(ac.get("mode_s").getAsString()).append("`\n\n");

                    if (ac.has("url_photo") && !ac.get("url_photo").isJsonNull()) {
                        photoUrl = ac.get("url_photo").getAsString();
                    }
                }

                if (resp.has("flightroute") && !resp.get("flightroute").isJsonNull()) {
                    JsonObject fr = resp.getAsJsonObject("flightroute");
                    mess.append("ℹ️ *INFORMAZIONI VOLO*\n");

                    if (fr.has("origin") && !fr.get("origin").isJsonNull()) {
                        JsonObject o = fr.getAsJsonObject("origin");
                        mess.append("• Partenza: ").append(o.get("municipality").getAsString()).append(" (").append(o.get("iata_code").getAsString()).append(") ").append(o.get("name").getAsString()).append("\n");
                    }
                    if (fr.has("destination") && !fr.get("destination").isJsonNull()) {
                        JsonObject d = fr.getAsJsonObject("destination");
                        mess.append("• Arrivo: ").append(d.get("municipality").getAsString()).append(" (").append(d.get("iata_code").getAsString()).append(") ").append(d.get("name").getAsString()).append("\n");
                    }
                    mess.append("\n");
                }
            }

            if (liveData != null) {
                mess.append("📡 *DATI RADAR LIVE*\n");
                mess.append("• Altitudine: ").append(liveData.alt_baro != null ? liveData.alt_baro : "N/D").append(" ft\n");
                mess.append("• Velocità: ").append(liveData.gs != null ? liveData.gs : 0).append(" kt\n");
                mess.append("• Latitudine: ").append(liveData.lat != null ? liveData.lat : "N/D").append("\n");
                mess.append("• Longitudine: ").append(liveData.lon != null ? liveData.lon : "N/D").append("\n");
                String emergency = (liveData.emergency != null) ? liveData.emergency : "none";
                if (!emergency.equals("none")) {
                    mess.append("• STATO EMERGENZA: ").append(emergency).append("\n");
                }
            }

            if (mess.isEmpty()) {
                return "❌ Nessun dato trovato per il callsign " + callsignInput;
            } else {
                if (photoUrl != null && photoUrl.isEmpty()) {
                    mess.append("\n📸 *FOTO DELL'AEROMOBILE*\n");
                    mess.insert(0, "[\u200E](" + photoUrl + ")"); // Associo il link ad un carattere vuoto
                }
                return mess.toString();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ Errore durante la ricerca!";
        }
    }

    String ottieniVoli(String callsign) {
        try {
            String url = "https://api.adsbdb.com/v0/callsign/" + URLEncoder.encode(callsign, StandardCharsets.UTF_8);
            JsonObject root = gson.fromJson(chiamataAPI(url), JsonObject.class);
            if (root.has("response") && !root.get("response").isJsonNull()) {
                JsonObject route = root.getAsJsonObject("response").getAsJsonObject("flightroute");
                if (route.has("origin") && !route.get("origin").isJsonNull()) {
                    JsonObject origin = route.getAsJsonObject("origin");
                    JsonObject dest = route.getAsJsonObject("destination");
                    String from = origin.get("iata_code").getAsString() + Format.getEmoji(origin.get("country_iso_name").getAsString());
                    String to = dest.get("iata_code").getAsString() + Format.getEmoji(dest.get("country_iso_name").getAsString());
                    return from + " ➜ " + to;
                }
            }
        } catch (Exception e) { return "Rotta non disponibile"; }
        return "Rotta non disponibile";
    }

    private boolean valido(JsonObject obj, String property) {
        return obj != null && obj.has(property) && !obj.get(property).isJsonNull();
    }
}