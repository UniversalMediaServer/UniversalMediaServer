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
import { Avatar, Group, Text } from '@mantine/core'
import { IconUser, IconUserPlus } from '@tabler/icons-react'

import { UmsGroup, UmsUser } from '../../services/session-service'

export default function UserAccordionLabel({ user, group }: { user: UmsUser, group: UmsGroup }) {
  const showAsUsername = (user.displayName == null || user.displayName.length === 0 || user.displayName === user.username)
  const groupDisplayName = group.displayName.length !== 0 ? ' ('.concat(group.displayName).concat(')') : ''
  const displayName = showAsUsername ? user.username : user.displayName
  return (
    <Group wrap="nowrap">
      <Avatar radius="xl" size="lg" src={user.avatar}>
        {!user.avatar && (user.id === 0
          ? (<IconUserPlus size={24} />)
          : (<IconUser size={24} />)
        )}
      </Avatar>
      <div>
        <Text>
          {displayName}
          {groupDisplayName}
        </Text>
        {!showAsUsername && (
          <Text size="sm" c="dimmed" fw={400}>
            {user.username}
          </Text>
        )}
      </div>
    </Group>
  )
}
