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
import { useContext } from 'react';
import { IconLogout, IconUser } from '@tabler/icons-react';

import I18nContext from '../../contexts/i18n-context';
import SessionContext from '../../contexts/session-context';
import { redirectToLogin } from '../../services/auth-service';

function UserMenu() {
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
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
              redirectToLogin();
            }}
          >
            {i18n.get('LogOut')}
          </Menu.Item>
        )}
      </Menu.Dropdown>
    </Menu>
  );
}
export default UserMenu;
