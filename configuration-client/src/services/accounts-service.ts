import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import { clearJwt } from './auth-service';

import { UmsAccounts } from '../contexts/accounts-context';
import { UmsAccount, UmsGroup, UmsSession, UmsUser } from '../contexts/session-context';

export const accountHavePermission = (account: UmsAccount, permission: string) => {
  return (typeof account.group !== "undefined"
	&& typeof account.group.permissions !== "undefined"
	&& (account.group.permissions.includes("*") ||
	account.group.permissions.includes(permission))
  );
}

export const havePermission = (session: UmsSession, permission: string) => {
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
  return {id:0,displayName:"",permissions:[]} as UmsGroup;
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
  return axios.post('/v1/api/account/action', data)
    .then(function () {
      showNotification({
        title: title,
        message: message,
      })
    })
    .catch(function (error: Error) {
        console.log(error);
        showNotification({
          color: 'red',
          title: 'Error',
          message: errormessage,
        })
    });
};

export const postAccountAuthAction = (data: any, errormessage: string) => {
  return axios.post('/v1/api/account/action', data)
    .then(function () {
        clearJwt();
        window.location.reload();
    })
    .catch(function (error: Error) {
        console.log(error);
        showNotification({
          color: 'red',
          title: 'Error',
          message: errormessage,
        })
    });
};