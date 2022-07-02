import { Accordion, Box, Button, Checkbox, Grid, Group, MultiSelect, Navbar, NumberInput, Select, Space, Stack, Tabs, Text, TextInput, Tooltip } from '@mantine/core';
import { useForm } from '@mantine/form';
import { showNotification } from '@mantine/notifications';
import _ from 'lodash';
import { useContext, useEffect, useState } from 'react';
import axios from 'axios';

import I18nContext from '../../contexts/i18n-context';
import {getToolTipContent} from '../../utils';
import SessionContext from '../../contexts/session-context';
import { havePermission } from '../../services/accounts-service';
import DirectoryChooser from '../DirectoryChooser/DirectoryChooser';
import { ArrowNarrowDown, ArrowNarrowUp, ArrowsVertical } from 'tabler-icons-react';

export default function Settings() {
  const [activeTab, setActiveTab] = useState(0);
  const [activeGeneralSettingsTab, setGeneralSettingsTab] = useState(0);
  const [isLoading, setLoading] = useState(true);
  const [transcodingContent, setTranscodingContent] = useState('common');

  // key/value pairs for dropdowns
  const [selectionSettings, setSelectionSettings] = useState({
    allRendererNames: [],
    audioCoverSuppliers: [],
    enabledRendererNames: [],
    languages: [],
    networkInterfaces: [],
    serverEngines: [],
    sortMethods: [],
    subtitlesInfoLevels: [],
	transcodingEngines: {} as {[key: string]: {id:string,name:string,isAvailable:boolean,purpose:number}},
	transcodingEnginesPurposes: [],
  });

  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);

  const defaultTooltipSettings = {
    width: 350,
    color: "blue",
    wrapLines: true,
    withArrow: true,
  }

  const openGitHubNewIssue = () => {
    window.location.href = 'https://github.com/UniversalMediaServer/UniversalMediaServer/issues/new';
  };

  const [defaultConfiguration, setDefaultConfiguration] = useState({} as any);
  const [configuration, setConfiguration] = useState({} as any);

  const form = useForm({ initialValues: {} as any });
  const formSetValues = form.setValues;

  const canModify = havePermission(session, "settings_modify");
  const canView = canModify || havePermission(session, "settings_view");

  // Code here will run just like componentDidMount
  useEffect(() => {
    canView && axios.get('/configuration-api/settings')
      .then(function (response: any) {
        const settingsResponse = response.data;
        setSelectionSettings(settingsResponse);
        setDefaultConfiguration(settingsResponse.userSettingsDefaults);

        // merge defaults with what we receive, which might only be non-default values
        const userConfig = _.merge({}, settingsResponse.userSettingsDefaults, settingsResponse.userSettings);

        setConfiguration(userConfig);
        formSetValues(userConfig);
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
        setLoading(false);
      });
  }, [canView, formSetValues]);

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

  const getI18nSelectData = (values: [{value:string;label:string}]) => {
    return values.map((value : {value:string;label:string}) => {
      return {value : value.value, label: i18n.getI18nString(value.label)};
    });
  }

  const getTranscodingEnginesAccordionItems = () => {
	return selectionSettings.transcodingEnginesPurposes.map((value: string, index) => {
      return (
        <Accordion.Item label={i18n.getI18nString(value)}>
          <Stack justify="flex-start" align="flex-start" spacing="xs">
            {getTranscodingEnginesButtons(index)}
          </Stack>
        </Accordion.Item>);
    });
  }

  const getTranscodingEnginesPriority = (purpose:number) => {
    return configuration['engines_priority'].filter((value: string) => 
      selectionSettings.transcodingEngines[value] && selectionSettings.transcodingEngines[value].purpose === purpose
    );
  }

  const getTranscodingEnginesButtonsArrow = (index:number, tePriority:Array<string>) => {
    const isFirst = index===0;
    const isLast = index===tePriority.length - 1;
    return isFirst ? isLast ? null : (<ArrowNarrowDown size={10} color={'grey'} />) : isLast ? (<ArrowNarrowUp size={10} color='grey' />) : (<ArrowsVertical size={10} color={'grey'} />);
  }

  const getTranscodingEnginesButtons = (purpose:number) => {
    const tePriority = getTranscodingEnginesPriority(purpose);
    return tePriority.map((value: string, index: number) => (
      <Button variant="subtle" color='gray' leftIcon={getTranscodingEnginesButtonsArrow(index, tePriority)} size="xs" compact onClick={() => setTranscodingContent(selectionSettings.transcodingEngines[value].id)}>
        {selectionSettings.transcodingEngines[value].name}
      </Button>
    ));
  }

  const getTranscodingCommon = () => { return (<>
      <TextInput
      label={i18n.get['TrTab2.23']}
      name="maximum_video_buffer_size"
      sx={{ flex: 1 }}
      size="xs"
      {...form.getInputProps('maximum_video_buffer_size')}
    />
    <NumberInput
      label={i18n.get['TrTab2.24']?.replace('%d', defaultConfiguration.number_of_cpu_cores)}
      size="xs"
      max={defaultConfiguration.number_of_cpu_cores}
      min={1}
      disabled={false}
      {...form.getInputProps('number_of_cpu_cores')}
    />
    <Space h="xs"/>
    <Grid>
      <Grid.Col span={10}>
        <Checkbox
          size="xs"
          label={i18n.get['TrTab2.52']}
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
      label={i18n.get['TrTab2.51']}
      {...form.getInputProps('disable_subtitles', { type: 'checkbox' })}
    />
    <Space h="md"/>
    <Tabs active={activeGeneralSettingsTab} onTabChange={setGeneralSettingsTab}>
      <Tabs.Tab label={i18n.get['TrTab2.67']}>
        <Checkbox
          size="xs"
          label={i18n.get['TrTab2.70']}
          {...form.getInputProps('gpu_acceleration', { type: 'checkbox' })}
        />
        <Space h="xs" />
        <Tooltip label={getToolTipContent(i18n.get['TrTab2.82'])} {...defaultTooltipSettings}>
          <Checkbox
            size="xs"
            label={i18n.get['MEncoderVideo.39']}
            {...form.getInputProps('mencoder_remux_mpeg2', { type: 'checkbox' })}
          />
        </Tooltip>
        <Space h="xs" />
        <Tooltip label={getToolTipContent(i18n.get['TrTab2.74'])} {...defaultTooltipSettings}>
          <TextInput
            label={i18n.get['TrTab2.32']}
            sx={{ flex: 1 }}
            disabled={form.values['automatic_maximum_bitrate']}
            {...form.getInputProps('mpeg2_main_settings')}
          />
        </Tooltip>
        <Space h="xs" />
        <Tooltip label={getToolTipContent(i18n.get['TrTab2.81'])} {...defaultTooltipSettings}>
          <TextInput
            label={i18n.get['TrTab2.79']}
            sx={{ flex: 1 }}
            disabled={form.values['automatic_maximum_bitrate']}
            {...form.getInputProps('x264_constant_rate_factor')}
          />
        </Tooltip>
        <TextInput
          label={i18n.get['TrTab2.8']}
          sx={{ flex: 1 }}
          {...form.getInputProps('disable_transcode_for_extensions')}
        />
        <TextInput
          label={i18n.get['TrTab2.9']}
          sx={{ flex: 1 }}
          {...form.getInputProps('force_transcode_for_extensions')}
        />
      </Tabs.Tab>
      <Tabs.Tab label={i18n.get['TrTab2.68']}>
        <Select
          label={i18n.get['TrTab2.50']}
          data={[{value: '6', label: '6 channels (5.1)'}, {value: '2', label: '2 channels (Stereo)'}]}
          size="xs"
          {...form.getInputProps('audio_channels')}
        />
        <Space h="xs" />
        <Checkbox
          size="xs"
          label={i18n.get['TrTab2.27']}
          {...form.getInputProps('audio_use_pcm', { type: 'checkbox' })}
        />
        <Space h="xs" />
        <Checkbox
          size="xs"
          label={i18n.get['TrTab2.26']}
          {...form.getInputProps('audio_remux_ac3', { type: 'checkbox' })}
        />
        <Space h="xs" />
        <Checkbox
          size="xs"
          label={i18n.get['TrTab2.28']}
          {...form.getInputProps('audio_embed_dts_in_pcm', { type: 'checkbox' })}
        />
        <Space h="xs" />
        <Checkbox
          size="xs"
          label={i18n.get['TrTab2.53']}
          {...form.getInputProps('encoded_audio_passthrough', { type: 'checkbox' })}
        />
        <Space h="xs" />
        <TextInput
          label={i18n.get['TrTab2.29']}
          sx={{ flex: 1 }}
          size="xs"
          {...form.getInputProps('audio_bitrate')}
        />
        <TextInput
          label={i18n.get['MEncoderVideo.7']}
          sx={{ flex: 1 }}
          size="xs"
          {...form.getInputProps('audio_languages')}
        />
        </Tabs.Tab>
        <Tabs.Tab label={i18n.get['MEncoderVideo.8']}>
          Subtitles settings
        </Tabs.Tab>
      </Tabs>
    </>);
  }

  const getTranscodingContent = () => {
    switch(transcodingContent) {
      case 'common':
        return getTranscodingCommon();
      default:
      return null;
    }
  }

  return canView ? (
    <Box sx={{ maxWidth: 700 }} mx="auto">
      <form onSubmit={form.onSubmit(handleSubmit)}>
        <Tabs active={activeTab} onTabChange={setActiveTab}>
          <Tabs.Tab label={i18n.get['LooksFrame.TabGeneralSettings']}>
            <Select
              disabled={!canModify}
              label={i18n.get['LanguageSelection.Language']}
              data={selectionSettings.languages}
              {...form.getInputProps('language')}
            />

            <Group mt="xs">
              <TextInput
                disabled={!canModify}
                label={i18n.get['NetworkTab.71']}
                name="server_name"
                sx={{ flex: 1 }}
                {...form.getInputProps('server_name')}
              />
              <Tooltip label={getToolTipContent(i18n.get['NetworkTab.73'])} width={350} color="blue" wrapLines withArrow>
                <Checkbox
                  disabled={!canModify}
                  mt="xl"
                  label={i18n.get['NetworkTab.72']}
                  {...form.getInputProps('append_profile_name', { type: 'checkbox' })}
                />
                </Tooltip>
            </Group>
            <Group mt="md">
              <Checkbox
                disabled={!canModify}
                label={i18n.get['NetworkTab.3']}
                {...form.getInputProps('minimized', { type: 'checkbox' })}
              />
              <Checkbox
                disabled={!canModify}
                label={i18n.get['NetworkTab.74']}
                {...form.getInputProps('show_splash_screen', { type: 'checkbox' })}
              />
            </Group>

            <Checkbox
              disabled={!canModify}
              mt="xs"
              label={i18n.get['NetworkTab.9']}
              {...form.getInputProps('auto_update', { type: 'checkbox' })}
            />

            <Accordion mt="xl">
              <Accordion.Item label={i18n.get['NetworkTab.22']}>
                <Select
                  disabled={!canModify}
                  label={i18n.get['NetworkTab.20']}
                  data={selectionSettings.networkInterfaces}
                  {...form.getInputProps('network_interface')}
                />

                <TextInput
                  disabled={!canModify}
                  mt="xs"
                  label={i18n.get['NetworkTab.23']}
                  {...form.getInputProps('hostname')}
                />

                <TextInput
                  disabled={!canModify}
                  mt="xs"
                  label={i18n.get['NetworkTab.24']}
                  {...form.getInputProps('port')}
                />

                <TextInput
                  disabled={!canModify}
                  mt="xs"
                  label={i18n.get['NetworkTab.30']}
                  {...form.getInputProps('ip_filter')}
                />

                <Group mt="xs">
                  <TextInput
                    sx={{ flex: 1 }}
                    label={i18n.get['NetworkTab.35']}
                    disabled={!canModify || form.values['automatic_maximum_bitrate']}
                    {...form.getInputProps('maximum_bitrate')}
                  />
                  
                  <Tooltip label={getToolTipContent(i18n.get['GeneralTab.12.Tooltip'])} {...defaultTooltipSettings}>
                    <Checkbox
                      disabled={!canModify}
                      mt="xl"
                      label={i18n.get['GeneralTab.12']}
                      {...form.getInputProps('automatic_maximum_bitrate', { type: 'checkbox' })}
                    />
                  </Tooltip>
                </Group>
              </Accordion.Item>
              <Accordion.Item label={i18n.get['NetworkTab.31']}>
              
                <Tooltip label={getToolTipContent(i18n.get['NetworkTab.MediaServerEngineTooltip'])} {...defaultTooltipSettings}>
                  <Select
                    disabled={!canModify}
                    label={i18n.get['NetworkTab.MediaServerEngine']}
                    data={getI18nSelectData(selectionSettings.serverEngines as unknown as [{value:string;label:string}])}
                    {...form.getInputProps('server_engine')}
                  />
                </Tooltip>

                <MultiSelect
                  disabled={!canModify}
                  mt="xs"
                  data={selectionSettings.allRendererNames}
                  label={i18n.get['NetworkTab.62']}
                  {...form.getInputProps('selected_renderers')}
                />

                <Group mt="xs">
                  <Select
                    disabled={!canModify}
                    sx={{ flex: 1 }}
                    label={i18n.get['NetworkTab.36']}
                    data={selectionSettings.enabledRendererNames}
                    {...form.getInputProps('renderer_default')}
                    searchable
                  />

                  <Tooltip label={getToolTipContent(i18n.get['GeneralTab.ForceDefaultRendererTooltip'])} {...defaultTooltipSettings}>
                    <Checkbox
                      disabled={!canModify}
                      mt="xl"
                      label={i18n.get['GeneralTab.ForceDefaultRenderer']}
                      {...form.getInputProps('renderer_force_default', { type: 'checkbox' })}
                    />
                  </Tooltip>
                </Group>

                <Tooltip label={getToolTipContent(i18n.get['NetworkTab.67'])} {...defaultTooltipSettings}>
                  <Checkbox
                    disabled={!canModify}
                    mt="xs"
                    label={i18n.get['NetworkTab.56']}
                    {...form.getInputProps('external_network', { type: 'checkbox' })}
                  />
                </Tooltip>

              </Accordion.Item>
            </Accordion>
          </Tabs.Tab>
          <Tabs.Tab label={i18n.get['LooksFrame.TabNavigationSettings']}>
            <Group mt="xs">
              <Checkbox
                mt="xl"
                label={i18n.get['NetworkTab.2']}
                {...form.getInputProps('generate_thumbnails', { type: 'checkbox' })}
              />
              <TextInput
                sx={{ flex: 1 }}
                label={i18n.get['NetworkTab.16']}
                disabled={!form.values['generate_thumbnails']}
                {...form.getInputProps('thumbnail_seek_position')}
              />
            </Group>
            <Select
              mt="xs"
              label={i18n.get['FoldTab.26']}
              data={getI18nSelectData(selectionSettings.audioCoverSuppliers as unknown as [{value:string;label:string}])}
              {...form.getInputProps('audio_thumbnails_method')}
            />
            <DirectoryChooser
              path={form.getInputProps('alternate_thumb_folder').value}
              callback={form.setFieldValue}
              label={i18n.get['FoldTab.27']}
              formKey="alternate_thumb_folder"
            ></DirectoryChooser>
            <Accordion mt="xl">
              <Accordion.Item label={i18n.get['NetworkTab.59']}>
                <Group mt="xs">
                  <Select
                    label={i18n.get['FoldTab.26']}
                    data={getI18nSelectData(selectionSettings.sortMethods as unknown as [{value:string;label:string}])}
                    {...form.getInputProps('sort_method')}
                  />
                  <Checkbox
                    mt="xl"
                    label={i18n.get['FoldTab.39']}
                    {...form.getInputProps('ignore_the_word_a_and_the', { type: 'checkbox' })}
                  />
                </Group>
                <Tooltip label={getToolTipContent(i18n.get['FoldTab.45'])} {...defaultTooltipSettings}>
                  <Checkbox
                    mt="md"
                    label={i18n.get['FoldTab.43']}
                    {...form.getInputProps('prettify_filenames', { type: 'checkbox' })}
                  />
                </Tooltip>
                <Checkbox
                  mt="md"
                  label={i18n.get['FoldTab.5']}
                  disabled={form.values['prettify_filenames']}
                  {...form.getInputProps('hide_extensions', { type: 'checkbox' })}
                />
                <Tooltip label={getToolTipContent(i18n.get['FoldTab.UseInfoFromAPITooltip'])} {...defaultTooltipSettings}>
                  <Checkbox
                    mt="md"
                    label={i18n.get['FoldTab.UseInfoFromAPI']}
                    {...form.getInputProps('use_imdb_info', { type: 'checkbox' })}
                  />
                </Tooltip>
                <Group mt="xs">
                  <Tooltip label={getToolTipContent(i18n.get['FoldTab.addSubtitlesInfoToolTip'])} {...defaultTooltipSettings}>
                    <Select
                      label={i18n.get['FoldTab.addSubtitlesInfo']}
                      data={getI18nSelectData(selectionSettings.subtitlesInfoLevels as unknown as [{value:string;label:string}])}
                      {...form.getInputProps('subs_info_level')}
                    />
                  </Tooltip>
                  <Tooltip label={getToolTipContent(i18n.get['FoldTab.showEngineNamesAfterFilenamesToolTip'])} {...defaultTooltipSettings}>
                    <Checkbox
                      mt="xl"
                      label={i18n.get['FoldTab.showEngineNamesAfterFilenames']}
                      checked={!form.values['hide_enginenames']}
                      onChange={(event) => {
                        form.setFieldValue('hide_enginenames', !event.currentTarget.checked);
                      }}
                    />
                  </Tooltip>
                </Group>
              </Accordion.Item>
              <Accordion.Item label={i18n.get['NetworkTab.60']}>
                <Tooltip label={getToolTipContent(i18n.get['NavigationSettingsTab.EnableCacheTooltip'])} {...defaultTooltipSettings}>
                  <Checkbox
                    mt="xl"
                    label={i18n.get['NavigationSettingsTab.EnableCache']}
                    {...form.getInputProps('use_cache', { type: 'checkbox' })}
                  />
                </Tooltip>
              </Accordion.Item>
            </Accordion>
          </Tabs.Tab>
          <Tabs.Tab label={i18n.get['LooksFrame.TabSharedContent']}>
            
          </Tabs.Tab>
          <Tabs.Tab label={i18n.get['LooksFrame.21']}>
            <Grid>
              <Grid.Col span={5}>
                <Navbar width={{ }} p="xs">
                  <Navbar.Section>
                    <Button variant="subtle" color="gray" size="xs" compact onClick={() => setTranscodingContent('common')}>
                      {i18n.get['TrTab2.5']}
                    </Button>
                  </Navbar.Section>
                  <Navbar.Section>
                  <Accordion>
                    {getTranscodingEnginesAccordionItems()}
                  </Accordion>
                  </Navbar.Section>
                </Navbar>
              </Grid.Col>
              <Grid.Col span={7}>
                {getTranscodingContent()}
              </Grid.Col>
            </Grid>
          </Tabs.Tab>
        </Tabs>
        {canModify && (
          <Group position="right" mt="md">
            <Button type="submit" loading={isLoading}>
              {i18n.get['LooksFrame.9']}
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
