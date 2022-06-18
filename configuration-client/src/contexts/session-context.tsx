import { Context, createContext } from "react";

export const sessionContext: Context<Session> = createContext({loggedin:false,firstlogin:false} as Session);

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
}

export interface Account {
  user : User,
  group : Group,
  permissions : {[Key: string]: [Value: boolean]},
}

export interface Session {
    firstlogin : boolean,
	account? : Account,
}