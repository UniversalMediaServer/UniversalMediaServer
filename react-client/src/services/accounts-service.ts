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
import { SessionInterface, UmsAccount, UmsGroup, UmsUser } from './session-service'

export interface UmsAccounts {
  users: UmsUser[]
  groups: UmsGroup[]
  enabled: boolean
  localhost: boolean
}

export interface UmsAccounts {
  users: UmsUser[]
  groups: UmsGroup[]
  enabled: boolean
  localhost: boolean
}

export const Permissions = {
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

export const accountHavePermission = (account: UmsAccount, permission: number) => {
  return (typeof account.group !== 'undefined'
    && typeof account.group.permissions !== 'undefined'
    && (account.group.permissions.value & permission) !== 0
  )
}

export const havePermission = (session: SessionInterface, permission: number) => {
  return (typeof session.account !== 'undefined'
    && accountHavePermission(session.account, permission)
  )
}

export const getUserGroup = (user: UmsUser, accounts: UmsAccounts) => {
  const group = accounts.groups && accounts.groups.find((group: UmsGroup) => group.id === user.groupId)
  return group !== undefined ? group : { id: 0, displayName: '' } as UmsGroup
}

export const getGroupName = (groupId: number, groups: UmsGroup[]) => {
  const group = groups && groups.find((group: UmsGroup) => group.id === groupId)
  return group !== undefined ? group.displayName : null
}

export const getUserGroupsSelection = (groups: UmsGroup[], none?: string) => {
  const result = []
  if (none) {
    result.push({ value: '0', label: none })
  }
  if (groups) {
    groups.forEach((group: UmsGroup) => {
      if (group.id > 0) {
        result.push({ value: group.id.toString(), label: group.displayName })
      }
    })
  }
  return result
}
