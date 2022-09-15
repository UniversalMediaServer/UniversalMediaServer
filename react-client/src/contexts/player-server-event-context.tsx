import { Context, createContext } from "react";

export const PlayerEventContext: Context<{
  connectionStatus: number;
  browseId: string;
  askBrowseId : (id:string) => void;
  playId: string;
  askPlayId : (id:string) => void;
}> = createContext({
  connectionStatus : 0,
  browseId: '0',
  askBrowseId : (id:string) => {},
  playId: '',
  askPlayId : (id:string) => {},
});

export default PlayerEventContext;
