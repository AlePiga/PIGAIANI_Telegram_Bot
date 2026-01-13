package com.alepiga.flightrack;

public class UserSession {
    public long chatId;
    public String username;
    public String firstName;
    public String luogo;
    public int raggio;
    public float latitudine;
    public float longitudine;
    public boolean attesa;
    public String tipoAttesa;

    public UserSession(){
        luogo = "";
        raggio = 0;
        latitudine = 0;
        longitudine = 0;
    }

    public UserSession(long chatId){
        this.chatId = chatId;
        this.luogo = "";
        this.raggio = 0;
        this.latitudine = 0;
        this.longitudine = 0;
    }

    public UserSession(long chatId, int raggio, float latitudine, float longitudine, String luogo){
        this.chatId = chatId;
        this.luogo = luogo;
        this.raggio = raggio;
        this.latitudine = latitudine;
        this.longitudine = longitudine;
    }

}