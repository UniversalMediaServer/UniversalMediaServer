import { UmsAccount, UmsGroup, UmsSession, UmsUser } from '../contexts/session-context';
import { UmsAccounts } from '../contexts/accounts-context';
import { showNotification } from '@mantine/notifications';
import axios from 'axios';

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

export const getUserGroupsSelection = (accounts: UmsAccounts) => {
  const result = [];
  result.push({ value: '0', label: 'None' });
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