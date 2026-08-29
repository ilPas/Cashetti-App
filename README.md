# 🗄️ Cashetti App

**Cashetti App** è un'applicazione Android nativa sviluppata in **Kotlin** e **Jetpack Compose**, progettata per offrire un controllo granulare, reattivo e trasparente delle finanze personali e familiari. L'app supera la rigidità dei budget tradizionali implementando un'architettura modulare a **Cassetti**, gestione del **Rollover dinamico**, isolamento dei **Costi Fissi strutturali** e categorizzazione assistita dall'**Intelligenza Artificiale (Gemini AI)**.

---

## 🎯 Architettura & Filosofia Finanziaria

L'applicazione si basa su pilastri contabili distinti per evitare l'attrito cognitivo e mantenere separate le spese di routine dai costi strutturali:

### 1. Sistema a Cassetti (Budget Mensile Spendibile)
* **👤 Cassetto Personale**: Budget discrezionale mensile (es. 700 €) dedicato allo svago, uscite e spese individuali. Si resetta ad ogni ciclo mensile e assorbe gli abbonamenti attivi.
* **🏠 Cassetto Casa / Ginevra (con Rollover)**: Fondo dedicato a imprevisti domestici e spese condivise (es. 180 €). Integra un algoritmo di **Rollover dinamico cumulativo**: i fondi non spesi nei cicli precedenti si accumulano automaticamente come disponibilità extra, mentre gli sforamenti passati vengono recuperati.
* **✨ Saldo Hero (Home)**: Calcola in tempo reale la somma spendibile disponibile tra i due cassetti.
* **📥 Gestione Entrate**: Motore finanziario bidirezionale per registrare entrate extra, aumentando dinamicamente il saldo residuo di un cassetto nel ciclo in corso.

### 2. Costi Fissi (Baseline Permanente)
* Pannello dedicato alle spese strutturali ricorrenti (affitto, bollette, assicurazioni, rate).
* **Memoria Perenne (No Reset)**: I costi fissi rimangono memorizzati nel tempo come baseline di spesa fissa mensile, senza essere azzerati dal ciclo mensile e senza inquinare lo storico delle spese vive quotidiane.

### 3. Ciclo di Fatturazione Personalizzabile
* Possibilità di definire il giorno del mese in cui inizia il ciclo contabile (es. il 27 del mese, in coincidenza con l'accredito dello stipendio).

### 4. Fondo Risparmi & Eventi
* Gestione di salvadanai specifici con depositi, prelievi e tracciamento dell'avanzamento verso obiettivi di spesa futuri o vacanze.

### 5. Controllo "Grief Spending" & Spese Eccezionali
* Tracciamento immediato degli acquisti d'impulso o emotivi (*Grief Spending*) rispetto alle spese strettamente necessarie.
* Possibilità di escludere spese straordinarie una-tantum dalle medie statistiche.

---

## 🚀 Funzionalità Principali

* **⚡ Intercettazione Notifiche di Pagamento**: Servizio in background (`PaymentNotificationListenerService`) per catturare automaticamente le notifiche inviate da app bancarie e fintech (PayPal, Revolut, Intesa, Sella, N26, Hype, Scalapay, Apple/Google Pay) ed estrarre importo e merchant.
* **🤖 Integrazione Gemini AI**: Assistente intelligente integrato (`GeminiApiService`) per categorizzare automaticamente le transazioni e analizzare note o scontrini.
* **🔄 Gestione Rimborsi**: Flusso nativo per segnalare spese con rimborsi attesi e processarli (totalmente o parzialmente) storicamente e finanziariamente.
* **💾 Backup & Ripristino Dati**:
  * **Backup Locale JSON (Offline/Sicuro)**: Esportazione e importazione istantanea con un click dell'intero database in formato `.json`.
  * **Google Drive Sync**: Integrazione cloud per salvataggio automatico e ripristino multi-dispositivo.
* **📊 Report & Statistiche Avanzate (`StatisticsScreen`)**:
  * **Matrice a Pallini Giornaliera (Tendency Dot Matrix)**: Rappresentazione visiva day-by-day della spesa nel ciclo con altezza proporzionale, colorazione in base alla categoria prevalente e ispezione interattiva del singolo giorno.
  * **Grafico a Ciambella a Pillole Arrotondate (Spending Donut)**: Ripartizione visiva moderna delle categorie con totale centrale e percentuali.
  * **Curva di Burn-Rate & Andamento Cumulativo (Line Chart)**: Tracciamento dell'andamento reale rispetto alla retta ideale di consumo del budget.
  * **Metriche & KPI Intelligenti**: Giorno più costoso (*Peak Day*), media giornaliera effettiva, contatore giorni *No-Spend*, proiezione di chiusura ciclo (*Surplus/Deficit*) e rapporto qualità della spesa (*Grief/Impulso vs Necessità*).
* **📱 Widget Schermata Home**: Widget nativo per visualizzare a colpo d'occhio il budget spendibile residuo senza aprire l'applicazione.

---

## 🛠️ Stack Tecnologico

* **Linguaggio**: [Kotlin](https://kotlinlang.org/) (100%)
* **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) con **Material Design 3 (M3)**
* **Architettura**: MVVM (Model-View-ViewModel) + Unidirectional Data Flow (StateFlow / Flow)
* **Database Locale**: [Room Database](https://developer.android.com/training/data-storage/room) (SQLite) con migrazioni gestite
* **Networking & AI**: Retrofit / OkHttp & Gemini REST API
* **Asincronia**: Kotlin Coroutines & Reactive Flow Pipelines
* **Requisiti di Sistema**:
  * **Min SDK**: `24` (Android 7.0 Nougat)
  * **Target / Compile SDK**: `36` (Android 16)
  * **Java Version**: 11

---

## 📂 Struttura del Progetto

```
app/src/main/java/com/example/
├── data/
│   ├── AppDatabase.kt                   # Database Room e istanza singleton
│   ├── BudgetDao.kt                     # Query SQL reattive per spese, categorie, impostazioni
│   ├── BudgetRepository.kt              # Repository pattern per la sincronizzazione dei dati
│   ├── ExpenseEntity.kt                 # Modello transazione (Cassetto, Importo, Entrate, Grief, Rimborsi)
│   ├── SubscriptionEntity.kt            # Modello abbonamenti ricorrenti
│   ├── SettingEntity.kt                 # Modello configurazioni chiave/valore
│   ├── GeminiApiService.kt              # Client API per l'integrazione Gemini
│   └── backup/                          # Logica di import/export JSON e serializzazione
├── service/
│   ├── PaymentNotificationListenerService.kt # Listener notifiche bancarie in background
│   └── GoogleDriveBackupService.kt      # Servizio di autenticazione e sync Google Drive
├── ui/
│   ├── BudgetViewModel.kt               # ViewModel principale con StateFlow consolidato
│   ├── screens/
│   │   ├── DashboardScreen.kt           # Panoramica home, Hero balance e schede Cassetti
│   │   ├── AddExpenseScreen.kt          # Form inserimento manuale spesa/movimento
│   │   ├── AddIncomeScreen.kt           # Form dedicato per la registrazione delle entrate extra
│   │   ├── EssentialScreen.kt           # Gestione costi fissi strutturali
│   │   ├── SubscriptionsScreen.kt       # Gestione abbonamenti mensili
│   │   ├── HistoryScreen.kt             # Storico transazioni e accesso rapido statistiche
│   │   ├── StatisticsScreen.kt          # Sezione dedicata statistiche avanzate (Matrice, Donut, Burn-rate)
│   │   ├── EventFundScreen.kt           # Gestione Fondo Risparmi ed Eventi
│   │   ├── PlanningScreen.kt            # Pianificazione budget ed estimatori
│   │   ├── RefundsScreen.kt             # Gestione dei rimborsi attesi e ricevuti
│   │   ├── SettingsScreen.kt            # Impostazioni generali, backup e parametri
│   │   └── NotificationLogsScreen.kt    # Log delle notifiche catturate
│   ├── components/                      # Componenti riutilizzabili (Dialog, Card, Badge)
│   ├── theme/                           # Palette colori M3, tipografia e forme
│   └── widget/                          # Provider widget per la home di Android
└── MainActivity.kt                      # Activity principale con navigazione Compose
```

---

## ⚙️ Come Compilare e Avviare il Progetto

### Prerequisiti
1. **Android Studio** (Ladybug / Koala o versione più recente consigliata).
2. **Android SDK** con supporto ad API 34+.
3. **JDK 11** configurato nelle impostazioni di Gradle.

### 1. Clonazione del Repository
```bash
git clone https://github.com/tuo-username/cashetti-app.git
cd cashetti-app
```

### 2. Configurazione Opzionale Chiavi API
Se desideri abilitare l'assistente Gemini per il riconoscimento automatico intelligente:
* Inserisci la tua API Key nelle **Impostazioni dell'App** (sezione *Google Gemini API Key*), oppure impostala tramite variabile d'ambiente/BuildConfig.

### 3. Compilazione tramite Gradle CLI
Per compilare l'APK in modalità Debug:
```bash
# Su Linux/macOS
./gradlew assembleDebug

# Su Windows
gradlew.bat assembleDebug
```
L'APK generato sarà disponibile in: `app/build/outputs/apk/debug/app-debug.apk`.

### 4. Esecuzione da Android Studio
1. Apri la cartella del progetto in Android Studio.
2. Attendi la sincronizzazione di Gradle (*Gradle Sync*).
3. Seleziona un dispositivo fisico o un emulatore Android.
4. Premi **Run (Shift + F10)** o il pulsante verde ▶️.

---

## 🔒 Privacy & Permessi

* **Privacy First**: Tutti i dati contabili rimangono salvati esclusivamente in locale sul dispositivo.
* **Accesso Notifiche**: Il permesso di ascolto notifiche viene utilizzato esclusivamente in locale per rilevare le spese bancarie e non trasmette dati a server esterni.
* **Backup Sicuro**: I file di backup esportati sono file JSON in chiaro controllati interamente dall'utente.

---

*Nota: Questo documento viene mantenuto costantemente allineato ad ogni modifica architetturale o funzionale di rilievo.*
