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
import { ActionIcon, Button, Code, Group, Modal, ScrollArea, Select, Table, TextInput, Tooltip } from '@mantine/core';
import { useForm } from '@mantine/form';
import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import _ from 'lodash';
import { useContext, useEffect, useState } from 'react';
import { arrayMove, List } from 'react-movable';
import { Analyze, AnalyzeOff, ArrowsVertical, Edit, EyeCheck, EyeOff, FolderX, ListSearch, Loader, Plus, Share, ShareOff, SquareX, ZoomCheck } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import ServerEventContext from '../../contexts/server-event-context';
import SessionContext from '../../contexts/session-context';
import { havePermission, Permissions } from '../../services/accounts-service';
import { sendAction } from '../../services/actions-service';
import { defaultTooltipSettings, openGitHubNewIssue, sharedApiUrl } from '../../utils';
import DirectoryChooser from '../DirectoryChooser/DirectoryChooser';

export default function SharedContentSettings(
  form: any,
  configuration: any,
) {
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
  const sse = useContext(ServerEventContext);
  const canModify = havePermission(session, Permissions.settings_modify);
  const [sharedContents, setSharedContents] = useState([] as SharedContent[]);
  const [isLoading, setLoading] = useState(false);
  const [newOpened, setNewOpened] = useState(false);
  const [editingIndex, setEditingIndex] = useState(-1);

  /**
   * Shared Directories
  */
  const scanAllSharedFolders = async () => {
    setLoading(true);
    try {
      await sendAction('Server.ScanAllSharedFolders');
    } catch (err) {
      console.error(err);
    }
    setLoading(false);
  }

  const scanAllSharedFoldersCancel = async () => {
    setLoading(true);
    try {
      await sendAction('Server.ScanAllSharedFoldersCancel');
    } catch (err) {
      console.error(err);
    }
    setLoading(false);
  }

  const markDirectoryFullyPlayed = async (item: string, isPlayed: boolean) => {
    setLoading(true);
    try {
      await axios.post(
        sharedApiUrl + 'mark-directory',
        { directory: item, isPlayed },
      );

      showNotification({
        message: i18n.get['Saved'],
      })
    } catch (err) {
      showNotification({
        color: 'red',
        title: i18n.get['Error'],
        message: i18n.get['ConfigurationNotSaved'] + ' ' + i18n.get['ClickHereReportBug'],
        onClick: () => { openGitHubNewIssue(); },
      })
    }
    setLoading(false);
  }

  /**
   * SharedContent
  */
  const updateSharedContentFeedName = async (value: Feed) => {
    setLoading(true);
    try {
      const sharedContentsTemp = _.cloneDeep(sharedContents);
      const index = sharedContents.indexOf(value);
      const response: { data: { name: string } } = await axios.post(sharedApiUrl + 'web-content-name', { source: value.uri });
      if (response.data.name) {
        (sharedContentsTemp[index] as Feed).name = response.data.name;
        setSharedContents(sharedContentsTemp);
      } else {
        showNotification({
          color: 'orange',
          title: i18n.get['Information'],
          message: i18n.get['FeedNameNotFound'],
        })
      }
    } catch (err) {
      showNotification({
        color: 'red',
        title: i18n.get['Error'],
        message: i18n.get['DataNotReceived'],
      })
    }
    setLoading(false);
  }

  const getSharedContentTypeLocalized = (value: string) => {
    switch (value) {
      case 'FeedAudio':
        return i18n.get['Podcast'];
      case 'FeedImage':
        return i18n.get['ImageFeed'];
      case 'FeedVideo':
        return i18n.get['VideoFeed'];
      case 'StreamAudio':
        return i18n.get['AudioStream'];
      case 'StreamVideo':
        return i18n.get['VideoStream'];
      case 'Folder':
        return i18n.get['Folder'];
      case 'VirtualFolder':
        return i18n.get['VirtualFolders'];
    }
  }

  const getSharedContentParents = (value: Feed | Stream | VirtualFolder) => {
    if (!value.parent) {
      return null;
    }
    const parents = value.parent.split('/');
    return parents.map((parent: string, index) => (
      <Code color='teal' key={index} mr='xs'>{parent}</Code>
    ));
  }

  const getSharedContentFeedView = (value: Feed) => {
    const type = getSharedContentTypeLocalized(value.type);
    return (
      <div>
        <div>{type}</div>
        <div>{getSharedContentParents(value)}{value.name ? <Code color='teal'>{value.name}</Code> : <Code color='red'>{i18n.get['FeedNameNotFound']}</Code>}</div>
        <div><Code color='blue'>{value.uri}</Code></div>
      </div>
    );
  }

  const getSharedContentStreamView = (value: Stream) => {
    const type = getSharedContentTypeLocalized(value.type);
    return (
      <div>
        <div>{type}</div>
        <div>{getSharedContentParents(value)}<Code color='teal'>{value.name}</Code></div>
        <div><Code color='blue'>{value.uri}</Code></div>
      </div>
    );
  }

  const toggleFolderMonitored = (value: Folder) => {
    const sharedContentsTemp = _.cloneDeep(sharedContents);
    const index = sharedContents.indexOf(value);
    (sharedContentsTemp[index] as Folder).monitored = !(sharedContentsTemp[index] as Folder).monitored;
    setSharedContents(sharedContentsTemp);
  }

  const getFolderName = (value: string) => {
    return value?.split('\\').pop()?.split('/').pop();
  }

  const getSharedContentFolderView = (value: Folder) => {
    const type = getSharedContentTypeLocalized(value.type);
    return (
      <div>
        <div>{type}</div>
        <div><Code color='teal'>{getFolderName(value.file)}</Code></div>
        <div><Code color='blue'>{value.file}</Code></div>
      </div>
    );
  }

  const getSharedContentVirtualFolderChildsView = (value: VirtualFolder) => {
    return value.childs ? value.childs.map((child: SharedContent, index) => (
      child.type === 'Folder' &&
      <div key={index}><Code color='blue'>{(child as Folder).file}</Code></div>
    )) : null;
  }

  const getSharedContentVirtualFolderView = (value: VirtualFolder) => {
    const type = getSharedContentTypeLocalized(value.type);
    return (
      <div>
        <div>{type}</div>
        <div>{getSharedContentParents(value)}<Code color='teal'>{value.name}</Code></div>
        {getSharedContentVirtualFolderChildsView(value)}
      </div>
    );
  }

  const getSharedContentView = (value: SharedContent) => {
    switch (value.type) {
      case 'FeedAudio':
      case 'FeedImage':
      case 'FeedVideo':
        return getSharedContentFeedView(value as Feed);
      case 'StreamAudio':
      case 'StreamVideo':
        return getSharedContentStreamView(value as Stream);
      case 'Folder':
        return getSharedContentFolderView(value as Folder);
      case 'VirtualFolder':
        return getSharedContentVirtualFolderView(value as VirtualFolder);
    }
    return (<div>{i18n.get['Unknown']}</div>);
  }

  const getSharedContentFeedActions = (value: Feed) => {
    return (
      <Tooltip label={i18n.get['UpdateFeedName']} {...defaultTooltipSettings}>
        <ActionIcon
          color='blue'
          variant='transparent'
          disabled={!canModify || !value.uri || isLoading}
          onClick={() => updateSharedContentFeedName(value)}
        >
          <ZoomCheck />
        </ActionIcon>
      </Tooltip>
    );
  }

  const getSharedContentFolderActions = (value: Folder) => {
    return (
      <>
        <Tooltip label={i18n.get['MonitorPlayedStatusFiles']} {...defaultTooltipSettings}>
          <ActionIcon
            color='blue'
            variant='transparent'
            disabled={!canModify || !configuration.use_cache}
            onClick={() => toggleFolderMonitored(value)}
          >
            {value.monitored ? <Analyze /> : <AnalyzeOff />}
          </ActionIcon>
        </Tooltip>
        <Tooltip label={i18n.get['MarkContentsFullyPlayed']} {...defaultTooltipSettings}>
          <ActionIcon
            color='blue'
            variant='transparent'
            disabled={!canModify || !value.file || isLoading || !configuration.use_cache}
            onClick={() => markDirectoryFullyPlayed(value.file, true)}
          >
            <EyeCheck />
          </ActionIcon>
        </Tooltip>
        <Tooltip label={i18n.get['MarkContentsUnplayed']} {...defaultTooltipSettings}>
          <ActionIcon
            color='green'
            variant='transparent'
            disabled={!canModify || !value.file || isLoading || !configuration.use_cache}
            onClick={() => markDirectoryFullyPlayed(value.file, false)}
          >
            <EyeOff />
          </ActionIcon>
        </Tooltip>
      </>
    );
  }

  const getSharedContentActions = (item: SharedContent) => {
    switch (item.type) {
      case 'FeedAudio':
      case 'FeedImage':
      case 'FeedVideo':
        return getSharedContentFeedActions(item as Feed);
      case 'Folder':
        return getSharedContentFolderActions(item as Folder);
    }
    return (<></>);
  }

  const editSharedContentItem = (value: SharedContent) => {
    const index = sharedContents.indexOf(value);
    setEditingIndex(index);
    setNewOpened(true);
  }

  const toogleSharedContentItemActive = (item: SharedContent) => {
    const sharedContentsTemp = _.cloneDeep(sharedContents);
    const index = sharedContents.indexOf(item);
    sharedContentsTemp[index].active = !sharedContentsTemp[index].active;
    setSharedContents(sharedContentsTemp);
  }

  const removeSharedContentItem = (item: SharedContent) => {
    const sharedContentsTemp = _.cloneDeep(sharedContents);
    const index = sharedContents.indexOf(item);
    sharedContentsTemp.splice(index, 1);
    setSharedContents(sharedContentsTemp);
  }

  const moveSharedContentItem = (oldIndex: number, newIndex: number) => {
    let sharedContentsTemp = _.cloneDeep(sharedContents);
    sharedContentsTemp = arrayMove(sharedContentsTemp, oldIndex, newIndex);
    setSharedContents(sharedContentsTemp);
  }

  const getSharedContentsList = () => {
    return (
      <List
        lockVertically
        values={sharedContents}
        onChange={({ oldIndex, newIndex }) => {
          canModify && moveSharedContentItem(oldIndex, newIndex);
        }}
        renderList={
          ({ children, props }) => {
            return (
              <Table highlightOnHover>
                <thead>
                  <tr>
                    <th></th>
                    <th>Share</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody {...props}>
                  {children}
                </tbody>
              </Table>
            )
          }
        }
        renderItem={
          ({ value, props, isDragged, isSelected }) => {
            return (
              <tr {...props}>
                <td>
                  <ActionIcon
                    data-movable-handle
                    size={20}
                    style={{ cursor: isDragged ? 'grabbing' : 'grab', }}
                    variant={isDragged || isSelected ? 'outline' : 'subtle'}
                    disabled={!canModify}
                  >
                    <ArrowsVertical />
                  </ActionIcon>
                </td>
                <td>
                  {getSharedContentView(value)}
                </td>
                <td>
                  <Group position='right'>
                    {getSharedContentActions(value)}
                    <Tooltip label={i18n.get['Edit']} {...defaultTooltipSettings}>
                      <ActionIcon
                        color='green'
                        variant='transparent'
                        disabled={!canModify}
                        onClick={() => editSharedContentItem(value)}
                      >
                        <Edit />
                      </ActionIcon>
                    </Tooltip>
                    <Tooltip label={value.active ? i18n.get['Disable'] : i18n.get['Enable']} {...defaultTooltipSettings}>
                      <ActionIcon
                        color={value.active ? 'blue' : 'orange'}
                        variant='transparent'
                        disabled={!canModify}
                        onClick={() => toogleSharedContentItemActive(value)}
                      >
                        {value.active ? <Share /> : <ShareOff />}
                      </ActionIcon>
                    </Tooltip>
                    <Tooltip label={i18n.get['Delete']} {...defaultTooltipSettings}>
                      <ActionIcon
                        color='red'
                        variant='transparent'
                        disabled={!canModify}
                        onClick={() => removeSharedContentItem(value)}
                      >
                        <SquareX />
                      </ActionIcon>
                    </Tooltip>
                  </Group>
                </td>
              </tr>
            )
          }
        }
      />
    );
  }

  const modalForm = useForm({
    initialValues: {
      contentType: 'Folder',
      contentName: '',
      contentPath: '',
      contentSource: '',
      contentChilds: [] as Folder[],
    },
  });

  const getSharedContentChilds = () => {
    return modalForm.values['contentChilds'] && modalForm.values['contentChilds'].length > 0 ? (<>
      <label>{i18n.get['SharedFolders']}</label>
      {getSharedContentChildsDirectoryChooser()}
    </>) : null;
  }

  const getSharedContentChildsDirectoryChooser = () => {
    return modalForm.values['contentChilds'].map((child: Folder, index) => (
      <Group key={index} position='apart' spacing={0}>
        <DirectoryChooser
          disabled={!canModify}
          size='xs'
          path={child.file}
          callback={(directory: string) => setSharedContentChild(directory, index)}
        ></DirectoryChooser>
        <ActionIcon variant='filled' color='red' onClick={() => removeSharedContentChild(index)}>
          <FolderX size={18} />
        </ActionIcon>
      </Group>
    ))
  }

  const setSharedContentChild = (file: string, index: number) => {
    const contentChilds = _.cloneDeep(modalForm.values['contentChilds']);
    if (index < 0) {
      contentChilds.push({ type: 'Folder', file: file, monitored: true, metadata: true, active: true } as Folder);
    } else {
      contentChilds[index].file = file;
    }
    modalForm.setFieldValue('contentChilds', contentChilds);
  }

  const removeSharedContentChild = (index: number) => {
    const contentChilds = _.cloneDeep(modalForm.values['contentChilds']);
    contentChilds.splice(index, 1);
    modalForm.setFieldValue('contentChilds', contentChilds);
  }

  const getSharedContentModifyModal = () => {
    const isNew = editingIndex < 0;
    return (
      <Modal scrollAreaComponent={ScrollArea.Autosize} opened={newOpened} onClose={() => setNewOpened(false)} title={i18n.get['SharedContent']}>
        <Select
          disabled={!canModify || !isNew}
          label={i18n.get['Type']}
          data={[
            { value: 'Folder', label: i18n.get['Folder'] },
            { value: 'VirtualFolder', label: i18n.get['VirtualFolders'] },
            { value: 'FeedAudio', label: i18n.get['Podcast'] },
            { value: 'FeedImage', label: i18n.get['ImageFeed'] },
            { value: 'FeedVideo', label: i18n.get['VideoFeed'] },
            { value: 'StreamAudio', label: i18n.get['AudioStream'] },
            { value: 'StreamVideo', label: i18n.get['VideoStream'] },
          ]}
          {...modalForm.getInputProps('contentType')}
        ></Select>
        {modalForm.values['contentType'] !== 'Folder' && (
          <TextInput
            disabled={!canModify || modalForm.values['contentType'].startsWith('Feed')}
            label={i18n.get['Name']}
            placeholder={modalForm.values['contentType'].startsWith('Feed') ? i18n.get['NamesSetAutomaticallyFeeds'] : ''}
            name='contentName'
            sx={{ flex: 1 }}
            {...modalForm.getInputProps('contentName')}
          />
        )}
        {modalForm.values['contentType'] !== 'Folder' && (
          <TextInput
            disabled={!canModify}
            label={i18n.get['Path']}
            placeholder={modalForm.values['contentType'] !== 'VirtualFolder' ? 'Web' : ''}
            name='contentPath'
            sx={{ flex: 1 }}
            {...modalForm.getInputProps('contentPath')}
          />
        )}
        {modalForm.values['contentType'] === 'Folder' ? (
          <DirectoryChooser
            disabled={!canModify}
            label={i18n.get['Folder']}
            size='xs'
            path={modalForm.values['contentSource']}
            callback={(directory: string) => modalForm.setFieldValue('contentSource', directory)}
          ></DirectoryChooser>
        ) : modalForm.values['contentType'] !== 'VirtualFolder' && (
          <TextInput
            disabled={!canModify}
            label={i18n.get['SourceURLColon']}
            name='contentSource'
            sx={{ flex: 1 }}
            {...modalForm.getInputProps('contentSource')}
          />
        )}
        {modalForm.values['contentType'] === 'VirtualFolder' && (<>
          {getSharedContentChilds()}
          <label>{i18n.get['AddFolder']}</label>
          <DirectoryChooser
            disabled={!canModify}
            size='xs'
            path={''}
            callback={(directory: string) => setSharedContentChild(directory, -1)}
          ></DirectoryChooser>
        </>)}
        <Group position='right' mt='sm'>
          <Button variant='outline' onClick={() => { canModify ? saveModal(modalForm.values) : setNewOpened(false) }}>
            {canModify ? isNew ? i18n.get['Add'] : i18n.get['Apply'] : i18n.get['Close']}
          </Button>
        </Group>
      </Modal>
    );
  }

  const saveModal = (values: typeof modalForm.values) => {
    const sharedContentsTemp = _.cloneDeep(sharedContents);
    switch (values.contentType) {
      case 'Folder':
        if (editingIndex < 0) {
          sharedContentsTemp.push({ type: values.contentType, active: true, file: values.contentSource, monitored: true, metadata: true } as Folder);
        } else {
          (sharedContentsTemp[editingIndex] as Folder).file = values.contentSource;
        }
        break;
      case 'VirtualFolder':
        if (editingIndex < 0) {
          sharedContentsTemp.push({ type: values.contentType, active: true, parent: values.contentPath, name: values.contentName, childs: values.contentChilds, addToMediaLibrary: true } as VirtualFolder);
        } else {
          (sharedContentsTemp[editingIndex] as VirtualFolder).parent = values.contentPath;
          (sharedContentsTemp[editingIndex] as VirtualFolder).name = values.contentName;
          (sharedContentsTemp[editingIndex] as VirtualFolder).childs = values.contentChilds;
        }
        break;
      case 'FeedAudio':
      case 'FeedImage':
      case 'FeedVideo':
        if (editingIndex < 0) {
          sharedContentsTemp.push({ type: values.contentType, active: true, parent: values.contentPath, name: values.contentName, uri: values.contentSource } as Feed);
        } else {
          (sharedContentsTemp[editingIndex] as Feed).parent = values.contentPath;
          (sharedContentsTemp[editingIndex] as Feed).name = values.contentName;
          (sharedContentsTemp[editingIndex] as Feed).uri = values.contentSource;
        }
        break;
      case 'StreamAudio':
      case 'StreamVideo':
        if (editingIndex < 0) {
          sharedContentsTemp.push({ type: values.contentType, active: true, parent: values.contentPath, name: values.contentName, uri: values.contentSource } as Stream);
        } else {
          (sharedContentsTemp[editingIndex] as Stream).parent = values.contentPath;
          (sharedContentsTemp[editingIndex] as Stream).name = values.contentName;
          (sharedContentsTemp[editingIndex] as Stream).uri = values.contentSource;
        }
        break;
    }
    setSharedContents(sharedContentsTemp);
    setNewOpened(false);
    setEditingIndex(-1);
  };

  const getScanSharedFoldersButton = () => {
    const haveFolder = sharedContents.find(sharedContent => sharedContent.type.startsWith('Folder'));
    return haveFolder ? (
      <Tooltip label={i18n.get[sse.scanLibrary.running ? 'CancelScanningSharedFolders' : 'ScanAllSharedFolders']} {...defaultTooltipSettings}>
        <ActionIcon
          size='xl'
          disabled={!canModify || isLoading || !sse.scanLibrary.enabled || (!configuration.use_cache && !sse.scanLibrary.running)}
          variant='transparent'
          color={sse.scanLibrary.running ? 'red' : 'blue'}
          title={i18n.get[sse.scanLibrary.running ? 'CancelScanningSharedFolders' : 'ScanAllSharedFolders']}
          onClick={() => sse.scanLibrary.running ? scanAllSharedFoldersCancel() : scanAllSharedFolders()}
        >
          <ListSearch />
          {sse.scanLibrary.running && (<Loader />)}
        </ActionIcon>
      </Tooltip>
    ) : null;
  }

  useEffect(() => {
    form.setFieldValue('shared_content', sharedContents);
  }, [sharedContents]);

  useEffect(() => {
    const sharedContent = editingIndex > -1 ? sharedContents.at(editingIndex) : null;
    const isNew = !sharedContent;
    const contentType = isNew ? 'Folder' : sharedContent.type;
    const contentName = isNew || sharedContent.type === 'Folder' ? '' : (sharedContent as any).name;
    const contentPath = isNew || sharedContent.type === 'Folder' ? '' : (sharedContent as any).parent;
    const contentSource = isNew || sharedContent.type === 'VirtualFolder' ? '' : (sharedContent as any).uri ? (sharedContent as any).uri : (sharedContent as any).file;
    const contentChilds = isNew || sharedContent.type !== 'VirtualFolder' ? [] : (sharedContent as any).childs ? (sharedContent as any).childs : [];
    modalForm.setValues({ contentType: contentType, contentName: contentName, contentPath: contentPath, contentSource: contentSource, contentChilds: contentChilds });
  }, [sharedContents, editingIndex]);

  useEffect(() => {
    const sharedContentTemp = _.merge([], configuration.shared_content);
    setSharedContents(sharedContentTemp);
  }, [configuration]);

  return (
    <>
      <Group>
        <Button leftIcon={<Plus />} variant='outline' onClick={() => { setEditingIndex(-1); setNewOpened(true) }}>
          {i18n.get['AddNewSharedContent']}
        </Button>
        {getScanSharedFoldersButton()}
      </Group>
      {getSharedContentModifyModal()}
      {getSharedContentsList()}
    </>
  );
}

interface SharedContent {
  type: string;
  active: boolean;
}

interface Folder extends SharedContent {
  file: string;
  monitored: boolean;
  metadata: boolean;
}

interface VirtualFolder extends SharedContent {
  parent: string;
  name: string;
  childs: SharedContent[];
  addToMediaLibrary: boolean;
}

interface Feed extends SharedContent {
  parent: string;
  name: string;
  uri: string;
}

interface Stream extends Feed {
  thumbnail: string;
}