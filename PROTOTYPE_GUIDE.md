# Event Scanner – MVP (prototyp)

Aplikacja robi zdjęcie plakatu, „rozpoznaje” wydarzenie i dodaje je do systemowego kalendarza.
W prototypie **AI nie jest podłączone** – zamiast odpowiedzi LLM używane są stałe dane,
ale przechodzą przez **prawdziwy** parser JSON, walidację, obsługę wyjątków i zapis do bazy.

Cały kod jest w `app/src/main/java/com/example/myapplication/` (płaska struktura, jeden pakiet).

---

## 1. Gdzie co zmieniać

| Co chcesz zrobić | Plik |
|---|---|
| Ręcznie dodać / edytować wpisy w bazie (historia, „biblioteka aplikacji”) | **`SeedData.kt`** |
| Zmienić dane domyślne ekranu 3 „Wrap up” (to, co normalnie zwróci LLM) | **`MockData.kt`** |
| Wymusić błąd, włączyć/wyłączyć pasek debug, timeouty, kompresję | **`AppConfig.kt`** |
| Podłączyć prawdziwy LLM (np. Gemini) | **`EventExtractor.kt`** → `ExtractorFactory.create()` |
| Kroki procesu + wyjątki | `ScanPipeline.kt`, `ScanException.kt` |

### Ręczne wpisy do bazy (`SeedData.kt`)
Dopisz kolejny `EventEntity(...)` do listy `ENTRIES`. Format: `date = "yyyy-MM-dd"`,
`time = "HH:mm"` lub `null` (całodniowe), `saved = true/false` (ikona Saved / Not saved).

Baza (`events.db`, Room) wypełnia się tymi danymi przy pierwszym utworzeniu. Żeby wczytać zmiany:
- ustaw `AppConfig.RESEED_DATABASE_ON_START = true` (baza czyszczona i wypełniana przy każdym starcie), **albo**
- zwiększ `EventDatabase.DB_VERSION` o 1, **albo**
- odinstaluj aplikację / „Wyczyść dane”.

Na żywo (bez rebuildu): **View → Tool Windows → App Inspection → Database Inspector → events.db** –
można tam dodawać i edytować wiersze, a po wejściu w historię aplikacja pokaże zmiany.

---

## 2. Debug – jak sprawdzić, że proces działa bez błędów

**Logcat** → filtr `tag:EventScanner`. Każdy krok loguje `[OK]`, `[INFO]`, `[WARN]` albo `[FAIL]`.
Ostatnie 4 linie widać też na ekranie (czarny pasek; wyłączasz `AppConfig.SHOW_DEBUG_OVERLAY = false`).

Poprawny przebieg wygląda tak:
```
[OK] [SELFTEST] Database events.db OK (5 entries)
[OK] [SELFTEST] Calendar app available (ACTION_INSERT)
[OK] [CAMERA] Back camera started (preview + capture)
[OK] [CAPTURE] Photo captured (1200x1600)
[OK] [COMPRESS] JPEG 180 KB (max 1280px, quality 80)
[OK] [INPUT] Image ready: ...
[OK] [NETWORK] Connection check passed
[INFO] [EXTRACT] MOCK: no AI connected, returning hard-coded data
[OK] [EXTRACT] Response received
[OK] [PARSE] JSON parsed -> title="Bus Drivers Link Up", ...
[OK] [VALIDATE] Required fields present and formats valid
[OK] [PIPELINE] Scan finished without errors -> Wrap up
[OK] [FORM] Wrap up form is valid
[OK] [DATABASE] Event #6 stored (status: not saved in calendar yet)
[INFO] [CALENDAR] Opening system calendar (ACTION_INSERT)
[OK] [DATABASE] Event #6 marked as saved
[OK] [FLOW] Full flow finished without errors
```

### Testowanie błędów
W `AppConfig.kt` ustaw `SIMULATED_ERROR` – aplikacja pokaże ekran Error z kodem:

| `SimulatedError` | Wyjątek | Kod |
|---|---|---|
| `NO_NETWORK` | `ScanException.NoNetwork` | `E_NO_NETWORK` |
| `LLM_FAILURE` | `ScanException.Llm` | `E_LLM` |
| `LLM_TIMEOUT` | `ScanException.Timeout` (po 30 s) | `E_TIMEOUT` |
| `INVALID_JSON` | `ScanException.Parse` | `E_PARSE` |
| `MISSING_TITLE`, `INVALID_DATE` | `ScanException.Validation` | `E_VALIDATION` |
| `NO_CALENDAR_APP` | `ScanException.CalendarUnavailable` | `E_CALENDAR` |

Pozostałe: `E_CAMERA`, `E_COMPRESSION`, `E_DATABASE`, `E_UNKNOWN`.
Błędna data/godzina wpisana ręcznie na ekranie „Wrap up” też daje `E_VALIDATION`.

### Testy automatyczne
- `app/src/test/.../ScanPipelineTest.kt` i `EventParsingTest.kt` – 25 testów JVM (bez telefonu):
  ścieżka poprawna + każdy wyjątek, kontrakt JSON, formaty dat.
  Uruchom: prawy klik na folder `test` → *Run Tests*, albo `gradlew testDebugUnitTest`.
- `app/src/androidTest/.../EventDatabaseTest.kt` – testy bazy na urządzeniu/emulatorze
  (osobna baza testowa, nie rusza `events.db`).

---

## 3. Podłączenie LLM później

1. Nowa klasa `GeminiEventExtractor : EventExtractor` (`requiresNetwork = true`),
   w `extractJson()` wysyła JPEG + `EXTRACTION_PROMPT` i zwraca surowy tekst odpowiedzi.
   Błędy HTTP → `throw ScanException.Llm("...")`.
2. W `ExtractorFactory.create()` zwróć nową klasę zamiast `MockEventExtractor()`.
3. Odpowiedź modelu musi mieć format z `MockData.kt`:
   `{"title", "date": "yyyy-MM-dd", "time": "HH:mm"|null, "location", "description"}`.
   Parser toleruje ```json …``` i tekst dookoła.
4. Dodaj test w `ScanPipelineTest` z prawdziwymi odpowiedziami modelu – reszta (timeout, brak sieci,
   ekran błędu) już działa.

---

## 4. Uwagi do prototypu

- Kalendarz: systemowy Intent `ACTION_INSERT` (ADR 2). Android nie zwraca informacji, czy użytkownik
  kliknął „Zapisz”, więc powrót z kalendarza = „Saved” (jest to zalogowane jako INFO).
- Brak kamery / brak zgody → spust używa obrazka „MOCK POSTER”, żeby dało się przejść cały flow na emulatorze.
- Baza: Room (zgodnie z dokumentacją). `EventEntity.kt` = tabela, `EventDao.kt` = zapytania, `EventDatabase.kt` = plik bazy + wersja + wypełnianie z `SeedData`. Room generuje kod przez KSP; zmiana `EventEntity` wymaga podbicia `DB_VERSION` (baza jest wtedy tworzona od nowa).
- Ikony: Google Material Icons (`material-icons-extended`). Bez dźwięków i bez animacji przejść między ekranami.
- Ustawienia: tylko rozmiar tekstu 50% / 100% / 150% i wibracje.
- Pliki szablonu `ui/theme/*` i `res/drawable/ic_home|ic_favorite|ic_account_box` nie są już używane – można je usunąć.
