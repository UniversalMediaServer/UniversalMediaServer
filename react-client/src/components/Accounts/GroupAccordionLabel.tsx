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
import { IconFolder, IconFolderPlus } from '@tabler/icons-react'

import { UmsGroup } from '../../services/session-service'

export default function GroupAccordionLabel({ group }: { group: UmsGroup }) {
  return (
    <Group wrap="nowrap">
      <Avatar radius="xl" size="lg">
        {group.id === 0
          ? (
              <IconFolderPlus size={24} />
            )
          : (
              <IconFolder size={24} />
            )}
      </Avatar>
      <Text>{group.displayName}</Text>
    </Group>
  )
}
