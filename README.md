# Ferrari History — Application Android & iOS

Application mobile (Flutter) qui retrace l'histoire de **Ferrari**, « une grande place dans l'histoire de l'automobile » : tous les modèles produits par la marque, avec pour chacun une **photo** (Wikimedia Commons, licences libres), sa **fiche technique complète** (moteur, puissance, couple, 0–100 km/h, vitesse max, poids, production) et son histoire, plus une **ligne du temps** d'Enzo Ferrari (1898) au Purosangue (2022).

## 📱 Liens de téléchargement

| Plateforme | Lien |
|---|---|
| 🤖 **Android (APK)** | [Télécharger depuis les Releases GitHub](https://github.com/pixalain/TEST-MistralCode/releases) — artefact `ferrari-history-android-apk` / APK `app-release.apk` |
| 🍎 **iOS (app .app)** | [Télécharger depuis les Releases GitHub](https://github.com/pixalain/TEST-MistralCode/releases) — artefact `ferrari-history-ios-app` (build simulator, sans signature) |

> Android : installez l'APK (autoriser « sources inconnues »). iOS : le build CI est un `.app` non signé destiné au **simulateur iOS** ; pour un IPA signé destiné à un appareil, compilez avec votre compte Apple Developer (`flutter build ipa`).

## Contenu

- **30 modèles** : 125 S, 166 MM, 250 GTO, 250 California, 250 LM, 275 GTB, Dino 246 GT, 365 GTB/4 Daytona, 308 GTB, 288 GTO, Testarossa, F40, 512 BB, 348, F355, 550 Maranello, 360 Modena, F430, 458 Italia, F50, F12berlinetta, LaFerrari, California T, Portofino, Enzo, 812 Superfast, SF90 Stradale, F8 Tributo, Roma, Purosangue…
- Recherche par nom, catégorie ou année.
- Ligne du temps de 1898 à 2024.

## Développement local

```bash
flutter pub get
flutter run        # Android / iOS connecté ou émulateur
flutter test       # tests unitaires et widgets
flutter build apk --release   # Android
flutter build ios --no-codesign # iOS (simulateur)
```

## CI/CD

Le workflow GitHub Actions [`.github/workflows/build-apps.yml`](.github/workflows/build-apps.yml) construit à chaque push :
- **Android** : `flutter build apk --release` → APK publié en artefact + Release GitHub sur `main` ;
- **iOS** : `flutter build ios --no-codesign --simulator` → `.app` zippé en artefact.

## Crédits

Photos : [Wikimedia Commons](https://commons.wikimedia.org) (licences CC BY / CC BY-SA / domaine public), crédit individuel indiqué sur chaque fiche. Application non officielle, réalisée par un passionné.
