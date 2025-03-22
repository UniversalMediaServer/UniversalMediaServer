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
import { Card, Group } from '@mantine/core'
import { IItemProps } from 'react-movable'

import { I18nInterface } from '../../services/i18n-service'
import { UmsGroup } from '../../services/session-service'
import { SharedContentInterface } from '../../services/shared-service'
import MovableActionIcon from './MovableActionIcon'
import SharedContentItemDetails from './SharedContentItemDetails'
import SharedContentItemMenu from './SharedContentItemMenu'

export default function SharedContentItem({
  i18n,
  value,
  sharedContents,
  setSharedContents,
  openEditModal,
  groups,
  canModify,
  usekey,
  isDragged,
  isSelected,
  isFirst,
  isLast,
  props,
}: {
  i18n: I18nInterface
  value: SharedContentInterface
  sharedContents: SharedContentInterface[]
  setSharedContents: (sharedContents: SharedContentInterface[]) => void
  openEditModal: (index: number) => void
  groups: UmsGroup[]
  canModify: boolean
  usekey?: string | number
  isDragged?: boolean
  isSelected?: boolean
  isFirst?: boolean
  isLast?: boolean
  props?: IItemProps
}) {
  return (
    <Card
      py="xs"
      shadow="sm"
      withBorder
      {...props}
      key={usekey}
    >
      <Group justify="space-between">
        <Group justify="flex-start">
          {canModify && usekey !== undefined && (
            <MovableActionIcon isDragged={isDragged} isSelected={isSelected} isFirst={isFirst} isLast={isLast} />
          )}
          <SharedContentItemDetails i18n={i18n} value={value} groups={groups} />
        </Group>
        <Group justify="flex-end">
          <SharedContentItemMenu
            i18n={i18n}
            value={value}
            sharedContents={sharedContents}
            setSharedContents={setSharedContents}
            openEditModal={openEditModal}
            canModify={canModify}
          />
        </Group>
      </Group>
    </Card>
  )
}
