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
import { ActionIcon, Group, Menu } from '@mantine/core'
import { IconEdit, IconMenu2, IconShare, IconShareOff, IconSquareX } from '@tabler/icons-react'
import _ from 'lodash'

import { I18nInterface } from '../../services/i18n-service'
import { Feed, Folder, SharedContentInterface } from '../../services/shared-service'
import SharedContentFeedActions from './SharedContentFeedActions'
import SharedContentFolderActions from './SharedContentFolderActions'

export default function SharedContentItemMenu({
  i18n,
  value,
  sharedContents,
  setSharedContents,
  canModify,
  openEditModal,
}: {
  i18n: I18nInterface
  value: SharedContentInterface
  sharedContents: SharedContentInterface[]
  setSharedContents: (sharedContents: SharedContentInterface[]) => void
  canModify: boolean
  openEditModal: (index: number) => void
}) {
  const toogleSharedContentItemActive = (item: SharedContentInterface) => {
    const sharedContentsTemp = _.cloneDeep(sharedContents)
    const index = sharedContents.indexOf(item)
    sharedContentsTemp[index].active = !sharedContentsTemp[index].active
    setSharedContents(sharedContentsTemp)
  }

  const removeSharedContentItem = (item: SharedContentInterface) => {
    const sharedContentsTemp = _.cloneDeep(sharedContents)
    const index = sharedContents.indexOf(item)
    sharedContentsTemp.splice(index, 1)
    setSharedContents(sharedContentsTemp)
  }

  const editSharedContentItem = (value: SharedContentInterface) => {
    const index = sharedContents.indexOf(value)
    openEditModal(index)
  }

  return (
    <Group justify="flex-end">
      <ActionIcon
        variant="subtle"
        size={30}
        color={value.active ? 'blue' : 'orange'}
        disabled={!canModify}
        visibleFrom="sm"
        onClick={() => toogleSharedContentItemActive(value)}
      >
        {value.active
          ? <IconShare size={16} />
          : <IconShareOff size={16} />}
      </ActionIcon>
      <Menu zIndex={5000}>
        <Menu.Target>
          <ActionIcon variant="default" size={30}>
            <IconMenu2 size={16} />
          </ActionIcon>
        </Menu.Target>
        <Menu.Dropdown>
          <Menu.Item
            color="green"
            leftSection={<IconEdit />}
            disabled={!canModify}
            onClick={() => editSharedContentItem(value)}
          >
            {i18n.get('Edit')}
          </Menu.Item>
          <Menu.Item
            color={value.active ? 'blue' : 'orange'}
            leftSection={value.active ? <IconShare /> : <IconShareOff />}
            disabled={!canModify}
            hiddenFrom="sm"
            onClick={() => toogleSharedContentItemActive(value)}
          >
            {value.active ? i18n.get('Disable') : i18n.get('Enable')}
          </Menu.Item>
          { value.type == 'Folder'
            ? (
                <SharedContentFolderActions
                  i18n={i18n}
                  value={value as Folder}
                  sharedContents={sharedContents}
                  setSharedContents={setSharedContents}
                  canModify={canModify}
                />
              )
            : ['FeedAudio', 'FeedImage', 'FeedVideo'].includes(value.type)
              && (
                <SharedContentFeedActions
                  i18n={i18n}
                  value={value as Feed}
                  sharedContents={sharedContents}
                  setSharedContents={setSharedContents}
                  canModify={canModify}
                />
              )}
          <Menu.Divider />
          <Menu.Item
            color="red"
            leftSection={<IconSquareX />}
            disabled={!canModify}
            onClick={() => removeSharedContentItem(value)}
          >
            {i18n.get('Delete')}
          </Menu.Item>
        </Menu.Dropdown>
      </Menu>
    </Group>
  )
}
