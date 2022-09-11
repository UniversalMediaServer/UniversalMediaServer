import { Context, createContext } from "react";

export const serverEventContext: Context<{
  connectionStatus: number;
  memory: {max:number,used:number,buffer:number};
  updateAccounts: boolean;
  setUpdateAccounts: (updateAccounts: boolean) => void;
  reloadable: boolean;
  userConfiguration: any;
  setUserConfiguration: (config: any) => void;
  scanLibrary: {enabled:boolean,running:boolean};
  hasRendererAction : boolean;
  getRendererAction : () => any;
  hasNewLogLine : boolean;
  getNewLogLine : () => any;
}> = createContext({
  connectionStatus : 0,
  memory : {max:0,used:0,buffer:0},
  updateAccounts : false as boolean,
  setUpdateAccounts : (updateAccounts: boolean) => {},
  reloadable : false as boolean,
  userConfiguration : null,
  setUserConfiguration : (config: any) => {},
  scanLibrary : {enabled: false as boolean,running: false as boolean},
  hasRendererAction : false as boolean,
  getRendererAction : () => null,
  hasNewLogLine : false as boolean,
  getNewLogLine : () => null,
});

export default serverEventContext;
