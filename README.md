# MonPremierVST

Ton premier plugin VST3 (effet de gain / volume), développé avec [JUCE](https://juce.com) 8.0.15.
Licence : GPL v3 (voir `LICENSE`). Le SDK VST3 de Steinberg est sous licence MIT depuis la v3.8.0.

![Aperçu du plugin MonPremierVST v0.3.0](https://raw.githubusercontent.com/pixalain/TEST-MistralCode/releases/assets/monpremiervst-mockup-v0.3.0.jpg)

## Télécharger et installer (Windows 64 bits)

1. Va dans l'onglet **[Releases](https://github.com/pixalain/TEST-MistralCode/releases)** de ce dépôt.
2. Télécharge **`MonPremierVST-Setup-v0.3.0.exe`** (installeur automatique) ou **`MonPremierVST-Windows-v0.3.0.zip`** (le dossier `.vst3` à copier à la main dans `C:\Program Files\Common Files\VST3`).
3. Lance l'installeur (ou copie le `.vst3`), puis ouvre ta DAW (REAPER, Ableton, FL Studio, etc.) : le plugin apparaît sous le nom **MonPremierVST** dans la liste des effets VST3.

## Utiliser

Insère le plugin sur une piste audio, bouge le curseur **Gain** : le volume du son change en temps réel (0 = muet, 1 = niveau d'origine).

## Comment c'est fait

- `CMakeLists.txt` : déclare le projet et télécharge JUCE automatiquement (version épinglée 8.0.15).
- `Source/PluginProcessor.cpp` : le cœur du plugin. La fonction `processBlock` reçoit des blocs de son et multiplie chaque échantillon par la valeur du paramètre `gain`.
- `Source/PluginEditor.cpp` : la petite fenêtre avec le curseur, reliée au paramètre via un `SliderAttachment`.
- `.github/workflows/build-release.yml` : compile automatiquement le plugin sur les serveurs de GitHub à chaque push et publie les binaires dans les Releases.

## Compiler soi-même

Prérequis : Visual Studio 2022 (charge « Développement Desktop en C++ »), CMake, Git.

```powershell
git clone https://github.com/pixalain/TEST-MistralCode.git
cd TEST-MistralCode
cmake -B build -G "Visual Studio 17 2022" -A x64
cmake --build build --config Release
```

Le plugin se trouve dans `build\MonPremierVST_artefacts\Release\VST3\MonPremierVST.vst3`.

## Prochaines étapes possibles

- Remplacer le gain par un autre effet (écho, distorsion, filtre, compresseur) en modifiant uniquement `processBlock`.
- Ajouter d'autres paramètres (ex : fréquence d'un filtre) dans `createParameterLayout`.
