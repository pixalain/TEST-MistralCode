# 📱 Téléchargements — Ferrari History

| Plateforme | Fichier | Lien direct (raw, public) |
|---|---|---|
| 🤖 Android | `ferrari-history.apk` (~20 Mo) | [Télécharger l'APK](https://raw.githubusercontent.com/pixalain/TEST-MistralCode/vibe/ferrari-history-app-25937e/builds/ferrari-history.apk) |
| 🍎 iOS | `ferrari-history-ios.zip` (~47 Mo, Runner.app pour simulateur) | [Télécharger le ZIP iOS](https://raw.githubusercontent.com/pixalain/TEST-MistralCode/vibe/ferrari-history-app-25937e/builds/ferrari-history-ios.zip) |

- **Android** : autoriser « sources inconnues » puis installer l'APK. APK **signé release (v1+v2+v3, vérifié par  en CI)**, minSdk 21 (Android 5.0+).
- **iOS** : dézipper, puis installer sur le simulateur : `xcrun simctl install booted Runner.app` (build non signé ; pour un appareil physique : `flutter build ipa` avec un compte Apple Developer).

Builds produits par la CI : run [#43088152482](https://github.com/pixalain/TEST-MistralCode/actions/runs/43088152482) (Android ✅ iOS ✅).

empreintes SHA-256 :
- `ferrari-history.apk` : `68a36bfb9b579542c47b66affc9a0d0f1e516e3a90b125552a0030f22da3141f`
- `ferrari-history-ios.zip` : `3ac2e834c6602e820b95d350a05777843fc050f5b3def0ae783ee8466e1d6b52`
