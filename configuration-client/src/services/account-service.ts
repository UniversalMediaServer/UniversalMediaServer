import { Account, Session } from '../contexts/session-context';

export const havePermission = (session: Session, permission: string) => {
  return (typeof session.account !== "undefined"
    && accountHavePermission(session.account, permission)
  );
}

export const accountHavePermission = (account: Account, permission: string) => {
  return (typeof account.group !== "undefined"
	&& typeof account.group.permissions !== "undefined"
	&& (account.group.permissions.includes("*") ||
	account.group.permissions.includes(permission))
  );
}
