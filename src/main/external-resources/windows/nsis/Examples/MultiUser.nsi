Name "MultiUser example"
OutFile "MultiUser.exe"

!define UNINSTKEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)"
!define MULTIUSER_INSTALLMODE_DEFAULT_REGISTRY_KEY "${UNINSTKEY}"
!define MULTIUSER_INSTALLMODE_DEFAULT_REGISTRY_VALUENAME "CurrentUser"
!define MULTIUSER_INSTALLMODE_INSTDIR "$(^Name)"
!define MULTIUSER_INSTALLMODE_COMMANDLINE
!define MULTIUSER_EXECUTIONLEVEL Highest
!define MULTIUSER_MUI

!include "LogicLib.nsh"
!include "MultiUser.nsh"
!include "MUI2.nsh"

!insertmacro MUI_PAGE_WELCOME
!insertmacro MULTIUSER_PAGE_INSTALLMODE
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_LANGUAGE "English"

Function .onInit
  !insertmacro MULTIUSER_INIT
FunctionEnd

Function un.onInit
  !insertmacro MULTIUSER_UNINIT
FunctionEnd

Section
  SetOutPath "$InstDir"
  WriteUninstaller "$InstDir\Uninstall.exe"
  WriteRegStr ShCtx "${UNINSTKEY}" DisplayName "$(^Name)"
  WriteRegStr ShCtx "${UNINSTKEY}" UninstallString '"$InstDir\Uninstall.exe"'
  WriteRegStr ShCtx "${UNINSTKEY}" $MultiUser.InstallMode 1 ; Write MULTIUSER_INSTALLMODE_DEFAULT_REGISTRY_VALUENAME so the correct context can be detected in the uninstaller.

  !tempfile APP
  !makensis '-v2 "-DOUTFILE=${APP}" "-DNAME=NsisMultiUserExample" -DCOMPANY=Nullsoft "AppGen.nsi"' = 0
  File "/oname=$InstDir\MyApp.exe" "${APP}" ; Pretend that we have a real application to install
  !delfile "${APP}"
SectionEnd

Section "Start Menu shortcut"
  CreateShortcut /NoWorkingDir "$SMPrograms\$(^Name).lnk" "$InstDir\MyApp.exe"
SectionEnd

Section "-Uninstall"
  Delete "$SMPrograms\$(^Name).lnk"

  Delete "$InstDir\MyApp.exe"
  Delete "$InstDir\Uninstall.exe"
  DeleteRegKey ShCtx "${UNINSTKEY}"
  RMDir $InstDir
SectionEnd
