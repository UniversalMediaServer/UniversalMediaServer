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
import { Menu, Group, Avatar, Text } from '@mantine/core'
import { IconLogout, IconShieldOff, IconUser, IconUserStar } from '@tabler/icons-react'
import { useNavigate } from 'react-router-dom'

import { I18nInterface } from '../../services/i18n-service'
import { SessionInterface } from '../../services/session-service'

export default function UserMenu({ i18n, session }: { i18n: I18nInterface, session: SessionInterface }) {
  const navigate = useNavigate()
  return session.account && session.account.user
    ? session.authenticate
      ? session.account?.user.id !== 2147483647
        ? (
            <Menu>
              <Menu.Target>
                <Group gap="xs" style={{ cursor: 'pointer' }}>
                  <Text truncate="end">{session.account.user.displayName}</Text>
                  <Avatar variant="outline" size={30} radius="sm" src={session.account.user.avatar !== '' ? session.account.user.avatar : null}>
                    {session.account.user.avatar === '' && <IconUser size={16} />}
                  </Avatar>
                </Group>
              </Menu.Target>
              <Menu.Dropdown>
                <Menu.Item
                  color="red"
                  leftSection={<IconLogout size={14} />}
                  onClick={() => {
                    session.logout()
                    navigate('/')
                  }}
                >
                  {i18n.get('LogOut')}
                </Menu.Item>
              </Menu.Dropdown>
            </Menu>
          )
        : (
            <Group gap="xs" style={{ cursor: 'default' }}>
              <Text>Localhost</Text>
              <Avatar variant="outline" size={30} radius="sm">
                <IconUserStar size={16} />
              </Avatar>
            </Group>
          )
      : (
          <Group gap="xs" style={{ cursor: 'default' }}>
            <Text>Anonymous</Text>
            <Avatar variant="outline" size={30} radius="sm">
              <IconShieldOff size={16} />
            </Avatar>
          </Group>
        )
    : undefined
}
