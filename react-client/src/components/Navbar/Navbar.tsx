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
import { IconHome, IconInfoCircle, IconPlayerPlay, IconSettings, IconShare, IconTool, IconUser, IconUsers } from '@tabler/icons-react';

import { havePermission, Permissions } from '../../services/accounts-service';
import { UmsSession } from '../../contexts/session-context';
import { I18nInterface } from '../../contexts/i18n-context';

export enum NavbarItems {
  Home = 'home',
  Player = 'player',
  SharedContent = 'sharedcontent',
  Tools = 'tools',
  ServerSettings = 'serversettings',
  ManageAccounts = 'manageaccounts',
  About = 'about',
}

function Navbar( props: { i18n: I18nInterface, session: UmsSession, selectedKey: NavbarItems } ) {
  return (<>
    {<>
      {!props.session.player && havePermission(props.session, Permissions.settings_view) &&
        <Button
          color='gray'
          variant={props.selectedKey === NavbarItems.Home ? undefined : 'subtle'}
          size='compact-md'
          leftSection={<IconHome size={14} />}
          onClick={() => { window.location.href = '/'; }}
          >
          {props.i18n.get('Home')}
        </Button>
      }
      {havePermission(props.session, Permissions.settings_view) && (
        <Button
          color='gray'
          variant={props.selectedKey === NavbarItems.SharedContent ? undefined : 'subtle'}
          size='compact-md'
          leftSection={<IconShare size={14} />}
          onClick={() => { window.location.href = '/shared'; }}
        >
          {props.i18n.get('SharedContent')}
        </Button>
      )}
      {havePermission(props.session, (Permissions.server_restart | Permissions.computer_shutdown) | Permissions.settings_modify) && (
        <Button
          color='gray'
          variant={props.selectedKey === NavbarItems.Tools ? undefined : 'subtle'}
          size='compact-md'
          leftSection={<IconTool size={14} />}
          onClick={() => { window.location.href = '/actions'; }}
        >
          {props.i18n.get('Tools')}
        </Button>
      )}
      {havePermission(props.session, Permissions.settings_view) && (
        <Button
          color='gray'
          variant={props.selectedKey === NavbarItems.ServerSettings ? undefined : 'subtle'}
          size='compact-md'
          leftSection={<IconSettings size={14} />}
          onClick={() => { window.location.href = '/settings'; }}
        >
          {props.i18n.get('ServerSettings')}
        </Button>
      )}
      <Button
        color='gray'
        variant={props.selectedKey === NavbarItems.ManageAccounts ? undefined : 'subtle'}
        size='compact-md'
        leftSection={havePermission(props.session, Permissions.users_manage) ? <IconUsers size={14} /> : <IconUser size={14} />}
        onClick={() => { window.location.href = '/accounts'; }}
      >
        {havePermission(props.session, Permissions.users_manage) ? props.i18n.get('ManageAccounts') : props.i18n.get('MyAccount')}
      </Button>
    </>}
    <Button
      color='gray'
      variant={props.selectedKey === NavbarItems.About ? undefined : 'subtle'}
      size='compact-md'
      leftSection={<IconInfoCircle size={14} />}
      onClick={() => { window.location.href = '/about'; }}
    >
      {props.i18n.get('About')}
    </Button>
    </>
  );
}
export default Navbar;
