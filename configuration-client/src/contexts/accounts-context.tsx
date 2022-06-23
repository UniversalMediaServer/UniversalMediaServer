import { Context, createContext } from "react";
import { UmsUser, UmsGroup } from '../contexts/session-context';

export const accountsContext: Context<UmsAccounts> = createContext({usersManage:false,groupsManage:false,users:[],groups:[]} as UmsAccounts);

export default accountsContext;

export interface UmsAccounts {
  usersManage : boolean,
  groupsManage : boolean,
  users : UmsUser[],
  groups : UmsGroup[],
}