# THE DG VST

Plugin **VST3 + application standalone Windows x64** qui regroupe les effets de guitare
utilisés par **David Gilmour** (Pink Floyd), modélisés d'après son pedalboard et ses racks
documentés (kitrae.net, gilmourish.com, equipboard).

## Chaîne de signal (ordre du pedalboard Gilmour)

```
IN ──► WAH (Cry Baby) ──► COMP (MXR Dyna Comp / Boss CS-2) ──► FUZZ (Fuzz Face / Big Muff π)
     ──► OCTAVE ──► UNI-VIBE ──► PHASER (MXR Phase 90) ──► FLANGER (EHX Electric Mistress)
     ──► CHORUS (Boss CE-2) ──► TREMOLO (Demeter Tremulator) ──► DELAY (Binson Echorec / TC 2290,
     dotted-eighth) ──► REVERB (style SPX90) ──► MASTER ──► OUT
```

Chaque étage est une pédale avec son **footswitch chrome + LED** et ses **potards chrome**.
Le **wah** répond aussi au **MIDI CC1 (mod wheel)**.

## Interface graphique

- Fond **gris sombre anthracite** (dégradé + brossé subtil)
- Boutons, footswitchs et potards à aspect **chrome** (dégradés multi-bandes façon métal poli)
- LED verte quand une pédale est enclenchée
- Titre « THE DG VST » sur plaque métal brossé

## Installation sous Windows

1. Copier le dossier **`THE DG VST.vst3`** dans :
   - `C:\Program Files\Common Files\VST3\` (tous les utilisateurs), ou
   - `%LOCALAPPDATA%\Programs\Common Files\VST3\` (utilisateur courant)
2. Ouvrir votre DAW (Reaper, Ableton, FL Studio, Cubase, Studio One, Cakewalk…) et
   rafraîchir la liste des plugins VST3 → **THE DG VST** apparaît.
3. Variante autonome : lancer **`THE DG VST.exe`** (hors DAW, avec entrée audio de la carte son).

> Le binaire est **entièrement statique** (runtime GCC embarqué) : seules les DLL système
> Windows sont requises. Fonctionne sur Windows 10 / 11 x64 sans installer de Visual C++
> Redistributable.

## Compilation (depuis Linux, cross-compilation Windows)

```bash
# Prérequis : clone de JUCE 8 + toolchain MinGW-w64 hébergée sur Linux (xPack)
git clone --depth 1 --branch 8.0.10 https://github.com/juce-framework/JUCE.git
# xpack-mingw-w64-gcc-15.2.0-2-linux-x64.tar.gz depuis
#   https://github.com/xpack-dev-tools/mingw-w64-gcc-xpack/releases

cmake -S THE-DG-VST -B build-win \
  -DCMAKE_TOOLCHAIN_FILE=THE-DG-VST/cmake/mingw-w64-toolchain.cmake \
  -DMINGW_ROOT=/opt/xpack-mingw-w64-gcc-15.2.0-2 \
  -DJUCE_DIR=/path/to/JUCE \
  -DJUCE_WINDOWS_HELPERS_CAN_RUN=TRUE \
  -DCMAKE_BUILD_TYPE=Release
cmake --build build-win --config Release -j
```

Artefacts produits :
- `build-win/TheDGVST_artefacts/Release/VST3/THE DG VST.vst3/`
- `build-win/TheDGVST_artefacts/Release/Standalone/THE DG VST.exe`

## Test du moteur DSP (natif Linux)

```bash
g++ -std=c++20 -O2 -o dg_dsp_test THE-DG-VST/tests/dsp_test.cpp && ./dg_dsp_test
```

18 tests : transparence du bypass, chaque effet actif produit un signal fini et modifié,
chaîne Gilmour complète stable, silence→silence, écho de delay au temps programmé + tap
dotted-eighth.

## Structure

```
THE-DG-VST/
├── CMakeLists.txt                  # cible VST3 + Standalone + test DSP
├── cmake/
│   ├── mingw-w64-toolchain.cmake   # cross-compilateur Windows x64 depuis Linux
│   └── JUCEUtilsNoAide.cmake       # JUCE sans juceaide (génération JuceHeader/BinaryData en CMake pur)
├── Source/
│   ├── DspChain.h                  # cœur DSP pur C++ (11 modules, testable seul)
│   ├── PluginProcessor.{h,cpp}    # processeur JUCE + paramètres APVTS
│   ├── PluginEditor.{h,cpp}       # pédales, footswitchs, potards
│   ├── ChromeLook.{h,cpp}          # LookAndFeel chrome / fond gris sombre
│   └── DspChain.cpp                # TU vide (DSP header-only)
├── Resources/icon.{png,ico}        # icône
└── tests/dsp_test.cpp              # tests unitaires DSP
```

## Effets implémentés (références matérielles de Gilmour)

| Module | Référence pédale/rack |
|---|---|
| WAH | Dunlop Cry Baby (Vox wah des débuts) |
| COMP | MXR Dyna Comp, Boss CS-2, Ibanez CP-9 |
| FUZZ | Dallas Arbiter Fuzz Face (germanium/silicium) + EHX Big Muff π Ram's Head (solo de *Comfortably Numb*) |
| OCTAVE | octave up/sub (effets « mouette » d'*Echoes*) |
| UNI-VIBE | Univox Uni-Vibe |
| PHASER | MXR Phase 90 |
| FLANGER | Electro-Harmonix Electric Mistress |
| CHORUS | Boss CE-2 / CE-3 (modifié Cornish) |
| TREMOLO | Demeter Tremulator |
| DELAY | Binson Echorec II, TC Electronic 2290, Boss DD-2 (tap dotted-eighth) |
| REVERB | Yamaha SPX90 / Lexicon PCM-70 (rack) |
