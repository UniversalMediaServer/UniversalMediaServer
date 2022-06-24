import { Menu, ActionIcon } from '@mantine/core';
import React, { useContext } from 'react';
import { Trash, Settings, Lock, Refresh } from 'tabler-icons-react';
import I18nContext from '../../contexts/i18n-context';
import SessionContext from '../../contexts/session-context';
import { sendAction } from '../../services/actions-service';
import { havePermission } from '../../services/accounts-service';

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
          <Settings size={16} />
        </ActionIcon>
      }
    >
      <Menu.Label>Settings</Menu.Label>
      {havePermission(session, "server_restart")  && (
        <Menu.Item
          icon={<Refresh size={14} />}
          onClick={restartServer}
        >
          {i18n['LooksFrame.12']}
        </Menu.Item>
      )}
      <Menu.Item
        icon={<Lock size={14} />}
        onClick={() => {
          window.location.href = '/accounts';
        }}
      >{havePermission(session, "users_manage") ? 'Manage accounts' : 'My account'}</Menu.Item>
      <Menu.Item
        color="red"
        icon={<Trash size={14} />}
        onClick={() => {
          localStorage.removeItem('user');
          window.location.reload();
          }
        }
        >Log out</Menu.Item>
    </Menu>
  );
}
export default UserMenu;