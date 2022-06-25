import { UmsAccount, UmsSession } from '../contexts/session-context';
import axios from 'axios';

export const havePermission = (session: UmsSession, permission: string) => {
  return (typeof session.account !== "undefined"
    && accountHavePermission(session.account, permission)
  );
}

export const accountHavePermission = (account: UmsAccount, permission: string) => {
  return (typeof account.group !== "undefined"
	&& typeof account.group.permissions !== "undefined"
	&& (account.group.permissions.includes("*") ||
	account.group.permissions.includes(permission))
  );
}

export const action = (data: any) => {
  return axios
    .post('/v1/api/account/action', data)
    .then((response) => {
      return response.data;
    });
};
