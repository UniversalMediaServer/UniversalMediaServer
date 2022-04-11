(*
 * Digital Media Server, for streaming digital media to UPnP AV or DLNA
 * compatible devices based on PS3 Media Server and Universal Media Server.
 * Copyright (C) 2016 Digital Media Server developers.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see http://www.gnu.org/licenses/.
 *)
program CtrlSender;

{
  This program has been made specifically to be called from Java. To simplify
  its use, it produces no output whatsoever. The only feedback is through
  the exit code, which means the following:

    0 - Successfully sent Ctrl code
    1 - Could not attach to PID's console
    2 - Illegal arguments

  The arguments are:

    ControlSender <PID> <CtrlCode>

  Both arguments are expected to be integers, the first the PID of the process
  whose console to attach to and the second the Ctrl code to send.

  Valid Ctrl codes are:

    0 - CTRL_C_EVENT
    1 - CTRL_BREAK_EVENT
}

{$mode objfpc}{$H+}

uses
  {$IFDEF UNIX}{$IFDEF UseCThreads}
  cthreads,
  {$ENDIF}{$ENDIF}
  Classes, SysUtils, CustApp;

type

  { TCtrlSender }

  TCtrlSender = class(TCustomApplication)
  protected
    procedure DoRun; override;
  public
    constructor Create(TheOwner: TComponent); override;
    destructor Destroy; override;
  end;

{ TCtrlSender }

function FreeConsole:longbool; stdcall; external 'kernel32' name 'FreeConsole';
function AttachConsole(dwProcessId:DWord):longbool; stdcall; external 'kernel32' name 'AttachConsole';
function SetConsoleCtrlHandler(HandlerRoutine:Pointer; Add:LongBool):longbool; stdcall; external 'kernel32' name 'SetConsoleCtrlHandler';
function GenerateConsoleCtrlEvent(dwCtrlEvent:DWord; dwProcessGroupId:DWord):longbool; stdcall; external 'kernel32' name 'GenerateConsoleCtrlEvent';


procedure TCtrlSender.DoRun;
var
  PID: DWord;
  Code: DWord;

begin
  if ParamCount <> 2 then begin
    Terminate(2);
    Exit;
  end;
  try
    PID := StrToDWord(ParamStr(1));
    Code := StrToDWord(ParamStr(2));
  except
    on E: EConvertError do begin
      Terminate(2);
      Exit;
    end;
  end;

  FreeConsole;
  if AttachConsole(PID) then begin
    SetConsoleCtrlHandler(NIL, True);
    GenerateConsoleCtrlEvent(Code, 0);
    Terminate(0);
    Exit;
  end;
  Terminate(1);
  Exit;
end;

constructor TCtrlSender.Create(TheOwner: TComponent);
begin
  inherited Create(TheOwner);
  StopOnException:=False;
end;

destructor TCtrlSender.Destroy;
begin
  inherited Destroy;
end;

var
  Application: TCtrlSender;

{$R *.res}

begin
  Application:=TCtrlSender.Create(nil);
  Application.Run;
  Application.Free;
end.
