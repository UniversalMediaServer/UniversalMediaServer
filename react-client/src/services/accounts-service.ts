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
import { showNotification, updateNotification } from '@mantine/notifications';
import axios from 'axios';

import { UmsAccounts } from '../contexts/accounts-context';
import { UmsAccount, UmsGroup, UmsSession, UmsUser } from '../contexts/session-context';
import { accountApiUrl } from '../utils';
import { clearJwt } from './auth-service';

export const Permissions = {
  'users_manage': 1,
  'groups_manage': 1 << 1,
  'settings_view': 1 << 10,
  'settings_modify': 1 << 11,
  'devices_control': 1 << 12,
  'server_restart': 1 << 20,
  'application_restart': 1 << 21,
  'application_shutdown': 1 << 22,
  'computer_shutdown': 1 << 23,
  'web_player_browse': 1 << 25,
  'web_player_download': 1 << 26,
  'all': -1
};

export const accountHavePermission = (account: UmsAccount, permission: number) => {
  return (typeof account.group !== 'undefined'
    && typeof account.group.permissions !== 'undefined'
    && (account.group.permissions.value & permission) !== 0
  );
}

export const havePermission = (session: UmsSession, permission: number) => {
  return (typeof session.account !== 'undefined'
    && accountHavePermission(session.account, permission)
  );
}

export const getUserGroup = (user: UmsUser, accounts: UmsAccounts) => {
  accounts.groups.forEach((group) => {
    if (group.id === user.groupId) {
      return group;
    }
  });
  return { id: 0, displayName: '' } as UmsGroup;
};

export const getUserGroupsSelection = (accounts: UmsAccounts, none: string) => {
  const result = [];
  result.push({ value: '0', label: none });
  accounts.groups.forEach((group) => {
    if (group.id > 0) {
      result.push({ value: group.id.toString(), label: group.displayName });
    }
  });
  return result;
};

export const postAccountAction = (data: any, title: string, message: string, successmessage: string, errormessage: string) => {
  showNotification({
    id: 'account-action',
    title: title,
    message: message,
  });
  return axios.post(accountApiUrl + 'action', data)
    .then(function() {
      updateNotification({
        id: 'account-action',
        color: 'teal',
        title: title,
        message: successmessage
      });
    })
    .catch(function() {
      updateNotification({
        id: 'account-action',
        color: 'red',
        title: 'Error',
        message: errormessage
      });
    });
};

export const postAccountAuthAction = (data: any, errormessage: string) => {
  return axios.post(accountApiUrl + 'action', data)
    .then(function() {
      clearJwt();
      window.location.reload();
    })
    .catch(function() {
      showNotification({
        color: 'red',
        title: 'Error',
        message: errormessage,
      })
    });
};
