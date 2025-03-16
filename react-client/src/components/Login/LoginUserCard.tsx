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
import { Avatar, Paper, Text } from '@mantine/core'
import { IconUser } from '@tabler/icons-react'

import { UmsUserLogin } from '../../services/session-service'

export default function LoginUserCard({ user }: { user: UmsUserLogin }) {
  return (
    <Paper
      radius="md"
      withBorder
      p="lg"
      bg="var(--mantine-color-body)"
      w="180"
    >
      <Avatar
        src={user.avatar ? user.avatar : undefined}
        size={120}
        radius={10}
        mx="auto"
      >
        <IconUser size={60} />
      </Avatar>
      <Text ta="center" fz="lg" fw={500} mt="md">
        {user.displayName}
      </Text>
    </Paper>
  )
}
