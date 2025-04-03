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
import { useForm } from '@mantine/form'
import _ from 'lodash'
import { useEffect, useState } from 'react'

import { I18nInterface } from '../../services/i18n-service'
import { ServerEventInterface } from '../../services/server-event-service'
import { Feed, Folder, getFeedName, ITunes, NamedSharedContent, SharedContentConfiguration, SharedContentData, SharedContentInterface, Stream, VirtualFolder } from '../../services/shared-service'
import SharedContentModal from './SharedContentModal'
import SharedContentList from './SharedContentList'
import NewSharedContentButton from './NewSharedContentButton'
import ScanSharedFoldersButton from './ScanSharedFoldersButton'

export default function SharedContentSettings({
  i18n,
  canModify,
  sse,
  configuration,
  sharedContents,
  setSharedContents,
}: {
  i18n: I18nInterface
  sse: ServerEventInterface
  canModify: boolean
  configuration: SharedContentConfiguration
  sharedContents: SharedContentInterface[]
  setSharedContents: (sharedContents: SharedContentInterface[]) => void
}) {
  const [editOpened, setEditOpened] = useState(false)
  const modalForm = useForm()

  /**
   * Shared Directories
  */

  const setEditModalData = (index: number) => {
    const sharedContent = index > -1 ? sharedContents.at(index) : null
    const isNew = !sharedContent
    const isFolder = !isNew && sharedContent.type === 'Folder'
    const isITunes = !isNew && sharedContent.type === 'ITunes'
    const isVirtualFolder = !isNew && sharedContent.type === 'VirtualFolder'
    const isFeed = !isNew && (sharedContent.type.endsWith('Feed') || sharedContent.type.endsWith('Stream'))
    const type = isNew ? 'Folder' : sharedContent.type

    const groups = isNew || !sharedContent.groups ? [] : sharedContent.groups.map(String)
    const name = isNew || isFolder || isITunes ? '' : (sharedContent as NamedSharedContent).name
    const parent = isNew || isFolder || isITunes ? '' : (sharedContent as NamedSharedContent).parent
    const source = isFeed ? (sharedContent as Feed).uri : isFolder ? (sharedContent as Folder).file : isITunes ? (sharedContent as ITunes).path : ''
    const childs = isVirtualFolder && (sharedContent as VirtualFolder).childs ? (sharedContent as VirtualFolder).childs : []
    modalForm.setValues({
      index,
      type,
      groups,
      name,
      parent,
      source,
      childs,
    })
  }

  const openEditModal = (index: number) => {
    setEditModalData(index)
    setEditOpened(true)
  }

  const saveModal = async () => {
    const sharedContentsTemp = _.cloneDeep(sharedContents)
    const values = {
      index: modalForm.values.index as number,
      type: modalForm.values.type as string,
      groups: modalForm.values.groups as string[],
      name: modalForm.values.name as string,
      parent: modalForm.values.parent as string,
      source: modalForm.values.source as string,
      childs: modalForm.values.childs as SharedContentInterface[],
    } as SharedContentData
    const groups = values.groups.map(Number)

    switch (values.type) {
      case 'Folder':
        if (values.index < 0) {
          sharedContentsTemp.push({ type: values.type, active: true, groups: groups, file: values.source, monitored: true, metadata: true } as Folder)
        }
        else {
          (sharedContentsTemp[values.index] as Folder).groups = groups;
          (sharedContentsTemp[values.index] as Folder).file = values.source
        }
        break
      case 'VirtualFolder':
        if (values.index < 0) {
          sharedContentsTemp.push({ type: values.type, active: true, groups: groups, parent: values.parent, name: values.name, childs: values.childs, addToMediaLibrary: true } as VirtualFolder)
        }
        else {
          (sharedContentsTemp[values.index] as VirtualFolder).groups = groups;
          (sharedContentsTemp[values.index] as VirtualFolder).parent = values.parent;
          (sharedContentsTemp[values.index] as VirtualFolder).name = values.name;
          (sharedContentsTemp[values.index] as VirtualFolder).childs = values.childs
        }
        break
      case 'FeedAudio':
      case 'FeedImage':
      case 'FeedVideo':
        values.name = await getFeedName(values.source)
        if (values.index < 0) {
          sharedContentsTemp.push({ type: values.type, active: true, groups: groups, parent: values.parent, name: values.name, uri: values.source } as Feed)
        }
        else {
          (sharedContentsTemp[values.index] as Feed).groups = groups;
          (sharedContentsTemp[values.index] as Feed).parent = values.parent;
          (sharedContentsTemp[values.index] as Feed).name = values.name;
          (sharedContentsTemp[values.index] as Feed).uri = values.source
        }
        break
      case 'StreamAudio':
      case 'StreamVideo':
        if (values.index < 0) {
          sharedContentsTemp.push({ type: values.type, active: true, groups: groups, parent: values.parent, name: values.name, uri: values.source } as Stream)
        }
        else {
          sharedContentsTemp[values.index].groups = groups;
          (sharedContentsTemp[values.index] as Stream).parent = values.parent;
          (sharedContentsTemp[values.index] as Stream).name = values.name;
          (sharedContentsTemp[values.index] as Stream).uri = values.source
        }
        break
      case 'iTunes':
        if (values.index < 0) {
          sharedContentsTemp.push({ type: values.type, active: true, groups: groups, path: values.source } as ITunes)
        }
        else {
          (sharedContentsTemp[values.index] as ITunes).groups = groups;
          (sharedContentsTemp[values.index] as ITunes).path = values.source
        }
        break
      case 'iPhoto':
      case 'Aperture':
        if (values.index < 0) {
          sharedContentsTemp.push({ type: values.type, active: true, groups: groups } as SharedContentInterface)
        }
        else {
          (sharedContentsTemp[values.index] as SharedContentInterface).groups = groups
        }
        break
    }
    setSharedContents(sharedContentsTemp)
    setEditOpened(false)
    setEditModalData(-1)
  }

  useEffect(() => {
    // avoid mistake
    setEditOpened(false)
    setEditModalData(-1)
  }, [sharedContents])

  useEffect(() => {
    const sharedContentTemp = _.merge([], configuration.shared_content)
    setSharedContents(sharedContentTemp)
  }, [configuration])

  return (
    <>
      <Group mb="md">
        <NewSharedContentButton i18n={i18n} openEditModal={openEditModal} />
        <ScanSharedFoldersButton i18n={i18n} sse={sse} sharedContents={sharedContents} canModify={canModify} />
      </Group>
      <SharedContentModal i18n={i18n} canModify={canModify} opened={editOpened} setOpened={setEditOpened} form={modalForm} configuration={configuration} save={saveModal} />
      <SharedContentList
        i18n={i18n}
        sharedContents={sharedContents}
        setSharedContents={setSharedContents}
        openEditModal={openEditModal}
        groups={configuration.groups}
        canModify={canModify}
      />
    </>
  )
}
