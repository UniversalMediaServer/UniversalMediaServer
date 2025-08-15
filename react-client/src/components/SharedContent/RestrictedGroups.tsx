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
import { Group } from '@mantine/core'

import { getGroupName } from '../../services/accounts-service'
import { I18nInterface } from '../../services/i18n-service'
import { UmsGroup } from '../../services/session-service'
import { SharedContentInterface } from '../../services/shared-service'
import SharedContentText from './SharedContentText'

export default function RestrictedGroups({
  i18n,
  value,
  groups,
}: {
  i18n: I18nInterface
  value: SharedContentInterface
  groups: UmsGroup[]
}) {
  return value.groups && value.groups.length > 0
    ? (
        <Group gap={5}>
          {value.groups.map((groupId: number) => {
            const groupName = getGroupName(groupId, groups)
            return <SharedContentText key={'groupname' + groupId} color={groupName ? 'red' : 'grape'}>{groupName ? groupName : (i18n.get('NonExistentGroup') + ' ' + groupId)}</SharedContentText>
          })}
        </Group>
      )
    : undefined
}
