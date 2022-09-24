import { Context, createContext } from "react";

export const PlayerEventContext: Context<{
  connectionStatus: number;
  reqId: string;
  reqType: string;
  askReqId : (id:string, type:string) => void;
  askBrowseId : (id:string) => void;
  askPlayId : (id:string) => void;
  askShowId : (id:string) => void;
}> = createContext({
  connectionStatus : 0,
  reqId: '0',
  reqType: 'browse',
  askReqId : (id:string, type:string) => {},
  askBrowseId : (id:string) => {},
  askPlayId : (id:string) => {},
  askShowId : (id:string) => {},
});

export default PlayerEventContext;
