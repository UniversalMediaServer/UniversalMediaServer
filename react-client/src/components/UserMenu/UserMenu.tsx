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
import { Menu, ActionIcon, VisuallyHidden } from '@mantine/core';
import { useContext } from 'react';
import { IconHome, IconInfoCircle, IconLogout, IconMenu2, IconPlayerPlay, IconSettings, IconShare, IconTool, IconUser, IconUsers } from '@tabler/icons-react';

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
        <ActionIcon variant='default' size={30}>
          <VisuallyHidden>{i18n.get('MainMenu')}</VisuallyHidden>
          <IconMenu2 size={16} />
        </ActionIcon>
      </Menu.Target>
      <Menu.Dropdown>
        {!session.player && havePermission(session, Permissions.settings_view) &&
          <Menu.Item
            color='green'
            leftSection={<IconHome size={14} />}
            onClick={() => { window.location.href = '/'; }}
          >
            {i18n.get('Home')}
          </Menu.Item>
        }
        {havePermission(session, Permissions.web_player_browse) && (
          <Menu.Item
            color='blue'
            leftSection={<IconPlayerPlay size={14} />}
            onClick={() => { window.location.href = '/player'; }}
          >
            {i18n.getI18nString('Player')}
          </Menu.Item>
        )}
        {!session.player && <>
          <Menu.Divider />
          <Menu.Label>{i18n.get('Settings')}</Menu.Label>
          {havePermission(session, Permissions.settings_view) && (
            <Menu.Item
              leftSection={<IconShare size={14} />}
              onClick={() => { window.location.href = '/shared'; }}
            >
              {i18n.get('SharedContent')}
            </Menu.Item>
          )}
          {havePermission(session, (Permissions.server_restart | Permissions.computer_shutdown) | Permissions.settings_modify) && (
            <Menu.Item
              leftSection={<IconTool size={14} />}
              onClick={() => { window.location.href = '/actions'; }}
            >
              {i18n.get('Tools')}
            </Menu.Item>
          )}
          {havePermission(session, Permissions.settings_view) && (
            <Menu.Item
              leftSection={<IconSettings size={14} />}
              onClick={() => { window.location.href = '/settings'; }}
            >
              {i18n.get('ServerSettings')}
            </Menu.Item>
          )}
          <Menu.Item
            leftSection={havePermission(session, Permissions.users_manage) ? <IconUsers size={14} /> : <IconUser size={14} />}
            onClick={() => { window.location.href = '/accounts'; }}
          >
            {havePermission(session, Permissions.users_manage) ? i18n.get('ManageAccounts') : i18n.get('MyAccount')}
          </Menu.Item>
        </>}
        <Menu.Divider />
        <Menu.Item
          color='yellow'
          leftSection={<IconInfoCircle size={14} />}
          onClick={() => { window.location.href = '/about'; }}
        >
          {i18n.get('About')}
        </Menu.Item>
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
