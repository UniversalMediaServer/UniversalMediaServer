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

; use javaw.exe to avoid dosbox.
; use java.exe to keep stdout/stderr
!define JAVAEXE "javaw.exe"

ManifestDPIAware true
RequestExecutionLevel user
SilentInstall silent
AutoCloseWindow true
ShowInstDetails nevershow

!include "FileFunc.nsh"
!insertmacro GetParameters

Section ""
	Pop $R0

	StrCpy $R0 "jre${PROJECT_JRE_VERSION}\bin\${JAVAEXE}"
	; Change for your purpose (-jar, -Xmx etc.)
	${GetParameters} $1

	StrCpy $0 '"$R0" -classpath update.jar;${PROJECT_JARFILE} -Djava.net.preferIPv4Stack=true -Dfile.encoding=${PROJECT_ENCODING} ${PROJECT_MAIN_CLASS} $1'
	SetOutPath $EXEDIR
	Exec $0
SectionEnd
