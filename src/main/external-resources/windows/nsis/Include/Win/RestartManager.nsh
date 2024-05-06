!ifndef __WIN_RESTARTMANAGER__INC
!define __WIN_RESTARTMANAGER__INC 1

/**************************************************
WinBase.h
**************************************************/
!define /IfNDef RESTART_MAX_CMD_LINE 1024

!define /IfNDef RESTART_NO_CRASH  1
!define /IfNDef RESTART_NO_HANG   2
!define /IfNDef RESTART_NO_PATCH  4
!define /IfNDef RESTART_NO_REBOOT 8 ; Do not restart the process when the system is rebooted due to patch installations

!define /IfNDef RECOVERY_DEFAULT_PING_INTERVAL 5000
!define /IfNDef /math RECOVERY_MAX_PING_INTERVAL 5000 * 60


/**************************************************
RestartManager.h
**************************************************/
!define RM_SESSION_KEY_LEN 16
!define CCH_RM_SESSION_KEY 32
!define CCH_RM_MAX_APP_NAME 255
!define CCH_RM_MAX_SVC_NAME 63
!define RM_INVALID_TS_SESSION -1
!define RM_INVALID_PROCESS -1

!define RmUnknownApp  0
!define RmMainWindow  1
!define RmOtherWindow 2
!define RmService     3
!define RmExplorer    4
!define RmConsole     5
!define RmCritical 1000 ; Application is critical system process where a reboot is required to restart

!define RmForceShutdown           0x1 ; Force unresponsive applications and services to shut down after the timeout period
!define RmShutdownOnlyRegistered 0x10 ; Only shutdown apps if all apps registered for restart (RegisterApplicationRestart)

!define RmStatusUnknown           0
!define RmStatusRunning           1
!define RmStatusStopped           2 ; Application stopped by Restart Manager
!define RmStatusStoppedOther      4
!define RmStatusRestarted         8
!define RmStatusErrorOnStop    0x10
!define RmStatusErrorOnRestart 0x20
!define RmStatusShutdownMasked 0x40
!define RmStatusRestartMasked  0x80

!define RmRebootReasonNone             0
!define RmRebootReasonPermissionDenied 1
!define RmRebootReasonSessionMismatch  2
!define RmRebootReasonCriticalProcess  4
!define RmRebootReasonCriticalService  8
!define RmRebootReasonDetectedSelf  0x10


!define SYSSIZEOF_RM_UNIQUE_PROCESS 12
!define SYSSTRUCT_RM_UNIQUE_PROCESS (i,l)


!include LogicLib.nsh
!include Util.nsh


!macro RestartManager_StartSession outvarhandle
System::Call 'RSTRTMGR::RmStartSession(*i-1s, i0, w)i.s'
Pop ${outvarhandle}
${If} ${outvarhandle} <> 0
${OrIf} ${outvarhandle} == error
	Pop ${outvarhandle}
	Push ""
${EndIf}
Pop ${outvarhandle}
!macroend

!macro RestartManager_EndSession handle
System::Call 'RSTRTMGR::RmEndSession(i${handle})'
!macroend

!macro RestartManager_RegisterFile handle path
System::Call 'RSTRTMGR::RmRegisterResources(i${handle},i1,*ws,i0,p0,i0,p0)i.r0' "${path}"
!macroend

!macro RestartManager_Shutdown handle
System::Call 'RSTRTMGR::RmShutdown(i${handle}, i${RmForceShutdown}, p0)'
!macroend

!macro RestartManager_Restart handle
System::Call 'RSTRTMGR::RmRestart(i${handle}, i0, p0)'
!macroend


!macro RestartManager_ShutdownFile fullpath outvar_errcode
Push "${fullpath}"
!insertmacro CallArtificialFunction RestartManager_ShutdownFileImp
Pop ${outvar_errcode}
!macroend
!macro RestartManager_ShutdownFileImp
Exch $1
Push $0
System::Call 'RSTRTMGR::RmStartSession(*i-1s, i0, w)i.r0'
${If} $0 == error
	Pop $0
	StrCpy $0 1150
${Else}
	System::Call 'RSTRTMGR::RmRegisterResources(isr1,i1,*wr1,i0,p0,i0,p0)i.r0'
	${If} $0 = 0
		System::Call 'RSTRTMGR::RmShutdown(ir1, i${RmForceShutdown}, p0)i.r0'
	${EndIf}
	System::Call 'RSTRTMGR::RmEndSession(ir1)'
${EndIf}
Exch
Pop $1
Exch $0
!macroend

!endif ;~ Include guard
