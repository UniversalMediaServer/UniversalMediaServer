import { Menu, ActionIcon } from '@mantine/core';
import React from 'react';
import { Trash, Settings, Lock } from 'tabler-icons-react';

function UserMenu() {
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