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

import { SessionInterface, UmsMemory } from '../services/session-service'
import { RendererAction } from '../services/home-service'

const SessionContext: Context<SessionInterface> = createContext({
  initialized: false,
  noAdminFound: false,
  account: undefined,
  authenticate: false,
  player: false,
  users: undefined,
  localUsers: [],
  isDefaultUser: false,
  canSwitchUser: false,
  havePermission: (_permission: number) => false,
  refresh: () => { },
  token: '',
  setToken: (_token: string) => { },
  login: async (_username: string, _password: string) => { },
  loginPin: async (_id: number, _pin: string) => { },
  logout: async (_keepLocal: boolean) => { },
  isLogout: false,
  resetLogout: () => { },
  removeLocalUser: (_id: number) => { },
  lastUserId: 0,
  subscribe: '',
  subscribeTo: (_name: string) => { },
  unsubscribe: () => { },
  usePlayerSse: false,
  startPlayerSse: () => { },
  stopPlayerSse: () => { },
  uuid: '',
  setUuid: (_uuid: string) => { },
  serverName: 'Universal Media Server',
  setServerName: (_serverName: string) => { },
  setDocumentTitle: (_documentTitle: string) => { },
  setDocumentI18nTitle: (_documentTitle: string) => { },
  playerNavbar: true,
  setPlayerNavbar: (_playerNavbar: boolean) => { },
  playerDirectPlay: false,
  setPlayerDirectPlay: (_playerDirectPlay: boolean) => { },
  hasNavbar: false,
  navbarOpened: false as boolean,
  setNavbarOpened: (_navbarOpened: boolean) => { },
  navbarValue: undefined as React.ReactNode,
  setNavbarValue: (_navbarValue: React.ReactNode) => { },
  navbarManage: '',
  setNavbarManage: (_navbarManage: string) => { },
  statusLine: '',
  setStatusLine: (_statusLine: string) => { },
  serverConfiguration: null,
  setServerConfiguration: (_configuration: Record<string, unknown> | null) => { },
  addLogLine: (_line: string) => { },
  hasNewLogLine: false,
  getNewLogLine: () => undefined,
  addRendererAction: (_rendererAction: RendererAction) => { },
  hasRendererAction: false,
  getRendererAction: () => undefined,
  mediaScan: false,
  setMediaScan: (_mediaScan: boolean) => { },
  reloadable: false,
  setReloadable: (_reloadable: boolean) => { },
  memory: { max: 0, used: 0, dbcache: 0, buffer: 0 },
  setMemory: (_memory: UmsMemory) => { },
  updateAccounts: false,
  setUpdateAccounts: (_updateAccounts: boolean) => { },
} as SessionInterface)

export default SessionContext
