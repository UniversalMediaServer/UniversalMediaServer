import { Menu, ActionIcon } from '@mantine/core';
import React, { useContext } from 'react';
import { InfoCircle, Logout, Menu2, PlayerPlay, Refresh, Settings, User, Users } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import SessionContext from '../../contexts/session-context';
import { sendAction } from '../../services/actions-service';
import { havePermission } from '../../services/accounts-service';
import { redirectToLogin } from '../../services/auth-service';

function UserMenu() {
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);

  const restartServer = async () => {
    await sendAction('Server.Restart');
  };

  return (
    <Menu
      control={
        <ActionIcon variant="default" size={30}>
          <Menu2 size={16} />
        </ActionIcon>
      }
    >
      <Menu.Label>Settings</Menu.Label>
      {havePermission(session, "server_restart")  && (
        <Menu.Item
          icon={<Refresh size={14} />}
          onClick={restartServer}
        >
          {i18n.get['RestartServer']}
        </Menu.Item>
      )}
      {havePermission(session, "settings_view")  && (
        <Menu.Item
          icon={<Settings size={14} />}
          onClick={() => { window.location.href = '/settings'; }}
        >
          {i18n.get['ServerSettings']}
        </Menu.Item>
      )}
      <Menu.Item
        icon={havePermission(session, "users_manage") ? <Users size={14} /> : <User size={14} />}
        onClick={() => { window.location.href = '/accounts'; }}
      >
        {havePermission(session, "users_manage") ? i18n.get['WebGui.UserMenuManageAccounts'] : i18n.get['WebGui.UserMenuManageAccount']}
      </Menu.Item>
      <Menu.Item
        color="blue"
        icon={<PlayerPlay size={14} />}
        onClick={() => { window.location.href = '/player'; }}
      >
        {i18n.getI18nString("Player")}
      </Menu.Item>
      <Menu.Item
        color="yellow"
        icon={<InfoCircle size={14} />}
        onClick={() => { window.location.href = '/about'; }}
      >
        {i18n.get['About']}
      </Menu.Item>
      <Menu.Item
        color="red"
        icon={<Logout size={14} />}
        onClick={() => {
          redirectToLogin();
        }}
      >
        {i18n.get['WebGui.ButtonLogout']}
      </Menu.Item>
    </Menu>
  );
}
export default UserMenu;