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
import { Button, Group, Modal, MultiSelect, ScrollArea, Select, Stack, TextInput } from '@mantine/core'
import { UseFormReturnType } from '@mantine/form'
import { Folder, SharedContentConfiguration, SharedContentData, SharedContentInterface } from '../../services/shared-service'
import { I18nInterface } from '../../services/i18n-service'
import DirectoryChooser from '../DirectoryChooser/DirectoryChooser'
import { IconUsers } from '@tabler/icons-react'
import _ from 'lodash'
import SharedContentChildsDirectoryChooser from './SharedContentChildsDirectoryChooser'
import { getUserGroupsSelection } from '../../services/accounts-service'

export default function SharedContentModal({
  i18n,
  canModify,
  opened,
  setOpened,
  form,
  save,
  configuration,
}: {
  i18n: I18nInterface
  canModify: boolean
  opened: boolean
  setOpened: (value: boolean) => void
  form: UseFormReturnType<Record<string, unknown>, (values: Record<string, unknown>) => Record<string, unknown>>
  save: () => void
  configuration: SharedContentConfiguration
}) {
  const sharedContent = {
    index: form.values.index as number,
    type: form.values.type as string,
    groups: form.values.groups as string[],
    name: form.values.name as string,
    parent: form.values.parent as string,
    source: form.values.source as string,
    childs: form.values.childs as SharedContentInterface[],
  } as SharedContentData
  const isNew = sharedContent.index < 0

  const data = [
    { value: 'Folder', label: i18n.get('Folder') },
    { value: 'VirtualFolder', label: i18n.get('VirtualFolders') },
    { value: 'FeedAudio', label: i18n.get('Podcast') },
    { value: 'FeedImage', label: i18n.get('ImageFeed') },
    { value: 'FeedVideo', label: i18n.get('VideoFeed') },
    { value: 'StreamAudio', label: i18n.get('AudioStream') },
    { value: 'StreamVideo', label: i18n.get('VideoStream') },
  ]
  if (configuration.show_itunes_library) {
    data.push({ value: 'iTunes', label: i18n.get('ItunesLibrary') })
  }
  if (configuration.show_iphoto_library) {
    data.push({ value: 'iPhoto', label: i18n.get('IphotoLibrary') })
  }
  if (configuration.show_aperture_library) {
    data.push({ value: 'Aperture', label: i18n.get('ApertureLibrary') })
  }

  const getUniqueFolders = (array: Folder[]) => {
    return Array.from(array.reduce(function (map, item) {
      if (item.file && !map.has(item.file)) map.set(item.file, item)
      return map
    }, new Map()).values())
  }

  const setSharedContentChild = (file: string, index: number) => {
    const contentChilds = _.cloneDeep(sharedContent.childs as Folder[])
    if (index < 0 && file) {
      contentChilds.push({ type: 'Folder', file: file, monitored: true, metadata: true, active: true } as Folder)
    }
    else {
      contentChilds[index].file = file
    }
    const filteredContentChilds = getUniqueFolders(contentChilds)
    form.setFieldValue('childs', filteredContentChilds)
  }

  return sharedContent.type && (
    <Modal
      scrollAreaComponent={ScrollArea.Autosize}
      opened={opened}
      onClose={() => setOpened(false)}
      title={i18n.get('SharedContent')}
      lockScroll={false}
    >
      <Select
        disabled={!canModify || !isNew}
        label={i18n.get('Type')}
        data={data}
        maxDropdownHeight={120}
        {...form.getInputProps('type')}
      />
      {sharedContent.type !== 'Folder' && sharedContent.type !== 'iTunes' && (
        <TextInput
          disabled={!canModify || sharedContent.type.startsWith('Feed')}
          label={i18n.get('Name')}
          placeholder={sharedContent.type.startsWith('Feed') ? i18n.get('NamesSetAutomaticallyFeeds') : ''}
          name="contentName"
          style={{ flex: 1 }}
          {...form.getInputProps('name')}
        />
      )}
      {sharedContent.type !== 'Folder' && sharedContent.type !== 'iTunes' && (
        <TextInput
          disabled={!canModify}
          label={i18n.get('Path')}
          placeholder={sharedContent.type !== 'VirtualFolder' ? 'Web' : ''}
          name="contentPath"
          style={{ flex: 1 }}
          {...form.getInputProps('contentPath')}
        />
      )}
      {sharedContent.type === 'Folder' || sharedContent.type === 'iTunes'
        ? (
            <DirectoryChooser
              i18n={i18n}
              disabled={!canModify}
              label={i18n.get('Folder')}
              size="xs"
              path={sharedContent.source}
              callback={(directory: string) => form.setFieldValue('source', directory)}
              placeholder={sharedContent.type === 'iTunes' ? i18n.get('AutoDetect') : undefined}
              withAsterisk={sharedContent.type === 'Folder'}
            >
            </DirectoryChooser>
          )
        : sharedContent.type !== 'VirtualFolder' && (
          <TextInput
            disabled={!canModify}
            label={i18n.get('SourceURLColon')}
            name="contentSource"
            style={{ flex: 1 }}
            {...form.getInputProps('source')}
          />
        )}
      {sharedContent.type === 'VirtualFolder' && (
        <Stack gap="3">
          <label>{i18n.get('SharedFolders')}</label>
          <SharedContentChildsDirectoryChooser i18n={i18n} childs={sharedContent.childs as Folder[]} canModify={canModify} setSharedContentChild={setSharedContentChild} />
          <DirectoryChooser
            i18n={i18n}
            disabled={!canModify}
            size="xs"
            path=""
            placeholder={i18n.get('AddFolder')}
            callback={(directory: string) => setSharedContentChild(directory, -1)}
          />
        </Stack>
      )}
      <MultiSelect
        leftSection={<IconUsers />}
        disabled={!canModify}
        data={getUserGroupsSelection(configuration.groups)}
        label={i18n.get('AuthorizedGroups')}
        placeholder={sharedContent.groups.length > 0 ? undefined : i18n.get('NoGroupRestrictions')}
        maxDropdownHeight={120}
        hidePickedOptions
        {...form.getInputProps('groups')}
      />
      <Group justify="flex-end" mt="sm">
        <Button
          variant="outline"
          onClick={() => {
            if (canModify) {
              save()
            }
            else {
              setOpened(false)
            }
          }}
        >
          {canModify ? isNew ? i18n.get('Add') : i18n.get('Apply') : i18n.get('Close')}
        </Button>
      </Group>
    </Modal>
  )
}
