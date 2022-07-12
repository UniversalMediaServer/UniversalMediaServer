import { Context, createContext } from "react";
import { UmsUser, UmsGroup } from '../contexts/session-context';

export const accountsContext: Context<UmsAccounts> = createContext({users:[],groups:[],enabled:true,localhost:false} as UmsAccounts);

export default accountsContext;

export interface UmsAccounts {
  users : UmsUser[],
  groups : UmsGroup[],
  enabled : boolean,
  localhost : boolean,
}