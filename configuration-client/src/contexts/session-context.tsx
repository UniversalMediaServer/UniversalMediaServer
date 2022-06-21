import { Context, createContext } from "react";

export const sessionContext: Context<Session> = createContext({firstLogin:false} as Session);

export default sessionContext;

export interface User {
  id : number,
  username : string,
  displayName : string,
  groupId : number,
  lastLoginTime : number,
  loginFailedTime : number,
  loginFailedCount : number,
}

export interface Group {
    id : number,
    displayName : string,
	permissions : string[],
}

export interface Account {
  user : User,
  group : Group,
}

export interface Session {
    firstLogin : boolean,
	account? : Account,
}