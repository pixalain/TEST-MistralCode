# TV Live & Replay — Android

Portage natif **Android (Kotlin + Jetpack Compose)** de l'application « TV Live & Replay » : TV en direct et téléchargement de replays.

> **Avertissement légal** : l'application ne lit et ne télécharge que des **flux accessibles en clair** (HLS non chiffré, playlists M3U publiques, flux officiels ouverts). Elle ne contourne **aucun DRM**, géo-blocage ou paywall : un flux protégé échoue simplement. Certaines chaînes du catalogue intégré sont géo-restreintes.

## Fonctionnalités

- **TV en direct** : lecture HLS via ExoPlayer (Media3), embarqué dans l'écran de lecture ; bouton « Ouvrir dans un lecteur externe » (VLC, etc.).
- **Téléchargement des replays** :
  - flux HLS (`.m3u8`) → téléchargés en arrière-plan par le `DownloadManager` de Media3, stockés dans le cache de l'app et **relisibles hors ligne** depuis l'onglet Téléchargements ;
  - fichiers directs (mp4, mp3…) → téléchargés avec progression vers `Android/data/com.tvlive.app/files/Downloads/TVLive/`, ouvrables via le gestionnaire de fichiers.
- **Sources** : catalogue intégré (`app/src/main/assets/sources_builtin.json`, chaînes en flux ouvert : Arte, TV5 Monde, France 24, BFM TV, CGTN, Al Jazeera, NHK, ARD/ZDF, RTÉ…) + playlists M3U **par URL** ou **fichier local** (sélecteur de fichiers, permissions persistantes), mémorisées entre les lancements.
- Recherche dans la liste des chaînes, groupement par catégorie, thème sombre.

## Compilation de l'APK

Prérequis : JDK 17 + Android SDK (API 35). Deux options :

```bash
# APK debug (signé automatiquement, installable)
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk

# APK release + tests unitaires
./gradlew testDebugUnitTest assembleRelease
```

Ou ouvrez le projet dans **Android Studio** et lancez l'app sur un émulateur/appareil.

La **CI GitHub** (`.github/workflows/android.yml`) compile automatiquement à chaque push sur `vibe/**` et chaque PR : elle exécute les tests unitaires puis produit les artefacts `tvlive-debug-apk` (installable) et `tvlive-release-apk` (signé uniquement sur les tags `v*`, via les secrets `ANDROID_KEYSTORE_B64`, `ANDROID_STORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`).

## Structure

```
app/src/main/java/com/tvlive/app/
  MainActivity.kt            navigation + onglets (Chaînes / Téléchargements)
  TvLiveApp.kt               initialise le gestionnaire de téléchargements
  data/model/Channel.kt      modèle de chaîne (+ détection playlist)
  data/m3u/M3uParser.kt      parseur de playlists M3U/M3U8
  data/sources/...           catalogue intégré + playlists URL/fichier persistées
  player/TvPlayer.kt         ExoPlayer (flux clairs + cache hors ligne)
  player/TvDownloads.kt      DownloadManager Media3 + service de téléchargement
  player/DirectDownloads.kt  téléchargements de fichiers directs (OkHttp, progression)
  ui/                        écrans Compose + ViewModel
app/src/main/assets/sources_builtin.json   catalogue de chaînes en flux ouvert
app/src/test/.../M3uParserTest.kt          tests unitaires (JUnit)
```

## Tests

```bash
./gradlew testDebugUnitTest
```
