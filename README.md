# Labirynt

Plugin do serwera Minecraft (Paper/Spigot 1.21.1) umożliwiający udział w minigrze polegającej na przejściu proceduralnie generowanego labiryntu na czas.

## Funkcje
- Automatyczne generowanie unikalnego labiryntu za pomocą algorytmu DFS.
- Precyzyjny pomiar czasu burtowego dla każdego gracza osobno (Multiplayer).
- Zapisywanie najlepszych wyników w lokalnej bazie danych SQLite.
- System anty-cheat (blokada niszczenia/stawiania bloków, reset czasu przy teleportacji lub śmierci).
- Automatyczny restart i czyszczenie wyników co 24 godziny wraz z rozdawaniem nagród dla TOP 3.

## Komendy
- `/labirynt` - Generuje labirynt i przenosi gracza na start.
- `/labirynt restart` - Przerywa aktualny bieg i cofa gracza na start wygenerowanego labiryntu.
- `/labirynt top` - Wyświetla TOP 5 najlepszych czasów na serwerze.
- `/labirynt mytime` - Pokazuje osobisty rekord gracza.
- `/labirynt reset` - Wymusza natychmiastowy reset bazy danych i rozdanie nagród (wymaga uprawnienia administratora).

## Instalacja
1. Skompilowany plik `.jar` wrzuć do folderu `plugins` na serwerze Minecraft.
2. Upewnij się, że serwer działa na wersji Java 21 oraz silniku Paper/Spigot 1.21.1.
3. Uruchom serwer – baza danych i pliki konfiguracyjne stworzą się automatycznie.