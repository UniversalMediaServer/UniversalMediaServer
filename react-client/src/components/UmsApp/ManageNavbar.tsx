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
import { Button } from '@mantine/core'
import { IconDeviceDesktopCog, IconHome, IconInfoCircle, IconSettings, IconShare, IconTool, IconUser, IconUsers } from '@tabler/icons-react'
import { useNavigate } from 'react-router-dom'

import { I18nInterface } from '../../services/i18n-service'
import { SessionInterface, UmsPermission } from '../../services/session-service'
import About from '../About/About'
import Actions from '../Actions/Actions'
import Accounts from '../Accounts/Accounts'
import BrowserSettings from '../BrowserSettings/BrowserSettings'
import Home from '../Home/Home'
import Logs from '../Logs/Logs'
import ServerSettings from '../ServerSettings/ServerSettings'
import SharedContent from '../SharedContent/SharedContent'

export default function ManageNavbar({ i18n, session, from }: { i18n: I18nInterface, session: SessionInterface, from: string }) {
  const navigate = useNavigate()
  return (
    <>
      {!session.player && session.havePermission(UmsPermission.settings_view)
        && (
          <Button
            color="gray"
            variant={from === Home.name ? undefined : 'subtle'}
            size="compact-md"
            leftSection={<IconHome size={14} />}
            onClick={() => {
              navigate('/')
              session.setNavbarOpened(false)
            }}
          >
            {i18n.get('Home')}
          </Button>
        )}
      {!session.player && session.havePermission(UmsPermission.settings_view) && (
        <Button
          color="gray"
          variant={from === SharedContent.name ? undefined : 'subtle'}
          size="compact-md"
          leftSection={<IconShare size={14} />}
          onClick={() => {
            navigate('/shared')
            session.setNavbarOpened(false)
          }}
        >
          {i18n.get('SharedContent')}
        </Button>
      )}
      {!session.player && session.havePermission((UmsPermission.server_restart | UmsPermission.computer_shutdown) | UmsPermission.settings_modify) && (
        <Button
          color="gray"
          variant={from === Actions.name || from === Logs.name ? undefined : 'subtle'}
          size="compact-md"
          leftSection={<IconTool size={14} />}
          onClick={() => {
            navigate('/actions')
            session.setNavbarOpened(false)
          }}
        >
          {i18n.get('Tools')}
        </Button>
      )}
      {!session.player && session.havePermission(UmsPermission.settings_view) && (
        <Button
          color="gray"
          variant={from === ServerSettings.name ? undefined : 'subtle'}
          size="compact-md"
          leftSection={<IconSettings size={14} />}
          onClick={() => {
            navigate('/settings')
            session.setNavbarOpened(false)
          }}
        >
          {i18n.get('ServerSettings')}
        </Button>
      )}
      {!session.player && (
        <Button
          color="gray"
          variant={from === Accounts.name ? undefined : 'subtle'}
          size="compact-md"
          leftSection={session.havePermission(UmsPermission.users_manage) ? <IconUsers size={14} /> : <IconUser size={14} />}
          onClick={() => {
            navigate('/accounts')
            session.setNavbarOpened(false)
          }}
        >
          {session.havePermission(UmsPermission.users_manage) ? i18n.get('ManageAccounts') : i18n.get('MyAccount')}
        </Button>
      )}
      <Button
        color="gray"
        variant={from === BrowserSettings.name ? undefined : 'subtle'}
        size="compact-md"
        leftSection={<IconDeviceDesktopCog size={14} />}
        onClick={() => {
          navigate('/customize')
          session.setNavbarOpened(false)
        }}
      >
        {i18n.get('Customize')}
      </Button>
      <Button
        color="gray"
        variant={from === About.name ? undefined : 'subtle'}
        size="compact-md"
        leftSection={<IconInfoCircle size={14} />}
        onClick={() => {
          navigate('/about')
          session.setNavbarOpened(false)
        }}
      >
        {i18n.get('About')}
      </Button>
    </>
  )
}
