!include "MUI.nsh"
!include "MUI2.nsh"
!include "FileFunc.nsh"
!include "TextFunc.nsh"
!include "WordFunc.nsh"
!include "LogicLib.nsh"
!include "nsDialogs.nsh"
!include "x64.nsh"

!define REG_KEY_UNINSTALL "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PROJECT_NAME}"
!define REG_KEY_SOFTWARE "SOFTWARE\${PROJECT_NAME}"
!define SERVICE_NAME "UniversalMediaServer"
!define OLD_SERVICE_NAME "Universal Media Server"

VIAddVersionKey "ProductName" "${PROJECT_NAME}"
VIAddVersionKey "Comments" ""
VIAddVersionKey "CompanyName" "${PROJECT_ORGANIZATION_NAME}"
VIAddVersionKey "LegalTrademarks" ""
VIAddVersionKey "LegalCopyright" ""
VIAddVersionKey "FileDescription" "${PROJECT_NAME} Update"
VIAddVersionKey "FileVersion" "${PROJECT_VERSION}"
VIProductVersion "${PROJECT_VERSION_SHORT}.0"

ManifestDPIAware true
RequestExecutionLevel admin

Name "${PROJECT_NAME}"

SetCompressor /SOLID zlib

!define MUI_ABORTWARNING
!define MUI_FINISHPAGE_RUN
!define MUI_FINISHPAGE_RUN_FUNCTION RunUMS
!define MUI_ICON "${PROJECT_BASEDIR}\src\main\external-resources\icon.ico"
!define MUI_WELCOMEFINISHPAGE_BITMAP "${NSISDIR}\Contrib\Graphics\Wizard\ums.bmp"
!define MUI_PAGE_CUSTOMFUNCTION_LEAVE WelcomeLeave

!insertmacro MUI_PAGE_WELCOME
Page Custom LockedListShow LockedListLeave
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_LANGUAGE "English"

Function .onInit
	SetRegView 64
	SetShellVarContext current
	ReadRegStr $0 SHCTX "${REG_KEY_SOFTWARE}" "BinaryRevision"
	${If} $0 != "${PROJECT_BINARY_REVISION}"
		SetShellVarContext all
		ReadRegStr $0 SHCTX "${REG_KEY_SOFTWARE}" "BinaryRevision"
		${If} $0 != "${PROJECT_BINARY_REVISION}"
			MessageBox MB_OK|MB_ICONSTOP "Can't update this version, use the full installer" 0 0
			Abort
		${EndIf}
	${EndIf}
	ReadRegStr $0 SHCTX "${REG_KEY_SOFTWARE}" ""
	${IfNot} $0 == ""
		StrCpy $INSTDIR "$0"
	${Else}
		MessageBox MB_OK|MB_ICONSTOP "Can't update this version, use the full installer" 0 0
		Abort
	${EndIf}
FunctionEnd

Function WelcomeLeave
	StrCpy $R1 0
FunctionEnd

Function LockedListShow
	StrCmp $R1 0 +2 ; Skip the page if clicking Back from the next page.
		Abort
	!insertmacro MUI_HEADER_TEXT `UMS must be closed before installation` `Clicking Next will automatically close it and stop the service.`
	${If} ${RunningX64}
		File /oname=$PLUGINSDIR\LockedList64.dll "${NSISDIR}\Plugins\x86-unicode\LockedList64.dll"
	${EndIf}
	LockedList::AddModule "$INSTDIR\bin\MediaInfo.dll"
	LockedList::Dialog /autonext /autoclosesilent "" ""
	Pop $R0

	SimpleSC::ServiceIsRunning "${SERVICE_NAME}"
	Pop $0 ; returns an errorcode (<>0) otherwise success (0)
	Pop $1 ; returns 1 (service is running) - returns 0 (service is not running)
	${If} $1 == 1
		SimpleSC::StopService "${SERVICE_NAME}" 1 30
		Pop $2 ; returns an errorcode (<>0) otherwise success (0)
		IntCmp $2 0 success 0
			Push $0
			SimpleSC::GetErrorMessage
			Pop $0
			MessageBox MB_OK|MB_ICONSTOP 'Failed to stop service - Reason: $0' 0 0
			Abort
		success:
	${EndIf}
FunctionEnd

Function LockedListLeave
	StrCpy $R1 1
FunctionEnd

;Run program through explorer.exe to de-evaluate user from admin to regular one.
;http://mdb-blog.blogspot.ru/2013/01/nsis-lunch-program-as-user-from-uac.html
Function RunUMS
	SimpleSC::ExistsService "${SERVICE_NAME}"
	Pop $0
	${If} $0 == 0
		SimpleSC::StartService "${SERVICE_NAME}" "" 30
		Pop $1
		IntCmp $1 0 success 0
			; If we failed to start the service it might be disabled, so we start the GUI
			Exec '"$WINDIR\explorer.exe" "$INSTDIR\UMS.exe"'
		success:
	${Else}
		Exec '"$WINDIR\explorer.exe" "$INSTDIR\UMS.exe"'
	${EndIf}
FunctionEnd 

Section "Program Files"
	SetOutPath "$INSTDIR"
	SetOverwrite on

	RMDir /R /REBOOTOK "$INSTDIR\web\react-client\static"
	File /r "${PROJECT_BASEDIR}\src\main\external-resources\documentation"
	File /r "${PROJECT_BASEDIR}\src\main\external-resources\renderers"
	File /r "${PROJECT_BASEDIR}\src\main\external-resources\web"
	File "${PROJECT_BASEDIR}\src\main\external-resources\UMS.bat"

	File "${PROJECT_BUILD_DIR}\UMS.exe"
	File "${PROJECT_BUILD_DIR}\ums.jar"
	File "${PROJECT_BASEDIR}\CHANGELOG.md"
	File "${PROJECT_BASEDIR}\README.md"
	File "${PROJECT_BASEDIR}\LICENSE.txt"
	File "${PROJECT_BASEDIR}\src\main\external-resources\logback.xml"
	File "${PROJECT_BASEDIR}\src\main\external-resources\logback.headless.xml"
	File "${PROJECT_BASEDIR}\src\main\external-resources\icon.ico"
	File "${PROJECT_BASEDIR}\src\main\external-resources\DummyInput.ass"
	File "${PROJECT_BASEDIR}\src\main\external-resources\DummyInput.jpg"

	; The user may have set the installation dir as the profile dir, so we can't clobber this
	SetOverwrite off
	File "${PROJECT_BASEDIR}\src\main\external-resources\UMS.conf"
	File "${PROJECT_BASEDIR}\src\main\external-resources\ffmpeg.webfilters"

	WriteRegStr SHCTX "${REG_KEY_UNINSTALL}" "DisplayVersion" "${PROJECT_VERSION}"
SectionEnd
