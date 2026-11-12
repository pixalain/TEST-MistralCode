# Suivi Basic-Fit 🏋️

Application Android (natif, WebView) pour suivre vos séances de sport chez Basic-Fit.

## Fonctionnalités
- Enregistrement de séances : date, type, durée, club, exercices (séries / reps / poids)
- Tableau de bord : séances totales, temps total, volume soulevé, séries de la semaine
- Heatmap d'activité (12 dernières semaines) + graphique de progression
- Historique complet, sauvegarde locale (localStorage) et export/partage JSON

## Récupérer l'APK
1. Onglet **Actions** → dernière exécution **Build APK** → artefact **SuiviBasicFit-APK**
2. Téléchargez et dézippez pour obtenir `SuiviBasicFit.apk`
3. Sur Android : autorisez « Installer des applications inconnues » puis installez

## Compiler soi-même
```bash
gradle assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```
Ou ouvrir le projet dans **Android Studio**.

## Structure
- `app/src/main/assets/www/` — application web (HTML/CSS/JS)
- `app/src/main/java/.../MainActivity.java` — WebView native
- `.github/workflows/build-apk.yml` — compilation automatique de l'APK
