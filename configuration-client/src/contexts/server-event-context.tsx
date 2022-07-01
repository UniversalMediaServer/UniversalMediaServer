import { Context, createContext } from "react";

export const serverEventContext: Context<{
  connectionStatus: number;
  memory: {max:number,used:number,buffer:number};
  message: string;
  updateAccounts: boolean;
  setUpdateAccounts: (updateAccounts: boolean) => void;
}> = createContext({
  connectionStatus : 0,
  memory : {max:0,used:0,buffer:0},
  message : '',
  updateAccounts : false as boolean,
  setUpdateAccounts : (updateAccounts: boolean) => {}
});

export default serverEventContext;
