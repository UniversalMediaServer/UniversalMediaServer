import { TextInput, Checkbox, MultiSelect, Button, Group, Space, Box, Select, Tabs, Accordion, Grid, Title, Navbar } from '@mantine/core';
import { useForm } from '@mantine/form';
import { showNotification } from '@mantine/notifications';
import _ from 'lodash';
import { useContext, useEffect, useRef, useState } from 'react';
import axios from 'axios';

import I18nContext from '../../contexts/i18n-context';

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

  const i18n = useContext(I18nContext);

  const defaultSettings: Record<string, any> = {
    append_profile_name: false,
    audio_channels: 6,
    audio_embed_dts_in_pcm: false,
    audio_bitrate: '448',
    audio_remux_ac3: true,
    auto_update: true,
    automatic_maximum_bitrate: true,
    chapter_interval: 5,
    chapter_support: false,
    disable_subtitles: false,
    disable_transcode_for_extensions: '',
    encoded_audio_passthrough: false,
    force_transcode_for_extensions: '',
    gpu_acceleration: false,
    external_network: true,
    hostname: '',
    ip_filter: '',
    language: 'en-US',
    lossless_dvd_todo: true, //todo
    lpcm_todo: false, //todo
    maximum_video_buffer_size: 200,
    maximum_bitrate: '90',
    minimized: false,
    network_interface: '',
    number_of_cpu_cores: '4',
    port: '',
    renderer_default: '',
    renderer_force_default: false,
    selected_renderers: ['All renderers'],
    server_engine: '0',
    server_name: 'Universal Media Server',
    show_splash_screen: true,
    transcoding_quality: 'Automatic', //todo
    transcoding_quality_mp4: 'Automatic' //todo
  };

  const cores = [...Array(16)].map((_, i) => {
    return {
      value: String(i+1),
      label: String(i+1)
    }
  });
  
  const openGitHubNewIssue = () => {
    window.location.href = 'https://github.com/UniversalMediaServer/UniversalMediaServer/issues/new';
  };

  const [configuration, setConfiguration] = useState(defaultSettings);

  const form = useForm({ initialValues: defaultSettings });

  // Code here will run just like componentDidMount
  useEffect(() => {
    axios.get('/configuration-api/settings')
      .then(function (response: any) {
        const settingsResponse = response.data;
        languageSettingsRef.current = settingsResponse.languages;
        networkInterfaceSettingsRef.current = settingsResponse.networkInterfaces;
        serverEnginesSettingsRef.current = settingsResponse.serverEngines;
        allRendererNamesSettingsRef.current = settingsResponse.allRendererNames;
        enabledRendererNamesSettingsRef.current = settingsResponse.enabledRendererNames;

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

  const [content, setContent] = useState('common');
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

  return (
    <Box sx={{ maxWidth: 700 }} mx="auto">
      <form onSubmit={form.onSubmit(handleSubmit)}>
        <Tabs active={activeTab} onTabChange={setActiveTab}>
          <Tabs.Tab label={i18n['LooksFrame.TabGeneralSettings']}>
            <Select
              label={i18n['LanguageSelection.Language']}
              data={languageSettingsRef.current}
              {...form.getInputProps('language')}
            />

            <Group mt="xs">
              <TextInput
                label={i18n['NetworkTab.71']}
                name="server_name"
                sx={{ flex: 1 }}
                {...form.getInputProps('server_name')}
              />

              <Checkbox
                mt="xl"
                label={i18n['NetworkTab.72']}
                {...form.getInputProps('append_profile_name', { type: 'checkbox' })}
              />
            </Group>

            <Group mt="md">
              <Checkbox
                label={i18n['NetworkTab.3']}
                {...form.getInputProps('minimized', { type: 'checkbox' })}
              />
              <Checkbox
                label={i18n['NetworkTab.74']}
                {...form.getInputProps('show_splash_screen', { type: 'checkbox' })}
              />
            </Group>

            <Checkbox
              mt="xs"
              label={i18n['NetworkTab.9']}
              {...form.getInputProps('auto_update', { type: 'checkbox' })}
            />

            <Accordion mt="xl">
              <Accordion.Item label={i18n['NetworkTab.22']}>
                <Select
                  label={i18n['NetworkTab.20']}
                  data={networkInterfaceSettingsRef.current}
                  {...form.getInputProps('network_interface')}
                />

                <TextInput
                  mt="xs"
                  label={i18n['NetworkTab.23']}
                  {...form.getInputProps('hostname')}
                />

                <TextInput
                  mt="xs"
                  label={i18n['NetworkTab.24']}
                  {...form.getInputProps('port')}
                />

                <TextInput
                  mt="xs"
                  label={i18n['NetworkTab.30']}
                  {...form.getInputProps('ip_filter')}
                />

                <Group mt="xs">
                  <TextInput
                    sx={{ flex: 1 }}
                    label={i18n['NetworkTab.35']}
                    disabled={form.values['automatic_maximum_bitrate']}
                    {...form.getInputProps('maximum_bitrate')}
                  />

                  <Checkbox
                    mt="xl"
                    label={i18n['GeneralTab.12']}
                    {...form.getInputProps('automatic_maximum_bitrate', { type: 'checkbox' })}
                  />
                </Group>
              </Accordion.Item>
              <Accordion.Item label={i18n['NetworkTab.31']}>
                <Select
                  label={i18n['NetworkTab.MediaServerEngine']}
                  data={serverEnginesSettingsRef.current}
                  value={String(form.getInputProps('server_engine').value)}
                />

                <MultiSelect
                  mt="xs"
                  data={allRendererNamesSettingsRef.current}
                  label={i18n['NetworkTab.62']}
                  {...form.getInputProps('selected_renderers')}
                />

                <Group mt="xs">
                  <Select
                    sx={{ flex: 1 }}
                    label={i18n['NetworkTab.36']}
                    data={enabledRendererNamesSettingsRef.current}
                    {...form.getInputProps('renderer_default')}
                    searchable
                  />

                  <Checkbox
                    mt="xl"
                    label={i18n['GeneralTab.ForceDefaultRenderer']}
                    {...form.getInputProps('renderer_force_default', { type: 'checkbox' })}
                  />
                </Group>

                <Checkbox
                  mt="xs"
                  label={i18n['NetworkTab.56']}
                  {...form.getInputProps('external_network', { type: 'checkbox' })}
                />
              </Accordion.Item>
            </Accordion>
          </Tabs.Tab>
          <Tabs.Tab label={i18n['LooksFrame.TabNavigationSettings']}>

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
              {content == 'common' ? (
                [<>
                  <TextInput
                    label={i18n['TrTab2.23']}
                    name="maximum_video_buffer_size"
                    sx={{ flex: 1 }}
                    size="xs"
                    {...form.getInputProps('maximum_video_buffer_size')}
                  />
                  <Select
                    label={i18n['TrTab2.24']}
                    name="number_of_cpu_cores"
                    data={cores}
                    size="xs"
                    {...form.getInputProps('number_of_cpu_cores')}
                  />
                  <Space h="xs"/>
                  <Grid key='1'>
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
                        <Checkbox
                          size="xs"
                          label={i18n['MEncoderVideo.39']}
                          {...form.getInputProps('lossless_dvd_todo', { type: 'checkbox' })}
                        />
                        <Space h="xs" />
                        <TextInput
                          label={i18n['TrTab2.32']}
                          sx={{ flex: 1 }}
                          disabled={true} // disable when use auto bandwidth is selected
                          {...form.getInputProps('transcoding_quality')}
                        />
                        <TextInput
                          label={i18n['TrTab2.79']}
                          sx={{ flex: 1 }}
                          disabled={true} // disable when use auto bandwidth is selected
                          {...form.getInputProps('transcoding_quality_mp4')}
                        />
                        <TextInput
                          label={i18n['TrTab2.8']}
                          sx={{ flex: 1 }}
                          {...form.getInputProps('disable_transcode_for_extensions')}
                        />
                        <TextInput
                          label={i18n['TrTab2.9']}
                          sx={{ flex: 1 }}
                          {...form.getInputProps('force_transcode_for_extensions')}
                        />
                      </Tabs.Tab>
                      <Tabs.Tab label={i18n['TrTab2.68']}>
                        <Select
                          label={i18n['TrTab2.50']}
                          data={['6', '2']} // todo
                          size="xs"
                          {...form.getInputProps('audio_channels')}
                        />
                        <Space h="xs" />
                        <Checkbox
                          size="xs"
                          label={i18n['TrTab2.27']}
                          {...form.getInputProps('lpcm_todo', { type: 'checkbox' })}
                        />
                        <Space h="xs" />
                        <Checkbox
                          size="xs"
                          label={i18n['TrTab2.26']}
                          {...form.getInputProps('audio_remux_ac3', { type: 'checkbox' })}
                        />
                        <Space h="xs" />
                        <Checkbox
                          size="xs"
                          label={i18n['TrTab2.28']}
                          {...form.getInputProps('audio_embed_dts_in_pcm', { type: 'checkbox' })}
                        />
                        <Space h="xs" />
                        <Checkbox
                          size="xs"
                          label={i18n['TrTab2.53']}
                          {...form.getInputProps('encoded_audio_passthrough', { type: 'checkbox' })}
                        />
                        <Space h="xs" />
                        <TextInput
                          label={i18n['TrTab2.29']}
                          sx={{ flex: 1 }}
                          size="xs"
                          {...form.getInputProps('audio_bitrate')}
                        />
                        <TextInput
                          label={i18n['MEncoderVideo.7']}
                          sx={{ flex: 1 }}
                          size="xs"
                          {...form.getInputProps('audio_languages')}
                        />
                      </Tabs.Tab>
                      <Tabs.Tab label={i18n['MEncoderVideo.8']}>
                        Subtitles settings
                      </Tabs.Tab>
                    </Tabs>
                </>
                ]
              ) : (<p>{content}</p>) }
            </Grid.Col>
          </Grid>
          </Tabs.Tab>
        </Tabs>
        <Group position="right" mt="md">
          <Button type="submit" loading={isLoading}>
            {i18n['LooksFrame.9']}
          </Button>
        </Group>
      </form>
    </Box>
  );
 }
