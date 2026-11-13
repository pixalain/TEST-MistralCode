# La Traverse — Cléon 🎸

Application Android (non officielle) pour suivre la programmation de **La Traverse**, la salle de concerts de Cléon (76410), près de Rouen.

## Fonctionnalités

- **Concerts à venir** : toute la programmation à venir, triée par date
- **Concerts passés** : archives des concerts des saisons 2025-2026
- **Recherche** par artiste, première partie ou genre
- **Détail d'un concert** : date, heure, genre, description, accès direct à la billetterie et à l'itinéraire
- **Infos salle** : adresse, téléphone, site officiel
- Recherche et tri automatiques selon la date du jour (passé / à venir)

## Données

La liste des concerts est intégrée dans l'app (`app/src/main/java/com/latraverse/app/data/Concert.kt`), pré-remplie avec la programmation réelle de la salle (saison 2025-2026 et premières dates 2027). Ajouter un concert = ajouter une entrée dans la liste `Concerts.all`.

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
