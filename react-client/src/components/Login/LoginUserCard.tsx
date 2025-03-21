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
import { Avatar, Indicator, Paper, Text } from '@mantine/core'
import { IconDeviceDesktopStar, IconUser, IconUserQuestion } from '@tabler/icons-react'

import { I18nInterface } from '../../services/i18n-service'
import { UmsUserLogin } from '../../services/session-service'
import { useRef } from 'react'

export default function LoginUserCard({ i18n, user, selectedUser, selectUser }: { i18n: I18nInterface, user: UmsUserLogin, selectedUser: UmsUserLogin | undefined, selectUser: (user: UmsUserLogin) => void }) {
  const viewport = useRef<HTMLDivElement>(null)
  const isConnected = user.login === 'token'
  const isSelected = selectedUser && selectedUser.id === user.id

  const getDisplayName = () => {
    if (user.id === 2147483647) {
      return i18n.get('Localhost')
    }
    if (user.id === 0) {
      return i18n.get('User')
    }
    return user.displayName
  }

  const LoginIcon = ({ user }: { user: UmsUserLogin }) => {
    if (user.id === 2147483647) {
      return <IconDeviceDesktopStar size={60} />
    }
    if (user.id === 0) {
      return <IconUserQuestion size={60} />
    }
    return <IconUser size={60} />
  }

  return (
    <Paper
      ref={viewport}
      style={{ cursor: 'pointer' }}
      radius="md"
      withBorder
      p="xs"
      bg={isSelected ? 'var(--mantine-color-default-hover)' : 'var(--mantine-color-body)'}
      miw="100"
      onClick={() => {
        viewport.current?.scrollIntoView({ inline: 'center', block: 'start' })
        if (isSelected) {
          // handle connect
        }
        else {
          selectUser(user)
        }
      }}
    >
      <Indicator
        color="teal"
        processing={isConnected}
        disabled={!isConnected}
      >
        <Avatar
          src={user.avatar ? user.avatar : undefined}
          size={60}
          radius={6}
          mx="auto"
        >
          <LoginIcon user={user} />
        </Avatar>
      </Indicator>
      <Text ta="center" mt="xs">
        {getDisplayName()}
      </Text>
    </Paper>
  )
}
