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
import { Button } from '@mantine/core';
import { IconHome, IconInfoCircle, IconSettings, IconShare, IconTool, IconUser, IconUsers } from '@tabler/icons-react';
import { useNavigate } from 'react-router-dom'

import { havePermission, Permissions } from '../../services/accounts-service';
import { I18nInterface } from '../../services/i18n-service';
import { SessionInterface } from '../../services/session-service';
import { NavbarItems } from '../../services/navbar-items';

const ManageNavbar = ({ i18n, session, selectedKey }: { i18n: I18nInterface, session: SessionInterface, selectedKey: NavbarItems }) => {
  const navigate = useNavigate()
  return (
    <>
      {!session.player && havePermission(session, Permissions.settings_view) &&
        <Button
          color='gray'
          variant={selectedKey === NavbarItems.Home ? undefined : 'subtle'}
          size='compact-md'
          leftSection={<IconHome size={14} />}
          onClick={() => { navigate('/'); }}
        >
          {i18n.get('Home')}
        </Button>
      }
      {havePermission(session, Permissions.settings_view) && (
        <Button
          color='gray'
          variant={selectedKey === NavbarItems.SharedContent ? undefined : 'subtle'}
          size='compact-md'
          leftSection={<IconShare size={14} />}
          onClick={() => { navigate('/shared'); }}
        >
          {i18n.get('SharedContent')}
        </Button>
      )}
      {havePermission(session, (Permissions.server_restart | Permissions.computer_shutdown) | Permissions.settings_modify) && (
        <Button
          color='gray'
          variant={selectedKey === NavbarItems.Tools ? undefined : 'subtle'}
          size='compact-md'
          leftSection={<IconTool size={14} />}
          onClick={() => { navigate('/actions'); }}
        >
          {i18n.get('Tools')}
        </Button>
      )}
      {havePermission(session, Permissions.settings_view) && (
        <Button
          color='gray'
          variant={selectedKey === NavbarItems.ServerSettings ? undefined : 'subtle'}
          size='compact-md'
          leftSection={<IconSettings size={14} />}
          onClick={() => { navigate('/settings'); }}
        >
          {i18n.get('ServerSettings')}
        </Button>
      )}
      <Button
        color='gray'
        variant={selectedKey === NavbarItems.ManageAccounts ? undefined : 'subtle'}
        size='compact-md'
        leftSection={havePermission(session, Permissions.users_manage) ? <IconUsers size={14} /> : <IconUser size={14} />}
        onClick={() => { navigate('/accounts'); }}
      >
        {havePermission(session, Permissions.users_manage) ? i18n.get('ManageAccounts') : i18n.get('MyAccount')}
      </Button>
      <Button
        color='gray'
        variant={selectedKey === NavbarItems.About ? undefined : 'subtle'}
        size='compact-md'
        leftSection={<IconInfoCircle size={14} />}
        onClick={() => { navigate('/about'); }}
      >
        {i18n.get('About')}
      </Button>
    </>
  );
}

export default ManageNavbar;
