# F1 Live

Application Android (Kotlin + Jetpack Compose) pour suivre la Formule 1 en temps réel :

- **Classement des pilotes** (points, victoires, écurie)
- **Fiche pilote** en cliquant sur un pilote : nom, âge, nationalité, écurie actuelle, palmarès complet (victoires, poles, podiums, tours rapides, points, saisons, meilleur championnat, titres) et derniers résultats
- **Classement des écuries** (constructors)
- **Calendrier de la saison en cours** avec statut Terminé / À venir
- **Onglet « Séances »** : affiche le **GP en cours** (séance « En cours » surlignée) ou à défaut les **dernières séances** du dernier GP — FP1, FP2, FP3, Sprint Qualifs, Sprint, Qualifs, Course, horaires locaux, statut En cours / À venir / Terminée
- **Détail de chaque GP avec onglets dynamiques** : FP1, FP2, FP3, Qualifs, Sprint Qualifs, Sprint, Course — résultats complets (position, pilote, écurie, meilleur temps, tours) via l'API alpha de Jolpica (`/f1/alpha/results/{round}/{session}/`)
- **Résultats complets de chaque Grand Prix** (course et sprint), avec temps et points
- **Onglet « Grille de départ »** dans chaque GP + **places gagnées/perdues** (+3 / -2 / =) à côté de chaque pilote du classement

Les données proviennent de l'API ouverte [Jolpica F1](https://github.com/jolpica/jolpica-f1)
(successeur de l'API Ergast) : `https://api.jolpi.ca/ergast/f1/current/...`

## Installation sur Android

1. Téléchargez l'APK signé : **[app-release.apk](https://raw.githubusercontent.com/pixalain/TEST-MistralCode/vibe/f1-live-app-bae9bb/releases/app-release.apk)**
   ( branche `vibe/f1-live-app-bae9bb`, dossier `releases/` ; l'artifact `f1live-apk` est aussi disponible dans l'onglet **Actions** du dépôt ).
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

## 🔒 Sécurité
- Trafic **HTTPS uniquement** : `usesCleartextTraffic=false` + `network_security_config.xml` (cleartext interdit partout, ancres système uniquement)
- **Aucune permission dangereuse** : seule `INTERNET` (la permission réseau inutile a été retirée)
- `allowBackup=false` + règles d'extraction cloud/device vides (aucune donnée sauvegardée)
- Un seul composant exporté : l'activité launcher (aucun deep link, pas de WebView)
- Release minifié (R8) + ressources shrinkées ; **logs HTTP uniquement en debug** (`BuildConfig.DEBUG`), jamais en release
- Signature release via variables d'environnement/CI — **aucun keystore ni mot de passe dans le dépôt** (`.gitignore` : `*.jks`, `f1-signing.properties`)
- `apksigner verify` OK sur l'APK publié

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
