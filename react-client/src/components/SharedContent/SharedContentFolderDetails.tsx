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
import { Stack, Text } from '@mantine/core'

import { I18nInterface } from '../../services/i18n-service'
import { UmsGroup } from '../../services/session-service'
import { Folder } from '../../services/shared-service'
import SharedContentText from './SharedContentText'
import RestrictedGroups from './RestrictedGroups'

export default function SharedContentFolderDetails({
  i18n,
  value,
  groups,
}: {
  i18n: I18nInterface
  value: Folder
  groups: UmsGroup[]
}) {
  const getFolderName = (value: string) => {
    return value?.split('\\').pop()?.split('/').pop()
  }
  return (
    <Stack gap={5}>
      <Text>{i18n.get('Folder')}</Text>
      <SharedContentText color="teal">{getFolderName(value.file)}</SharedContentText>
      <SharedContentText color="blue">{value.file}</SharedContentText>
      <RestrictedGroups i18n={i18n} value={value} groups={groups} />
    </Stack>
  )
}
