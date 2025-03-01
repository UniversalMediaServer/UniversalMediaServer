/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
import { Menu, Button, Avatar, useComputedColorScheme } from '@mantine/core';
import { IconLogout, IconUser } from '@tabler/icons-react';
import { useNavigate } from 'react-router-dom';

import { I18nInterface } from '../../services/i18n-service';
import { SessionInterface } from '../../services/session-service';

export default function UserMenu({ i18n, session}: { i18n:I18nInterface, session:SessionInterface }) {
  const navigate = useNavigate();
  const computedColorScheme = useComputedColorScheme('dark', { getInitialValueInEffect: true });
  return (
    <Menu>
      <Menu.Target>
        {session.account && session.account.user &&
          <Button
            variant='transparent'
            style={() => ({ cursor: 'default', color: computedColorScheme === 'dark' ? 'white' : 'black' })}
            rightSection={
              <Avatar radius='sm' size='sm' src={session.account.user.avatar !== '' ? session.account.user.avatar : null}>
                {session.account.user.avatar === '' && <IconUser size={16} />}
              </Avatar>
            }
          >
            {session.account.user.displayName}
          </Button>
        }
      </Menu.Target>
      <Menu.Dropdown>
        {session.authenticate && session.account?.user.id !== 2147483647 && (
          <Menu.Item
            color='rgba(255, 0, 0, 1)'
            leftSection={<IconLogout size={14} />}
            onClick={() => {
              session.logout()
              navigate('/')
            }}
          >
            {i18n.get('LogOut')}
          </Menu.Item>
        )}
      </Menu.Dropdown>
    </Menu>
  );
}
