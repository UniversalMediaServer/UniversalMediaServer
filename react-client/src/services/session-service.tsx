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
export interface UmsUser {
  id: number
  username: string
  displayName: string
  groupId: number
  avatar?: string
  pinCode?: string
  lastLoginTime: number
  loginFailedTime: number
  loginFailedCount: number
  libraryHidden: boolean
}

export interface UmsGroupPermissions {
  value: number
}

export interface UmsGroup {
  id: number
  displayName: string
  permissions?: UmsGroupPermissions
}

export interface UmsAccount {
  user: UmsUser
  group: UmsGroup
}

export interface UmsUserLogin {
  id: number
  username: string
  displayName: string
  avatar?: string
  login: string
  token?: string
}

export interface UmsSession {
  noAdminFound: boolean
  account?: UmsAccount
  authenticate: boolean
  player: boolean
  users?: UmsUserLogin[]
}

export interface LocalUser {
  id: number
  displayName: string
  token: string
  avatar?: string
}

export const UmsPermission = {
  users_manage: 1,
  groups_manage: 1 << 1,
  settings_view: 1 << 10,
  settings_modify: 1 << 11,
  devices_control: 1 << 12,
  server_restart: 1 << 20,
  application_restart: 1 << 21,
  application_shutdown: 1 << 22,
  computer_shutdown: 1 << 23,
  web_player_browse: 1 << 25,
  web_player_download: 1 << 26,
  web_player_edit: 1 << 27,
  all: -1,
}

export interface SessionInterface extends UmsSession {
  initialized: boolean
  havePermission(permission: number): boolean
  users?: UmsUserLogin[]
  isDefaultUser: boolean
  canSwitchUser: boolean
  refresh: () => void
  token: string
  setToken: (token: string) => void
  login: (username: string, password: string) => Promise<void>
  loginPin: (id: number, pin: string) => Promise<void>
  logout: (keepLocal: boolean) => Promise<void>
  isLogout: boolean
  resetLogout: () => void
  lastUserId: number
  sseAs: string
  useSseAs: (name: string) => void
  stopSse: () => void
  usePlayerSse: boolean
  startPlayerSse: () => void
  stopPlayerSse: () => void
  uuid: string
  setUuid: (_uuid: string) => void
  serverName: string
  setServerName: (serverName: string) => void
  setDocumentTitle: (documentTitle: string) => void
  setDocumentI18nTitle: (documentTitle: string) => void
  playerNavbar: boolean
  setPlayerNavbar: (playerNavbar: boolean) => void
  playerDirectPlay: boolean
  setPlayerDirectPlay: (playerDirectPlay: boolean) => void
  hasNavbar: boolean
  navbarValue: React.ReactNode
  setNavbarValue: (navbarValue: React.ReactNode) => void
  navbarOpened: boolean
  setNavbarOpened: (navbarOpened: boolean) => void
  statusLine: any
  setStatusLine: (statusLine: any) => void
  navbarManage: string
  setNavbarManage: (_navbarManage: string) => void
}
