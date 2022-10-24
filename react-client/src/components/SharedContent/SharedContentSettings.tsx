import { Accordion, ActionIcon, Group, Select, Switch, Table, Text, TextInput, Tooltip } from '@mantine/core';
import { UseFormReturnType } from '@mantine/form';
import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import _ from 'lodash';
import { useContext, useEffect, useState } from 'react';
import { arrayMove, List } from 'react-movable';
import { ArrowsVertical, EyeCheck, EyeOff, ListSearch, Loader, SquarePlus, SquareX, ZoomCheck } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import ServerEventContext from '../../contexts/server-event-context';
import SessionContext from '../../contexts/session-context';
import { havePermission, Permissions } from '../../services/accounts-service';
import { sendAction } from '../../services/actions-service';
import { defaultTooltipSettings, openGitHubNewIssue, settingsApiUrl } from '../../utils';
import DirectoryChooser from '../DirectoryChooser/DirectoryChooser';

export default function SharedContentSettings(
  form: any,
  configuration: any,
) {
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
  const sse = useContext(ServerEventContext);
  const canModify = havePermission(session, Permissions.settings_modify);
  const [sharedDirectories, setSharedDirectories] = useState([] as SharedDirectory[]);
  const [sharedWebContent, setSharedWebContent] = useState([] as SharedWebContentItem[]);
  const [isLoading, setLoading] = useState(false);
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

  const markDirectoryFullyPlayed = async (item: SharedDirectory, isPlayed: boolean) => {
    setLoading(true);
    try {
      await axios.post(
        settingsApiUrl + 'mark-directory',
        { directory: item.directory, isPlayed },
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

  const updateSharedDirectoriesToSave = (sharedDirectoriesTemp: SharedDirectory[]) => {
    setSharedDirectories(sharedDirectoriesTemp);

    const sharedDirectoriesArray = [] as string[];
    const monitoredDirectoriesArray = [] as string[];
    _.each(sharedDirectoriesTemp, (sharedDirectory) => {
      if (sharedDirectory.directory) {
        sharedDirectoriesArray.push(sharedDirectory.directory);
        if (sharedDirectory.isMonitored) {
          monitoredDirectoriesArray.push(sharedDirectory.directory);
        }
      }
    });
    form.setFieldValue('folders', sharedDirectoriesArray);
    form.setFieldValue('folders_monitored', monitoredDirectoriesArray);
  }

  const moveSharedDirectory = (oldIndex: number, newIndex: number) => {
    let sharedDirectoriesTemp = _.cloneDeep(sharedDirectories);
    sharedDirectoriesTemp = arrayMove(sharedDirectoriesTemp, oldIndex, newIndex);
    updateSharedDirectoriesToSave(sharedDirectoriesTemp);
  }

  const toggleMonitored = (item: SharedDirectory, isMonitored: boolean) => {
    const sharedDirectoriesTemp = _.cloneDeep(sharedDirectories);
    const index = _.findIndex(sharedDirectoriesTemp, (realItem) => {
      return realItem.directory === item.directory;
    });
    sharedDirectoriesTemp[index].isMonitored = isMonitored;
    updateSharedDirectoriesToSave(sharedDirectoriesTemp);
  }

  const setDirectory = (item: SharedDirectory, directory: string) => {
    const sharedDirectoriesTemp = _.cloneDeep(sharedDirectories);
    const index = _.findIndex(sharedDirectoriesTemp, (realItem) => {
      return realItem.directory === item.directory;
    });
    sharedDirectoriesTemp[index].directory = directory;

    // ensure there is always an empty entry at the bottom
    if (index === sharedDirectoriesTemp.length - 1) {
      sharedDirectoriesTemp.push({ directory: '', isMonitored: true } as SharedDirectory);
    }
    updateSharedDirectoriesToSave(sharedDirectoriesTemp);
  }

  const removeDirectory = (item: SharedDirectory) => {
    const sharedDirectoriesTemp = _.cloneDeep(sharedDirectories);
    const index = _.findIndex(sharedDirectoriesTemp, (realItem) => {
      return realItem.directory === item.directory;
    });
    sharedDirectoriesTemp.splice(index, 1);
    updateSharedDirectoriesToSave(sharedDirectoriesTemp);
  }

  /**
   * WebContent
  */
  const updateSharedWebContentToSave = (sharedWebContentTemp: SharedWebContentItem[]) => {
    setSharedWebContent(sharedWebContentTemp);
	const sharedWebContentForm = sharedWebContentTemp.filter(sharedWebContentItem => !sharedWebContentItem.isnew);
    form.setFieldValue('shared_web_content', sharedWebContentForm);
  }

  const setSharedWebContentItemAttribute = (attribute: 'name' | 'folders' | 'source' | 'type', item: SharedWebContentItem, value: string | null) => {
    const sharedWebContentTemp = _.cloneDeep(sharedWebContent);
    const index = _.findIndex(sharedWebContentTemp, (realItem) => {
      return realItem.source === item.source;
    });
    if (value && sharedWebContentTemp[index][attribute] !== value) {
      if (attribute === 'type' && value !== 'audiostream' && value !== 'videostream') {
        sharedWebContentTemp[index]['name'] = '';
      }
      sharedWebContentTemp[index][attribute] = value;
      updateSharedWebContentToSave(sharedWebContentTemp);
    }
  }

  const getSharedWebContentFeedName = async (item: SharedWebContentItem) => {
    setLoading(true);
    try {
      const sharedWebContentTemp = _.cloneDeep(sharedWebContent);
      const index = _.findIndex(sharedWebContentTemp, (realItem) => {
        return realItem.source === item.source;
      });
      const response: { data: { name: string } } = await axios.post(settingsApiUrl + 'web-content-name', { source: item.source });
      if (response.data.name) {
        sharedWebContentTemp[index].name = response.data.name;
        updateSharedWebContentToSave(sharedWebContentTemp);
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
        message: i18n.get['DatasNotReceived'],
      })
    }
    setLoading(false);
  }

  const addSharedWebContentItem = () => {
    const sharedWebContentTemp = _.cloneDeep(sharedWebContent);
    sharedWebContentTemp[sharedWebContentTemp.length - 1].isnew = false;
    sharedWebContentTemp.push({ source: '', name: '', type: 'audiofeed', folders: 'Web,', isnew: true } as SharedWebContentItem);
    updateSharedWebContentToSave(sharedWebContentTemp);
  }

  const removeSharedWebContentItem = (item: SharedWebContentItem) => {
    const sharedWebContentTemp = _.cloneDeep(sharedWebContent);
    const index = _.findIndex(sharedWebContentTemp, (realItem) => {
      return realItem.source === item.source;
    });
    sharedWebContentTemp.splice(index, 1);
    updateSharedWebContentToSave(sharedWebContentTemp);
  }

  const moveSharedWebContentItem = (oldIndex: number, newIndex: number) => {
    let sharedWebContentTemp = _.cloneDeep(sharedWebContent);
    sharedWebContentTemp = arrayMove(sharedWebContentTemp, oldIndex, newIndex);
    updateSharedWebContentToSave(sharedWebContentTemp);
  }

  useEffect(() => {
        const sharedWebContentTemp = _.merge([], configuration.shared_web_content);
		sharedWebContentTemp.push({ source: '', name: '', type: 'audiofeed', folders: 'Web,', isnew: true } as SharedWebContentItem);
        setSharedWebContent(sharedWebContentTemp);

        // convert the folders and folders_monitored strings into a more usable structure
        const sharedContentTemp = [];
        if (configuration.folders) {
          for (let i = 0; i < configuration.folders.length; i++) {
            const sharedFolder = configuration.folders[i];
            const isMonitored = configuration.folders_monitored.indexOf(sharedFolder) > -1;
            sharedContentTemp.push({
              directory: sharedFolder,
              isMonitored,
            });
          }
        }
        sharedContentTemp.push({ directory: '', isMonitored: true } as SharedDirectory);

        setSharedDirectories(sharedContentTemp);
  }, [configuration]);

  return (
      <Accordion mt="xl">
        <Accordion.Item value="SharedFolders">
          <Accordion.Control>{i18n.get['SharedFolders']}</Accordion.Control>
          <Accordion.Panel>
            <List
              lockVertically
              values={sharedDirectories}
              onChange={({ oldIndex, newIndex }) => {
                canModify && moveSharedDirectory(oldIndex, newIndex);
              }}
              renderList={
                ({ children, props }) => {
                  return (
                    <Table>
                      <thead>
                        <tr>
                          <th></th>
                          <th>{i18n.get['Folder']}</th>
                          <th>{i18n.get['MonitorPlayedStatusFiles']}</th>
                          <th>
                            <Group position="right">
                              <Tooltip label={i18n.get[sse.scanLibrary.running ? 'CancelScanningSharedFolders' : 'ScanAllSharedFolders']}>
                                <ActionIcon
                                  size="xl"
                                  disabled={!canModify || isLoading || !sse.scanLibrary.enabled || (!configuration.use_cache && !sse.scanLibrary.running)}
                                  variant="transparent"
                                  color={sse.scanLibrary.running ? "red" : "blue"}
                                  title={i18n.get[sse.scanLibrary.running ? 'CancelScanningSharedFolders' : 'ScanAllSharedFolders']}
                                  onClick={() => sse.scanLibrary.running ? scanAllSharedFoldersCancel() : scanAllSharedFolders()}
                                >
                                  <ListSearch />
                                  {sse.scanLibrary.running  && (<Loader />)}
                                </ActionIcon>
                              </Tooltip>
                            </Group>
                          </th>
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
                          disabled={!canModify || !value.directory}
                        >
                          <ArrowsVertical />
                        </ActionIcon>
                      </td>
                      <td>
                        <DirectoryChooser
                          disabled={!canModify}
                          size="xs"
                          path={value.directory}
                          callback={(directory: string) => setDirectory(value, directory)}
                        ></DirectoryChooser>
                      </td>
                      <td>
                        <Switch
                          disabled={!canModify || !value.directory}
                          checked={value.isMonitored}
                          onChange={(event) => toggleMonitored(value, event.currentTarget.checked)}
                        />
                      </td>
                      <td align="right">
                        <Group position="right">
                          <Tooltip label={i18n.get['MarkContentsFullyPlayed']}>
                            <ActionIcon
                              color="blue"
                              variant="transparent"
                              disabled={!canModify || !value.directory || isLoading}
                              onClick={() => markDirectoryFullyPlayed(value, true)}
                            >
                              <EyeCheck />
                            </ActionIcon>
                          </Tooltip>
                          <Tooltip label={i18n.get['MarkContentsUnplayed']}>
                            <ActionIcon
                              color="green"
                              variant="transparent"
                              disabled={!canModify || !value.directory || isLoading}
                              onClick={() => markDirectoryFullyPlayed(value, false)}
                            >
                              <EyeOff />
                            </ActionIcon>
                          </Tooltip>
                          <ActionIcon
                            color="red"
                            variant="transparent"
                            disabled={!canModify || !value.directory}
                            onClick={() => removeDirectory(value)}
                          >
                            <SquareX />
                          </ActionIcon>
                        </Group>
                      </td>
                    </tr>
                  )
                }
              }
            />
          </Accordion.Panel>
        </Accordion.Item>
        <Accordion.Item value="WebContent">
          <Accordion.Control>{i18n.get['WebContent']}</Accordion.Control>
          <Accordion.Panel>
            <List
              lockVertically
              values={sharedWebContent}
              onChange={({ oldIndex, newIndex }) => {
                canModify && moveSharedWebContentItem(oldIndex, newIndex);
              }}
              renderList={
                ({ children, props }) => {
                  return (
                    <Table>
                      <thead>
                        <tr>
                          <th></th>
                          <th>{i18n.get['Type']}</th>
                          <th>{i18n.get['Name']}</th>
                          <th>{i18n.get['VirtualFolders']}</th>
                          <th>{i18n.get['Source']}</th>
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
                          disabled={!canModify || value.isnew}
                        >
                          <ArrowsVertical />
                        </ActionIcon>
                      </td>
                      <td>
                        <Select
                          disabled={!canModify}
                          data={[
                            {value: 'audiofeed', label: i18n.get['Podcast']},
                            {value: 'videofeed', label: i18n.get['VideoFeed']},
                            {value: 'imagefeed', label: i18n.get['ImageFeed']},
                            {value: 'audiostream', label: i18n.get['AudioStream']},
                            {value: 'videostream', label: i18n.get['VideoStream']},
                          ]}
                          withinPortal={true}
                          size="xs"
                          value={value.type}
                          onChange={(itemValue) => setSharedWebContentItemAttribute('type', value, itemValue)}
                        />
                      </td>
                      <td>
                        {(value.type !== 'audiostream' && value.type !== 'videostream') ? (
                          <Group>
                            <Tooltip label={i18n.get['NamesSetAutomaticallyFeeds']} {...defaultTooltipSettings}>
                              <Text lineClamp={1}>
                                {value.name ? value.name : '-'}
                              </Text>
                            </Tooltip>
                            <ActionIcon
                              color="red"
                              variant="transparent"
                              disabled={!canModify || !value.source || isLoading}
                              onClick={() => getSharedWebContentFeedName(value)}
                            >
                              <ZoomCheck />
                            </ActionIcon>
                          </Group>
                        ) : (
                          <TextInput
                            size="xs"
                            disabled={!canModify}
                            value={value.name}
                            onChange={(event) => setSharedWebContentItemAttribute('name', value, event.currentTarget.value)}
                          />
						)}
                      </td>
                      <td>
                        <TextInput
                          size="xs"
                          disabled={!canModify}
                          value={value.folders}
                          onChange={(event) => setSharedWebContentItemAttribute('folders', value, event.currentTarget.value)}
                        />
                      </td>
                      <td>
                        <TextInput
                          size="xs"
                          disabled={!canModify}
                          value={value.source}
                          onChange={(event) => setSharedWebContentItemAttribute('source', value, event.currentTarget.value)}
                        />
                      </td>
                      <td>
                        {!value.isnew ? (
                          <ActionIcon
                            color="red"
                            variant="transparent"
                            disabled={!canModify}
                            onClick={() => removeSharedWebContentItem(value)}
                          >
                            <SquareX />
                          </ActionIcon>
                        ) : (
                          <ActionIcon
                            color="green"
                            variant="transparent"
                            disabled={!canModify || !value.source || (!value.name && (value.type === 'audiostream' || value.type === 'videostream'))}
                            onClick={() => addSharedWebContentItem()}
                          >
                            <SquarePlus />
                          </ActionIcon>
                        )}
                      </td>
                    </tr>
                  )
                }
              }
            />
          </Accordion.Panel>
        </Accordion.Item>
      </Accordion>
      );
}

interface SharedDirectory {
  directory: string;
  isMonitored: boolean;
}

interface SharedWebContentItem {
  name: string;
  type: string;
  folders: string;
  source: string;
  isnew?: boolean;
}
