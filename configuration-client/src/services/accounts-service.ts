import { showNotification } from '@mantine/notifications';
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
  'server_restart': 1 << 20,
  'application_restart': 1 << 21,
  'application_shutdown': 1 << 21,
  'all': -1
};

export const accountHavePermission = (account: UmsAccount, permission: number) => {
  return (typeof account.group !== "undefined"
	&& typeof account.group.permissions !== "undefined"
	&& (account.group.permissions.value & permission) !== 0
  );
}

export const havePermission = (session: UmsSession, permission: number) => {
  return (typeof session.account !== "undefined"
    && accountHavePermission(session.account, permission)
  );
}

export const getUserGroup = (user: UmsUser, accounts: UmsAccounts) => {
  accounts.groups.forEach((group) => {
    if (group.id === user.groupId) {
      return group;
    }
  });
  return {id:0,displayName:""} as UmsGroup;
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

export const postAccountAction = (data: any, title: string, message: string, errormessage: string) => {
  return axios.post(accountApiUrl + 'action', data)
    .then(function () {
      showNotification({
        title: title,
        message: message,
      })
    })
    .catch(function () {
        showNotification({
          color: 'red',
          title: 'Error',
          message: errormessage,
        })
    });
};

export const postAccountAuthAction = (data: any, errormessage: string) => {
  return axios.post(accountApiUrl + 'action', data)
    .then(function () {
        clearJwt();
        window.location.reload();
    })
    .catch(function () {
        showNotification({
          color: 'red',
          title: 'Error',
          message: errormessage,
        })
    });
};