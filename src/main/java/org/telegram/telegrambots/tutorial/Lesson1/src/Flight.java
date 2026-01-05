package org.telegram.telegrambots.tutorial.Lesson1.src;

public class Flight {
    public String hex; // Codice ICAO
    public String flight; // Callsign
    public String t; // Modello dell'aereo
    public float lat;
    public float lon;
    public Object alt_baro;
    public float gs; // Velocità rispetto al suolo
    public String emergency; // 0 = nessuna, 7500, 7700 ecc.
}