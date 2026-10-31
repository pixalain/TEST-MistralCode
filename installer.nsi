; Installeur NSIS pour MonPremierVST (VST3, 64 bits)

!define PLUGIN_NAME "MonPremierVST"
!define VERSION "0.3.0"
!define PUBLISHER "Pixalain"

Name "${PLUGIN_NAME} v${VERSION}"
OutFile "MonPremierVST-Setup-v${VERSION}.exe"
InstallDir "$PROGRAMFILES64\Common Files\VST3"
InstallDirRegKey HKLM "Software\${PUBLISHER}\${PLUGIN_NAME}" "InstallDir"
RequestExecutionLevel admin

Page directory
Page instfiles

Section "Install"
  SetOutPath "$INSTDIR"
  File /r "build\MonPremierVST_artefacts\Release\VST3\${PLUGIN_NAME}.vst3"

  WriteRegStr HKLM "Software\${PUBLISHER}\${PLUGIN_NAME}" "InstallDir" "$INSTDIR"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PLUGIN_NAME}" "DisplayName" "${PLUGIN_NAME} v${VERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PLUGIN_NAME}" "UninstallString" "$INSTDIR\uninstall.exe"
  WriteUninstaller "$INSTDIR\uninstall.exe"
SectionEnd

Section "Uninstall"
  RMDir /r "$INSTDIR\${PLUGIN_NAME}.vst3"
  Delete "$INSTDIR\uninstall.exe"
  RMDir "$INSTDIR"
  DeleteRegKey HKLM "Software\${PUBLISHER}\${PLUGIN_NAME}"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PLUGIN_NAME}"
SectionEnd
