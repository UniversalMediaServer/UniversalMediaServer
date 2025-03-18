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
import { ActionIcon, Button, Card, Group, Menu, Modal, MultiSelect, ScrollArea, Select, Stack, Text, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { IconAnalyze, IconAnalyzeOff, IconArrowNarrowDown, IconArrowNarrowUp, IconArrowsVertical, IconEdit, IconEyeCheck, IconEyeOff, IconListSearch, IconLoader, IconMenu2, IconPlus, IconShare, IconShareOff, IconSquareX, IconUsers, IconZoomCheck } from '@tabler/icons-react'
import axios from 'axios'
import _ from 'lodash'
import { CSSProperties, useEffect, useState } from 'react'
import { arrayMove, List } from 'react-movable'

import { getGroupName, getUserGroupsSelection } from '../../services/accounts-service'
import { sendAction } from '../../services/actions-service'
import { I18nInterface } from '../../services/i18n-service'
import { ServerEventInterface } from '../../services/server-event-service'
import { SessionInterface, UmsPermission } from '../../services/session-service'
import { sharedApiUrl } from '../../utils'
import DirectoryChooser from '../DirectoryChooser/DirectoryChooser'
import { showError, showInfo, showWarning } from '../../utils/notifications'
import SharedContentText from './SharedContentText'

export default function SharedContentSettings({
  i18n,
  session,
  sse,
  form,
  configuration,
}: {
  i18n: I18nInterface
  sse: ServerEventInterface
  session: SessionInterface
  form: any
  configuration: any
}) {
  const canModify = session.havePermission(UmsPermission.settings_modify)
  const [sharedContents, setSharedContents] = useState([] as SharedContentInterface[])
  const [isLoading, setLoading] = useState(false)
  const [newOpened, setNewOpened] = useState(false)
  const [editingIndex, setEditingIndex] = useState(-1)

  /**
   * Shared Directories
  */
  const scanAllSharedFolders = async () => {
    setLoading(true)
    try {
      await sendAction('Server.ScanAllSharedFolders')
    }
    catch (err) {
      console.error(err)
    }
    setLoading(false)
  }

  const scanAllSharedFoldersCancel = async () => {
    setLoading(true)
    try {
      await sendAction('Server.ScanAllSharedFoldersCancel')
    }
    catch (err) {
      console.error(err)
    }
    setLoading(false)
  }

  const markDirectoryFullyPlayed = async (item: string, isPlayed: boolean) => {
    setLoading(true)
    try {
      await axios.post(
        sharedApiUrl + 'mark-directory',
        { directory: item, isPlayed },
      )

      showInfo({
        message: i18n.get('Saved'),
      })
    }
    catch (err) {
      showError({
        title: i18n.get('Error'),
        message: i18n.get('ConfigurationNotSaved'),
        message2: i18n.getReportLink(),
      })
    }
    setLoading(false)
  }

  /**
   * Shared Content
   */
  const getFeedName = async (uri: string): Promise<string> => {
    const response: { data: { name: string } } = await axios.post(sharedApiUrl + 'web-content-name', { source: uri })
    return response.data.name
  }

  const updateSharedContentFeedName = async (value: Feed) => {
    setLoading(true)
    try {
      const sharedContentsTemp = _.cloneDeep(sharedContents)
      const index = sharedContents.indexOf(value)
      const name = await getFeedName(value.uri)
      if (name) {
        (sharedContentsTemp[index] as Feed).name = name
        setSharedContents(sharedContentsTemp)
      }
      else {
        showWarning({
          title: i18n.get('Information'),
          message: i18n.get('FeedNameNotFound'),
        })
      }
    }
    catch (err) {
      console.error(err)
      showError({
        title: i18n.get('Error'),
        message: i18n.get('DataNotReceived'),
      })
    }
    setLoading(false)
  }

  const getSharedContentTypeLocalized = (value: string) => {
    switch (value) {
      case 'FeedAudio':
        return i18n.get('Podcast')
      case 'FeedImage':
        return i18n.get('ImageFeed')
      case 'FeedVideo':
        return i18n.get('VideoFeed')
      case 'StreamAudio':
        return i18n.get('AudioStream')
      case 'StreamVideo':
        return i18n.get('VideoStream')
      case 'Folder':
        return i18n.get('Folder')
      case 'VirtualFolder':
        return i18n.get('VirtualFolders')
      case 'iTunes':
        return i18n.get('ItunesLibrary')
      case 'iPhoto':
        return i18n.get('IphotoLibrary')
      case 'Aperture':
        return i18n.get('ApertureLibrary')
    }
  }

  const getSharedContentParents = (value: Feed | Stream | VirtualFolder) => {
    if (!value.parent) {
      return null
    }
    const parents = value.parent.split('/')
    return parents.map((parent: string, _) => (
      <SharedContentText color="teal">{parent}</SharedContentText>
    ))
  }

  const getRestrictedGroups = (value: SharedContentInterface) => {
    return value.groups && value.groups.length > 0 ? <Group gap={5}>{getRestrictedGroupsNames(value)}</Group> : undefined
  }

  const getRestrictedGroupsNames = (value: SharedContentInterface) => {
    return value.groups.map((groupId: number) => {
      const groupName = getGroupName(groupId, configuration.groups)
      return <SharedContentText color={groupName ? 'red' : 'grape'}>{groupName ? groupName : (i18n.get('NonExistentGroup') + ' ' + groupId)}</SharedContentText>
    })
  }

  const getSharedContentFeedView = (value: Feed) => {
    const type = getSharedContentTypeLocalized(value.type)
    return (
      <Stack gap={5}>
        <Text>{type}</Text>
        <Group gap={5}>
          {getSharedContentParents(value)}
          {
            value.name
              ? <SharedContentText color="teal">{value.name}</SharedContentText>
              : <SharedContentText color="red">{i18n.get('FeedNameNotFound')}</SharedContentText>
          }
        </Group>
        <SharedContentText color="blue">{value.uri}</SharedContentText>
        {getRestrictedGroups(value)}
      </Stack>
    )
  }

  const getSharedContentStreamView = (value: Stream) => {
    const type = getSharedContentTypeLocalized(value.type)
    return (
      <Stack gap={5}>
        <Text>{type}</Text>
        <Group gap={5}>
          {getSharedContentParents(value)}
          <SharedContentText color="teal">{value.name}</SharedContentText>
        </Group>
        <SharedContentText color="blue">{value.uri}</SharedContentText>
        {getRestrictedGroups(value)}
      </Stack>
    )
  }

  const toggleFolderMonitored = (value: Folder) => {
    const sharedContentsTemp = _.cloneDeep(sharedContents)
    const index = sharedContents.indexOf(value);
    (sharedContentsTemp[index] as Folder).monitored = !(sharedContentsTemp[index] as Folder).monitored
    setSharedContents(sharedContentsTemp)
  }

  const getFolderName = (value: string) => {
    return value?.split('\\').pop()?.split('/').pop()
  }

  const getSharedContentFolderView = (value: Folder) => {
    const type = getSharedContentTypeLocalized(value.type)
    return (
      <Stack gap={5}>
        <Text>{type}</Text>
        <SharedContentText color="teal">{getFolderName(value.file)}</SharedContentText>
        <SharedContentText color="blue">{value.file}</SharedContentText>
        {getRestrictedGroups(value)}
      </Stack>
    )
  }

  const getSharedContentVirtualFolderChildsView = (value: VirtualFolder) => {
    const childsFolder = value.childs ? value.childs.filter(child => child.type === 'Folder') : []
    return childsFolder.map(child => (
      <SharedContentText color="blue">{(child as Folder).file}</SharedContentText>
    ))
  }

  const getSharedContentVirtualFolderView = (value: VirtualFolder) => {
    const type = getSharedContentTypeLocalized(value.type)
    return (
      <Stack gap={5}>
        <Text>{type}</Text>
        <Group gap={5}>
          {getSharedContentParents(value)}
          <SharedContentText color="teal">{value.name}</SharedContentText>
        </Group>
        {getSharedContentVirtualFolderChildsView(value)}
        {getRestrictedGroups(value)}
      </Stack>
    )
  }

  const getSharedContentView = (value: SharedContentInterface) => {
    switch (value.type) {
      case 'FeedAudio':
      case 'FeedImage':
      case 'FeedVideo':
        return getSharedContentFeedView(value as Feed)
      case 'StreamAudio':
      case 'StreamVideo':
        return getSharedContentStreamView(value as Stream)
      case 'Folder':
        return getSharedContentFolderView(value as Folder)
      case 'VirtualFolder':
        return getSharedContentVirtualFolderView(value as VirtualFolder)
      case 'iTunes':
        return i18n.get('ItunesLibrary')
      case 'iPhoto':
        return i18n.get('IphotoLibrary')
      case 'Aperture':
        return i18n.get('ApertureLibrary')
    }
    return (i18n.get('Unknown'))
  }

  const getSharedContentFeedActions = (value: Feed) => {
    return (
      <>
        <Menu.Divider />
        <Menu.Item
          color="blue"
          leftSection={<IconZoomCheck />}
          disabled={!canModify || !value.uri || isLoading}
          onClick={() => updateSharedContentFeedName(value)}
        >
          {i18n.get('UpdateFeedName')}
        </Menu.Item>
      </>
    )
  }

  const getSharedContentFolderActions = (value: Folder) => {
    return (
      <>
        <Menu.Divider />
        <Menu.Item
          color={value.monitored ? 'green' : 'red'}
          leftSection={value.monitored ? <IconAnalyze /> : <IconAnalyzeOff />}
          disabled={!canModify}
          onClick={() => toggleFolderMonitored(value)}
        >
          {i18n.get('MonitorPlayedStatusFiles')}
        </Menu.Item>
        <Menu.Item
          color="blue"
          leftSection={<IconEyeCheck />}
          disabled={!canModify || !value.file || isLoading}
          onClick={() => markDirectoryFullyPlayed(value.file, true)}
        >
          {i18n.get('MarkContentsFullyPlayed')}
        </Menu.Item>
        <Menu.Item
          color="green"
          leftSection={<IconEyeOff />}
          disabled={!canModify || !value.file || isLoading}
          onClick={() => markDirectoryFullyPlayed(value.file, false)}
        >
          {i18n.get('MarkContentsUnplayed')}
        </Menu.Item>
      </>
    )
  }

  const getSharedContentActions = (item: SharedContentInterface) => {
    switch (item.type) {
      case 'FeedAudio':
      case 'FeedImage':
      case 'FeedVideo':
        return getSharedContentFeedActions(item as Feed)
      case 'Folder':
        return getSharedContentFolderActions(item as Folder)
    }
    return undefined
  }

  const editSharedContentItem = (value: SharedContentInterface) => {
    const index = sharedContents.indexOf(value)
    setEditingIndex(index)
    setNewOpened(true)
  }

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

  const moveSharedContentItem = (oldIndex: number, newIndex: number) => {
    let sharedContentsTemp = _.cloneDeep(sharedContents)
    sharedContentsTemp = arrayMove(sharedContentsTemp, oldIndex, newIndex)
    setSharedContents(sharedContentsTemp)
  }

  const getMovableActionIcon = (value: SharedContentInterface, isDragged: boolean, isSelected: boolean) => {
    return (
      <ActionIcon
        data-movable-handle
        size={24}
        style={{ cursor: isDragged ? 'grabbing' : 'grab' }}
        variant={isDragged || isSelected ? 'outline' : 'subtle'}
        disabled={!canModify}
      >
        {
          sharedContents.indexOf(value) === 0
            ? (<IconArrowNarrowDown />)
            : sharedContents.indexOf(value) === sharedContents.length - 1
              ? (<IconArrowNarrowUp />)
              : (<IconArrowsVertical />)
        }
      </ActionIcon>
    )
  }

  const getSharedContentMenu = (value: SharedContentInterface) => {
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
          {value.active ? <IconShare size={16} /> : <IconShareOff size={16} />}
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
            {getSharedContentActions(value)}
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

  const getSharedContentsList = () => {
    return (
      <List
        lockVertically
        values={sharedContents}
        onChange={({ oldIndex, newIndex }) => {
          if (canModify) {
            moveSharedContentItem(oldIndex, newIndex)
          }
        }}
        renderList={
          ({ children, props }) => {
            return (
              <Stack gap="xs" {...props}>
                {children}
              </Stack>
            )
          }
        }
        renderItem={
          ({ value, props, isDragged, isSelected }) => {
            // react-movable has a bug, hack this until it's solved
            props.style = props.style ? { ...props.style, zIndex: isSelected ? 5000 : 'auto' } as CSSProperties : {} as CSSProperties
            return (
              <Card py="xs" shadow="sm" withBorder {...props}>
                <Group justify="space-between">
                  <Group justify="flex-start">
                    {getMovableActionIcon(value, isDragged, isSelected)}
                    {getSharedContentView(value)}
                  </Group>
                  <Group justify="flex-end">
                    {getSharedContentMenu(value)}
                  </Group>
                </Group>
              </Card>
            )
          }
        }
      />
    )
  }

  const modalForm = useForm({
    initialValues: {
      contentType: 'Folder',
      contentGroups: [] as string[],
      contentName: '',
      contentPath: '',
      contentSource: '',
      contentChilds: [] as Folder[],
    },
  })

  const getSharedContentChildsDirectoryChooser = () => {
    return modalForm.values['contentChilds'].map((child: Folder, index) => (
      <DirectoryChooser
        i18n={i18n}
        disabled={!canModify}
        size="xs"
        path={child.file}
        callback={(directory: string) => setSharedContentChild(directory, index)}
      >
      </DirectoryChooser>
    ))
  }

  const getUniqueFolders = (array: Folder[]) => {
    return Array.from(array.reduce(function (map, item) {
      if (item.file && !map.has(item.file)) map.set(item.file, item)
      return map
    }, new Map()).values())
  }

  const setSharedContentChild = (file: string, index: number) => {
    const contentChilds = _.cloneDeep(modalForm.values['contentChilds'])
    if (index < 0 && file) {
      contentChilds.push({ type: 'Folder', file: file, monitored: true, metadata: true, active: true } as Folder)
    }
    else {
      contentChilds[index].file = file
    }
    const filteredContentChilds = getUniqueFolders(contentChilds)
    modalForm.setFieldValue('contentChilds', filteredContentChilds)
  }

  const getSharedContentModifyModal = () => {
    const isNew = editingIndex < 0
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
    return (
      <Modal
        scrollAreaComponent={ScrollArea.Autosize}
        opened={newOpened}
        onClose={() => setNewOpened(false)}
        title={i18n.get('SharedContent')}
        lockScroll={false}
      >
        <Select
          disabled={!canModify || !isNew}
          label={i18n.get('Type')}
          data={data}
          maxDropdownHeight={120}
          {...modalForm.getInputProps('contentType')}
        />
        {modalForm.values['contentType'] !== 'Folder' && modalForm.values['contentType'] !== 'iTunes' && (
          <TextInput
            disabled={!canModify || modalForm.values['contentType'].startsWith('Feed')}
            label={i18n.get('Name')}
            placeholder={modalForm.values['contentType'].startsWith('Feed') ? i18n.get('NamesSetAutomaticallyFeeds') : ''}
            name="contentName"
            style={{ flex: 1 }}
            {...modalForm.getInputProps('contentName')}
          />
        )}
        {modalForm.values['contentType'] !== 'Folder' && modalForm.values['contentType'] !== 'iTunes' && (
          <TextInput
            disabled={!canModify}
            label={i18n.get('Path')}
            placeholder={modalForm.values['contentType'] !== 'VirtualFolder' ? 'Web' : ''}
            name="contentPath"
            style={{ flex: 1 }}
            {...modalForm.getInputProps('contentPath')}
          />
        )}
        {modalForm.values['contentType'] === 'Folder' || modalForm.values['contentType'] === 'iTunes'
          ? (
              <DirectoryChooser
                i18n={i18n}
                disabled={!canModify}
                label={i18n.get('Folder')}
                size="xs"
                path={modalForm.values['contentSource']}
                callback={(directory: string) => modalForm.setFieldValue('contentSource', directory)}
                placeholder={modalForm.values['contentType'] === 'iTunes' ? i18n.get('AutoDetect') : undefined}
                withAsterisk={modalForm.values['contentType'] === 'Folder'}
              >
              </DirectoryChooser>
            )
          : modalForm.values['contentType'] !== 'VirtualFolder' && (
            <TextInput
              disabled={!canModify}
              label={i18n.get('SourceURLColon')}
              name="contentSource"
              style={{ flex: 1 }}
              {...modalForm.getInputProps('contentSource')}
            />
          )}
        {modalForm.values['contentType'] === 'VirtualFolder' && (
          <Stack gap="3">
            <label>{i18n.get('SharedFolders')}</label>
            {getSharedContentChildsDirectoryChooser()}
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
          placeholder={modalForm.values['contentGroups'].length > 0 ? undefined : i18n.get('NoGroupRestrictions')}
          maxDropdownHeight={120}
          hidePickedOptions
          {...modalForm.getInputProps('contentGroups')}
        />
        <Group justify="flex-end" mt="sm">
          <Button
            variant="outline"
            disabled={isLoading}
            onClick={() => {
              if (canModify) {
                saveModal(modalForm.values)
              }
              else {
                setNewOpened(false)
              }
            }}
          >
            {canModify ? isNew ? i18n.get('Add') : i18n.get('Apply') : i18n.get('Close')}
          </Button>
        </Group>
      </Modal>
    )
  }

  const saveModal = async (values: typeof modalForm.values) => {
    const sharedContentsTemp = _.cloneDeep(sharedContents)
    const contentGroups = values.contentGroups.map(Number)

    switch (values.contentType) {
      case 'Folder':
        if (editingIndex < 0) {
          sharedContentsTemp.push({ type: values.contentType, active: true, groups: contentGroups, file: values.contentSource, monitored: true, metadata: true } as Folder)
        }
        else {
          (sharedContentsTemp[editingIndex] as Folder).groups = contentGroups;
          (sharedContentsTemp[editingIndex] as Folder).file = values.contentSource
        }
        break
      case 'VirtualFolder':
        if (editingIndex < 0) {
          sharedContentsTemp.push({ type: values.contentType, active: true, groups: contentGroups, parent: values.contentPath, name: values.contentName, childs: values.contentChilds, addToMediaLibrary: true } as VirtualFolder)
        }
        else {
          (sharedContentsTemp[editingIndex] as VirtualFolder).groups = contentGroups;
          (sharedContentsTemp[editingIndex] as VirtualFolder).parent = values.contentPath;
          (sharedContentsTemp[editingIndex] as VirtualFolder).name = values.contentName;
          (sharedContentsTemp[editingIndex] as VirtualFolder).childs = values.contentChilds
        }
        break
      case 'FeedAudio':
      case 'FeedImage':
      case 'FeedVideo':
        setLoading(true)
        values.contentName = await getFeedName(values.contentSource)
        setLoading(false)

        if (editingIndex < 0) {
          sharedContentsTemp.push({ type: values.contentType, active: true, groups: contentGroups, parent: values.contentPath, name: values.contentName, uri: values.contentSource } as Feed)
        }
        else {
          (sharedContentsTemp[editingIndex] as Feed).groups = contentGroups;
          (sharedContentsTemp[editingIndex] as Feed).parent = values.contentPath;
          (sharedContentsTemp[editingIndex] as Feed).name = values.contentName;
          (sharedContentsTemp[editingIndex] as Feed).uri = values.contentSource
        }
        break
      case 'StreamAudio':
      case 'StreamVideo':
        if (editingIndex < 0) {
          sharedContentsTemp.push({ type: values.contentType, active: true, groups: contentGroups, parent: values.contentPath, name: values.contentName, uri: values.contentSource } as Stream)
        }
        else {
          sharedContentsTemp[editingIndex].groups = contentGroups;
          (sharedContentsTemp[editingIndex] as Stream).parent = values.contentPath;
          (sharedContentsTemp[editingIndex] as Stream).name = values.contentName;
          (sharedContentsTemp[editingIndex] as Stream).uri = values.contentSource
        }
        break
      case 'iTunes':
        if (editingIndex < 0) {
          sharedContentsTemp.push({ type: values.contentType, active: true, groups: contentGroups, path: values.contentSource } as ITunes)
        }
        else {
          (sharedContentsTemp[editingIndex] as ITunes).groups = contentGroups;
          (sharedContentsTemp[editingIndex] as ITunes).path = values.contentSource
        }
        break
      case 'iPhoto':
      case 'Aperture':
        if (editingIndex < 0) {
          sharedContentsTemp.push({ type: values.contentType, active: true, groups: contentGroups } as SharedContentInterface)
        }
        else {
          (sharedContentsTemp[editingIndex] as SharedContentInterface).groups = contentGroups
        }
        break
    }
    setSharedContents(sharedContentsTemp)
    setNewOpened(false)
    setEditingIndex(-1)
  }

  const NewSharedContentButton = () => {
    return (
      <Button
        leftSection={<IconPlus />}
        variant="outline"
        onClick={() => {
          setEditingIndex(-1)
          setNewOpened(true)
        }}
      >
        {i18n.get('AddNewSharedContent')}
      </Button>
    )
  }

  const getScanSharedFoldersButton = () => {
    const haveFolder = sharedContents.find(sharedContent => sharedContent.type.startsWith('Folder'))
    return haveFolder
      ? (
          <Button
            disabled={!canModify || isLoading}
            leftSection={<IconListSearch />}
            variant="outline"
            color={sse.mediaScan ? 'red' : 'blue'}
            onClick={() => sse.mediaScan ? scanAllSharedFoldersCancel() : scanAllSharedFolders()}
          >
            {i18n.get(sse.mediaScan ? 'CancelScanningSharedFolders' : 'ScanAllSharedFolders')}
            {sse.mediaScan && (<IconLoader />)}
          </Button>
        )
      : null
  }

  useEffect(() => {
    form.setFieldValue('shared_content', sharedContents)
  }, [sharedContents])

  useEffect(() => {
    const sharedContent = editingIndex > -1 ? sharedContents.at(editingIndex) : null
    const isNew = !sharedContent
    const contentType = isNew ? 'Folder' : sharedContent.type
    const contentGroups = isNew || !sharedContent.groups ? [] : sharedContent.groups.map(String)
    const contentName = isNew || sharedContent.type === 'Folder' ? '' : (sharedContent as any).name
    const contentPath = isNew || sharedContent.type === 'Folder' ? '' : (sharedContent as any).parent
    const contentSource = isNew || sharedContent.type === 'VirtualFolder' ? '' : (sharedContent as any).uri ? (sharedContent as any).uri : (sharedContent as any).file ? (sharedContent as any).file : (sharedContent as any).path
    const contentChilds = isNew || sharedContent.type !== 'VirtualFolder' ? [] : (sharedContent as any).childs ? (sharedContent as any).childs : []
    modalForm.setValues({ contentType: contentType, contentGroups: contentGroups, contentName: contentName, contentPath: contentPath, contentSource: contentSource, contentChilds: contentChilds })
  }, [sharedContents, editingIndex])

  useEffect(() => {
    const sharedContentTemp = _.merge([], configuration.shared_content)
    setSharedContents(sharedContentTemp)
  }, [configuration])

  return (
    <>
      <Group mb="md">
        <NewSharedContentButton />
        {getScanSharedFoldersButton()}
      </Group>
      {getSharedContentModifyModal()}
      {getSharedContentsList()}
    </>
  )
}

interface SharedContentInterface {
  type: string
  active: boolean
  groups: number[]
}

interface Folder extends SharedContentInterface {
  file: string
  monitored: boolean
  metadata: boolean
}

interface VirtualFolder extends SharedContentInterface {
  parent: string
  name: string
  childs: SharedContentInterface[]
  addToMediaLibrary: boolean
}

interface Feed extends SharedContentInterface {
  parent: string
  name: string
  uri: string
}

interface Stream extends Feed {
  thumbnail: string
}

interface ITunes extends SharedContentInterface {
  path: string
}
