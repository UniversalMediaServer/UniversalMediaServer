import { Context, createContext } from "react";

export const sessionContext: Context<UmsSession> = createContext({} as UmsSession);

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

export interface UmsGroupPermissions {
  value : number,
}

export interface UmsGroup {
  id : number,
  displayName : string,
  permissions? : UmsGroupPermissions,
}

export interface UmsAccount {
  user : UmsUser,
  group : UmsGroup,
}

export interface UmsSession {
  account? : UmsAccount,
  authenticate : boolean,
  initialized: boolean,
  refresh: () => void
}
