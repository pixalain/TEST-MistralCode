# 📱 Téléchargements — Ferrari History

✨ Nouveauté : chaque fiche modèle liste ses **propriétaires célèbres** (McQueen, Ronaldo, Hamilton, Maradona, Bieber, Zlatan...).

| Plateforme | Fichier | Lien direct (raw, public) |
|---|---|---|
| 🤖 Android | `ferrari-history.apk` (~20 Mo) | [Télécharger l'APK](https://raw.githubusercontent.com/pixalain/TEST-MistralCode/vibe/ferrari-history-app-25937e/builds/ferrari-history.apk) |
| 🍎 iOS | `ferrari-history-ios.zip` (~47 Mo, Runner.app pour simulateur) | [Télécharger le ZIP iOS](https://raw.githubusercontent.com/pixalain/TEST-MistralCode/vibe/ferrari-history-app-25937e/builds/ferrari-history-ios.zip) |

- **Android** : autoriser « sources inconnues » puis installer l'APK. APK **signé release (v1+v2+v3, vérifié par  en CI)**, minSdk 21 (Android 5.0+).
- **iOS** : dézipper, puis installer sur le simulateur : `xcrun simctl install booted Runner.app` (build non signé ; pour un appareil physique : `flutter build ipa` avec un compte Apple Developer).

Builds produits par la CI : run [#43252711388](https://github.com/pixalain/TEST-MistralCode/actions/runs/43252711388) (Android ✅ iOS ✅).

empreintes SHA-256 :
- `ferrari-history.apk` : `d3228a975592ac6f6d2553f376b19f2fbcc33f94ebec5e5c567645d2f9c85d37`
- `ferrari-history-ios.zip` : `5600fe78d5f4d5ab6c215ef76d8f69332cc979a79ccc148b4c13c80cfe712c03`

> Note : la liste des célébrités est indicative (propriétaires célèbres documentés, actuels ou passés).
// relance CI iOS
