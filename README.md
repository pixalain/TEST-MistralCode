# TV Live & Replay — Android (v2)

Application **Android native** (Kotlin + Jetpack Compose + Media3/ExoPlayer) pour regarder la TV en direct et télécharger des replays. L'APK est compilé automatiquement par la CI GitHub.

> **Avertissement légal** : l'application ne lit et ne télécharge que des **flux accessibles en clair** (HLS non chiffré, playlists M3U publiques, flux officiels ouverts). Elle ne contourne **aucun DRM**, géo-blocage ou paywall : un flux protégé échoue simplement. Certaines chaînes du catalogue intégré sont géo-restreintes selon le pays.

## Fonctionnalités

- **TV en direct** : lecture HLS via ExoPlayer (Media3) embarqué + bouton « Lecteur externe » (VLC…)
- **Replays HLS (`.m3u8`)** : téléchargés en arrière-plan (service foreground + notification), stockés dans le cache de l'app et **relisibles hors ligne**
- **Fichiers directs (mp4…)** : téléchargés avec progression vers `Android/data/com.tvlive.app/files/Downloads/TVLive/`, ouvrables ensuite
- **Sources** : catalogue intégré de chaînes en flux ouvert (`app/src/main/assets/sources_builtin.json` : Arte, TV5 Monde, France 24, BFM TV, CGTN, Al Jazeera, NHK, ARD/ZDF, RTÉ…) + playlists M3U **par URL** ou **fichier local** (sélecteur système, permission persistante), mémorisées entre les lancements
- UI Compose : onglets **Chaînes** / **Téléchargements**, recherche, liste groupée par catégorie, thème sombre

## Télécharger l'APK (compilé par la CI)

À chaque push/PR, le workflow `.github/workflows/android.yml` exécute les tests puis produit :

- **`tvlive-debug-apk`** → `app-debug.apk` **signé debug, installable directement** sur un téléphone ;
- `tvlive-release-apk` → non signé (la signature release est faite sur les tags `v*` via les secrets `ANDROID_KEYSTORE_B64`, `ANDROID_STORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`).

Les artefacts sont téléchargeables depuis la page du run (section « Artifacts ») ou en CLI :

```bash
gh run list --repo <owner>/<repo> --branch <branche>   # trouver le run
gh run download <run-id> --repo <owner>/<repo> -n tvlive-debug-apk
```

## Build local

Prérequis : JDK 17 + Android SDK (API 35), ou Android Studio.

```bash
./gradlew testDebugUnitTest assembleDebug assembleRelease
# APK debug : app/build/outputs/apk/debug/app-debug.apk
```

## Structure

```
app/src/main/java/com/tvlive/app/
  MainActivity.kt            navigation + barre d'onglets (Chaînes / Téléchargements)
  TvLiveApp.kt               Application : initialise le gestionnaire de téléchargements
  model/Channel.kt           modèle de chaîne (+ détection de playlist)
  m3u/M3uParser.kt           parseur de playlists M3U/M3U8
  sources/SourcesRepository.kt  catalogue intégré + playlists persistées + réseau (OkHttp)
  player/TvDownloads.kt      DownloadManager Media3 + service foreground (replays HLS)
  player/PlayerFactory.kt    ExoPlayer (flux clairs + cache hors ligne)
  player/FileDownloads.kt    téléchargements de fichiers directs avec progression
  ui/                        écrans Compose + ViewModel + thème
app/src/main/assets/sources_builtin.json   catalogue de chaînes en flux ouvert
app/src/test/java/com/tvlive/app/M3uParserTest.kt   tests unitaires JUnit
```

## Tests

```bash
./gradlew testDebugUnitTest   # exécuté aussi par la CI
```
