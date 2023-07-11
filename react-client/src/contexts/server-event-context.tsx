/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
import { Context, createContext } from 'react';

export const serverEventContext: Context<{
  connectionStatus: number;
  memory: { max: number, used: number, buffer: number };
  updateAccounts: boolean;
  setUpdateAccounts: (updateAccounts: boolean) => void;
  reloadable: boolean;
  userConfiguration: any;
  setUserConfiguration: (config: any) => void;
  scanLibrary: { enabled: boolean, running: boolean };
  hasRendererAction: boolean;
  getRendererAction: () => any;
  hasNewLogLine: boolean;
  getNewLogLine: () => any;
}> = createContext({
  connectionStatus: 0,
  memory: { max: 0, used: 0, buffer: 0 },
  updateAccounts: false as boolean,
  setUpdateAccounts: (updateAccounts: boolean) => { },
  reloadable: false as boolean,
  userConfiguration: null,
  setUserConfiguration: (config: any) => { },
  scanLibrary: { enabled: false as boolean, running: false as boolean },
  hasRendererAction: false as boolean,
  getRendererAction: () => null,
  hasNewLogLine: false as boolean,
  getNewLogLine: () => null,
});

export default serverEventContext;
