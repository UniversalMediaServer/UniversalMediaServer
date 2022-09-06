import { Accordion, ActionIcon, Box, Button, Checkbox, ColorPicker, ColorSwatch, Grid, Group, Modal, MultiSelect, NavLink, NumberInput, Select, Stack, Switch, Table, Tabs, Text, Textarea, TextInput, Title, Tooltip } from '@mantine/core';
import { useForm } from '@mantine/form';
import { Prism } from '@mantine/prism';
import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import _ from 'lodash';
import { useContext, useEffect, useState } from 'react';
import { arrayMove, List } from 'react-movable';
import { ArrowNarrowDown, ArrowNarrowUp, ArrowsVertical, Ban, ExclamationMark, PlayerPlay, Search, SquareX } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import ServerEventContext from '../../contexts/server-event-context';
import SessionContext from '../../contexts/session-context';
import { havePermission } from '../../services/accounts-service';
import {allowHtml, openGitHubNewIssue} from '../../utils';
import DirectoryChooser from '../DirectoryChooser/DirectoryChooser';
import { sendAction } from '../../services/actions-service';

export default function Settings() {
  const [subColorModalOpened, setSubColorModalOpened] = useState(false);
  const [mencoderAdvancedOpened, setMencoderAdvancedOpened] = useState(false);
  const [subColor, setSubColor] = useState('rgba(255, 255, 255, 255)');
  const [isLoading, setLoading] = useState(true);
  const [transcodingContent, setTranscodingContent] = useState('common');
  const [defaultConfiguration, setDefaultConfiguration] = useState({} as any);
  const [configuration, setConfiguration] = useState({} as any);

  interface mantineSelectData {
    value: string;
    label: string;
  }

  // key/value pairs for dropdowns
  const [selectionSettings, setSelectionSettings] = useState({
    allRendererNames: [] as mantineSelectData[],
    audioCoverSuppliers: [] as mantineSelectData[],
    enabledRendererNames: [] as mantineSelectData[],
    ffmpegLoglevels: [],
    fullyPlayedActions: [] as mantineSelectData[],
    gpuAccelerationMethod: [],
    networkInterfaces: [],
    serverEngines: [] as mantineSelectData[],
    sortMethods: [] as mantineSelectData[],
    subtitlesDepth: [],
    subtitlesCodepages: [] as mantineSelectData[],
    subtitlesInfoLevels: [] as mantineSelectData[],
    transcodingEngines: {} as { [key: string]: { id: string, name: string, isAvailable: boolean, purpose: number, statusText: string[] } },
    transcodingEnginesPurposes: [],
  });

  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
  const sse = useContext(ServerEventContext);
  const form = useForm({ initialValues: {} as any });
  const formSetValues = form.setValues;

  const canModify = havePermission(session, "settings_modify");
  const canView = canModify || havePermission(session, "settings_view");

  const defaultTooltipSettings = {
    width: 350,
    color: 'blue',
    multiline: true,
    withArrow: true,
  }

  useEffect(() => {
    if (sse.userConfiguration === null) {
      return;
    }
    const userConfig = _.merge({}, configuration, sse.userConfiguration);
    sse.setUserConfiguration(null);
    setConfiguration(userConfig);
    formSetValues(userConfig);
  }, [configuration, sse, formSetValues]);

  // Code here will run just like componentDidMount
  useEffect(() => {
    canView && axios.get('/configuration-api/settings')
      .then(function (response: any) {
        const settingsResponse = response.data;
        setSelectionSettings(settingsResponse);
        setDefaultConfiguration(settingsResponse.userSettingsDefaults);
        setSharedWebContent(settingsResponse.sharedWebContent);

        // merge defaults with what we receive, which might only be non-default values
        const userConfig = _.merge({}, settingsResponse.userSettingsDefaults, settingsResponse.userSettings);

        // convert the folders and folders_monitored strings into a more usable structure
        const sharedContentTemp = [];
        for (let i = 0; i < userConfig.folders.length; i++) {
          const sharedFolder = userConfig.folders[i];
          const isMonitored = userConfig.folders_monitored.indexOf(sharedFolder) > -1;
          sharedContentTemp.push({
            directory: sharedFolder,
            isMonitored,
          });
        }
        sharedContentTemp.push({ directory: '', isMonitored: true } as SharedDirectory);

        setSharedDirectories(sharedContentTemp);
        setConfiguration(userConfig);
        formSetValues(userConfig);
      })
      .catch(function () {
        showNotification({
          id: 'data-loading',
          color: 'red',
          title: i18n.get['Error'],
          message: i18n.get['ConfigurationNotReceived'] +  ' ' + i18n.get['ClickHereReportBug'],
          onClick: () => { openGitHubNewIssue(); },
          autoClose: 3000,
        });
      })
      .then(function () {
        setLoading(false);
      });
	  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [canView, formSetValues]);

  const handleSubmit = (values: typeof form.values) => {
    const changedValues: Record<string, any> = {};

    // construct an object of only changed values to send
    for (const key in values) {
      if (!_.isEqual(configuration[key], values[key])) {
        changedValues[key] = values[key];
      }
    };

    if (_.isEmpty(changedValues)) {
      showNotification({
        title: i18n.get['Saved'],
        message: i18n.get['ConfigurationHasNoChanges'],
      })
      return;
    }

    setLoading(true);
    axios.post('/configuration-api/settings', changedValues)
      .then(function () {
        setConfiguration(values);
        showNotification({
          title: i18n.get['Saved'],
          message: i18n.get['ConfigurationSaved'],
        })
      })
      .catch(function () {
        showNotification({
          color: 'red',
          title: i18n.get['Error'],
          message: i18n.get['ConfigurationNotSaved'] + ' ' + i18n.get['ClickHereReportBug'],
          onClick: () => { openGitHubNewIssue(); },
        })
      })
      .then(function () {
        setLoading(false);
      });
  };

  const getI18nSelectData = (values: mantineSelectData[]) => {
    return values.map((value: mantineSelectData) => {
      return {value: value.value, label: i18n.getI18nString(value.label)};
    });
  }

  /*
  GENERAL SETTINGS
  */
  const getLanguagesSelectData = () => {
    return i18n.languages.map((language) => {
      return {
        value : language.id,
        label: language.name
		  + (language.name!==language.defaultname?' ('+language.defaultname+')':'')
		  + (!language.id.startsWith('en-')?' ('+language.coverage+'%)':'')
      };
    });
  }

  const getGeneralSettingsTab = () => {
    return (
        <>
          <Accordion>
            <Accordion.Item value='Application'>
              <Accordion.Control>{i18n.get['Application']}</Accordion.Control>
              <Accordion.Panel>
                <Select
                  disabled={!canModify}
                  label={i18n.get['Language']}
                  data={getLanguagesSelectData()}
                  {...form.getInputProps('language')}
                />
                <Stack align="flex-start" mt="sm">
                  <Checkbox
                    disabled={!canModify}
                    label={i18n.get['StartMinimizedSystemTray']}
                    {...form.getInputProps('minimized', { type: 'checkbox' })}
                  />
                  <Checkbox
                    disabled={!canModify}
                    label={i18n.get['EnableSplashScreen']}
                    {...form.getInputProps('show_splash_screen', { type: 'checkbox' })}
                  />
                  <Checkbox
                    disabled={!canModify}
                    label={i18n.get['CheckAutomaticallyForUpdates']}
                    {...form.getInputProps('auto_update', { type: 'checkbox' })}
                  />
                </Stack>
              </Accordion.Panel>
            </Accordion.Item>
            <Accordion.Item value='Services'>
              <Accordion.Control>{i18n.get['Services']}</Accordion.Control>
              <Accordion.Panel>
                <Group>
                  <TextInput
                    disabled={!canModify}
                    label={i18n.get['ServerName']}
					placeholder={defaultConfiguration.server_name}
                    name="server_name"
                    sx={{ flex: 1 }}
                    {...form.getInputProps('server_name')}
                  />
                  <Tooltip label={allowHtml(i18n.get['WhenEnabledUmsProfileName'])} {...defaultTooltipSettings}>
                    <Checkbox
                      disabled={!canModify}
                      mt="xl"
                      label={i18n.get['AppendProfileName']}
                      {...form.getInputProps('append_profile_name', { type: 'checkbox' })}
                    />
                  </Tooltip>
                </Group>
                <Tooltip label={allowHtml(i18n.get['DefaultOptionIsHighlyRecommended'])} {...defaultTooltipSettings}>
                  <Select
                    disabled={!canModify}
                    label={i18n.get['MediaServerEngine']}
                    data={getI18nSelectData(selectionSettings.serverEngines)}
                    {...form.getInputProps('server_engine')}
                  />
                </Tooltip>
              </Accordion.Panel>
            </Accordion.Item>
            <Accordion.Item value='NetworkSettingsAdvanced'>
              <Accordion.Control>{i18n.get['NetworkSettingsAdvanced']}</Accordion.Control>
              <Accordion.Panel>
                <Select
                  disabled={!canModify}
                  label={i18n.get['ForceNetworkingInterface']}
                  data={selectionSettings.networkInterfaces}
                  {...form.getInputProps('network_interface')}
                />

                <TextInput
                  disabled={!canModify}
                  mt="xs"
                  label={i18n.get['ForceIpServer']}
                  {...form.getInputProps('hostname')}
                />

                <TextInput
                  disabled={!canModify}
                  mt="xs"
                  label={i18n.get['ForcePortServer']}
                  {...form.getInputProps('port')}
                />

                <TextInput
                  disabled={!canModify}
                  mt="xs"
                  label={i18n.get['UseIpFilter']}
                  {...form.getInputProps('ip_filter')}
                />

                <Group mt="xs">
                  <TextInput
                    sx={{ flex: 1 }}
                    label={i18n.get['MaximumBandwidthMbs']}
                    disabled={!canModify || form.values['automatic_maximum_bitrate']}
                    {...form.getInputProps('maximum_bitrate')}
                  />
                  
                  <Tooltip label={allowHtml(i18n.get['ItSetsOptimalBandwidth'])} {...defaultTooltipSettings}>
                    <Checkbox
                      disabled={!canModify}
                      mt="xl"
                      label={i18n.get['UseAutomaticMaximumBandwidth']}
                      {...form.getInputProps('automatic_maximum_bitrate', { type: 'checkbox' })}
                    />
                  </Tooltip>
                </Group>
              </Accordion.Panel>
            </Accordion.Item>
            <Accordion.Item value='ExternalOutgoingTraffic'>
              <Accordion.Control>{i18n.get['ExternalOutgoingTraffic']}</Accordion.Control>
              <Accordion.Panel>
                <Stack>
                  <Tooltip label={allowHtml(i18n.get['ThisControlsWhetherUmsTry'])} {...defaultTooltipSettings}>
                    <Checkbox
                      disabled={!canModify}
                      label={i18n.get['EnableExternalNetwork']}
                      {...form.getInputProps('external_network', { type: 'checkbox' })}
                    />
                  </Tooltip>
                  <Tooltip label={allowHtml(i18n.get['UsesInformationApiAllowBrowsing'])} {...defaultTooltipSettings}>
                    <Checkbox
                      disabled={!canModify}
                      label={i18n.get['UseInfoFromOurApi']}
                      {...form.getInputProps('use_imdb_info', { type: 'checkbox' })}
                    />
                </Tooltip>
				</Stack>
              </Accordion.Panel>
            </Accordion.Item>
            <Accordion.Item value='Renderers'>
              <Accordion.Control>{i18n.get['Renderers']}</Accordion.Control>
              <Accordion.Panel>
                <Stack>
                  <MultiSelect
                    disabled={!canModify}
                    data={getI18nSelectData(selectionSettings.allRendererNames)}
                    label={i18n.get['EnabledRenderers']}
                    {...form.getInputProps('selected_renderers')}
                  />
                  <Select
                    disabled={!canModify}
                    sx={{ flex: 1 }}
                    label={i18n.get['DefaultRendererWhenAutoFails']}
                    data={getI18nSelectData(selectionSettings.enabledRendererNames)}
                    {...form.getInputProps('renderer_default')}
                    searchable
                  />
                  <Tooltip label={allowHtml(i18n.get['DisablesAutomaticDetection'])} {...defaultTooltipSettings}>
                    <Checkbox
                      disabled={!canModify}
                      label={i18n.get['ForceDefaultRenderer']}
                      {...form.getInputProps('renderer_force_default', { type: 'checkbox' })}
                    />
                  </Tooltip>
                </Stack>
              </Accordion.Panel>
            </Accordion.Item>
          </Accordion>
        </>
    );
  }

  /*
  NAVIGATION SETTINGS
  */
  const resetCache = async () => {
    await sendAction('Server.ResetCache');
  };

  const getNavigationSettingsTab = () => {
    return (
        <>
          <Accordion>
            <Accordion.Item value='NavigationGeneralSettings'>
              <Accordion.Control>{i18n.get['GeneralSettings']}</Accordion.Control>
              <Accordion.Panel>
                <Group position="apart">
                  <Tooltip label={allowHtml(i18n.get['DisablingWillDisableFullyPlayed'])} {...defaultTooltipSettings}>
                    <Checkbox
                      disabled={!canModify}
                      label={i18n.get['EnableCache']}
                      {...form.getInputProps('use_cache', { type: 'checkbox' })}
                    />
                  </Tooltip>
                  {canModify && (<Tooltip label={allowHtml(i18n.get['CacheEmptiedExceptFullyPlayed'])} {...defaultTooltipSettings}>
                    <Button
                      size="xs"
                      onClick={() => resetCache()}
                      disabled={!form.values['use_cache']}
                    >
                      {i18n.get['ResetCache']}
                    </Button>
                  </Tooltip>)}
                </Group>
				<Stack align="flex-start" mt="xs">
                  <Tooltip label={allowHtml(i18n.get['WhenEnabledPartiallyWatchVideo'])} {...defaultTooltipSettings}>
                    <Checkbox
                      disabled={!canModify}
                      label={i18n.get['EnableVideoResuming']}
                      {...form.getInputProps('resume', { type: 'checkbox' })}
                    />
                  </Tooltip>
                  <Tooltip label={allowHtml(i18n.get['ThisMakesBrowsingSlower'])} {...defaultTooltipSettings}>
                    <Checkbox
                      disabled={!canModify}
                      label={i18n.get['HideEmptyFolders']}
                      {...form.getInputProps('hide_empty_folders', { type: 'checkbox' })}
                    />
                  </Tooltip>
                  <Tooltip label={allowHtml(i18n.get['TreatMultipleSymbolicLinks'])} {...defaultTooltipSettings}>
                    <Checkbox
                      disabled={!canModify}
                      label={i18n.get['UseTargetFileSymbolicLinks']}
                      {...form.getInputProps('use_symlinks_target_file', { type: 'checkbox' })}
                    />
                  </Tooltip>
                  <Checkbox
                    disabled={!canModify}
                    label={i18n.get['BrowseCompressedArchives']}
                    {...form.getInputProps('enable_archive_browsing', { type: 'checkbox' })}
                  />
                  <Tooltip label={allowHtml(i18n.get['IfNumberItemsFolderExceeds'])} {...defaultTooltipSettings}>
                    <NumberInput
                      label={i18n.get['MinimumItemLimitBeforeAZ']}
                      disabled={!canModify}
                      {...form.getInputProps('atz_limit')}
                    />
                  </Tooltip>
				</Stack>
                <Group mt="md">
                  <Select
                    sx={{ flex: 1 }}
                    disabled={!canModify}
                    label={i18n.get['FullyPlayedAction']}
                    data={getI18nSelectData(selectionSettings.fullyPlayedActions)}
                    {...form.getInputProps('fully_played_action')}
                  />
				  {(form.values['fully_played_action'] === "3" || form.values['fully_played_action'] === "4") && (
                    <DirectoryChooser
                      disabled={!canModify}
                      label={i18n.get['DestinationFolder']}
                      path={form.getInputProps('fully_played_output_directory').value}
                      callback={form.setFieldValue}
                      formKey="fully_played_output_directory"
                    />
				  )}
                </Group>
              </Accordion.Panel>
            </Accordion.Item>
            <Accordion.Item value='NavigationThumbnails'>
              <Accordion.Control>{i18n.get['Thumbnails']}</Accordion.Control>
              <Accordion.Panel>  
                <Checkbox
                  disabled={!canModify}
                  label={i18n.get['GenerateThumbnails']}
                  {...form.getInputProps('generate_thumbnails', { type: 'checkbox' })}
                />
                <TextInput
                  sx={{ flex: 1 }}
                  label={i18n.get['ThumbnailSeekingPosition']}
                  disabled={!form.values['generate_thumbnails']}
                  {...form.getInputProps('thumbnail_seek_position')}
                />
                <DirectoryChooser
                  disabled={!canModify}
                  path={form.getInputProps('alternate_thumb_folder').value}
                  callback={form.setFieldValue}
                  label={i18n.get['AlternateVideoCoverArtFolder']}
                  formKey="alternate_thumb_folder"
                />
                <Select
                  disabled={!canModify}
                  label={i18n.get['AudioThumbnailsImport']}
                  data={getI18nSelectData(selectionSettings.audioCoverSuppliers)}
                  {...form.getInputProps('audio_thumbnails_method')}
                />
                </Accordion.Panel>
              </Accordion.Item>
              <Accordion.Item value='NavigationFileSortingNaming'>
                <Accordion.Control>{i18n.get['FileSortingNaming']}</Accordion.Control>
                <Accordion.Panel>
                <Stack align="flex-start">
                  <Select
                    disabled={!canModify}
                    label={i18n.get['FileOrder']}
                    data={getI18nSelectData(selectionSettings.sortMethods)}
                    {...form.getInputProps('sort_method')}
                  />
                  <Checkbox
                    disabled={!canModify}
                    label={i18n.get['IgnoreArticlesATheSorting']}
                    {...form.getInputProps('ignore_the_word_a_and_the', { type: 'checkbox' })}
                  />
                  <Tooltip label={allowHtml(i18n.get['IfEnabledFilesWillAppear'])} {...defaultTooltipSettings}>
                    <Checkbox
                      disabled={!canModify}
                      label={i18n.get['PrettifyFilenames']}
                      {...form.getInputProps('prettify_filenames', { type: 'checkbox' })}
                    />
                  </Tooltip>
                  <Checkbox
                    label={i18n.get['HideFileExtensions']}
                    disabled={!canModify || form.values['prettify_filenames']}
                    {...form.getInputProps('hide_extensions', { type: 'checkbox' })}
                  />
                  <Tooltip label={allowHtml(i18n.get['AddsInformationAboutSelectedSubtitles'])} {...defaultTooltipSettings}>
                    <Select
                      disabled={!canModify}
                      label={i18n.get['AddSubtitlesInformationVideoNames']}
                      data={getI18nSelectData(selectionSettings.subtitlesInfoLevels)}
                      {...form.getInputProps('subs_info_level')}
                    />
                  </Tooltip>
                  <Tooltip label={allowHtml(i18n.get['IfEnabledEngineNameDisplayed'])} {...defaultTooltipSettings}>
                    <Checkbox
                      disabled={!canModify}
                      label={i18n.get['AddEnginesNamesAfterFilenames']}
                      checked={!form.values['hide_enginenames']}
                      onChange={(event) => {
                        form.setFieldValue('hide_enginenames', !event.currentTarget.checked);
                      }}
                    />
                  </Tooltip>
                </Stack>
              </Accordion.Panel>
            </Accordion.Item>
            <Accordion.Item value='NavigationVirtualFoldersFiles'>
              <Accordion.Control>{i18n.get['VirtualFoldersFiles']}</Accordion.Control>
                <Accordion.Panel>
                <Stack>
                  <Tooltip label={allowHtml(i18n.get['MediaLibraryFolderWillAvailable'])} {...defaultTooltipSettings}>
                    <Checkbox
                      disabled={!canModify}
                      label={i18n.get['ShowMediaLibraryFolder']}
                      {...form.getInputProps('use_cache', { type: 'checkbox' })}
                    />
                  </Tooltip>
                  <Checkbox
                    disabled={!canModify}
                    label={i18n.get['ShowRecentlyPlayedFolder']}
                    {...form.getInputProps('show_recently_played_folder', { type: 'checkbox' })}
                  />
                  <Checkbox
                    disabled={!canModify}
                    label={i18n.get['ShowServerSettingsFolder']}
                    {...form.getInputProps('show_server_settings_folder', { type: 'checkbox' })}
                  />
                  <Checkbox
                    disabled={!canModify}
                    label={i18n.get['ShowTranscodeFolder']}
                    {...form.getInputProps('show_transcode_folder', { type: 'checkbox' })}
                  />
                  <Checkbox
                    disabled={!canModify}
                    label={i18n.get['ShowLiveSubtitlesFolder']}
                    {...form.getInputProps('show_live_subtitles_folder', { type: 'checkbox' })}
                  />
                </Stack>
              </Accordion.Panel>
            </Accordion.Item>
          </Accordion>
        </>
    );
  }

  /*
  SHARED CONTENT
  */
  const [sharedDirectories, setSharedDirectories] = useState([] as SharedDirectory[]);
  interface SharedDirectory {
    directory: string;
    isMonitored: boolean;
  }
  const [sharedWebContent, setSharedWebContent] = useState([] as SharedWebContentItem[]);
  interface SharedWebContentItem {
    name: string;
    type: string;
    folders: string;
    source: string;
  }

  const getSharedContentTab = () => {
    return (
      <Accordion mt="xl">
        <Accordion.Item value="SharedFolders">
          <Accordion.Control>{i18n.get['SharedFolders']}</Accordion.Control>
          <Accordion.Panel>
            <Group>
              <ActionIcon size="xl" variant="light" color="blue" title={i18n.get['ScanAllSharedFolders']}><Search /></ActionIcon>
            </Group>
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
                      <td>
                        <ActionIcon
                          color="red"
                          variant="transparent"
                          disabled={!canModify || !value.directory}
                          onClick={() => removeDirectory(value)}
                        >
                          <SquareX />
                        </ActionIcon>
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
                          <th>{i18n.get['Name']}</th>
                          <th>{i18n.get['Type']}</th>
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
                          disabled={!canModify || !value.name}
                        >
                          <ArrowsVertical />
                        </ActionIcon>
                      </td>
                      <td>
                        <Text lineClamp={1}>
                          {value.name}
                        </Text>
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
                          onChange={(itemValue) => setSharedWebContentItemType(value, itemValue)}
                        />
                      </td>
                      <td>
                        <TextInput
                          sx={{ flex: 1 }}
                          size="xs"
                          disabled={!canModify}
                          value={value.folders}
                        />
                      </td>
                      <td>
                        <TextInput
                          sx={{ flex: 1 }}
                          size="xs"
                          disabled={!canModify}
                          value={value.source}
                        />
                      </td>
                      <td>
                        <ActionIcon
                          color="red"
                          variant="transparent"
                          disabled={!canModify || !value.name}
                          onClick={() => removeSharedWebContentItem(value)}
                        >
                          <SquareX />
                        </ActionIcon>
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

  /**
   * Convert our shared directory structure to what the API wants.
   */
  const updateSharedDirectoriesToSave = (sharedDirectoriesTemp: SharedDirectory[]) => {
    setSharedDirectories(sharedDirectoriesTemp);

    const sharedDirectoriesArray = [] as string[];
    const monitoredDirectoriesArray = [] as string[];
    _.each(sharedDirectoriesTemp, (sharedDirectory) => {
      if (sharedDirectory.directory) {
        sharedDirectoriesArray.push(sharedDirectory.directory);
      }
      if (sharedDirectory.isMonitored) {
        monitoredDirectoriesArray.push(sharedDirectory.directory);
      }
    });
    form.setFieldValue('folders', sharedDirectoriesArray);
    form.setFieldValue('folders_monitored', monitoredDirectoriesArray);
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
    console.log(2,index);
    sharedDirectoriesTemp.splice(index, 1);
    updateSharedDirectoriesToSave(sharedDirectoriesTemp);
  }

  const moveSharedDirectory = (oldIndex: number, newIndex: number) => {
    let sharedDirectoriesTemp = _.cloneDeep(sharedDirectories);
    sharedDirectoriesTemp = arrayMove(sharedDirectoriesTemp, oldIndex, newIndex);
    updateSharedDirectoriesToSave(sharedDirectoriesTemp);
  }

  const setSharedWebContentItemType = (item: SharedWebContentItem, source: string | null) => {
    const sharedWebContentTemp = _.cloneDeep(sharedWebContent);
    const index = _.findIndex(sharedWebContentTemp, (realItem) => {
      return realItem.source === item.source;
    });
    if (source) {
      sharedWebContentTemp[index].source = source;
    }

    // ensure there is always an empty entry at the bottom
    if (index === sharedWebContentTemp.length - 1) {
      sharedWebContentTemp.push({ source: '', name: '', type: '', folders: '' } as SharedWebContentItem);
    }
    setSharedWebContent(sharedWebContentTemp);
  }

  const removeSharedWebContentItem = (item: SharedWebContentItem) => {
    const sharedWebContentTemp = _.cloneDeep(sharedWebContent);
    const index = _.findIndex(sharedWebContentTemp, (realItem) => {
      return realItem.source === item.source;
    });
    console.log(2,index);
    sharedWebContentTemp.splice(index, 1);
    setSharedWebContent(sharedWebContentTemp);
  }

  const moveSharedWebContentItem = (oldIndex: number, newIndex: number) => {
    let sharedWebContentTemp = _.cloneDeep(sharedWebContent);
    sharedWebContentTemp = arrayMove(sharedWebContentTemp, oldIndex, newIndex);
    setSharedWebContent(sharedWebContentTemp);
  }
	  
  /*
  TRANSCODING SETTINGS
  */
  const getTranscodingEnginesPriority = (purpose: number) => {
    return form.getInputProps('engines_priority').value !== undefined ? form.getInputProps('engines_priority').value.filter((value: string) => 
      selectionSettings.transcodingEngines[value] && selectionSettings.transcodingEngines[value].purpose === purpose
    ) : [];
  }

  const moveTranscodingEnginesPriority = (purpose: number, oldIndex: number, newIndex: number) => {
    if (form.getInputProps('engines_priority').value instanceof Array<string>) {
      const items = form.getInputProps('engines_priority').value as Array<string>;
      const index = items.indexOf(getTranscodingEnginesPriority(purpose)[oldIndex]);
      const moveTo = index - oldIndex + newIndex;
      form.setFieldValue('engines_priority', arrayMove(items, index, moveTo));
    }
  }

  const setTranscodingEngineStatus = (id: string, enabled: boolean) => {
    const items = (form.getInputProps('engines').value instanceof Array<string>) ?
      form.getInputProps('engines').value as Array<string> :
      [form.getInputProps('engines').value];
    const included = items.includes(id);
    if (enabled && !included) {
      const updated = items.concat(id);
      form.setFieldValue('engines', updated);
    } else if (!enabled && included) {
      const updated = items.filter(function(value){ return value !== id;});
      form.setFieldValue('engines', updated);
    }
  }

  const getTranscodingEngineStatus = (engine: {id:string,name:string,isAvailable:boolean,purpose:number,statusText:string[]}) => {
    const items = (form.getInputProps('engines').value instanceof Array<string>) ?
      form.getInputProps('engines').value as Array<string> :
      [form.getInputProps('engines').value];
    if (!engine.isAvailable) {
      return (
        <Tooltip label={allowHtml(i18n.get['ThereIsProblemTranscodingEngineX']?.replace('%s', engine.name))} {...defaultTooltipSettings}>
          <ExclamationMark color={'orange'} strokeWidth={3} size={14}/>
        </Tooltip>
      )
    } else if (items.includes(engine.id)) {
      return (
        <Tooltip label={allowHtml(i18n.get['TranscodingEngineXEnabled']?.replace('%s', engine.name))} {...defaultTooltipSettings}>
          <ActionIcon size={20} style={{ cursor: 'copy' }} onClick={(e: any) => {canModify && setTranscodingEngineStatus(engine.id, false); e.stopPropagation();}}>
            <PlayerPlay strokeWidth={2} color={'green'} size={14}/>
          </ActionIcon>
        </Tooltip>
      )
    }
    return (
      <Tooltip label={allowHtml(i18n.get['TranscodingEngineXDisabled']?.replace('%s', engine.name))} {...defaultTooltipSettings}>
        <ActionIcon size={20} style={{ cursor: 'copy' }} onClick={(e: any) => {canModify && setTranscodingEngineStatus(engine.id, true); e.stopPropagation();}}>
          <Ban color={'red'} size={14}/>
        </ActionIcon>
      </Tooltip>
    )
  }

  const getTranscodingEnginesList = (purpose:number) => {
    const engines = getTranscodingEnginesPriority(purpose);
    return engines.length > 1 ? (
      <List
        lockVertically
        values={getTranscodingEnginesPriority(purpose)}
        onChange={({ oldIndex, newIndex }) => {
          canModify && moveTranscodingEnginesPriority(purpose, oldIndex, newIndex);
        }}
        renderList={({ children, props }) => (
          <Stack justify="flex-start" align="flex-start" spacing="xs" {...props}>
            {children}
          </Stack>
        )}
        renderItem={({ value, props, isDragged, isSelected }) => (
          <Button {...props} color='gray' size="xs" compact
            variant={isDragged || isSelected ? 'outline' : 'subtle'}
            leftIcon={
              <>
                <ActionIcon data-movable-handle size={20} style={{ cursor: isDragged ? 'grabbing' : 'grab', }}>
                  { engines.indexOf(value) === 0 ? (<ArrowNarrowDown />) : engines.indexOf(value) === engines.length - 1 ? (<ArrowNarrowUp />) : (<ArrowsVertical />)}
                </ActionIcon>
                {getTranscodingEngineStatus(selectionSettings.transcodingEngines[value])}
              </>
            }
            onClick={() => setTranscodingContent(selectionSettings.transcodingEngines[value].id)}
          >
            {selectionSettings.transcodingEngines[value].name}
          </Button>
        )}
      />
    ) : (
      <Stack justify="flex-start" align="flex-start" spacing="xs">
        {engines.map((value: string) => (
          <Button variant="subtle" color='gray' size="xs" compact
            leftIcon={getTranscodingEngineStatus(selectionSettings.transcodingEngines[value])}
            onClick={() => setTranscodingContent(selectionSettings.transcodingEngines[value].id)}
          >
            {selectionSettings.transcodingEngines[value].name}
          </Button>
        ))}
      </Stack>
    );
  }

  const getTranscodingEnginesAccordionItems = () => {
    return selectionSettings.transcodingEnginesPurposes.map((value: string, index) => {
      return (
        <Accordion.Item value={'Transcoding' + index.toString()}>
          <Accordion.Control>{i18n.getI18nString(value)}</Accordion.Control>
          <Accordion.Panel>{getTranscodingEnginesList(index)}</Accordion.Panel>
        </Accordion.Item>);
    });
  }

  function rgbaToHexA(rgbaval: string) {
    if (rgbaval == null) { return '#00000000' }
    const sep = rgbaval.indexOf(",") > -1 ? "," : " "; 
    const rgbastr = rgbaval.substring(5).split(")")[0].split(sep);
    let hexa = '#';
    for (let i = 0; i < rgbastr.length; i++) {
      const hex = (i < 3) ? parseInt(rgbastr[i]).toString(16) : Math.round(parseFloat(rgbastr[i]) * 255).toString(16);
      if (hex.length < 2) { hexa = hexa + '0' }
	  hexa = hexa + hex;
    }
    return hexa;
  }

  function hexAToRgba(hex: string) {
    const red = parseInt(hex.slice(1, 3), 16);
    const green = parseInt(hex.slice(3, 5), 16);
    const blue = parseInt(hex.slice(5, 7), 16);
    const alpha = parseFloat((parseInt(hex.slice(7, 9), 16) / 255).toFixed(2));
    return "rgba(" + red.toString() + ", " + green.toString() + ", " + blue.toString() + ", " + alpha.toString() + ")";
  }

  const getTranscodingCommon = () => { return (<>
    <Title mt="sm" order={5}>{i18n.get['CommonTranscodeSettings']}</Title>
    <Stack spacing="xs">
      <TextInput
        label={i18n.get['MaximumTranscodeBufferSize']}
        name="maximum_video_buffer_size"
        sx={{ flex: 1 }}
        size="xs"
        disabled={!canModify}
        {...form.getInputProps('maximum_video_buffer_size')}
      />
      <NumberInput
        label={i18n.get['CpuThreadsToUse']?.replace('%d', defaultConfiguration.number_of_cpu_cores)}
        size="xs"
        max={defaultConfiguration.number_of_cpu_cores}
        min={1}
        disabled={!canModify}
        {...form.getInputProps('number_of_cpu_cores')}
      />
      <Grid>
        <Grid.Col span={10}>
          <Checkbox
            size="xs"
            disabled={!canModify}
            label={i18n.get['ChaptersSupportInTranscodeFolder']}
            {...form.getInputProps('chapter_support', { type: 'checkbox' })}
          />
        </Grid.Col>
        <Grid.Col span={2}>
          <TextInput
            size="xs"
            sx={{ flex: 1 }}
            disabled={!canModify || !form.values['chapter_support']}
            {...form.getInputProps('chapter_interval')}
          />
        </Grid.Col>
      </Grid>
      <Checkbox
        size="xs"
        disabled={!canModify}
        label={i18n.get['DisableSubtitles']}
        {...form.getInputProps('disable_subtitles', { type: 'checkbox' })}
      />
      <Tabs defaultValue="TranscodingVideoSettings">
        <Tabs.List>
          <Tabs.Tab value='TranscodingVideoSettings'>{i18n.get['VideoSettings']}</Tabs.Tab>
          <Tabs.Tab value='TranscodingAudioSettings'>{i18n.get['AudioSettings']}</Tabs.Tab>
          <Tabs.Tab value='TranscodingSubtitlesSettings'>{i18n.get['SubtitlesSettings']}</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value='TranscodingVideoSettings'>
          <Stack spacing="xs">
          <Checkbox
            mt="xs"
            disabled={!canModify}
            size="xs"
            label={i18n.get['EnableGpuAcceleration']}
            {...form.getInputProps('gpu_acceleration', { type: 'checkbox' })}
          />
          <Tooltip label={allowHtml(i18n.get['WhenEnabledMuxesDvd'])} {...defaultTooltipSettings}>
            <Checkbox
              disabled={!canModify}
              size="xs"
              label={i18n.get['LosslessDvdVideoPlayback']}
              {...form.getInputProps('mencoder_remux_mpeg2', { type: 'checkbox' })}
            />
          </Tooltip>
          <Tooltip label={allowHtml(i18n.get['AutomaticWiredOrWireless'])} {...defaultTooltipSettings}>
            <TextInput
              label={i18n.get['TranscodingQualityMpeg2']}
              size="xs"
              sx={{ flex: 1 }}
              disabled={!canModify || form.values['automatic_maximum_bitrate']}
              {...form.getInputProps('mpeg2_main_settings')}
            />
          </Tooltip>
          <Tooltip label={allowHtml(i18n.get['AutomaticSettingServeBestQuality'])} {...defaultTooltipSettings}>
            <TextInput
              label={i18n.get['TranscodingQualityH264']}
              size="xs"
              sx={{ flex: 1 }}
              disabled={!canModify || form.values['automatic_maximum_bitrate']}
              {...form.getInputProps('x264_constant_rate_factor')}
            />
          </Tooltip>
          <TextInput
            disabled={!canModify}
            label={i18n.get['SkipTranscodingFollowingExtensions']}
            size="xs"
            sx={{ flex: 1 }}
            {...form.getInputProps('disable_transcode_for_extensions')}
          />
          <TextInput
            disabled={!canModify}
            size="xs"
            label={i18n.get['ForceTranscodingFollowingExtensions']}
            sx={{ flex: 1 }}
            {...form.getInputProps('force_transcode_for_extensions')}
          />
          </Stack>
        </Tabs.Panel>
        <Tabs.Panel value='TranscodingAudioSettings'>
          <Stack spacing="xs">
          <Select
            disabled={!canModify}
            label={i18n.get['MaximumNumberAudioChannelsOutput']}
            data={[{value: '6', label: i18n.get['6Channels51']}, {value: '2', label: i18n.get['2ChannelsStereo']}]}
            size="xs"
            {...form.getInputProps('audio_channels')}
          />
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['UseLpcmForAudio']}
            {...form.getInputProps('audio_use_pcm', { type: 'checkbox' })}
          />
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['KeepAc3Tracks']}
            {...form.getInputProps('audio_remux_ac3', { type: 'checkbox' })}
          />
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['KeepDtsTracks']}
            {...form.getInputProps('audio_embed_dts_in_pcm', { type: 'checkbox' })}
          />
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['EncodedAudioPassthrough']}
            {...form.getInputProps('encoded_audio_passthrough', { type: 'checkbox' })}
          />
          <TextInput
            disabled={!canModify}
          label={i18n.get['Ac3ReencodingAudioBitrate']}
            sx={{ flex: 1 }}
            size="xs"
            {...form.getInputProps('audio_bitrate')}
          />
          <TextInput
            disabled={!canModify}
            label={i18n.get['AudioLanguagePriority']}
            sx={{ flex: 1 }}
            size="xs"
            {...form.getInputProps('audio_languages')}
          />
          </Stack>
        </Tabs.Panel>
        <Tabs.Panel value='TranscodingSubtitlesSettings'>
          <Stack spacing="xs">
            <Tooltip label={allowHtml(i18n.get['YouCanRearrangeOrderSubtitles'])} {...defaultTooltipSettings}>
              <TextInput
                disabled={!canModify}
                label={i18n.get['SubtitlesLanguagePriority']}
                sx={{ flex: 1 }}
                size="xs"
                {...form.getInputProps('subtitles_languages')}
              />
            </Tooltip>
            <TextInput
              disabled={!canModify}
              label={i18n.get['ForcedLanguage']}
              sx={{ flex: 1 }}
              size="xs"
              {...form.getInputProps('forced_subtitle_language')}
            />
            <TextInput
              disabled={!canModify}
              label={i18n.get['ForcedTags']}
              sx={{ flex: 1 }}
              size="xs"
              {...form.getInputProps('forced_subtitle_tags')}
            />
            <Tooltip label={allowHtml(i18n.get['AnExplanationDefaultValueAudio'])} {...defaultTooltipSettings}>
              <TextInput
                disabled={!canModify}
                label={i18n.get['AudioSubtitlesLanguagePriority']}
                sx={{ flex: 1 }}
                size="xs"
                {...form.getInputProps('audio_subtitles_languages')}
              />
            </Tooltip>
            <DirectoryChooser
              disabled={!canModify}
              size="xs"
              path={form.getInputProps('alternate_subtitles_folder').value}
              callback={form.setFieldValue}
              label={i18n.get['AlternateSubtitlesFolder']}
              formKey="alternate_subtitles_folder"
            ></DirectoryChooser>
            <Select
              disabled={!canModify}
              size="xs"
              label={i18n.get['NonUnicodeSubtitleEncoding']}
              data={getI18nSelectData(selectionSettings.subtitlesCodepages)}
              {...form.getInputProps('subtitles_codepage')}
            />
            <Checkbox
              disabled={!canModify}
              size="xs"
              label={i18n.get['FribidiMode']}
              {...form.getInputProps('mencoder_subfribidi', { type: 'checkbox' })}
            />
            <DirectoryChooser
              disabled={!canModify}
              size="xs"
              tooltipText={i18n.get['ToUseFontMustBeRegistered']}
              path={form.getInputProps('subtitles_font').value}
              callback={form.setFieldValue}
              label={i18n.get['SpecifyTruetypeFont']}
              formKey="subtitles_font"
            ></DirectoryChooser>
            <Text size="xs">{i18n.get['StyledSubtitles']}</Text>
            <Grid>
              <Grid.Col span={3}>
                <TextInput
                  disabled={!canModify}
                  label={i18n.get['FontScale']}
                  sx={{ flex: 1 }}
                  size="xs"
                  {...form.getInputProps('subtitles_ass_scale')}
                />
              </Grid.Col>
              <Grid.Col span={3}>
                <NumberInput
                  label={i18n.get['FontOutline']}
                  size="xs"
                  disabled={!canModify}
                  {...form.getInputProps('mencoder_noass_outline')}
                />
              </Grid.Col>
              <Grid.Col span={3}>
                <NumberInput
                    label={i18n.get['FontShadow']}
                    size="xs"
                    disabled={!canModify}
                    {...form.getInputProps('subtitles_ass_shadow')}
                />
              </Grid.Col>
              <Grid.Col span={3}>
                <NumberInput
                    label={i18n.get['MarginPx']}
                    size="xs"
                    disabled={!canModify}
                    {...form.getInputProps('subtitles_ass_margin')}
                />
              </Grid.Col>
            </Grid>
            <Tooltip label={allowHtml(i18n.get['IfEnabledExternalSubtitlesPrioritized'])} {...defaultTooltipSettings}>
              <Checkbox
                disabled={!canModify}
                size="xs"
                label={i18n.get['AutomaticallyLoadSrtSubtitles']}
                {...form.getInputProps('autoload_external_subtitles', { type: 'checkbox' })}
              />
            </Tooltip>
            <Tooltip label={allowHtml(i18n.get['IfEnabledExternalSubtitlesAlways'])} {...defaultTooltipSettings}>
              <Checkbox
                disabled={!canModify}
                size="xs"
                label={i18n.get['ForceExternalSubtitles']}
                {...form.getInputProps('force_external_subtitles', { type: 'checkbox' })}
              />
            </Tooltip>
            <Tooltip label={allowHtml(i18n.get['IfEnabledWontModifySubtitlesStyling'])} {...defaultTooltipSettings}>
              <Checkbox
                disabled={!canModify}
                size="xs"
                label={i18n.get['UseEmbeddedStyle']}
                {...form.getInputProps('use_embedded_subtitles_style', { type: 'checkbox' })}
              />
            </Tooltip>
            <Modal size="sm"
              title={i18n.get['Color']}
              opened={subColorModalOpened}
              onClose={() => setSubColorModalOpened(false)}
            >
              <Stack align="center">
                <ColorPicker
                  format="rgba"
                  swatches={['#25262b', '#868e96', '#fa5252', '#e64980', '#be4bdb', '#7950f2', '#4c6ef5', '#228be6', '#15aabf', '#12b886', '#40c057', '#82c91e', '#fab005', '#fd7e14']}
                  color={subColor}
                  onChange={setSubColor}
                ></ColorPicker>
                <Button
                  size="xs"
                  onClick={() => { canModify && form.setFieldValue('subtitles_color', rgbaToHexA(subColor)); setSubColorModalOpened(false); }}
                >{i18n.get['Confirm']}</Button>
              </Stack>
            </Modal>
            <Group>
              <TextInput
                disabled={!canModify}
                label={i18n.get['Color']}
                sx={{ flex: 1 }}
                size="xs"
                {...form.getInputProps('subtitles_color')}
              />
              <ColorSwatch radius={5}
                size={30}
                color={form.getInputProps('subtitles_color').value}
                style={{ cursor: 'pointer', marginTop: '25px'}}
                onClick={() => { if (canModify) { setSubColor(hexAToRgba(form.getInputProps('subtitles_color').value)); setSubColorModalOpened(true); } }}
              />
            </Group>
            <Tooltip label={allowHtml(i18n.get['DeterminesDownloadedLiveSubtitlesDeleted'])} {...defaultTooltipSettings}>
              <Checkbox
                disabled={!canModify}
                size="xs"
                label={i18n.get['DeleteDownloadedLiveSubtitlesAfter']}
                checked={!form.values['live_subtitles_keep']}
                onChange={(event) => {
                  form.setFieldValue('live_subtitles_keep', !event.currentTarget.checked);
                }}
              />
            </Tooltip>
            <Tooltip label={allowHtml(i18n.get['SetsMaximumNumberLiveSubtitles'])} {...defaultTooltipSettings}>
              <NumberInput
                label={i18n.get['LimitNumberLiveSubtitlesTo']}
                size="xs"
                disabled={!canModify}
                {...form.getInputProps('live_subtitles_limit')}
              />
            </Tooltip>
            <Select
              disabled={!canModify}
              size="xs"
              label={i18n.get['3dSubtitlesDepth']}
              data={selectionSettings.subtitlesDepth}
              {...form.getInputProps('3d_subtitles_depth')}
              value={String(form.values['3d_subtitles_depth'])}
              onChange={(val) => {
                form.setFieldValue('3d_subtitles_depth', val);
              }}
            />
          </Stack>
        </Tabs.Panel>
      </Tabs>
    </Stack>
  </>);
  }

  const getEngineStatus = () => {
    const currentEngine = selectionSettings.transcodingEngines[transcodingContent];
    if (!currentEngine.isAvailable) {
      return (
        <>
          <Title my="sm" order={5}>{currentEngine.name}</Title>
          <Stack spacing="xs">
            <Text size="xs"><ExclamationMark color={'orange'} strokeWidth={3} size={14}/> {i18n.get['ThisEngineNotLoaded']}</Text>
            <Text size="xs">{i18n.getI18nFormat(currentEngine.statusText)}</Text>
          </Stack>
        </>
      )
    }
    return;
  }

  const getVLCWebVideo = () => {
    const status = getEngineStatus();
    if (status) {
      return (status);
    }
    return (
      <>
        <Title my="sm" order={5}>{selectionSettings.transcodingEngines[transcodingContent].name}</Title>
        <Stack justify="flex-start" align="flex-start" spacing="xs">
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['EnableExperimentalCodecs']}
            {...form.getInputProps('vlc_use_experimental_codecs', { type: 'checkbox' })}
          />
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['AvSyncAlternativeMethod']}
            {...form.getInputProps('vlc_audio_sync_enabled', { type: 'checkbox' })}
          />
        </Stack>
      </>
    );
  }

  const getFFMPEGAudio = () => {
    const status = getEngineStatus();
    if (status) {
      return (status);
    }
    return (
      <>
        <Title my="sm" order={5}>{selectionSettings.transcodingEngines[transcodingContent].name}</Title>
        <Stack spacing="xs">
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['AutomaticAudioResampling']}
            {...form.getInputProps('audio_resample', { type: 'checkbox' })}
          />
        </Stack>
      </>
    )
  }

  const getTsMuxerVideo = () => {
    const status = getEngineStatus();
    if (status) {
      return (status);
    }
    return (
      <>
        <Title my="sm" order={5}>{selectionSettings.transcodingEngines[transcodingContent].name}</Title>
        <Stack spacing="xs">
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['ForceFpsParsedFfmpeg']}
            {...form.getInputProps('tsmuxer_forcefps', { type: 'checkbox' })}
          />
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['MuxAllAudioTracks']}
            {...form.getInputProps('tsmuxer_mux_all_audiotracks', { type: 'checkbox' })}
          />
        </Stack>
      </>
    )
  }

  const getMEncoderVideo = () => {
    const status = getEngineStatus();
    if (status) {
      return (status);
    }
    return (
      <>
        <Title my="sm" order={5}>{selectionSettings.transcodingEngines[transcodingContent].name}</Title>
        <Title mb="sm" order={6}>{i18n.get['GeneralSettings']}</Title>
        <Stack spacing="xs">
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['EnableMultithreading']}
            {...form.getInputProps('mencoder_mt', { type: 'checkbox' })}
          />
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['SkipLoopFilterDeblocking']}
            {...form.getInputProps('mencoder_skip_loop_filter', { type: 'checkbox' })}
          />
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['AvSyncAlternativeMethod']}
            {...form.getInputProps('mencoder_nooutofsync', { type: 'checkbox' })}
          />
          <Grid>
            <Grid.Col span={4}>
              <Checkbox
                disabled={!canModify}
                size="xs"
                label={i18n.get['ChangeVideoResolution']}
                {...form.getInputProps('mencoder_scaler', { type: 'checkbox' })}
              />
            </Grid.Col>
            <Grid.Col span={4}>
              <TextInput
                disabled={!canModify || !form.values['mencoder_scaler']}
                label={i18n.get['Width']}
                sx={{ flex: 1 }}
                size="xs"
                {...form.getInputProps('mencoder_scalex')}
              />
            </Grid.Col>
            <Grid.Col span={4}>
              <TextInput
                disabled={!canModify || !form.values['mencoder_scaler']}
                label={i18n.get['Height']}
                size="xs"
                {...form.getInputProps('mencoder_scaley')}
              />
            </Grid.Col>
          </Grid>
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['ForceFramerateParsedFfmpeg']}
            {...form.getInputProps('mencoder_forcefps', { type: 'checkbox' })}
          />
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['DeinterlaceFilter']}
            {...form.getInputProps('mencoder_yadif', { type: 'checkbox' })}
          />
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['RemuxVideosTsmuxer']}
            {...form.getInputProps('mencoder_mux_compatible ', { type: 'checkbox' })}
          />
          <TextInput
            disabled={!canModify}
            label={i18n.get['CustomOptionsVf']}
            name="mencoder_custom_options"
            size="xs"
            {...form.getInputProps('mencoder_custom_options')}
          />
          <Modal
            size="xl"
            opened={mencoderAdvancedOpened}
            onClose={() => setMencoderAdvancedOpened(false)}
            title={i18n.get['EditCodecSpecificParameters']}
          >
            <Checkbox
              disabled={!canModify}
              size="xs"
              label={i18n.get['UseApplicationDefaults']}
              {...form.getInputProps('mencoder_intelligent_sync', { type: 'checkbox' })}
            />
            <Prism language={'markup'}>{
              i18n.get['MencoderConfigScript.1.HereYouCanInputSpecific'] +
              i18n.get['MencoderConfigScript.2.WarningThisShouldNot'] +
              i18n.get['MencoderConfigScript.3.SyntaxIsJavaCondition'] +
              i18n.get['MencoderConfigScript.4.AuthorizedVariables'] +
              i18n.get['MencoderConfigScript.5.SpecialOptions'] +
              i18n.get['MencoderConfigScript.6.Noass'] +
              i18n.get['MencoderConfigScript.7.Nosync'] +
              i18n.get['MencoderConfigScript.8.Quality'] +
              i18n.get['MencoderConfigScript.9.Nomux'] +
              i18n.get['MencoderConfigScript.10.YouCanPut'] +
              i18n.get['MencoderConfigScript.11.ToRemoveJudder'] +
              i18n.get['MencoderConfigScript.12.ToRemux']}
            </Prism>
            <Textarea
              disabled={!canModify}
              label={i18n.get['CustomParameters']}
              name="mencoder_codec_specific_script"
              size="xs"
              {...form.getInputProps('mencoder_codec_specific_script')}
            />
          </Modal>
          <Group position="center">
            <Button variant="subtle" compact onClick={() => setMencoderAdvancedOpened(true)}>{i18n.get['CodecSpecificParametersAdvanced']}</Button>
          </Group>
          <Grid>
            <Grid.Col span={6}>
              <Text
                size="xs"
                style={{ marginTop: '14px'}}
              >{i18n.get['AddBordersOverscanCompensation']}</Text>
            </Grid.Col>
            <Grid.Col span={3}>
              <TextInput
                disabled={!canModify}
                label={i18n.get['Height'] + '(%)'}
                sx={{ flex: 1 }}
                size="xs"
                {...form.getInputProps('mencoder_overscan_compensation_height')}
              />
            </Grid.Col>
            <Grid.Col span={3}>
              <TextInput
                disabled={!canModify}
                label={i18n.get['Width'] + '(%)'}
                size="xs"
                {...form.getInputProps('mencoder_overscan_compensation_width')}
              />
            </Grid.Col>
          </Grid>
        </Stack>
        <Title my='sm' order={6}>{i18n.get['SubtitlesSettings']}</Title>
        <Stack spacing="xs">
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['UseAssSubtitlesStyling']}
            {...form.getInputProps('mencoder_ass', { type: 'checkbox' })}
          />
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['FonconfigEmbeddedFonts']}
            {...form.getInputProps('mencoder_fontconfig', { type: 'checkbox' })}
          />
        </Stack>
      </>
    )
  }

  const getFFMPEGVideo = () => {
    const status = getEngineStatus();
    if (status) {
      return (status);
    }
    return (
      <>
        <Title my="sm" order={5}>{selectionSettings.transcodingEngines[transcodingContent].name}</Title>
        <Stack spacing="xs">
          <Select
            disabled={!canModify}
            size="xs"
            label={i18n.get['LogLevelColon']}
            data={selectionSettings.ffmpegLoglevels}
            {...form.getInputProps('ffmpeg_logging_level')}
          />
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['EnableMultithreading']}
            {...form.getInputProps('ffmpeg_multithreading', { type: 'checkbox' })}
          />
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['RemuxVideosTsmuxer']}
            {...form.getInputProps('ffmpeg_mux_tsmuxer_compatible', { type: 'checkbox' })}
          />
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['UseFontSettings']}
            {...form.getInputProps('ffmpeg_fontconfig', { type: 'checkbox' })}
          />
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['DeferMencoderTranscodingProblematic']}
            {...form.getInputProps('ffmpeg_mencoder_problematic_subtitles', { type: 'checkbox' })}
          />
          <Checkbox
            disabled={!canModify}
            size="xs"
            label={i18n.get['UseSoxHigherQualityAudio']}
            {...form.getInputProps('fmpeg_sox', { type: 'checkbox' })}
          />
          <NumberInput
            disabled={!canModify}
            label={i18n.get['GpuDecodingThreadCount']}
            size="xs"
            max={16}
            min={0}
            {...form.getInputProps('ffmpeg_gpu_decoding_acceleration_thread_number')}
          />
        </Stack>
      </>
    )
  }

  const noSettingsForNow = () => {
    const status = getEngineStatus();
    if (status) {
      return (status)
    }
    return (
      <>
        <Title my="sm" order={5}>{selectionSettings.transcodingEngines[transcodingContent].name}</Title>
        <Text size="xs">{i18n.get['NoSettingsForNow']}</Text>
      </>
    )
  }

  const engineNotKnown = () => {
    const status = getEngineStatus();
    if (status) {
      return (status)
    }
    return (
      <>
        <Title my="sm" order={5}>{selectionSettings.transcodingEngines[transcodingContent].name}</Title>
        <Text size="xs">This engine is not known yet.</Text>
      </>
    )
  }

  const getTranscodingContent = () => {
    switch(transcodingContent) {
      case 'common':
        return getTranscodingCommon();
      case 'DCRaw':
        return noSettingsForNow();
      case 'FFmpegAudio':
        return getFFMPEGAudio();
      case 'FFmpegVideo':
        return getFFMPEGVideo();
      case 'FFmpegWebVideo':
        return noSettingsForNow();
      case 'MEncoderVideo':
        return getMEncoderVideo();
      case 'MEncoderWebVideo':
        return noSettingsForNow();
      case 'tsMuxeRAudio':
        return noSettingsForNow();
      case 'tsMuxeRVideo':
        return getTsMuxerVideo();
      case 'VLCAudioStreaming':
        return noSettingsForNow();
      case 'VLCVideo':
        return getVLCWebVideo();
      case 'VLCWebVideo':
        return getVLCWebVideo();
      case 'VLCVideoStreaming':
        return noSettingsForNow();
      case 'youtubeDl':
        return noSettingsForNow();
      default:
      return engineNotKnown();
    }
  }

  const getTranscodingSettingsTab = () => {
    return (
            <Grid>
              <Grid.Col span={5}>
                <Box p="xs">
                  <NavLink variant="subtle" color="gray" label={i18n.get['CommonTranscodeSettings']} onClick={() => setTranscodingContent('common')} />
                  <Accordion>
                    {getTranscodingEnginesAccordionItems()}
                  </Accordion>
                  <Text size="xs">{i18n.get['EnginesAreInDescending'] + ' ' + i18n.get['OrderTheHighestIsFirst']}</Text>
                </Box>
              </Grid.Col>
              <Grid.Col span={7}>
                {getTranscodingContent()}
              </Grid.Col>
            </Grid>
    );
  }

  return canView ? (
    <Box sx={{ maxWidth: 700 }} mx="auto">
      <form onSubmit={form.onSubmit(handleSubmit)}>
        <Tabs defaultValue="GeneralSettings">
          <Tabs.List>
            <Tabs.Tab value="GeneralSettings">{i18n.get['GeneralSettings']}</Tabs.Tab>
            <Tabs.Tab value="NavigationSettings">{i18n.get['NavigationSettings']}</Tabs.Tab>
            <Tabs.Tab value="SharedContent">{i18n.get['SharedContent']}</Tabs.Tab>
            <Tabs.Tab value="TranscodingSettings">{i18n.get['TranscodingSettings']}</Tabs.Tab>
          </Tabs.List>
          <Tabs.Panel value="GeneralSettings">{ getGeneralSettingsTab() }</Tabs.Panel>
          <Tabs.Panel value="NavigationSettings">{ getNavigationSettingsTab() }</Tabs.Panel>
          <Tabs.Panel value="SharedContent">{ getSharedContentTab() }</Tabs.Panel>
          <Tabs.Panel value="TranscodingSettings">{ getTranscodingSettingsTab() }</Tabs.Panel>
        </Tabs>
        {canModify && (
          <Group position="right" mt="md">
            <Button type="submit" loading={isLoading}>
              {i18n.get['Save']}
            </Button>
          </Group>
        )}
      </form>
    </Box>
  ) : (
    <Box sx={{ maxWidth: 700 }} mx="auto">
      <Text color="red">{i18n.get['YouNotHaveAccessArea']}</Text>
    </Box>
  );
}
