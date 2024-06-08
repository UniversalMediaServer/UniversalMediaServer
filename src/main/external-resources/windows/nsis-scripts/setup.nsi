!include "MUI.nsh"
!include "MUI2.nsh"
!include "FileFunc.nsh"
!include "TextFunc.nsh"
!include "WordFunc.nsh"
!include "LogicLib.nsh"
!include "nsDialogs.nsh"
!include "x64.nsh"
!include "GetWindowsVersion.nsh"

!define REG_KEY_UNINSTALL "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PROJECT_NAME}"
!define REG_KEY_SOFTWARE "SOFTWARE\${PROJECT_NAME}"
!define SERVICE_NAME "UniversalMediaServer"
!define OLD_SERVICE_NAME "Universal Media Server"

ManifestDPIAware true
RequestExecutionLevel admin

Name "${PROJECT_NAME}"
InstallDir "$PROGRAMFILES\${PROJECT_NAME}"

; Get install folder from registry for updates
InstallDirRegKey HKCU "${REG_KEY_SOFTWARE}" ""

SetCompressor /SOLID lzma
SetCompressorDictSize 32

!define MUI_ABORTWARNING
!define MUI_FINISHPAGE_RUN
!define MUI_FINISHPAGE_RUN_FUNCTION RunUMS
!define MUI_ICON "${PROJECT_BASEDIR}\src\main\external-resources\icon.ico"
!define MUI_WELCOMEFINISHPAGE_BITMAP "${NSISDIR}\Contrib\Graphics\Wizard\ums.bmp"
!define MUI_PAGE_CUSTOMFUNCTION_LEAVE WelcomeLeave

!define MUI_FINISHPAGE_SHOWREADME ""
!define MUI_FINISHPAGE_SHOWREADME_NOTCHECKED
!define MUI_FINISHPAGE_SHOWREADME_TEXT "Create Desktop Shortcut"
!define MUI_FINISHPAGE_SHOWREADME_FUNCTION CreateDesktopShortcut

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
Page Custom LockedListShow LockedListLeave
Page Custom AdvancedSettings AdvancedSettingsAfterwards ; Custom page
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_LANGUAGE "English"

ShowUninstDetails show

Function WelcomeLeave
	StrCpy $R1 0
FunctionEnd

Function LockedListShow
	StrCmp $R1 0 +2 ; Skip the page if clicking Back from the next page.
		Abort
	!insertmacro MUI_HEADER_TEXT `UMS must be closed before installation` `Clicking Next will automatically close it and stop the service.`

	${If} ${RunningX64}
		File /oname=$PLUGINSDIR\LockedList64.dll `${NSISDIR}\Plugins\x86-unicode\LockedList64.dll`
		; LockedList old MediaInfo locations
		LockedList::AddModule "$INSTDIR\MediaInfo64.dll"
		LockedList::AddModule "$INSTDIR\win32\MediaInfo64.dll"
	${Else}
		; LockedList old MediaInfo locations
		LockedList::AddModule "$INSTDIR\MediaInfo.dll"
		LockedList::AddModule "$INSTDIR\win32\MediaInfo.dll"
	${EndIf}
	LockedList::AddModule "$INSTDIR\bin\MediaInfo.dll"

	LockedList::Dialog /autonext /autoclosesilent
	Pop $R0

	SimpleSC::ServiceIsRunning "${OLD_SERVICE_NAME}"
	Pop $0 ; returns an errorcode (<>0) otherwise success (0)
	Pop $1 ; returns 1 (service is running) - returns 0 (service is not running)
	${If} $1 == 1
		SimpleSC::StopService "${OLD_SERVICE_NAME}" 1 30
		Pop $2 ; returns an errorcode (<>0) otherwise success (0)
		IntCmp $2 0 osuccess 0
			Push $0
			SimpleSC::GetErrorMessage
			Pop $0
			MessageBox MB_OK|MB_ICONSTOP 'Failed to stop service - Reason: $0' 0 0
			Abort
		osuccess:
	${EndIf}

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

Var Dialog
Var Text
Var LabelMemoryLimit
Var DescMemoryLimit
Var CheckboxCleanInstall
Var CheckboxCleanInstallState
Var DescCleanInstall
Var MaximumMemoryJava

Function AdvancedSettings
	!insertmacro MUI_HEADER_TEXT "Advanced Settings" "If you don't understand them, don't change them."
	nsDialogs::Create 1018
	Pop $Dialog

	${If} $Dialog == error
		Abort
	${EndIf}

	; Choose maximum memory limit based on java type installed
	ClearErrors
	${If} ${RunningX64}
		SetRegView 64
	${EndIf}

	ReadRegStr $0 HKCU "${REG_KEY_SOFTWARE}" "HeapMem"
	${If} $0 == ""
		; Get the amount of RAM on the computer
		System::Alloc 64
		Pop $1
		System::Call "*$1(i64)"
		System::Call "Kernel32::GlobalMemoryStatusEx(i r1)"
		System::Call "*$1(i.r2, i.r3, l.r4, l.r5, l.r6, l.r7, l.r8, l.r9, l.r10)"
		System::Free $1
		System::Int64Op $4 / 1048576
		Pop $4

		; Choose the maximum amount of RAM we want to use based on installed RAM
		${If} $4 > 16000 
			StrCpy $MaximumMemoryJava "4096"
		${ElseIf} $4 > 8000 
			StrCpy $MaximumMemoryJava "2048"
		${ElseIf} $4 > 4000 
			StrCpy $MaximumMemoryJava "1280"
		${Else}
			StrCpy $MaximumMemoryJava "768"
		${EndIf}
	${Else}
		StrCpy $MaximumMemoryJava $0
	${EndIf}

	${NSD_CreateLabel} 0 0 100% 20u "This allows you to set the Java Heap size limit. The default value is recommended." 
	Pop $DescMemoryLimit

	${NSD_CreateLabel} 2% 20% 37% 12u "Maximum memory in megabytes"
	Pop $LabelMemoryLimit

	${NSD_CreateText} 3% 30% 10% 12u $MaximumMemoryJava
	Pop $Text

	${NSD_CreateLabel} 0 50% 100% 20u "This allows you to take advantage of improved defaults. It deletes the UMS configuration directory, the UMS program directory and font cache."
	Pop $DescCleanInstall

	${NSD_CreateCheckbox} 3% 65% 100% 12u "Clean install"
	Pop $CheckboxCleanInstall

	nsDialogs::Show
FunctionEnd

Function AdvancedSettingsAfterwards
	${NSD_GetText} $Text $0
	WriteRegStr HKCU "${REG_KEY_SOFTWARE}" "HeapMem" "$0"

	${NSD_GetState} $CheckboxCleanInstall $CheckboxCleanInstallState
	${If} $CheckboxCleanInstallState == ${BST_CHECKED}
		ReadENVStr $R1 ALLUSERSPROFILE
		RMDir /r $R1\UMS
		RMDir /r $TEMP\fontconfig
		RMDir /r $LOCALAPPDATA\fontconfig
		RMDir /r $INSTDIR
		DeleteRegValue HKCU "${REG_KEY_SOFTWARE}" "BinaryRevision"
	${EndIf}
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

Function CreateDesktopShortcut
	CreateShortCut "$DESKTOP\${PROJECT_NAME}.lnk" "$INSTDIR\UMS.exe"
FunctionEnd

Section "Program Files"
	SetOutPath "$INSTDIR"
	SetOverwrite on

	File /r "${PROJECT_BASEDIR}\src\main\external-resources\documentation"
	File /r "${PROJECT_BASEDIR}\src\main\external-resources\renderers"

	RMDir /R /REBOOTOK "$INSTDIR\web\react-client\static"

	File "${PROJECT_BUILD_DIR}\UMS.exe"
	File "${PROJECT_BASEDIR}\src\main\external-resources\UMS.bat"
	File /r "${PROJECT_BASEDIR}\src\main\external-resources\web"
	File "${PROJECT_BUILD_DIR}\ums.jar"

	File "${PROJECT_BASEDIR}\CHANGELOG.md"
	File "${PROJECT_BASEDIR}\README.md"
	File "${PROJECT_BASEDIR}\LICENSE.txt"
	File "${PROJECT_BASEDIR}\src\main\external-resources\logback.xml"
	File "${PROJECT_BASEDIR}\src\main\external-resources\logback.headless.xml"
	File "${PROJECT_BASEDIR}\src\main\external-resources\icon.ico"
	File "${PROJECT_BASEDIR}\src\main\external-resources\DummyInput.ass"
	File "${PROJECT_BASEDIR}\src\main\external-resources\DummyInput.jpg"

	RMDir /R /REBOOTOK "$INSTDIR\jre${PROJECT_JRE_VERSION}"
	${If} ${RunningX64}
		File /r "${PROJECT_BUILD_DIR}\bin\windows\x86_64\jre${PROJECT_JRE_VERSION}"
	${Else}
		File /r "${PROJECT_BUILD_DIR}\bin\windows\x86\jre${PROJECT_JRE_VERSION}"
	${EndIf}

	SetOutPath "$INSTDIR\bin"
	File /r /x "x86" /x "x86_64" /x "winxp" "${PROJECT_BUILD_DIR}\bin\windows\*.*"
	${If} ${RunningX64}
		File /r /x "jre${PROJECT_JRE_VERSION}" "${PROJECT_BUILD_DIR}\bin\windows\x86_64\*.*"
	${Else}
		File /r /x "jre${PROJECT_JRE_VERSION}" "${PROJECT_BUILD_DIR}\bin\windows\x86\*.*"
	${EndIf}

	${GetWindowsVersion} $R0
	${If} $R0 == "XP"
		File /r "${PROJECT_BUILD_DIR}\bin\windows\winxp"
	${EndIf}
	WriteRegStr HKCU "${REG_KEY_SOFTWARE}" "BinaryRevision" "${PROJECT_BINARY_REVISION}"

	; The user may have set the installation dir as the profile dir, so we can't clobber this
	SetOutPath "$INSTDIR"
	SetOverwrite off
	File "${PROJECT_BASEDIR}\src\main\external-resources\UMS.conf"
	File "${PROJECT_BASEDIR}\src\main\external-resources\ffmpeg.webfilters"

	; Remove old renderer files to prevent conflicts
	Delete /REBOOTOK "$INSTDIR\renderers\AirPlayer.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Android.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\AndroidChromecast.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\BlackBerryPlayBook-KalemSoftMP.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Bravia4500.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Bravia5500.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\BraviaBX305.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\BraviaEX.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\BraviaEX620.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\BraviaHX.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\BraviaW.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\BraviaXBR.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\CambridgeAudioAzur752BD.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\DirecTVHR.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Dlink510.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\DLinkDSM510.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\FreeboxHD.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\FreecomMusicPal.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\iPad-iPhone.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Kuro.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\LG-42LA644V.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\LGST600.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\N900.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\NetgearNeoTV.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\OnkyoTX-NR717.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\OPPOBDP83.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\OPPOBDP93.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-DMRBWT740.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-SC-BTT.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-TH-P-U30Z.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\PanasonicTX-L32V10E.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-VT60.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Philips.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\PhilipsPFL.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\PS3.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Roku-Roku3.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SamsungAllShare.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SamsungAllShare-CD.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SamsungAllShare-D7000.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SamsungMobile.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-HT-E3.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-SMT-G7400.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-UE-ES6575.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SamsungWiseLink.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SharpAquos.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SMP-N100.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SonyBluray.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SonyHomeTheatreSystem.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SonySTR-5800ES.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SonyXperia.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Streamium.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\TelstraTbox.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\VideoWebTV.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\VizioSmartTV.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\WDTVLive.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\WMP.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\XBOX360.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\XboxOne.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\YamahaRXA1010.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\YamahaRXV671.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\YamahaRXV3900.conf"

	; Remove old folders
	RMDir /R /REBOOTOK "$INSTDIR\jre"
	RMDir /R /REBOOTOK "$INSTDIR\jre-x64"
	RMDir /R /REBOOTOK "$INSTDIR\jre-x86"
	RMDir /R /REBOOTOK "$INSTDIR\jre8"
	RMDir /R /REBOOTOK "$INSTDIR\jre14"
	RMDir /R /REBOOTOK "$INSTDIR\jre14-x64"
	RMDir /R /REBOOTOK "$INSTDIR\jre14-x86"
	RMDir /R /REBOOTOK "$INSTDIR\jre15"
	RMDir /R /REBOOTOK "$INSTDIR\win32"

	; remove old service
	SimpleSC::ExistsService "${OLD_SERVICE_NAME}"
	Pop $0
	${If} $0 == 0
		SimpleSC::RemoveService "${OLD_SERVICE_NAME}"
		Pop $1
		StrCmp $1 0 osuccess 0
		DeleteRegKey HKLM "SYSTEM\CurrentControlSet\Services\${OLD_SERVICE_NAME}"
		osuccess:
	${EndIf}

	; Delete old MediaInfo files
	Delete /REBOOTOK "$INSTDIR\MediaInfo.dll"
	Delete /REBOOTOK "$INSTDIR\MediaInfo64.dll"
	Delete /REBOOTOK "$INSTDIR\MediaInfo-License.html"

	; Delete old changelog file
	Delete /REBOOTOK "$INSTDIR\CHANGELOG.txt"

	; Store install folder
	WriteRegStr HKCU "${REG_KEY_SOFTWARE}" "" $INSTDIR

	; Create uninstaller
	WriteUninstaller "$INSTDIR\uninst.exe"

	WriteRegStr HKEY_LOCAL_MACHINE "${REG_KEY_UNINSTALL}" "DisplayName" "${PROJECT_NAME}"
	WriteRegStr HKEY_LOCAL_MACHINE "${REG_KEY_UNINSTALL}" "DisplayIcon" "$INSTDIR\icon.ico"
	WriteRegStr HKEY_LOCAL_MACHINE "${REG_KEY_UNINSTALL}" "DisplayVersion" "${PROJECT_VERSION}"
	WriteRegStr HKEY_LOCAL_MACHINE "${REG_KEY_UNINSTALL}" "Publisher" "${PROJECT_ORGANIZATION_NAME}"
	WriteRegStr HKEY_LOCAL_MACHINE "${REG_KEY_UNINSTALL}" "URLInfoAbout" "${PROJECT_ORGANIZATION_URL}"
	WriteRegStr HKEY_LOCAL_MACHINE "${REG_KEY_UNINSTALL}" "UninstallString" '"$INSTDIR\uninst.exe"'

	${GetSize} "$INSTDIR" "/S=0K" $0 $1 $2
	IntFmt $0 "0x%08X" $0
	WriteRegDWORD HKLM "${REG_KEY_UNINSTALL}" "EstimatedSize" "$0"

	WriteUnInstaller "uninst.exe"

	ReadENVStr $R0 ALLUSERSPROFILE
	SetOutPath "$R0\UMS"

	CreateDirectory "$R0\UMS\data"

	AccessControl::GrantOnFile "$R0\UMS" "(S-1-5-32-545)" "FullAccess"
	File "${PROJECT_BASEDIR}\src\main\external-resources\UMS.conf"
	File "${PROJECT_BASEDIR}\src\main\external-resources\ffmpeg.webfilters"

	; Remove existing firewall rules in case they need updating
	ExecWait 'netsh advfirewall firewall delete rule name=UMS'
	ExecWait 'netsh advfirewall firewall delete rule name="UMS Service"'

	; Add firewall rules
	ExecWait 'netsh advfirewall firewall add rule name="UMS Service" dir=in action=allow program="$INSTDIR\jre${PROJECT_JRE_VERSION}\bin\java.exe" enable=yes profile=public,private'
	ExecWait 'netsh advfirewall firewall add rule name=UMS dir=in action=allow program="$INSTDIR\jre${PROJECT_JRE_VERSION}\bin\javaw.exe" enable=yes profile=public,private'
SectionEnd

Section "Start Menu Shortcuts"
	SetShellVarContext all
	CreateDirectory "$SMPROGRAMS\${PROJECT_NAME}"
	CreateShortCut "$SMPROGRAMS\${PROJECT_NAME}\${PROJECT_NAME}.lnk" "$INSTDIR\UMS.exe" "" "$INSTDIR\UMS.exe" 0
	CreateShortCut "$SMPROGRAMS\${PROJECT_NAME}\${PROJECT_NAME} (Select Profile).lnk" "$INSTDIR\UMS.exe" "profiles" "$INSTDIR\UMS.exe" 0
	CreateShortCut "$SMPROGRAMS\${PROJECT_NAME}\Uninstall.lnk" "$INSTDIR\uninst.exe" "" "$INSTDIR\uninst.exe" 0

	SimpleSC::ExistsService "${SERVICE_NAME}"
	Pop $0
	${If} $0 != 0
		; Only start UMS with Windows when it is a new install
		IfFileExists "$SMPROGRAMS\${PROJECT_NAME}.lnk" 0 shortcut_file_not_found
			goto end_of_startup_section
		shortcut_file_not_found:
			CreateShortCut "$SMSTARTUP\${PROJECT_NAME}.lnk" "$INSTDIR\UMS.exe" "" "$INSTDIR\UMS.exe" 0
		end_of_startup_section:
	${EndIf}

	CreateShortCut "$SMPROGRAMS\${PROJECT_NAME}.lnk" "$INSTDIR\UMS.exe" "" "$INSTDIR\UMS.exe" 0
SectionEnd

Section "Uninstall"
	SetShellVarContext all

	; Current files/folders
	Delete /REBOOTOK "$INSTDIR\uninst.exe"
	RMDir /R /REBOOTOK "$INSTDIR\plugins"
	RMDir /R /REBOOTOK "$INSTDIR\documentation"
	RMDir /R /REBOOTOK "$INSTDIR\data"
	RMDir /R /REBOOTOK "$INSTDIR\jre${PROJECT_JRE_VERSION}"
	RMDir /R /REBOOTOK "$INSTDIR\web"
	RMDir /R /REBOOTOK "$INSTDIR\bin"

	; Old folders (maybe do this on install/upgrade ?)
	RMDir /R /REBOOTOK "$INSTDIR\jre17"

	; Current renderer files
	Delete /REBOOTOK "$INSTDIR\renderers\AnyCast.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Apple-TV-VLC.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Apple-TV-4K-VLC.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Apple-iDevice-AirPlayer.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Apple-iDevice-VLC.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Apple-iDevice-VLC32bit.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Apple-iDevice.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\BlackBerry-PlayBook-KalemSoftMP.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\CambridgeAudio-AzurBD.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\DefaultRenderer.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\DirecTV.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\DLink-DSM510.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\FetchTV.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Free-Freebox.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\foobar2000-mobile.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Freecom-MusicPal.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Google-Android-Chromecast.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Google-Android.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Google-Android-BubbleUPnP-MXPlayer.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Google-ChromecastUltra.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Hama-IR320.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Hisense-K680.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Kodi.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\LG-BP.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\LG-BP550.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\LG-EG910V.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\LG-LA6200.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\LG-LA644V.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\LG-LB.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\LG-LM620.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\LG-LM660.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\LG-LS5700.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\LG-ST600.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\LG-UB820V.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\LG-UH770.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\LG-WebOS.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Logitech-Squeezebox.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Microsoft-WindowsMediaPlayer.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Microsoft-Xbox360.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Microsoft-XboxOne.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Miracast-M806.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Mirascreen.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Naim-Mu-So.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Netgear-NeoTV.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Netgem-N7700.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Nokia-N900.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\OPPO-BDP.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\OPPO-BDP83.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Onkyo-TXNR7xx.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Onkyo-TXNR8xx.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-DMPBDT.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-DMPBDT220.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-DMPBDT360.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-DMR.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-SCBTT.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-Viera.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-VieraAS600E.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-VieraAS650.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-VieraCX680.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-VieraCX700.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-VieraDX.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-VieraE6.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-VieraET60.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-VieraGT50.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-VieraGX800B.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-VieraS60.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-VieraST60.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-VieraTHPU30Z.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-VieraTXL32V10E.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-VieraVT60.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Philips-AureaAndNetTV.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Philips-PFL.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Philips-PUS.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Philips-PUS-6500Series.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Philips-Streamium.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Pioneer-BDP.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Pioneer-Kuro.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\PopcornHour.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\README.txt"
	Delete /REBOOTOK "$INSTDIR\renderers\Realtek.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Roku-Roku3-3.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Roku-Roku3-5.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Roku-Roku3-6-7.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Roku-DVP10.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Roku-TV.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Roku-TV-4K.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Roku-TV8.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-8series.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-9series.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-BDC6800.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-BDH6500.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-C6600.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-CD.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-D6400.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-D7000.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-EH5300.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-EH6070.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-ES6100.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-ES6575.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-ES8000.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-ES8005.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-F5100.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-F5505.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-F5900.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-GalaxyS5.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-GalaxyS7.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-H4500.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-H6203.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-H6400.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-HTE3.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-HTF4.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-HU7000.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-HU9000.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-JU6400.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-J55xx.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-J6200.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-JU6400.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-Mobile.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-NotCD.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-PL51E490.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-SMTG7400.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-Soundbar.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-Soundbar-MS750.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-TV-2021-0.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-TV-2021-1.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-TV-2021-2.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-TV-2021-3.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-TV-2021-4.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-WiseLink.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-UHD.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-UHD-2019.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-UHD-2019-8K.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sharp-Aquos.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Showtime3.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Showtime4.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-Bluray.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-Bluray2013.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-Bluray-UBP-X800M2.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-Bravia4500.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-Bravia5500.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-BraviaBX305.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-BraviaEX.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-BraviaEX620.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-BraviaEX725.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-BraviaHX.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-BraviaHX75.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-BraviaNX70x.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-BraviaNX800.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-BraviaW.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-BraviaX.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-BraviaXBR.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-BraviaXBR-OLED.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-BraviaXD.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-HomeTheatreSystem.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-PlayStation3.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-PlayStation4.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-PlayStationVita.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-SMPN100.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-STR5800ES.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-X-Series-TV.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-Xperia.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Sony-XperiaZ3.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Technisat-S1Plus.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Telefunken-TV.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Telstra-Tbox.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Thomson-U3.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\VLC-for-desktop.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\VideoWeb-VideoWebTV.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Vizio-SmartTV.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\WesternDigital-WDTVLive.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\XBMC.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Yamaha-RN500.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Yamaha-RXA1010.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Yamaha-RXA2050.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Yamaha-RXV3900.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Yamaha-RXV500D.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Yamaha-RXV671.conf"

	; Old renderer files (should have already been removed during install/upgrade)
	Delete /REBOOTOK "$INSTDIR\renderers\AirPlayer.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Android.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\AndroidChromecast.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\BlackBerryPlayBook-KalemSoftMP.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Bravia4500.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Bravia5500.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\BraviaBX305.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\BraviaEX.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\BraviaEX620.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\BraviaHX.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\BraviaW.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\BraviaXBR.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\CambridgeAudioAzur752BD.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\DirecTVHR.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Dlink510.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\DLinkDSM510.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\FreeboxHD.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\FreecomMusicPal.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\iPad-iPhone.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Kuro.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\LG-42LA644V.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\LGST600.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\N900.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\NetgearNeoTV.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\OnkyoTX-NR717.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\OPPOBDP83.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\OPPOBDP93.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-DMRBWT740.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-SC-BTT.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-TH-P-U30Z.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\PanasonicTX-L32V10E.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Panasonic-VT60.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Philips.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\PhilipsPFL.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\PS3.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Roku-Roku3.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SamsungAllShare.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SamsungAllShare-CD.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SamsungAllShare-D7000.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SamsungMobile.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-HT-E3.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-SMT-G7400.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Samsung-UE-ES6575.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SamsungWiseLink.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SharpAquos.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SMP-N100.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SonyBluray.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SonyHomeTheatreSystem.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SonySTR-5800ES.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\SonyXperia.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\Streamium.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\TelstraTbox.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\VideoWebTV.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\VizioSmartTV.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\WDTVLive.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\WMP.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\XBOX360.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\XboxOne.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\YamahaRXA1010.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\YamahaRXV671.conf"
	Delete /REBOOTOK "$INSTDIR\renderers\YamahaRXV3900.conf"

	RMDir /REBOOTOK "$INSTDIR\renderers"
	Delete /REBOOTOK "$INSTDIR\UMS.exe"
	Delete /REBOOTOK "$INSTDIR\UMS.bat"
	Delete /REBOOTOK "$INSTDIR\ums.jar"

	; Old MediaInfo files
	${If} ${RunningX64}
		Delete /REBOOTOK "$INSTDIR\MediaInfo64.dll"
	${Else}
		Delete /REBOOTOK "$INSTDIR\MediaInfo.dll"
	${EndIf}
	Delete /REBOOTOK "$INSTDIR\MediaInfo-License.html"

	Delete /REBOOTOK "$INSTDIR\CHANGELOG.md"
	Delete /REBOOTOK "$INSTDIR\CHANGELOG.txt"
	Delete /REBOOTOK "$INSTDIR\WEB.conf"
	Delete /REBOOTOK "$INSTDIR\README.md"
	Delete /REBOOTOK "$INSTDIR\README.txt"
	Delete /REBOOTOK "$INSTDIR\LICENSE.txt"
	Delete /REBOOTOK "$INSTDIR\debug.log"
	Delete /REBOOTOK "$INSTDIR\debug.log.prev"
	Delete /REBOOTOK "$INSTDIR\ffmpeg.webfilters"
	Delete /REBOOTOK "$INSTDIR\logback.xml"
	Delete /REBOOTOK "$INSTDIR\logback.headless.xml"
	Delete /REBOOTOK "$INSTDIR\icon.ico"
	Delete /REBOOTOK "$INSTDIR\DummyInput.ass"
	Delete /REBOOTOK "$INSTDIR\DummyInput.jpg"
	Delete /REBOOTOK "$INSTDIR\new-version.exe"
	Delete /REBOOTOK "$INSTDIR\pms.pid"
	Delete /REBOOTOK "$INSTDIR\UMS.conf"
	Delete /REBOOTOK "$INSTDIR\VirtualFolders.conf"
	RMDir /REBOOTOK "$INSTDIR"

	Delete /REBOOTOK "$DESKTOP\${PROJECT_NAME}.lnk"
	Delete /REBOOTOK "$SMSTARTUP\${PROJECT_NAME}.lnk"
	RMDir /REBOOTOK "$SMPROGRAMS\${PROJECT_NAME}"
	Delete /REBOOTOK "$SMPROGRAMS\${PROJECT_NAME}\${PROJECT_NAME}.lnk"
	Delete /REBOOTOK "$SMPROGRAMS\${PROJECT_NAME}\${PROJECT_NAME} (Select Profile).lnk"
	Delete /REBOOTOK "$SMPROGRAMS\${PROJECT_NAME}\Uninstall.lnk"

	DeleteRegKey HKEY_LOCAL_MACHINE "${REG_KEY_UNINSTALL}"
	DeleteRegKey HKCU "${REG_KEY_SOFTWARE}"

	SimpleSC::ExistsService "${SERVICE_NAME}"
	Pop $0
	${If} $0 != 0
		SimpleSC::StopService "${SERVICE_NAME}" 1 30
		SimpleSC::RemoveService "${SERVICE_NAME}"
		DeleteRegKey HKLM "SYSTEM\CurrentControlSet\Services\${SERVICE_NAME}"
	${EndIf}

	ExecWait 'netsh advfirewall firewall delete rule name=UMS'
	ExecWait 'netsh advfirewall firewall delete rule name="UMS Service"'

SectionEnd
