import { UmsAccount, UmsSession } from '../contexts/session-context';

export const Permissions = {
  'users_manage': 1,
  'groups_manage': 1 << 1,
  'settings_view': 1 << 10,
  'settings_modify': 1 << 11,
  'devices_control': 1 << 12,
  'server_restart': 1 << 20,
  'application_restart': 1 << 21,
  'application_shutdown': 1 << 21,
  'web_player_browse': 1 << 25,
  'web_player_download': 1 << 26,
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
