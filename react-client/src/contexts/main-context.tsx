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

export const MainContext: Context<MainInterface> = createContext({
  navbarValue: undefined,
  setNavbarValue: (_navbarValue: any) => { },
  navbarOpened: false as boolean,
  setNavbarOpened: (_navbarOpened: any) => { },
  statusLine: undefined,
  setStatusLine: (_statusLine: any) => { },
});

export interface MainInterface {
  navbarValue: any;
  setNavbarValue: (navbarValue: any) => void;
  navbarOpened: boolean;
  setNavbarOpened: (navbarOpened: any) => void;
  statusLine: any,
  setStatusLine: (statusLine: any) => void;
}

export default MainContext;
