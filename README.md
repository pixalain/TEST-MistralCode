# F1 Live

Application Android (Kotlin + Jetpack Compose) pour suivre la Formule 1 en temps réel :

- **Classement des pilotes** (points, victoires, écurie)
- **Classement des écuries** (constructors)
- **Calendrier de la saison en cours** avec statut Terminé / À venir
- **Résultats complets de chaque Grand Prix** (course et sprint), avec temps et points

Les données proviennent de l'API ouverte [Jolpica F1](https://github.com/jolpica/jolpica-f1)
(successeur de l'API Ergast) : `https://api.jolpi.ca/ergast/f1/current/...`

## Installation sur Android

1. Téléchargez le fichier `app-release.apk` depuis la dernière
   [Release GitHub](../../releases) (ou l'artifact `f1live-apk` des Actions CI).
2. Ouvrez-le sur votre téléphone Android (Android 8.0 / API 26 minimum).
3. Si demandé, autorisez l'installation depuis cette source ("Sources inconnues").
4. Ouvrez **F1 Live**.

Alternative pour les développeurs :

```bash
git clone <repo-url>
cd <repo>
./gradlew installDebug   # avec un téléphone/émulateur connecté (adb)
```

## Développement

```bash
./gradlew assembleDebug      # APK de debug
./gradlew assembleRelease    # APK release (signé si f1-signing.properties présent)
./gradlew testDebugUnitTest  # tests unitaires
```

Stack : Kotlin 2.0, Jetpack Compose + Material 3, Navigation Compose, Retrofit/OkHttp,
kotlinx.serialization, Coil. AGP 8.7.3, Gradle 8.9, compileSdk 35, minSdk 26.

## Structure

```
app/src/main/java/com/f1live/app/
├── MainActivity.kt                  # navigation + bottom bar
├── data/
│   ├── api/JolpicaService.kt        # endpoints Retrofit (Jolpica/Ergast)
│   ├── model/Models.kt              # modèles sérialisables
│   └── repository/F1Repository.kt   # accès données
└── ui/
    ├── F1ViewModel.kt               # état des écrans
    ├── screens/                     # Classements, GP, Détail GP
    └── theme/                       # thème Material 3
```
