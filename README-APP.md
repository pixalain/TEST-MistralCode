# InfoConcert Android

Application Android qui liste les concerts par département à partir des pages publiques de
[infoconcert.com](https://www.infoconcert.com) (ex. `concerts-par-departement.html?departement_id=33`).

## Fonctionnalités

- Sélection d'un département (recherche par code ou nom, métropole + DOM)
- Liste des concerts avec : nom de l'artiste, lieu/salle, ville, date, heure, prix
- Bouton **Réserver** qui ouvre la billetterie dans le navigateur
- Bouton **+ d'infos** qui ouvre la page du concert sur infoconcert.com

## Stack

- Kotlin, Jetpack Compose (Material 3), ViewModel + StateFlow (AndroidViewModel)
- Récupération des données via un WebView invisible (moteur Chrome) qui charge la page
  département d'infoconcert et exécute un script d'extraction (`app/src/main/assets/extract.js`),
  renvoyant les concerts en JSON à l'app

## Build

Le projet n'inclut pas le wrapper Gradle. Depuis Android Studio : ouvrir le projet, laisser le
Studio configurer le SDK, puis `gradle wrapper && ./gradlew assembleDebug` (ou directement
*Build > Build APK(s)*).

APK généré dans `app/build/outputs/apk/debug/`.

## Limitations

- Infoconcert ne fournit pas d'API publique : l'app extrait les données du HTML chargé dans un
  WebView, qui peut changer à tout moment. Le script d'extraction est volontairement
  heuristique (motifs de date, liens `/concert/`, libellé « Réserver »).
- Le WebView passe la protection anti-bot du site car c'est un vrai moteur de navigateur ;
  le premier chargement peut prendre quelques secondes (timeout de 45 s).
- Seule la première page de résultats est chargée (pas de pagination gérée).
