import { Menu, ActionIcon } from '@mantine/core';
import React, { useContext } from 'react';
import { Home, InfoCircle, Logout, Menu2, Refresh, Settings, User, Users } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import SessionContext from '../../contexts/session-context';
import { havePermission } from '../../services/accounts-service';
import { sendAction } from '../../services/actions-service';
import { redirectToLogin } from '../../services/auth-service';

function UserMenu() {
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);

  const restartServer = async () => {
    await sendAction('Server.Restart');
  };

  return (
    <Menu>
      <Menu.Target>
        <ActionIcon variant="default" size={30}>
          <Menu2 size={16} />
        </ActionIcon>
      </Menu.Target>
      <Menu.Dropdown>
      <Menu.Item
        color="green"
        icon={<Home size={14} />}
        onClick={() => { window.location.href = '/'; }}
      >
        {i18n.get['Home']}
      </Menu.Item>
      <Menu.Divider />
      <Menu.Label>{i18n.get['Settings']}</Menu.Label>
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
        {havePermission(session, "users_manage") ? i18n.get['ManageAccounts'] : i18n.get['MyAccount']}
      </Menu.Item>
      <Menu.Divider />
      <Menu.Item
        color="yellow"
        icon={<InfoCircle size={14} />}
        onClick={() => { window.location.href = '/about'; }}
      >
        {i18n.get['About']}
      </Menu.Item>
      {session.authenticate && session.account?.user.id !== 2147483647 && (
        <Menu.Item
          color="red"
          icon={<Logout size={14} />}
          onClick={() => {
            redirectToLogin();
          }}
        >
          {i18n.get['LogOut']}
        </Menu.Item>
      )}
      </Menu.Dropdown>
    </Menu>
  );
}
export default UserMenu;