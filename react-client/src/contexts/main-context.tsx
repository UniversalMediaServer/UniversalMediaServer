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
import { Context, createContext } from 'react'

import { I18nInterface } from '../services/i18n-service'
import { MainInterface } from '../services/main-service'
import { SessionInterface } from '../services/session-service'

const MainContext: Context<MainInterface> = createContext({
  navbarValue: undefined as React.ReactNode,
  setNavbarValue: (_navbarValue: React.ReactNode) => { },
  navbarOpened: false as boolean,
  setNavbarOpened: (_navbarOpened: boolean) => { },
  statusLine: undefined,
  setStatusLine: (_statusLine: any) => { },
  setNavbarItem: (_i18n: I18nInterface, _session: SessionInterface, _navbarItem: string) => { },
})

export default MainContext
