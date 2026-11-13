# La Traverse — Cléon 🎸

Application Android (non officielle) pour suivre la programmation de **La Traverse**, la salle de concerts de Cléon (76410), près de Rouen.

## Fonctionnalités

- **Programmation dynamique** : à chaque lancement, l'app récupère les concerts à venir depuis [latraverse.org/Agenda](https://www.latraverse.org/Agenda) (date, artiste, première partie, heure, genre, photo, lien billetterie)
- **Concerts passés** : archives complètes intégrées — **537 concerts depuis 2006** (`assets/archive.json`), avec photos et genres
- **Recherche** par artiste, première partie ou genre
- **Détail enrichi** : description complète de l'événement (chargée depuis sa page officielle), crédit photo, vidéo YouTube de l'artiste, accès direct à la billetterie et à l'itinéraire
- **Infos salle** : adresse, téléphone, site officiel
- Recherche et tri automatiques selon la date du jour (passé / à venir)

## Données

L'archive complète des concerts passés (537 événements depuis 2006) est intégrée dans `assets/archive.json`. Les concerts à venir sont récupérés en direct depuis la page Agenda du site officiel (`RemoteSource.kt`) ; la liste intégrée sert de repli hors ligne.

## Charte graphique

L'app reprend la charte du site [latraverse.org](https://www.latraverse.org/) :
- Fond blanc / noir, accent **jaune #ECB31E**
- Titres en capitales, typographie condensée
- Logo losange

## Build

```bash
./gradlew assembleRelease
```

APK : `app/build/outputs/apk/release/`. Un workflow GitHub Actions (`.github/workflows/android.yml`) construit et téléverse l'APK à chaque push ; les releases signées sont publiées sur les tags `v*`.

## Renseignements

La Traverse — 37 rue Luis Corvalan, 76410 Cléon — 02 35 81 25 25 — [latraverse.org](https://www.latraverse.org/)

Application fan, non officielle, sans affiliation avec La Traverse.
