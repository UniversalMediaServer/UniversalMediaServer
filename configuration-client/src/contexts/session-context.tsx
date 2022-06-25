import { Context, createContext } from "react";

export const sessionContext: Context<UmsSession> = createContext({firstLogin:false} as UmsSession);

export default sessionContext;

export interface UmsUser {
  id : number,
  username : string,
  displayName : string,
  groupId : number,
  lastLoginTime : number,
  loginFailedTime : number,
  loginFailedCount : number,
}

export interface UmsGroup {
  id : number,
  displayName : string,
	permissions : string[],
}

export interface UmsAccount {
  user : UmsUser,
  group : UmsGroup,
}

export interface UmsSession {
  firstLogin : boolean,
	account? : UmsAccount,
  initialized: boolean,
}