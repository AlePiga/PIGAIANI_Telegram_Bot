# ✈️ FlightTrack

FlightTrack è un bot Telegram che ti permette di vedere quali aerei stanno volando sopra una determinata zona geografica.

Basta indicare:

* 📍 **Un luogo** (città, indirizzo o punto di riferimento)
* 📏 **Un raggio di copertura** (in km)

E il bot farà il resto: recupera i voli in tempo reale e ti mostra le informazioni principali su ogni aeromobile, usando il suo **callsign**. Il tutto è reso possibile grazie a diverse **API esterne**, alla **geocodifica** e a un **database SQLite** che salva le impostazioni dell’utente.

---

## ⭐ Cosa può fare il bot?

* 🌍 **Geocodifica automatica**
  Inserisci il nome di una città o di un punto di riferimento e il bot lo trasforma automaticamente in coordinate geografiche.

* 🛩️ **Monitoraggio dei voli**
  Mostra tutti gli aerei che passano entro un raggio massimo di 150 km.

* 🔍 **Dettagli dei voli**
  Puoi ottenere informazioni come rotta, modello dell’aereo e altitudine.

* 💾 **Salvataggio dei dati**
  Il bot ricorda le tue impostazioni e mantiene uno storico delle ricerche grazie a SQLite.

---

## 🛠️ API Utilizzate

Per funzionare, FlightTrack Bot utilizza alcune API esterne:

* **ADSB.lol**
  Per ottenere i dati di volo in tempo reale (posizione e callsign).

* **ADSBDB**
  Per recuperare informazioni aggiuntive su rotte e modelli degli aerei.

* **OpenStreetMap**
  Per convertire i nomi dei luoghi in coordinate geografiche.

---

## 🚀 Come avviare il progetto

### Requisiti

Assicurati di avere:

* Java 11 o superiore
* Maven

---

### 1️⃣ Dipendenze Maven

Aggiungi queste dipendenze al tuo `pom.xml`:

```xml
        <dependency>
            <groupId>org.telegram</groupId>
            <artifactId>telegrambots-longpolling</artifactId>
            <version>9.2.0</version>
        </dependency>
        <dependency>
            <groupId>org.telegram</groupId>
            <artifactId>telegrambots-client</artifactId>
            <version>9.2.0</version>
        </dependency>
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.13.2</version>
        </dependency>
        <dependency>
            <groupId>com.squareup.okhttp3</groupId>
            <artifactId>okhttp</artifactId>
            <version>4.12.0</version>
        </dependency>
        <dependency>
            <groupId>org.xerial</groupId>
            <artifactId>sqlite-jdbc</artifactId>
            <version>3.51.1.0</version>
            <scope>compile</scope>
        </dependency>
```

---

### 2️⃣ Configurare il bot Telegram

1. Apri Telegram e avvia una chat con **@BotFather**
2. Crea un nuovo bot
3. Copia il **Token API**
4. Incollalo all'interno del file config.properties

---

### 3️⃣ Database

Il database SQLite viene creato automaticamente al primo avvio del bot.

📁 File generato:

```
flighttrack.db
```

---

## 🎮 Come si usa il bot

### Comandi disponibili

| Comando            | Cosa fa                                  |
| ------------------ | ---------------------------------------- |
| `/start`           | Configurazione iniziale (luogo e raggio) |
| `/voli`            | Mostra gli aerei sopra la tua zona       |
| `/info`            | Dettagli di un volo specifico            |
| `/luogo`           | Cambia la località                       |
| `/raggio`          | Cambia il raggio di copertura            |

---

### Esempio di utilizzo

```
Utente: /info
Bot: ✈️ Di quale volo vuoi conoscere i dettagli? Inviami il callsign!

Utente: NOS6508
Bot: Rotta: Verona (VRN) → Sharm El Sheikh (SSH)
     Modello: Boeing 737
     Altitudine: 30.000 ft
     ...
```

---

## 🗄️ Database

Il progetto usa due tabelle principali:

```sql
-- Utenti
CREATE TABLE users (
    chatId INTEGER PRIMARY KEY,
    firstName TEXT,
    username TEXT,
    luogo TEXT,
    latitudine REAL,
    longitudine REAL,
    raggio INTEGER
);

-- Storico voli
CREATE TABLE flights (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    chatId INTEGER,
    callsign TEXT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(chatId) REFERENCES users(chatId)
);
```
