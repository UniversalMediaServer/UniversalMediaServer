import { Context, createContext } from "react";

export const sessionContext: Context<Session> = createContext({firstlogin:false} as Session);

export default sessionContext;

export interface User {
  id : number,
  username : string,
  name : string,
  groupId : number,
  lastLoginTime : number,
  loginFailedTime : number,
  loginFailedCount : number,
}

export interface Group {
    id : number,
    name : string,
	permissions : string[],
}

export interface Account {
  user : User,
  group : Group,
}

export interface Session {
    firstlogin : boolean,
	account? : Account,
}