package com.alepiga.flightrack;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Database {
    private static final String URL = "jdbc:sqlite:flightrack.db";

    public Database() {
        try (Connection conn = DriverManager.getConnection(URL)) {
            System.out.println("\u001B[1;32mConnesso al database!\u001B[0m");
            if (conn != null) {
                Statement stmt = conn.createStatement();

                stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                        "chatId INTEGER PRIMARY KEY, " +
                        "firstName TEXT, " +
                        "username TEXT, " +
                        "luogo TEXT, " +
                        "latitudine REAL, " +
                        "longitudine REAL, " +
                        "raggio INTEGER);");

                stmt.execute("CREATE TABLE IF NOT EXISTS flights (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "chatId INTEGER, " +
                        "callsign TEXT, " +
                        "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "FOREIGN KEY(chatId) REFERENCES users(chatId));");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<Long, UserSession> loadSessions() {
        Map<Long, UserSession> sessions = new ConcurrentHashMap<>();
        String sql = "SELECT * FROM users";

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                long chatId = rs.getLong("chatId");
                UserSession user = new UserSession(chatId);
                user.firstName = rs.getString("firstName");
                user.username = rs.getString("username");
                user.luogo = rs.getString("luogo");
                user.latitudine = rs.getFloat("latitudine");
                user.longitudine = rs.getFloat("longitudine");
                user.raggio = rs.getInt("raggio");

                sessions.put(chatId, user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sessions;
    }

    public void saveUpdateUser(UserSession user) {
        String sql = "INSERT INTO users(chatId, firstName, username, luogo, latitudine, longitudine, raggio) " + "VALUES(?,?,?,?,?,?,?) " +
                "ON CONFLICT(chatId) DO UPDATE SET " +
                "firstName=excluded.firstName, " +
                "username=excluded.username, " +
                "luogo=excluded.luogo, " +
                "latitudine=excluded.latitudine, " +
                "longitudine=excluded.longitudine, " +
                "raggio=excluded.raggio";

        try {
            Connection conn = DriverManager.getConnection(URL);
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, user.chatId);
            pstmt.setString(2, user.firstName);
            pstmt.setString(3, user.username);
            pstmt.setString(4, user.luogo);
            pstmt.setFloat(5, user.latitudine);
            pstmt.setFloat(6, user.longitudine);
            pstmt.setInt(7, user.raggio);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void aggiungiVolo(long chatId, String callsign) {
        String sql = "INSERT INTO flights(chatId, callsign) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, chatId);
            pstmt.setString(2, callsign.toUpperCase());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}