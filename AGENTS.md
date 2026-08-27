# Regole e Istruzioni per lo Sviluppo di Cashetti App

## 📋 Regole di Manutenzione del Progetto
1. **Aggiornamento README.md**: Ad ogni modifica sostanziale dell'architettura dati, introduzione di nuove schermate, servizi o funzionalità chiave, aggiornare costantemente il file `README.md` nella root del repository per mantenerlo allineato con lo stato attuale del codebase.
2. **Architettura Dati & Finanziaria**:
   - I **Costi Fissi** (`ESSENZIALE_REALE`) hanno memoria perenne e non devono subire reset all'inizio del ciclo di fatturazione mensile.
   - Il **Cassetto Personale** e il **Cassetto Ginevra** si resettano all'inizio del ciclo di fatturazione (configurabile dall'utente). Il Cassetto Ginevra calcola il rollover dinamico sui cicli precedenti.
   - I Costi Fissi non inquinano lo storico delle spese vive quotidiane e la lista dei movimenti in Home.
3. **Backup & Sicurezza**:
   - Garantire sempre la piena operatività del sistema di backup locale JSON (`exportDataToJson` / `restoreDataFromBackup`).
