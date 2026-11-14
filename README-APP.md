# InfoConcert Android

Application Android qui liste les concerts par département à partir des pages publiques de
[infoconcert.com](https://www.infoconcert.com) (ex. `concerts-par-departement.html?departement_id=33`).

## Fonctionnalités

- Sélection d'un département (recherche par code ou nom, métropole + DOM)
- Liste des concerts avec : nom de l'artiste, lieu/salle, ville, date, heure, prix
- Bouton **Réserver** qui ouvre la billetterie dans le navigateur
- Bouton **+ d'infos** qui ouvre la page du concert sur infoconcert.com

## Stack

- Kotlin, Jetpack Compose (Material 3), ViewModel + StateFlow
- Jsoup pour récupérer et analyser les pages HTML d'infoconcert.com

## Build

Le projet n'inclut pas le wrapper Gradle. Depuis Android Studio : ouvrir le projet, laisser le
Studio configurer le SDK, puis `gradle wrapper && ./gradlew assembleDebug` (ou directement
*Build > Build APK(s)*).

APK généré dans `app/build/outputs/apk/debug/`.

## Limitations

- Infoconcert ne fournit pas d'API publique : l'app parse le HTML, qui peut changer à tout moment.
  Le parser (`InfoConcertRepository`) est volontairement heuristique (motifs de date, liens
  `/concert/`, libellé « Réserver »).
- Le site protège ses pages contre les robots ; en cas d'échec répété depuis un appareil,
  vérifier que la page s'ouvre bien dans le navigateur du téléphone.
- Seule la première page de résultats est chargée (pas de pagination gérée).
