import { Menu, ActionIcon } from '@mantine/core';
import React, { useContext } from 'react';
import { Trash, Settings, Lock, Refresh } from 'tabler-icons-react';
import I18nContext from '../../contexts/i18n-context';
import { sendAction } from '../../services/actions-service';

function UserMenu() {
  const i18n = useContext(I18nContext);

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
      <Menu.Item
        icon={<Refresh size={14} />}
        onClick={restartServer}
      >
        {i18n['LooksFrame.12']}
      </Menu.Item>
      <Menu.Item
        icon={<Lock size={14} />}
        onClick={() => {
          window.location.href = '/changepassword';
          }
        }
      >Change password</Menu.Item>
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