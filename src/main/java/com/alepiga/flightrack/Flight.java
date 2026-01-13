package com.alepiga.flightrack;

public class Flight {
    public String hex; // Codice ICAO
    public String flight; // Callsign
    public Float lat;
    public Float lon;
    public Object alt_baro;
    public Float gs; // Velocità rispetto al suolo
    public String emergency; // 0 = nessuna, 7500, 7700 ecc.
}