; ---------------------
;       Util.nsh
; ---------------------
;
; Voodoo macros to make end-user usage easier. This may be documented someday.

!verbose push 3
!ifndef ___UTIL__NSH___
!define ___UTIL__NSH___

# CallArtificialFunction, see WinVer.nsh and *Func.nsh for usage examples
!macro CallArtificialFunctionHelper TYPE NAME
  !verbose pop
  Call :.${NAME}${TYPE}
  !ifndef ${NAME}${TYPE}_DEFINED
    !verbose push 2
    Goto ${NAME}${TYPE}_DONE
    !define ${NAME}${TYPE}_DEFINED
    !verbose pop
    .${NAME}${TYPE}:
      !insertmacro ${NAME}
      Return
    ${NAME}${TYPE}_DONE:
  !endif
  !verbose push 2
!macroend

!macro CallArtificialFunction NAME
  !verbose push 2
  !ifdef __UNINSTALL__
    !insertmacro CallArtificialFunctionHelper uninst ${NAME}
  !else
    !insertmacro CallArtificialFunctionHelper inst ${NAME}
  !endif
  !verbose pop
!macroend
!define CallArtificialFunction `!insertmacro CallArtificialFunction`

!macro CallArtificialFunction2 NAME ; Retained for v2.4x..v3.0b0 compatibility
  ${CallArtificialFunction} ${NAME}
!macroend
!define CallArtificialFunction2 `!insertmacro CallArtificialFunction`


!define Int32Op '!insertmacro Int32Op '
!define Int64Op '!insertmacro Int64Op '
!define IntPtrOp '!insertmacro IntPtrOp '
!macro Int32Op r a o b
!if ${NSIS_PTR_SIZE} <= 4
  IntOp `${r}` `${a}` `${o}` ${b}
!else
  !error "Int32Op not implemented"
!endif
!macroend
!macro Int64Op r a o b
!echo "Int64Op ${r}=${a}${o}${b}"
!verbose push 2
System::Int64Op `${a}` `${o}` ${b}
Pop ${r}
!verbose pop
!macroend
!macro IntPtrOp r a o b
!if ${NSIS_PTR_SIZE} <= 4
  ${Int32Op} `${r}` `${a}` `${o}` `${b}`
!else
  ${Int64Op} `${r}` `${a}` `${o}` `${b}`
!endif
!macroend

!define Int32Cmp '!insertmacro Int32Cmp '
!define Int64Cmp '!insertmacro Int64Cmp '
!define IntPtrCmp '!insertmacro IntPtrCmp '
!macro Int32Cmp a b jeek jles jgtr
!if ${NSIS_PTR_SIZE} <= 4
  IntCmp `${a}` `${b}` `${jeek}` `${jles}` `${jgtr}`
!else
  !error "Int32Cmp not implemented"
!endif
!macroend
!macro Int64Cmp a b jeek jles jgtr
!ifmacrondef _LOGICLIB_TEMP
!include LogicLib.nsh
!endif
!echo "Int64Cmp ${a}:${b} =${jeek}, <${jles}, >${jgtr}"
!verbose push 2
${IfThen} ${a} L= ${b} ${|} Goto ${jeek} ${|}
!insertmacro _L< ${a} ${b} `${jles}` `${jgtr}`
!verbose pop
!macroend
!macro IntPtrCmp a b jeek jles jgtr
!if ${NSIS_PTR_SIZE} <= 4
  ${Int32Cmp} `${a}` `${b}` `${jeek}` `${jles}` `${jgtr}`
!else
  ${Int64Cmp} `${a}` `${b}` `${jeek}` `${jles}` `${jgtr}`
!endif
!macroend

!define Int32CmpU '!insertmacro Int32CmpU '
!define Int64CmpU '!insertmacro Int64CmpU '
!define IntPtrCmpU '!insertmacro IntPtrCmpU '
!macro Int32CmpU a b jeek jles jgtr
!if ${NSIS_PTR_SIZE} <= 4
  IntCmpU `${a}` `${b}` `${jeek}` `${jles}` `${jgtr}`
!else
  !error "Int32CmpU not implemented"
!endif
!macroend
!macro Int64CmpUHelper
; This macro performs "$_LOGICLIB_TEMP = a < b ? -1 : a > b ? 1 : 0" but System::Int64Op does not support unsigned operations so we have to perform multiple steps
!ifmacrondef _LOGICLIB_TEMP
!include LogicLib.nsh
!endif
!insertmacro _LOGICLIB_TEMP
Exch $2 ; b
Exch
Exch $1 ; a
; if (a == b) return 0;
; if (a < 0)
; {
;   if (b >= 0) return 1
; }
; else
; {
;   if (b < 0) return -1
; }
; return a < b ? -1 : 1
System::Int64Op $1 ^ $2 ; Using xor so $_LOGICLIB_TEMP ends up as 0 when they are equal
Pop $_LOGICLIB_TEMP
StrCmp $_LOGICLIB_TEMP 0 ret ; NOTE: Must use StrCmp, IntCmp fails on "0x8000000000000001 Z> 1"
System::Int64Op $1 < 0
Pop $_LOGICLIB_TEMP
StrCmp $_LOGICLIB_TEMP 0 checkNegOther
System::Int64Op $2 < 0 ; System::Int64Op does not support the >= operator so we invert the operation
Pop $_LOGICLIB_TEMP
StrCmp $_LOGICLIB_TEMP 0 retPos finalCmp
retPos:
StrCpy $_LOGICLIB_TEMP "1"
Goto ret
checkNegOther:
System::Int64Op $2 < 0
Pop $_LOGICLIB_TEMP
StrCmp $_LOGICLIB_TEMP 0 finalCmp retNeg
retNeg:
StrCpy $_LOGICLIB_TEMP "-1"
Goto ret
finalCmp:
System::Int64Op $1 < $2
Pop $_LOGICLIB_TEMP
StrCmp $_LOGICLIB_TEMP 0 retPos retNeg
ret:
Pop $1
Pop $2
!macroend
!macro Int64CmpU a b jeek jles jgtr
!echo "Int64CmpU ${a}:${b} =${jeek}, <${jles}, >${jgtr}"
!verbose push 2
Push `${a}`
Push `${b}`
!insertmacro CallArtificialFunction Int64CmpUHelper
IntCmp $_LOGICLIB_TEMP 0 `${jeek}` `${jles}` `${jgtr}`
!verbose pop
!macroend
!macro IntPtrCmpU a b jeek jles jgtr
!if ${NSIS_PTR_SIZE} <= 4
  ${Int32CmpU} `${a}` `${b}` `${jeek}` `${jles}` `${jgtr}`
!else
  ${Int64CmpU} `${a}` `${b}` `${jeek}` `${jles}` `${jgtr}`
!endif
!macroend


!endif # !___UTIL__NSH___
!verbose pop
