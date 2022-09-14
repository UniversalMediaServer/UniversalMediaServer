import { Menu, ActionIcon } from '@mantine/core';
import { useContext } from 'react';
import { Activity, Home, InfoCircle, Logout, Menu2, PlayerPlay, Settings, User, Users } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import SessionContext from '../../contexts/session-context';
import { havePermission, Permissions } from '../../services/accounts-service';
import { redirectToLogin } from '../../services/auth-service';

function UserMenu() {
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);

  return (
    <Menu>
      <Menu.Target>
        <ActionIcon variant="default" size={30}>
          <Menu2 size={16} />
        </ActionIcon>
      </Menu.Target>
      <Menu.Dropdown>
      {!session.player && 
        <Menu.Item
          color="green"
          icon={<Home size={14} />}
          onClick={() => { window.location.href = '/'; }}
        >
          {i18n.get['Home']}
        </Menu.Item>
      }
      {havePermission(session, Permissions.web_player_browse) && (
        <Menu.Item
          color="blue"
          icon={<PlayerPlay size={14} />}
          onClick={() => { window.location.href = '/player'; }}
        >
          {i18n.getI18nString("Player")}
        </Menu.Item>
      )}
      {!session.player && <>
        <Menu.Divider />
        <Menu.Label>{i18n.get['Settings']}</Menu.Label>
        {havePermission(session, Permissions.server_restart | Permissions.settings_modify)  && (
          <Menu.Item
            icon={<Activity size={14} />}
            onClick={() => { window.location.href = '/actions'; }}
          >
            {i18n.get['ServerActivity']}
          </Menu.Item>
        )}
        {havePermission(session, Permissions.settings_view)  && (
          <Menu.Item
            icon={<Settings size={14} />}
            onClick={() => { window.location.href = '/settings'; }}
          >
            {i18n.get['ServerSettings']}
          </Menu.Item>
        )}
        <Menu.Item
          icon={havePermission(session, Permissions.users_manage) ? <Users size={14} /> : <User size={14} />}
          onClick={() => { window.location.href = '/accounts'; }}
        >
          {havePermission(session, Permissions.users_manage) ? i18n.get['ManageAccounts'] : i18n.get['MyAccount']}
        </Menu.Item>
      </>}
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
