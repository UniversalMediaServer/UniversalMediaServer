; Java Launcher with automatic JRE installation
; Modified to include auto-update functionality
;-----------------------------------------------

Name "${PROJECT_NAME_SHORT}"
Caption "${PROJECT_NAME}"
Icon "${PROJECT_BASEDIR}\src\main\external-resources\icon.ico"

VIAddVersionKey "ProductName" "${PROJECT_NAME}"
VIAddVersionKey "Comments" ""
VIAddVersionKey "CompanyName" "${PROJECT_ORGANIZATION_NAME}"
VIAddVersionKey "LegalTrademarks" ""
VIAddVersionKey "LegalCopyright" ""
VIAddVersionKey "FileDescription" "${PROJECT_NAME}"
VIAddVersionKey "FileVersion" "${PROJECT_VERSION}"
VIProductVersion "${PROJECT_VERSION_SHORT}.0"

!define PRODUCT_NAME "${PROJECT_NAME_SHORT}"
!define REG_KEY_SOFTWARE "SOFTWARE\${PROJECT_NAME}"

; use javaw.exe to avoid dosbox.
; use java.exe to keep stdout/stderr
!define JAVAEXE "javaw.exe"

ManifestDPIAware true
RequestExecutionLevel user
SilentInstall silent
AutoCloseWindow true
ShowInstDetails nevershow

!include "FileFunc.nsh"
!insertmacro GetFileVersion
!insertmacro GetParameters
!include "WordFunc.nsh"
!insertmacro VersionCompare
!include "x64.nsh"

Var HEAPMEM

Section ""
	Pop $R0

	; first check portable heapmem value.
	ClearErrors
	FileOpen $1 "$EXEDIR\portable\data\heapmem.conf" r
	IfErrors filenotfound
	FileRead $1 $HEAPMEM
	FileClose $1
	filenotfound:

	; next check user registry heapmem value.
	${If} $HEAPMEM == ""  ; no value found
		ReadRegStr $HEAPMEM HKCU "${REG_KEY_SOFTWARE}" "HeapMem"
	${EndIf}

	; next check system registry heapmem value.
	${If} $HEAPMEM == ""  ; no value found
		ReadRegStr $HEAPMEM HKLM "${REG_KEY_SOFTWARE}" "HeapMem"
	${EndIf}

	; finally set default heapmem value.
	${If} $HEAPMEM == ""  ; no value found
		StrCpy $HEAPMEM "1280"
	${EndIf}

	; sanitize number.
	IntOp $HEAPMEM $HEAPMEM + 1
	IntOp $HEAPMEM $HEAPMEM - 1
	${If} $HEAPMEM == "0"  ; wrong value found
		StrCpy $HEAPMEM "1280"
	${EndIf}

	StrCpy $HEAPMEM "-Xmx$HEAPMEMM"

	; Change for your purpose (-jar etc.)
	${GetParameters} $1

	StrCpy $R0 "jre${PROJECT_JRE_VERSION}\bin\${JAVAEXE}"

	StrCpy $0 '"$R0" -classpath update.jar;${PROJECT_JARFILE} $HEAPMEM -Djava.net.preferIPv4Stack=true -Dfile.encoding=${PROJECT_ENCODING} ${PROJECT_MAIN_CLASS} $1'
	SetOutPath $EXEDIR
	Exec $0
SectionEnd
