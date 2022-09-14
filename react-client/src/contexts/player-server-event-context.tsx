import { Context, createContext } from "react";

export const PlayerEventContext: Context<{
  connectionStatus: number;
  hasPlayerAction : boolean;
  getPlayerAction : () => any;
}> = createContext({
  connectionStatus : 0,
  hasPlayerAction : false as boolean,
  getPlayerAction : () => null,
});

export default PlayerEventContext;
