import { TextInput, Checkbox, MultiSelect, Modal, NumberInput, Button, Group, Space, Box, Select, Tabs, Accordion, Grid, Navbar, Tooltip, Text } from '@mantine/core';
import { useForm } from '@mantine/form';
import { showNotification } from '@mantine/notifications';
import _ from 'lodash';
import { useContext, useEffect, useRef, useState } from 'react';
import { SketchPicker } from 'react-color';
import axios from 'axios';

import I18nContext from '../../contexts/i18n-context';

import {getToolTipContent} from '../../utils';
import SessionContext from '../../contexts/session-context';
import { havePermission } from '../../services/accounts-service';
import DirectoryChooser from '../DirectoryChooser/DirectoryChooser';

export default function Settings() {
  const [activeTab, setActiveTab] = useState(0);
  const [activeGeneralSettingsTab, setGeneralSettingsTab] = useState(0);
  const [isLoading, setLoading] = useState(true);

  // key/value pairs for dropdowns
  const languageSettingsRef = useRef([]);
  const networkInterfaceSettingsRef = useRef([]);
  const serverEnginesSettingsRef = useRef([]);
  const allRendererNamesSettingsRef = useRef([]);
  const enabledRendererNamesSettingsRef = useRef([]);
  const audioCoverSuppliersSettingsRef = useRef([]);
  const sortMethodsSettingsRef = useRef([]);
  const numberOfCpuCoresSettingsRef = useRef(1);

  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
    
  const defaultSettings: Record<string, any> = {
    alternate_subtitles_folder: '',
    alternate_thumb_folder: '',
    append_profile_name: false,
    audio_channels: '6',
    audio_embed_dts_in_pcm: false,
    audio_bitrate: '448',
    audio_remux_ac3: true,
    audio_use_pcm: false,
    audio_subtitles_languages: 'eng,off;*,eng;*,und',
    audio_thumbnails_method: '1',
    auto_update: true,
    autoload_external_subtitles: true,
    automatic_maximum_bitrate: true,
    chapter_interval: 5,
    chapter_support: false,
    disable_subtitles: false,
    disable_transcode_for_extensions: '',
    encoded_audio_passthrough: false,
    force_transcode_for_extensions: '',
    gpu_acceleration: false,
    external_network: true,
    force_external_subtitles: true,
    forced_subtitle_language: 'en',
    forced_subtitle_tags: 'forced',
    generate_thumbnails: true,
    hostname: '',
    ip_filter: '',
    language: 'en-US',
    live_subtitles_keep: false,
    live_subtitles_limit: 20,
    maximum_video_buffer_size: 200,
    maximum_bitrate: '90',
    mencoder_noass_scale: 'todo',
    mencoder_noass_outline: 'todo',
    mencoder_remux_mpeg2: true,
    mencoder_subfribidi: false,
    minimized: false,
    mpeg2_main_settings: 'Automatic (Wired)',
    network_interface: '',
    number_of_cpu_cores: numberOfCpuCoresSettingsRef.current,
    port: '',
    renderer_default: '',
    renderer_force_default: false,
    selected_renderers: ['All renderers'],
    server_engine: '0',
    server_name: 'Universal Media Server',
    show_splash_screen: true,
    sort_method: '4',
    subtitles_ass_margin: 'tod',
    subtitles_ass_shadow: 'tod',
    subtitles_codepage: '',
    subtitles_color: '0xFFFFFFFF',
    subtitles_font: '',
    subtitles_languages: 'eng,fre,jpn,ger,und',
    thumbnail_seek_position: '4',
    use_embedded_subtitles_style: true,
    x264_constant_rate_factor: 'Automatic (Wired)',
    '3d_subtitles_depth': '0',
  };

  const subtitlesCodepage = [
    { label: i18n['Generic.AutoDetect'], value: ''},
    { label: i18n['CharacterSet.874'], value: 'cp874'},
    { label: i18n['CharacterSet.932'], value: 'cp932'},
    { label: i18n['CharacterSet.936'], value: 'cp936'},
    { label: i18n['CharacterSet.949'], value: 'cp949'},
    { label: i18n['CharacterSet.950'], value: 'cp950'},
    { label: i18n['CharacterSet.1250'], value: 'cp1250'},
    { label: i18n['CharacterSet.1251'], value: 'cp1251'},
    { label: i18n['CharacterSet.1252'], value: 'cp1252'},
    { label: i18n['CharacterSet.1253'], value: 'cp1253'},
    { label: i18n['CharacterSet.1254'], value: 'cp1254'},
    { label: i18n['CharacterSet.1255'], value: 'cp1255'},
    { label: i18n['CharacterSet.1256'], value: 'cp1256'},
    { label: i18n['CharacterSet.1257'], value: 'cp1257'},
    { label: i18n['CharacterSet.1258'], value: 'cp1258'},
    { label: i18n['CharacterSet.2022-CN'], value: 'ISO-2022-CN'},
    { label: i18n['CharacterSet.2022-JP'], value: 'ISO-2022-JP'},
    { label: i18n['CharacterSet.2022-KR'], value: 'ISO-2022-KR'},
    { label: i18n['CharacterSet.8859-1'], value: 'ISO-8859-1'},
    { label: i18n['CharacterSet.8859-2'], value: 'ISO-8859-2'},
    { label: i18n['CharacterSet.8859-3'], value: 'ISO-8859-3'},
    { label: i18n['CharacterSet.8859-4'], value: 'ISO-8859-4'},
    { label: i18n['CharacterSet.8859-5'], value: 'ISO-8859-5'},
    { label: i18n['CharacterSet.8859-6'], value: 'ISO-8859-6'},
    { label: i18n['CharacterSet.8859-7'], value: 'ISO-8859-7'},
    { label: i18n['CharacterSet.8859-8'], value: 'ISO-8859-8'},
    { label: i18n['CharacterSet.8859-9'], value: 'ISO-8859-9'},
    { label: i18n['CharacterSet.8859-10'], value: 'ISO-8859-10'},
    { label: i18n['CharacterSet.8859-11'], value: 'ISO-8859-11'},
    { label: i18n['CharacterSet.8859-13'], value: 'ISO-8859-13'},
    { label: i18n['CharacterSet.8859-14'], value: 'ISO-8859-14'},
    { label: i18n['CharacterSet.8859-15'], value: 'ISO-8859-15'},
    { label: i18n['CharacterSet.8859-16'], value: 'ISO-8859-16'},
    { label: i18n['CharacterSet.Big5'], value: 'Big5'},
    { label: i18n['CharacterSet.EUC-JP'], value: 'EUC-JP'},
    { label: i18n['CharacterSet.EUC-KR'], value: 'EUC-KR'},
    { label: i18n['CharacterSet.GB18030'], value: 'GB18030'},
    { label: i18n['CharacterSet.IBM420'], value: 'IBM420'},
    { label: i18n['CharacterSet.IBM424'], value: 'IBM424'},
    { label: i18n['CharacterSet.KOI8-R'], value: 'KOI8-R'},
    { label: i18n['CharacterSet.ShiftJIS]'], value: 'Shift_JIS'},
    { label: i18n['CharacterSet.TIS-620'], value: 'TIS-620'},
  ];

  const threeDSubs = ['-5', '-4', '-3', '-2', '-1', '0', '1', '2', '3', '4', '5']

  const defaultTooltipSettings = {
    width: 350,
    color: 'blue',
    wrapLines: true,
    withArrow: true,
  }

  const openGitHubNewIssue = () => {
    window.location.href = 'https://github.com/UniversalMediaServer/UniversalMediaServer/issues/new';
  };

  const [configuration, setConfiguration] = useState(defaultSettings);

  const form = useForm({ initialValues: defaultSettings });

  const canModify = havePermission(session, "settings_modify");
  const canView = canModify || havePermission(session, "settings_view");

  // Code here will run just like componentDidMount
  useEffect(() => {
    canView && axios.get('/configuration-api/settings')
      .then(function (response: any) {
        const settingsResponse = response.data;
        languageSettingsRef.current = settingsResponse.languages;
        networkInterfaceSettingsRef.current = settingsResponse.networkInterfaces;
        serverEnginesSettingsRef.current = settingsResponse.serverEngines;
        allRendererNamesSettingsRef.current = settingsResponse.allRendererNames;
        enabledRendererNamesSettingsRef.current = settingsResponse.enabledRendererNames;
        audioCoverSuppliersSettingsRef.current = settingsResponse.audioCoverSuppliers;
        sortMethodsSettingsRef.current = settingsResponse.sortMethods;
        numberOfCpuCoresSettingsRef.current = settingsResponse.numberOfCpuCores;

        //update default settings
        defaultSettings.number_of_cpu_cores = settingsResponse.numberOfCpuCores;

        // merge defaults with what we receive, which might only be non-default values
        const userConfig = _.merge(defaultSettings, settingsResponse.userSettings);

        setConfiguration(userConfig);
        form.setValues(configuration);
      })
      .catch(function (error: Error) {
        console.log(error);
        showNotification({
          id: 'data-loading',
          color: 'red',
          title: 'Error',
          message: 'Your configuration was not received from the server. Please click here to report the bug to us.',
          onClick: () => { openGitHubNewIssue(); },
          autoClose: 3000,
        });
      })
      .then(function () {
        form.validate();
        setLoading(false);
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  // eslint-disable-next-line
  const [content, setContent] = useState('common');
  const [opened, setOpened] = useState(false);
  const [subtitleColor, setSubtitleColor] = useState(defaultSettings.subtitles_color);
  const handleSubmit = (values: typeof form.values) => {
    const changedValues: Record<string, any> = {};

    // construct an object of only changed values to send
    for (let key in values) {
      if (!_.isEqual(configuration[key], values[key])) {
        changedValues[key] = values[key];
      }
    };

    if (_.isEmpty(changedValues)) {
      showNotification({
        title: 'Saved',
        message: 'Your configuration has no changes to save',
      })
      return;
    }

    setLoading(true);
    axios.post('/configuration-api/settings', changedValues)
      .then(function () {
        setConfiguration(values);
        showNotification({
          title: 'Saved',
          message: 'Your configuration changes were saved successfully',
        })
      })
      .catch(function (error: Error) {
        console.log(error);
        showNotification({
          color: 'red',
          title: 'Error',
          message: 'Your configuration changes were not saved. Please click here to report the bug to us.',
          onClick: () => { openGitHubNewIssue(); },
        })
      })
      .then(function () {
        setLoading(false);
      });
  };

  return canView ? (
    <Box sx={{ maxWidth: 700 }} mx="auto">
      <form onSubmit={form.onSubmit(handleSubmit)}>
        <Tabs active={activeTab} onTabChange={setActiveTab}>
          <Tabs.Tab label={i18n['LooksFrame.TabGeneralSettings']}>
            <Select
              disabled={!canModify}
              label={i18n['LanguageSelection.Language']}
              data={languageSettingsRef.current}
              {...form.getInputProps('language')}
            />

            <Group mt="xs">
              <TextInput
                disabled={!canModify}
                label={i18n['NetworkTab.71']}
                name="server_name"
                sx={{ flex: 1 }}
                {...form.getInputProps('server_name')}
              />
              <Tooltip label={getToolTipContent(i18n['NetworkTab.73'])} width={350} color="blue" wrapLines withArrow>
                <Checkbox
                  disabled={!canModify}
                  mt="xl"
                  label={i18n['NetworkTab.72']}
                  {...form.getInputProps('append_profile_name', { type: 'checkbox' })}
                />
                </Tooltip>
            </Group>
            <Group mt="md">
              <Checkbox
                disabled={!canModify}
                label={i18n['NetworkTab.3']}
                {...form.getInputProps('minimized', { type: 'checkbox' })}
              />
              <Checkbox
                disabled={!canModify}
                label={i18n['NetworkTab.74']}
                {...form.getInputProps('show_splash_screen', { type: 'checkbox' })}
              />
            </Group>

            <Checkbox
              disabled={!canModify}
              mt="xs"
              label={i18n['NetworkTab.9']}
              {...form.getInputProps('auto_update', { type: 'checkbox' })}
            />

            <Accordion mt="xl">
              <Accordion.Item label={i18n['NetworkTab.22']}>
                <Select
                  disabled={!canModify}
                  label={i18n['NetworkTab.20']}
                  data={networkInterfaceSettingsRef.current}
                  {...form.getInputProps('network_interface')}
                />

                <TextInput
                  disabled={!canModify}
                  mt="xs"
                  label={i18n['NetworkTab.23']}
                  {...form.getInputProps('hostname')}
                />

                <TextInput
                  disabled={!canModify}
                  mt="xs"
                  label={i18n['NetworkTab.24']}
                  {...form.getInputProps('port')}
                />

                <TextInput
                  disabled={!canModify}
                  mt="xs"
                  label={i18n['NetworkTab.30']}
                  {...form.getInputProps('ip_filter')}
                />

                <Group mt="xs">
                  <TextInput
                    sx={{ flex: 1 }}
                    label={i18n['NetworkTab.35']}
                    disabled={!canModify || form.values['automatic_maximum_bitrate']}
                    {...form.getInputProps('maximum_bitrate')}
                  />
                  
                  <Tooltip label={getToolTipContent(i18n['GeneralTab.12.Tooltip'])} {...defaultTooltipSettings}>
                    <Checkbox
                      disabled={!canModify}
                      mt="xl"
                      label={i18n['GeneralTab.12']}
                      {...form.getInputProps('automatic_maximum_bitrate', { type: 'checkbox' })}
                    />
                  </Tooltip>
                </Group>
              </Accordion.Item>
              <Accordion.Item label={i18n['NetworkTab.31']}>
              
                <Tooltip label={getToolTipContent(i18n['NetworkTab.MediaServerEngineTooltip'])} {...defaultTooltipSettings}>
                  <Select
                    disabled={!canModify}
                    label={i18n['NetworkTab.MediaServerEngine']}
                    data={serverEnginesSettingsRef.current}
                    {...form.getInputProps('server_engine')}
                  />
                </Tooltip>

                <MultiSelect
                  disabled={!canModify}
                  mt="xs"
                  data={allRendererNamesSettingsRef.current}
                  label={i18n['NetworkTab.62']}
                  {...form.getInputProps('selected_renderers')}
                />

                <Group mt="xs">
                  <Select
                    disabled={!canModify}
                    sx={{ flex: 1 }}
                    label={i18n['NetworkTab.36']}
                    data={enabledRendererNamesSettingsRef.current}
                    {...form.getInputProps('renderer_default')}
                    searchable
                  />

                  <Tooltip label={getToolTipContent(i18n['GeneralTab.ForceDefaultRendererTooltip'])} {...defaultTooltipSettings}>
                    <Checkbox
                      disabled={!canModify}
                      mt="xl"
                      label={i18n['GeneralTab.ForceDefaultRenderer']}
                      {...form.getInputProps('renderer_force_default', { type: 'checkbox' })}
                    />
                  </Tooltip>
                </Group>

                <Tooltip label={getToolTipContent(i18n['NetworkTab.67'])} {...defaultTooltipSettings}>
                  <Checkbox
                    disabled={!canModify}
                    mt="xs"
                    label={i18n['NetworkTab.56']}
                    {...form.getInputProps('external_network', { type: 'checkbox' })}
                  />
                </Tooltip>

              </Accordion.Item>
            </Accordion>
          </Tabs.Tab>
          <Tabs.Tab label={i18n['LooksFrame.TabNavigationSettings']}>
            <Group mt="xs">
              <Checkbox
                mt="xl"
                label={i18n['NetworkTab.2']}
                {...form.getInputProps('generate_thumbnails', { type: 'checkbox' })}
              />
              <TextInput
                sx={{ flex: 1 }}
                label={i18n['NetworkTab.16']}
                disabled={!form.values['generate_thumbnails']}
                {...form.getInputProps('thumbnail_seek_position')}
              />
            </Group>
            <Select
              mt="xs"
              label={i18n['FoldTab.26']}
              data={audioCoverSuppliersSettingsRef.current}
              {...form.getInputProps('audio_thumbnails_method')}
            />
            <DirectoryChooser
              path={form.getInputProps('alternate_thumb_folder').value}
              callback={form.setFieldValue}
              label={i18n['FoldTab.27']}
              formKey="alternate_thumb_folder"
            ></DirectoryChooser>
            <Accordion mt="xl">
              <Accordion.Item label={i18n['NetworkTab.59']}>
                <Select
                  mt="xs"
                  label={i18n['FoldTab.26']}
                  data={sortMethodsSettingsRef.current}
                  {...form.getInputProps('sort_method')}
                />
              </Accordion.Item>
            </Accordion>
          </Tabs.Tab>
          <Tabs.Tab label={i18n['LooksFrame.TabSharedContent']}>
            
          </Tabs.Tab>
          <Tabs.Tab label={i18n['LooksFrame.21']}>
            <Grid>
              <Grid.Col span={5}>
                <Navbar width={{ }} p="xs">
                  <Navbar.Section>
                    <Button variant="subtle" color="dark" size="xs" compact onClick={() => setContent('common')}>
                      Common transcode settings
                    </Button>
                  </Navbar.Section>
                  <Navbar.Section>
                  <Accordion>
                    <Accordion.Item label="Video Files Engines">
                      <Button variant="subtle" color="dark" size="xs" compact onClick={() => setContent('ffmpeg')}>
                        FFmpeg Video
                      </Button>
                      <Button variant="subtle" color="dark" size="xs" compact onClick={() => setContent('mencoder')}>
                        MEncoder Video
                      </Button>
                      <Button variant="subtle" color="dark" size="xs" compact onClick={() => setContent('tsmuxer')}>
                        tsMuxeR Video
                      </Button>
                      <Button variant="subtle" color="dark" size="xs" compact onClick={() => setContent('vlc')}>
                        VLC Video
                      </Button>
                    </Accordion.Item>
                    <Accordion.Item label="Audio Files Engines">
                      <Button variant="subtle" color="dark" size="xs" compact onClick={() => setContent('ffmpegaudio')}>
                        FFmpeg Audio
                      </Button>
                      <Button variant="subtle" color="dark" size="xs" compact onClick={() => setContent('tmuxeraudio')}>
                        tsMuxeR Video
                      </Button>
                    </Accordion.Item>
                    <Accordion.Item label="Web video streaming engines">
                      <Button variant="subtle" color="dark" size="xs" compact onClick={() => setContent('ffmpegweb')}>
                        FFmpeg Web Video
                      </Button>
                      <Button variant="subtle" color="dark" size="xs" compact onClick={() => setContent('youtube-dl')}>
                        youtube-dl
                      </Button>
                      <Button variant="subtle" color="dark" size="xs" compact onClick={() => setContent('vlcwebvideo')}>
                        VLC Web Video
                      </Button>
                      <Button variant="subtle" color="dark" size="xs" compact onClick={() => setContent('vlcwebvideolegacy')}>
                        VLC Web Video (legacy)
                      </Button>
                      <Button variant="subtle" color="dark" size="xs" compact onClick={() => setContent('mencoderwebvideo')}>
                        Mencoder Web Video
                      </Button>
                    </Accordion.Item>
                    <Accordion.Item label="Web audio streaming engines">
                      <Button variant="subtle" color="dark" size="xs" compact onClick={() => setContent('vlcwebaudio')}>
                        VLC Web Audio (Legacy)
                      </Button>
                    </Accordion.Item>
                    <Accordion.Item label="Misc engines">
                      <Button variant="subtle" color="dark" size="xs" compact onClick={() => setContent('dcraw')}>
                        DCRaw
                      </Button>
                    </Accordion.Item>
                  </Accordion>
                  </Navbar.Section>
                </Navbar>
              </Grid.Col>
              <Grid.Col span={7}>
                <Tooltip label={getToolTipContent(i18n['TrTab2.73'])} {...defaultTooltipSettings}>
                  <TextInput
                    label={i18n['TrTab2.23']}
                    name="maximum_video_buffer_size"
                    sx={{ flex: 1 }}
                    size="xs"
                    {...form.getInputProps('maximum_video_buffer_size')}
                  />
                </Tooltip>
                <NumberInput
                  label={i18n['TrTab2.24']?.replace('%d', numberOfCpuCoresSettingsRef.current.toString())}
                  size="xs"
                  max={numberOfCpuCoresSettingsRef.current}
                  min={1}
                  disabled={false}
                  {...form.getInputProps('number_of_cpu_cores')}
                />
                <Space h="xs"/>
                <Grid>
                  <Grid.Col span={10}>
                    <Checkbox
                      size="xs"
                      label={i18n['TrTab2.52']}
                      {...form.getInputProps('chapter_support', { type: 'checkbox' })}
                    />
                  </Grid.Col>
                  <Grid.Col span={2}>
                    <TextInput
                      sx={{ flex: 1 }}
                      disabled={!form.values['chapter_support']}
                      {...form.getInputProps('chapter_interval')}
                    />
                  </Grid.Col>
                </Grid>
                <Checkbox
                  size="xs"
                  label={i18n['TrTab2.51']}
                  {...form.getInputProps('disable_subtitles', { type: 'checkbox' })}
                />
                <Space h="md"/>
                <Tabs active={activeGeneralSettingsTab} onTabChange={setGeneralSettingsTab}>
                  <Tabs.Tab label={i18n['TrTab2.67']}>
                    <Checkbox
                      size="xs"
                      label={i18n['TrTab2.70']}
                      {...form.getInputProps('gpu_acceleration', { type: 'checkbox' })}
                    />
                    <Space h="xs" />
                    <Tooltip label={getToolTipContent(i18n['TrTab2.82'])} {...defaultTooltipSettings}>
                      <Checkbox
                        size="xs"
                        label={i18n['MEncoderVideo.39']}
                        {...form.getInputProps('mencoder_remux_mpeg2', { type: 'checkbox' })}
                      />
                    </Tooltip>
                    <Space h="xs" />
                    <Tooltip label={getToolTipContent(i18n['TrTab2.74'])} {...defaultTooltipSettings}>
                      <TextInput
                        label={i18n['TrTab2.32']}
                        sx={{ flex: 1 }}
                        disabled={form.values['automatic_maximum_bitrate']}
                        {...form.getInputProps('mpeg2_main_settings')}
                      />
                    </Tooltip>
                    <Space h="xs" />
                    <Tooltip label={getToolTipContent(i18n['TrTab2.81'])} {...defaultTooltipSettings}>
                      <TextInput
                        label={i18n['TrTab2.79']}
                        sx={{ flex: 1 }}
                        disabled={form.values['automatic_maximum_bitrate']}
                        {...form.getInputProps('x264_constant_rate_factor')}
                      />
                    </Tooltip>
                    <Tooltip label={getToolTipContent(i18n['TrTab2.96'])} {...defaultTooltipSettings}>
                      <TextInput
                        label={i18n['TrTab2.8']}
                        sx={{ flex: 1 }}
                        {...form.getInputProps('disable_transcode_for_extensions')}
                      />
                    </Tooltip>
                    <Tooltip label={getToolTipContent(i18n['TrTab2.96'])} {...defaultTooltipSettings}>
                      <TextInput
                        label={i18n['TrTab2.9']}
                        sx={{ flex: 1 }}
                        {...form.getInputProps('force_transcode_for_extensions')}
                      />
                    </Tooltip>
                  </Tabs.Tab>
                  <Tabs.Tab label={i18n['TrTab2.68']}>
                    <Select
                      label={i18n['TrTab2.50']}
                      data={[{value: '6', label: i18n['TrTab2.56']}, {value: '2', label: i18n['TrTab2.55']}]}
                      size="xs"
                      {...form.getInputProps('audio_channels')}
                    />
                    <Space h="xs" />
                    <Tooltip label={getToolTipContent(i18n['TrTab2.83'])} {...defaultTooltipSettings}>
                      <Checkbox
                        size="xs"
                        label={i18n['TrTab2.27']}
                        {...form.getInputProps('audio_use_pcm', { type: 'checkbox' })}
                      />
                    </Tooltip>
                    <Space h="xs" />
                    <Tooltip label={getToolTipContent(i18n['TrTab2.84'])} {...defaultTooltipSettings}>
                      <Checkbox
                        size="xs"
                        label={i18n['TrTab2.26']}
                        {...form.getInputProps('audio_remux_ac3', { type: 'checkbox' })}
                      />
                    </Tooltip>
                    <Space h="xs" />
                    <Tooltip label={getToolTipContent(i18n['TrTab2.85'])} {...defaultTooltipSettings}>
                      <Checkbox
                        size="xs"
                        label={i18n['TrTab2.28']}
                        {...form.getInputProps('audio_embed_dts_in_pcm', { type: 'checkbox' })}
                      />
                    </Tooltip>
                    <Space h="xs" />
                    <Tooltip label={getToolTipContent(i18n['TrTab2.86'])} {...defaultTooltipSettings}>
                      <Checkbox
                        size="xs"
                        label={i18n['TrTab2.53']}
                        {...form.getInputProps('encoded_audio_passthrough', { type: 'checkbox' })}
                      />
                    </Tooltip>
                    <Space h="xs" />
                    <TextInput
                      label={i18n['TrTab2.29']}
                      sx={{ flex: 1 }}
                      size="xs"
                      {...form.getInputProps('audio_bitrate')}
                    />
                    <Tooltip label={getToolTipContent(i18n['TrTab2.75'])} {...defaultTooltipSettings}>
                      <TextInput
                        label={i18n['MEncoderVideo.7']}
                        sx={{ flex: 1 }}
                        size="xs"
                        {...form.getInputProps('audio_languages')}
                      />
                    </Tooltip>
                  </Tabs.Tab>
                    <Tabs.Tab label={i18n['MEncoderVideo.8']}>
                      <Tooltip label={getToolTipContent(i18n['TrTab2.76'])} {...defaultTooltipSettings}>
                        <TextInput
                          label={i18n['MEncoderVideo.9']}
                          sx={{ flex: 1 }}
                          size="xs"
                          {...form.getInputProps('subtitles_languages')}
                        />
                      </Tooltip>
                      <TextInput
                        label={i18n['MEncoderVideo.94']}
                        sx={{ flex: 1 }}
                        size="xs"
                        {...form.getInputProps('forced_subtitle_language')}
                      />
                      <TextInput
                        label={i18n['MEncoderVideo.95']}
                        sx={{ flex: 1 }}
                        size="xs"
                        {...form.getInputProps('forced_subtitle_tags')}
                      />
                      <Tooltip label={getToolTipContent(i18n['TrTab2.77'])} {...defaultTooltipSettings}>
                        <TextInput
                          label={i18n['MEncoderVideo.10']}
                          sx={{ flex: 1 }}
                          size="xs"
                          {...form.getInputProps('audio_subtitles_languages')}
                        />
                      </Tooltip>
                      <DirectoryChooser
                        path={form.getInputProps('alternate_subtitles_folder').value}
                        callback={form.setFieldValue}
                        label={i18n['MEncoderVideo.37']}
                        formKey="alternate_subtitles_folder"
                      ></DirectoryChooser>
                      <Select
                        label={i18n['TrTab2.95']}
                        data={subtitlesCodepage}
                        {...form.getInputProps('subtitles_codepage')}
                      />
                      <Space h="xs" />
                      <Checkbox
                        size="xs"
                        label={i18n['MEncoderVideo.23']}
                        {...form.getInputProps('mencoder_subfribidi', { type: 'checkbox' })}
                      />
                      <Tooltip label={getToolTipContent(i18n['TrTab2.97'])} {...defaultTooltipSettings}>
                        <DirectoryChooser
                          path={form.getInputProps('subtitles_font').value}
                          callback={form.setFieldValue}
                          label={i18n['MEncoderVideo.24']}
                          formKey="subtitles_font"
                        ></DirectoryChooser>
                      </Tooltip>
                      <Text>{i18n['MEncoderVideo.12']}</Text>
                      <Space h="xs" />
                      <Grid>
                        <Grid.Col span={3}>
                          <TextInput
                            label={i18n['MEncoderVideo.133']}
                            sx={{ flex: 1 }}
                            size="xs"
                            {...form.getInputProps('mencoder_noass_scale')}
                          />
                        </Grid.Col>
                        <Grid.Col span={3}>
                          <NumberInput
                            label={i18n['MEncoderVideo.13']}
                            size="xs"
                            disabled={!canModify}
                            {...form.getInputProps('mencoder_noass_outline')}
                          />
                        </Grid.Col>
                        <Grid.Col span={3}>
                          <NumberInput
                              label={i18n['MEncoderVideo.14']}
                              size="xs"
                              disabled={!canModify}
                              {...form.getInputProps('subtitles_ass_shadow')}
                          />
                        </Grid.Col>
                        <Grid.Col span={3}>
                          <NumberInput
                              label={i18n['MEncoderVideo.15']}
                              size="xs"
                              disabled={!canModify}
                              {...form.getInputProps('subtitles_ass_margin')}
                          />
                        </Grid.Col>
                      </Grid>
                      <Tooltip label={getToolTipContent(i18n['TrTab2.78'])} {...defaultTooltipSettings}>
                        <Checkbox
                          size="xs"
                          label={i18n['MEncoderVideo.22']}
                          {...form.getInputProps('autoload_external_subtitles', { type: 'checkbox' })}
                        />
                      </Tooltip>
                      <Space h="xs" />
                      <Tooltip label={getToolTipContent(i18n['TrTab2.88'])} {...defaultTooltipSettings}>
                        <Checkbox
                          size="xs"
                          label={i18n['TrTab2.87']}
                          {...form.getInputProps('force_external_subtitles', { type: 'checkbox' })}
                        />
                      </Tooltip>
                      <Space h="xs" />
                      <Tooltip label={getToolTipContent(i18n['TrTab2.89'])} {...defaultTooltipSettings}>
                        <Checkbox
                          size="xs"
                          label={i18n['MEncoderVideo.36']}
                          {...form.getInputProps('use_embedded_subtitles_style', { type: 'checkbox' })}
                        />
                      </Tooltip>
                      <Space h="xs" />
                      <Modal size="sm"
                        title={i18n['MEncoderVideo.31']}
                        opened={opened}
                        onClose={() => setOpened(false)}
                      >
                         <Group position="center" direction="column">
                          <SketchPicker
                            color={subtitleColor}
                            onChange={(color)=> {setSubtitleColor(color.hex); form.setFieldValue('subtitles_color', color.hex)}}
                          ></SketchPicker>
                          <Text>{subtitleColor}</Text>
                        </Group>
                      </Modal>
                      <Grid>
                        <Grid.Col span={4}>
                        <Button
                          disabled={!canModify}
                          size="xs"
                          onClick={() => { setOpened(true); }}
                        >{i18n['MEncoderVideo.31']}</Button>
                        </Grid.Col>
                        <Grid.Col span={8}> <Text>{subtitleColor}</Text></Grid.Col>
                      </Grid>
                      <Tooltip label={getToolTipContent(i18n['TrTab2.DeleteLiveSubtitlesTooltip'])} {...defaultTooltipSettings}>
                        <Checkbox
                          disabled={!canModify}
                          size="xs"
                          label={i18n['TrTab2.DeleteLiveSubtitles']}
                          {...form.getInputProps('live_subtitles_keep', { type: 'checkbox' })}
                        />
                      </Tooltip>
                      <Space h="xs" />
                      <Tooltip label={getToolTipContent(i18n['TrTab2.LiveSubtitlesLimitTooltip'])} {...defaultTooltipSettings}>
                        <NumberInput
                          label={i18n['TrTab2.LiveSubtitlesLimit']}
                          size="xs"
                          disabled={!canModify}
                          {...form.getInputProps('live_subtitles_limit')}
                        />
                        </Tooltip>
                        <Select
                          disabled={!canModify}
                          label={i18n['TrTab2.90']}
                          data={threeDSubs}
                          {...form.getInputProps('3d_subtitles_depth')}
                        />
                    </Tabs.Tab>
                  </Tabs>
              </Grid.Col>
            </Grid>
          </Tabs.Tab>
        </Tabs>
        {canModify && (
          <Group position="right" mt="md">
            <Button type="submit" loading={isLoading}>
              {i18n['LooksFrame.9']}
            </Button>
          </Group>
        )}
      </form>
    </Box>
  ) : (
    <Box sx={{ maxWidth: 700 }} mx="auto">
      <Text color="red">You don't have access to this area.</Text>
    </Box>
  );
}
