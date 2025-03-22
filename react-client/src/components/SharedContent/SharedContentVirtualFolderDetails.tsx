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
import { Group, Stack, Text } from '@mantine/core'

import { I18nInterface } from '../../services/i18n-service'
import { UmsGroup } from '../../services/session-service'
import { Folder, VirtualFolder } from '../../services/shared-service'
import SharedContentText from './SharedContentText'
import RestrictedGroups from './RestrictedGroups'

export default function SharedContentVirtualFolderDetails({
  i18n,
  value,
  groups,
}: {
  i18n: I18nInterface
  value: VirtualFolder
  groups: UmsGroup[]
}) {
  const childsFolders = value.childs ? value.childs.filter(child => child.type === 'Folder') as Folder[] : []
  const parents = value.parent ? value.parent.split('/') : []
  return (
    <Stack gap={5}>
      <Text>{i18n.get('VirtualFolders')}</Text>
      <Group gap={5}>
        {
          parents.map((parent: string, index) => (
            <SharedContentText key={index} color="teal">{parent}</SharedContentText>
          ))
        }
        <SharedContentText color="teal">{value.name}</SharedContentText>
      </Group>
      {
        childsFolders.map((child: Folder, index) => (
          <SharedContentText key={index} color="blue">{child.file}</SharedContentText>
        ))
      }
      <RestrictedGroups i18n={i18n} value={value} groups={groups} />
    </Stack>
  )
}
