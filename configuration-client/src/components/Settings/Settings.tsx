import { Accordion, Modal, Center, ActionIcon, Box, Button, Checkbox, Grid, Group, MultiSelect, Navbar, NumberInput, Select, Space, Stack, Tabs, Text, TextInput, Title, Tooltip } from '@mantine/core';
import { useForm } from '@mantine/form';
import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import _ from 'lodash';
import { useContext, useEffect, useState } from 'react';
import { SketchPicker } from 'react-color';
import { arrayMove, List } from 'react-movable';
import { ArrowNarrowDown, ArrowNarrowUp, ArrowsVertical } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import ServerEventContext from '../../contexts/server-event-context';
import SessionContext from '../../contexts/session-context';
import { havePermission } from '../../services/accounts-service';
import {allowHtml, openGitHubNewIssue} from '../../utils';
import DirectoryChooser from '../DirectoryChooser/DirectoryChooser';

export default function Settings() {
  const [activeTab, setActiveTab] = useState(0);
  const [activeGeneralSettingsTab, setGeneralSettingsTab] = useState(0);
  const [isLoading, setLoading] = useState(true);
  const [modalOpened, setModalOpened] = useState(false);
  const [transcodingContent, setTranscodingContent] = useState('common');
  const [defaultConfiguration, setDefaultConfiguration] = useState({} as any);
  const [configuration, setConfiguration] = useState({} as any);

  // key/value pairs for dropdowns
  const [selectionSettings, setSelectionSettings] = useState({
    allRendererNames: [],
    audioCoverSuppliers: [],
    enabledRendererNames: [],
    ffmpegLoglevels: [],
    gpuAccelerationMethod: [],
    networkInterfaces: [],
    serverEngines: [],
    sortMethods: [],
    subtitlesDepth: [],
    subtitlesCodepages: [],
    subtitlesInfoLevels: [],
    transcodingEngines: {} as {[key: string]: {id:string,name:string,isAvailable:boolean,purpose:number}},
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
    wrapLines: true,
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
    for (let key in values) {
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
      .catch(function (error: Error) {
        console.log(error);
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

  const getI18nSelectData = (values: [{value:string;label:string}]) => {
    return values.map((value : {value:string;label:string}) => {
      return {value : value.value, label: i18n.getI18nString(value.label)};
    });
  }

  const getTranscodingEnginesPriority = (purpose:number) => {
    return form.getInputProps('engines_priority').value !== undefined ? form.getInputProps('engines_priority').value.filter((value: string) => 
      selectionSettings.transcodingEngines[value] && selectionSettings.transcodingEngines[value].purpose === purpose
    ) : [];
  }

  const moveTranscodingEnginesPriority = (purpose:number, oldIndex:number, newIndex:number) => {
    if (form.getInputProps('engines_priority').value instanceof Array<string>) {
      var items = form.getInputProps('engines_priority').value as Array<string>;
      let index = items.indexOf(getTranscodingEnginesPriority(purpose)[oldIndex]);
      let moveTo = index - oldIndex + newIndex;
      form.setFieldValue('engines_priority', arrayMove(items, index, moveTo));
    }
  }

  const getTranscodingEnginesList = (purpose:number) => {
    const engines = getTranscodingEnginesPriority(purpose);
    return engines.length > 1 ? (
      <List
        lockVertically
        values={getTranscodingEnginesPriority(purpose)}
        onChange={({ oldIndex, newIndex }) => {
          moveTranscodingEnginesPriority(purpose, oldIndex, newIndex);
        }}
        renderList={({ children, props, isDragged }) => (
          <Stack justify="flex-start" align="flex-start" spacing="xs" {...props}>
            {children}
          </Stack>
        )}
        renderItem={({ value, props, isDragged, isSelected }) => (
          <Button {...props} color='gray' size="xs" compact
            variant={isDragged || isSelected ? 'outline' : 'subtle'}
            leftIcon={
              <ActionIcon data-movable-handle size={10} style={{ cursor: isDragged ? 'grabbing' : 'grab', }}>
                { engines.indexOf(value) === 0 ? (<ArrowNarrowDown />) : engines.indexOf(value) === engines.length - 1 ? (<ArrowNarrowUp />) : (<ArrowsVertical />)}
              </ActionIcon>
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
        <Accordion.Item label={i18n.getI18nString(value)}>
          {getTranscodingEnginesList(index)}
        </Accordion.Item>);
    });
  }

  const getTranscodingCommon = () => { return (<>
    <TextInput
      label={i18n.get['MaximumTranscodeBufferSize']}
      name="maximum_video_buffer_size"
      sx={{ flex: 1 }}
      size="xs"
      {...form.getInputProps('maximum_video_buffer_size')}
    />
    <NumberInput
      label={i18n.get['CpuThreadsToUse']?.replace('%d', defaultConfiguration.number_of_cpu_cores)}
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
          label={i18n.get['ChaptersSupportInTranscodeFolder']}
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
      label={i18n.get['DisableSubtitles']}
      {...form.getInputProps('disable_subtitles', { type: 'checkbox' })}
    />
    <Space h="md"/>
    <Tabs active={activeGeneralSettingsTab} onTabChange={setGeneralSettingsTab}>
      <Tabs.Tab label={i18n.get['VideoSettings']}>
        <Checkbox
          size="xs"
          label={i18n.get['EnableGpuAcceleration']}
          {...form.getInputProps('gpu_acceleration', { type: 'checkbox' })}
        />
        <Space h="xs" />
        <Tooltip label={allowHtml(i18n.get['WhenEnabledMuxesDvd'])} {...defaultTooltipSettings}>
          <Checkbox
            size="xs"
            label={i18n.get['LosslessDvdVideoPlayback']}
            {...form.getInputProps('mencoder_remux_mpeg2', { type: 'checkbox' })}
          />
        </Tooltip>
        <Space h="xs" />
        <Tooltip label={allowHtml(i18n.get['AutomaticWiredOrWireless'])} {...defaultTooltipSettings}>
          <TextInput
            label={i18n.get['TranscodingQualityMpeg2']}
            sx={{ flex: 1 }}
            disabled={form.values['automatic_maximum_bitrate']}
            {...form.getInputProps('mpeg2_main_settings')}
          />
        </Tooltip>
        <Space h="xs" />
        <Tooltip label={allowHtml(i18n.get['AutomaticSettingServeBestQuality'])} {...defaultTooltipSettings}>
          <TextInput
            label={i18n.get['TranscodingQualityH264']}
            sx={{ flex: 1 }}
            disabled={form.values['automatic_maximum_bitrate']}
            {...form.getInputProps('x264_constant_rate_factor')}
          />
        </Tooltip>
        <TextInput
          label={i18n.get['SkipTranscodingFollowingExtensions']}
          sx={{ flex: 1 }}
          {...form.getInputProps('disable_transcode_for_extensions')}
        />
        <TextInput
          label={i18n.get['ForceTranscodingFollowingExtensions']}
          sx={{ flex: 1 }}
          {...form.getInputProps('force_transcode_for_extensions')}
        />
      </Tabs.Tab>
      <Tabs.Tab label={i18n.get['AudioSettings']}>
        <Select
          label={i18n.get['MaximumNumberAudioChannelsOutput']}
          data={[{value: '6', label: i18n.get['6Channels51']}, {value: '2', label: i18n.get['2ChannelsStereo']}]}
          size="xs"
          {...form.getInputProps('audio_channels')}
        />
        <Space h="xs" />
        <Checkbox
          size="xs"
          label={i18n.get['UseLpcmForAudio']}
          {...form.getInputProps('audio_use_pcm', { type: 'checkbox' })}
        />
        <Space h="xs" />
        <Checkbox
          size="xs"
          label={i18n.get['KeepAc3Tracks']}
          {...form.getInputProps('audio_remux_ac3', { type: 'checkbox' })}
        />
        <Space h="xs" />
        <Checkbox
          size="xs"
          label={i18n.get['KeepDtsTracks']}
          {...form.getInputProps('audio_embed_dts_in_pcm', { type: 'checkbox' })}
        />
        <Space h="xs" />
        <Checkbox
          size="xs"
          label={i18n.get['EncodedAudioPassthrough']}
          {...form.getInputProps('encoded_audio_passthrough', { type: 'checkbox' })}
        />
        <Space h="xs" />
        <TextInput
          label={i18n.get['Ac3ReencodingAudioBitrate']}
          sx={{ flex: 1 }}
          size="xs"
          {...form.getInputProps('audio_bitrate')}
        />
        <TextInput
          label={i18n.get['AudioLanguagePriority']}
          sx={{ flex: 1 }}
          size="xs"
          {...form.getInputProps('audio_languages')}
        />
        </Tabs.Tab>
        <Tabs.Tab label={i18n.get['SubtitlesSettings']}>
          <Tooltip label={allowHtml(i18n.get['YouCanRearrangeOrderSubtitles'])} {...defaultTooltipSettings}>
            <TextInput
              label={i18n.get['SubtitlesLanguagePriority']}
              sx={{ flex: 1 }}
              size="xs"
              {...form.getInputProps('subtitles_languages')}
            />
          </Tooltip>
          <TextInput
            label={i18n.get['ForcedLanguage']}
            sx={{ flex: 1 }}
            size="xs"
            {...form.getInputProps('forced_subtitle_language')}
          />
          <TextInput
            label={i18n.get['ForcedTags']}
            sx={{ flex: 1 }}
            size="xs"
            {...form.getInputProps('forced_subtitle_tags')}
          />
          <Tooltip label={allowHtml(i18n.get['AnExplanationDefaultValueAudio'])} {...defaultTooltipSettings}>
            <TextInput
              label={i18n.get['AudioSubtitlesLanguagePriority']}
              sx={{ flex: 1 }}
              size="xs"
              {...form.getInputProps('audio_subtitles_languages')}
            />
          </Tooltip>
          <DirectoryChooser
            path={form.getInputProps('alternate_subtitles_folder').value}
            callback={form.setFieldValue}
            label={i18n.get['AlternateSubtitlesFolder']}
            formKey="alternate_subtitles_folder"
          ></DirectoryChooser>
          <Select
            label={i18n.get['NonUnicodeSubtitleEncoding']}
            data={selectionSettings.subtitlesCodepages}
            {...form.getInputProps('subtitles_codepage')}
          />
          <Space h="xs" />
          <Checkbox
            size="xs"
            label={i18n.get['FribidiMode']}
            {...form.getInputProps('mencoder_subfribidi', { type: 'checkbox' })}
          />
          <DirectoryChooser
            tooltipText={i18n.get['ToUseFontMustBeRegistered']}
            path={form.getInputProps('subtitles_font').value}
            callback={form.setFieldValue}
            label={i18n.get['SpecifyTruetypeFont']}
            formKey="subtitles_font"
          ></DirectoryChooser>
          <Text size="xs">{i18n.get['StyledSubtitles']}</Text>
          <Space h="xs" />
          <Grid>
            <Grid.Col span={3}>
              <TextInput
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
              size="xs"
              label={i18n.get['AutomaticallyLoadSrtSubtitles']}
              {...form.getInputProps('autoload_external_subtitles', { type: 'checkbox' })}
            />
          </Tooltip>
          <Space h="xs" />
          <Tooltip label={allowHtml(i18n.get['IfEnabledExternalSubtitlesAlways'])} {...defaultTooltipSettings}>
            <Checkbox
              size="xs"
              label={i18n.get['ForceExternalSubtitles']}
              {...form.getInputProps('force_external_subtitles', { type: 'checkbox' })}
            />
          </Tooltip>
          <Space h="xs" />
          <Tooltip label={allowHtml(i18n.get['IfEnabledWontModifySubtitlesStyling'])} {...defaultTooltipSettings}>
            <Checkbox
              size="xs"
              label={i18n.get['UseEmbeddedStyle']}
              {...form.getInputProps('use_embedded_subtitles_style', { type: 'checkbox' })}
            />
          </Tooltip>
          <Space h="xs" />
          <Modal size="sm"
            title={i18n.get['Color']}
            opened={modalOpened}
            onClose={() => setModalOpened(false)}
          >
            <Group position="center" direction="column">
              <SketchPicker
                color={form.getInputProps('subtitles_color').value}
                onChange={(color)=> {form.setFieldValue('subtitles_color', color.hex)}}
              ></SketchPicker>
              <Button
                size="xs"
                onClick={() => { setModalOpened(false); }}
              >{i18n.get['Confirm']}</Button>
          </Group>
        </Modal>
        <Grid>
          <Grid.Col span={4}>
            <Center inline>
              <Button
                disabled={!canModify}
                size="xs"
                onClick={() => { setModalOpened(true); }}
              >{i18n.get['Color']}</Button>
            </Center>
          </Grid.Col>
          <Grid.Col span={8}>
            <Center inline>
              <TextInput
                label={i18n.get['Color']}
                sx={{ flex: 1 }}
                size="xs"
                {...form.getInputProps('subtitles_color')}
              />
            </Center>
          </Grid.Col>
        </Grid>
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
        <Space h="xs" />
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
          label={i18n.get['3dSubtitlesDepth']}
          data={selectionSettings.subtitlesDepth}
          {...form.getInputProps('3d_subtitles_depth')}
          value={String(form.values['3d_subtitles_depth'])}
          onChange={(val) => {
            form.setFieldValue('3d_subtitles_depth', val);
          }}
        />
        </Tabs.Tab>
      </Tabs>
    </>);
  }

  const getVLCWebVideo = () => {
    return (
      <>
        <Checkbox
          disabled={!canModify}
          mt="xl"
          label={i18n.get['EnableExperimentalCodecs']}
          {...form.getInputProps('vlc_use_experimental_codecs', { type: 'checkbox' })}
        />
        <Checkbox
          disabled={!canModify}
          mt="xl"
          label={i18n.get['AvSyncAlternativeMethod']}
          {...form.getInputProps('vlc_audio_sync_enabled', { type: 'checkbox' })}
        />
    </>
    );
  }

  const getFFMPEGAudio = () => {
    return (
      <>
        <Checkbox
          disabled={!canModify}
          mt="xl"
          label={i18n.get['AutomaticAudioResampling']}
          {...form.getInputProps('audio_resample', { type: 'checkbox' })}
        />
      </>
    )
  }

  const getTsMuxerVideo = () => {
    return (
      <>
        <Checkbox
          disabled={!canModify}
          mt="xl"
          label={i18n.get['ForceFpsParsedFfmpeg']}
          {...form.getInputProps('tsmuxer_forcefps', { type: 'checkbox' })}
        />
        <Checkbox
          disabled={!canModify}
          mt="xl"
          label={i18n.get['MuxAllAudioTracks']}
          {...form.getInputProps('tsmuxer_mux_all_audiotracks', { type: 'checkbox' })}
        />
      </>
    )
  }

  const getMEncoderVideo = () => {
    return (
      <>
        <Title order={5}>{i18n.get['GeneralSettings']}</Title>
        <Checkbox
          disabled={!canModify}
          mt="xl"
          label={i18n.get['EnableMultithreading']}
          {...form.getInputProps('mencoder_mt', { type: 'checkbox' })}
        />
        <Checkbox
          disabled={!canModify}
          mt="xl"
          label={i18n.get['SkipLoopFilterDeblocking']}
          {...form.getInputProps('mencoder_skip_loop_filter', { type: 'checkbox' })}
        />
        <Checkbox
          disabled={!canModify}
          mt="xl"
          label={i18n.get['AvSyncAlternativeMethod']}
          {...form.getInputProps('mencoder_nooutofsync', { type: 'checkbox' })}
        />
        <Checkbox
          disabled={!canModify}
          mt="xl"
          label={i18n.get['ChangeVideoResolution']}
          {...form.getInputProps('mencoder_scaler', { type: 'checkbox' })}
        />
        <Checkbox
          disabled={!canModify}
          mt="xl"
          label={i18n.get['ForceFramerateParsedFfmpeg']}
          {...form.getInputProps('mencoder_forcefps', { type: 'checkbox' })}
        />
        <Checkbox
          disabled={!canModify}
          mt="xl"
          label={i18n.get['DeinterlaceFilter']}
          {...form.getInputProps('mencoder_yadif', { type: 'checkbox' })}
        />
        <Checkbox
          disabled={!canModify}
          mt="xl"
          label={i18n.get['RemuxVideosTsmuxer']}
          {...form.getInputProps('mencoder_mux_compatible ', { type: 'checkbox' })}
        />
        <Title order={5}>{i18n.get['SubtitlesSettings']}</Title>
        <Checkbox
          disabled={!canModify}
          mt="xl"
          label={i18n.get['UseAssSubtitlesStyling']}
          {...form.getInputProps('mencoder_ass', { type: 'checkbox' })}
        />
        <Checkbox
          disabled={!canModify}
          mt="xl"
          label={i18n.get['FonconfigEmbeddedFonts']}
          {...form.getInputProps('mencoder_fontconfig', { type: 'checkbox' })}
        />
      </>
    )
  }

  const getFFMPEGVideo = () => {
    return (
      <>
        <Select
          disabled={!canModify}
          label={i18n.get['LogLevelColon']}
          data={selectionSettings.ffmpegLoglevels}
          {...form.getInputProps('ffmpeg_logging_level')}
        />
        <Checkbox
          disabled={!canModify}
          mt="xl"
          label={i18n.get['EnableMultithreading']}
          {...form.getInputProps('ffmpeg_multithreading', { type: 'checkbox' })}
        />
        <Checkbox
          disabled={!canModify}
          mt="xl"
          label={i18n.get['RemuxVideosTsmuxer']}
          {...form.getInputProps('ffmpeg_mux_tsmuxer_compatible', { type: 'checkbox' })}
        />
        <Checkbox
          disabled={!canModify}
          mt="xl"
          label={i18n.get['UseFontSettings']}
          {...form.getInputProps('ffmpeg_fontconfig', { type: 'checkbox' })}
        />
        <Checkbox
          disabled={!canModify}
          mt="xl"
          label={i18n.get['DeferMencoderTranscodingProblematic']}
          {...form.getInputProps('ffmpeg_mencoder_problematic_subtitles', { type: 'checkbox' })}
        />
        <Checkbox
          disabled={!canModify}
          mt="xl"
          label={i18n.get['UseSoxHigherQualityAudio']}
          {...form.getInputProps('fmpeg_sox', { type: 'checkbox' })}
        />
        <NumberInput
          label={i18n.get['GpuDecodingThreadCount']}
          size="xs"
          max={16}
          min={0}
          {...form.getInputProps('ffmpeg_gpu_decoding_acceleration_thread_number')}
        />
      </>
    )
  }

  const noSettingsForNow = () => {
    return (
      <Text>{i18n.get['NoSettingsForNow']}</Text>
    )
  }

  const getTranscodingContent = () => {
    switch(transcodingContent) {
      case 'common':
        return getTranscodingCommon();
      case 'DCRaw':
        return (noSettingsForNow());
      case 'FFmpegAudio':
        return getFFMPEGAudio();
      case 'FFmpegVideo':
        return getFFMPEGVideo();
      case 'FFmpegWebVideo':
        return (noSettingsForNow());
      case 'MEncoderVideo':
        return getMEncoderVideo();
      case 'MEncoderWebVideo':
        return (noSettingsForNow());
      case 'tsMuxeRAudio':
        return (noSettingsForNow());
      case 'tsMuxeRVideo':
        return getTsMuxerVideo();
      case 'VLCAudioStreaming':
        return (noSettingsForNow());
      case 'VLCVideo':
        return getVLCWebVideo();
      case 'VLCWebVideo':
        return getVLCWebVideo();
      case 'VLCVideoStreaming':
        return (noSettingsForNow());
      case 'youtubeDl':
        return (noSettingsForNow());
      default:
      return null;
    }
  }

  return canView ? (
    <Box sx={{ maxWidth: 700 }} mx="auto">
      <form onSubmit={form.onSubmit(handleSubmit)}>
        <Tabs active={activeTab} onTabChange={setActiveTab}>
          <Tabs.Tab label={i18n.get['GeneralSettings']}>
            <Select
              disabled={!canModify}
              label={i18n.get['Language']}
              data={getLanguagesSelectData()}
              {...form.getInputProps('language')}
            />

            <Group mt="xs">
              <TextInput
                disabled={!canModify}
                label={i18n.get['ServerName']}
                name="server_name"
                sx={{ flex: 1 }}
                {...form.getInputProps('server_name')}
              />
              <Tooltip label={allowHtml(i18n.get['WhenEnabledUmsProfileName'])} width={350} color="blue" wrapLines withArrow>
                <Checkbox
                  disabled={!canModify}
                  mt="xl"
                  label={i18n.get['AppendProfileName']}
                  {...form.getInputProps('append_profile_name', { type: 'checkbox' })}
                />
                </Tooltip>
            </Group>
            <Group mt="md">
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
            </Group>

            <Checkbox
              disabled={!canModify}
              mt="xs"
              label={i18n.get['CheckAutomaticallyForUpdates']}
              {...form.getInputProps('auto_update', { type: 'checkbox' })}
            />

            <Accordion mt="xl">
              <Accordion.Item label={i18n.get['NetworkSettingsAdvanced']}>
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
              </Accordion.Item>
              <Accordion.Item label={i18n.get['AdvancedHttpSystemSettings']}>
              
                <Tooltip label={allowHtml(i18n.get['DefaultOptionIsHighlyRecommended'])} {...defaultTooltipSettings}>
                  <Select
                    disabled={!canModify}
                    label={i18n.get['MediaServerEngine']}
                    data={getI18nSelectData(selectionSettings.serverEngines as unknown as [{value:string;label:string}])}
                    {...form.getInputProps('server_engine')}
                  />
                </Tooltip>

                <MultiSelect
                  disabled={!canModify}
                  mt="xs"
                  data={selectionSettings.allRendererNames}
                  label={i18n.get['EnabledRenderers']}
                  {...form.getInputProps('selected_renderers')}
                />

                <Group mt="xs">
                  <Select
                    disabled={!canModify}
                    sx={{ flex: 1 }}
                    label={i18n.get['DefaultRendererWhenAutoFails']}
                    data={selectionSettings.enabledRendererNames}
                    {...form.getInputProps('renderer_default')}
                    searchable
                  />

                  <Tooltip label={allowHtml(i18n.get['DisablesAutomaticDetection'])} {...defaultTooltipSettings}>
                    <Checkbox
                      disabled={!canModify}
                      mt="xl"
                      label={i18n.get['ForceDefaultRenderer']}
                      {...form.getInputProps('renderer_force_default', { type: 'checkbox' })}
                    />
                  </Tooltip>
                </Group>

                <Tooltip label={allowHtml(i18n.get['ThisControlsWhetherUmsTry'])} {...defaultTooltipSettings}>
                  <Checkbox
                    disabled={!canModify}
                    mt="xs"
                    label={i18n.get['EnableExternalNetwork']}
                    {...form.getInputProps('external_network', { type: 'checkbox' })}
                  />
                </Tooltip>

              </Accordion.Item>
            </Accordion>
          </Tabs.Tab>
          <Tabs.Tab label={i18n.get['NavigationSettings']}>
            <Group mt="xs">
              <Checkbox
                mt="xl"
                label={i18n.get['GenerateThumbnails']}
                {...form.getInputProps('generate_thumbnails', { type: 'checkbox' })}
              />
              <TextInput
                sx={{ flex: 1 }}
                label={i18n.get['ThumbnailSeekingPosition']}
                disabled={!form.values['generate_thumbnails']}
                {...form.getInputProps('thumbnail_seek_position')}
              />
            </Group>
            <Select
              mt="xs"
              label={i18n.get['AudioThumbnailsImport']}
              data={getI18nSelectData(selectionSettings.audioCoverSuppliers as unknown as [{value:string;label:string}])}
              {...form.getInputProps('audio_thumbnails_method')}
            />
            <DirectoryChooser
              path={form.getInputProps('alternate_thumb_folder').value}
              callback={form.setFieldValue}
              label={i18n.get['AlternateVideoCoverArtFolder']}
              formKey="alternate_thumb_folder"
            ></DirectoryChooser>
            <Accordion mt="xl">
              <Accordion.Item label={i18n.get['FileSortingNaming']}>
                <Group mt="xs">
                  <Select
                    label={i18n.get['AudioThumbnailsImport']}
                    data={getI18nSelectData(selectionSettings.sortMethods as unknown as [{value:string;label:string}])}
                    {...form.getInputProps('sort_method')}
                  />
                  <Checkbox
                    mt="xl"
                    label={i18n.get['IgnoreArticlesATheSorting']}
                    {...form.getInputProps('ignore_the_word_a_and_the', { type: 'checkbox' })}
                  />
                </Group>
                <Tooltip label={allowHtml(i18n.get['IfEnabledFilesWillAppear'])} {...defaultTooltipSettings}>
                  <Checkbox
                    mt="md"
                    label={i18n.get['PrettifyFilenames']}
                    {...form.getInputProps('prettify_filenames', { type: 'checkbox' })}
                  />
                </Tooltip>
                <Checkbox
                  mt="md"
                  label={i18n.get['HideFileExtensions']}
                  disabled={form.values['prettify_filenames']}
                  {...form.getInputProps('hide_extensions', { type: 'checkbox' })}
                />
                <Tooltip label={allowHtml(i18n.get['UsesInformationApiAllowBrowsing'])} {...defaultTooltipSettings}>
                  <Checkbox
                    mt="md"
                    label={i18n.get['UseInfoFromOurApi']}
                    {...form.getInputProps('use_imdb_info', { type: 'checkbox' })}
                  />
                </Tooltip>
                <Group mt="xs">
                  <Tooltip label={allowHtml(i18n.get['AddsInformationAboutSelectedSubtitles'])} {...defaultTooltipSettings}>
                    <Select
                      label={i18n.get['AddSubtitlesInformationVideoNames']}
                      data={getI18nSelectData(selectionSettings.subtitlesInfoLevels as unknown as [{value:string;label:string}])}
                      {...form.getInputProps('subs_info_level')}
                    />
                  </Tooltip>
                  <Tooltip label={allowHtml(i18n.get['IfEnabledEngineNameDisplayed'])} {...defaultTooltipSettings}>
                    <Checkbox
                      mt="xl"
                      label={i18n.get['AddEnginesNamesAfterFilenames']}
                      checked={!form.values['hide_enginenames']}
                      onChange={(event) => {
                        form.setFieldValue('hide_enginenames', !event.currentTarget.checked);
                      }}
                    />
                  </Tooltip>
                </Group>
              </Accordion.Item>
              <Accordion.Item label={i18n.get['VirtualFoldersFiles']}>
                <Tooltip label={allowHtml(i18n.get['DisablingWillDisableFullyPlayed'])} {...defaultTooltipSettings}>
                  <Checkbox
                    mt="xl"
                    label={i18n.get['EnableCache']}
                    {...form.getInputProps('use_cache', { type: 'checkbox' })}
                  />
                </Tooltip>
              </Accordion.Item>
            </Accordion>
          </Tabs.Tab>
          <Tabs.Tab label={i18n.get['SharedContent']}>
            
          </Tabs.Tab>
          <Tabs.Tab label={i18n.get['TranscodingSettings']}>
            <Grid>
              <Grid.Col span={5}>
                <Navbar width={{ }} p="xs">
                  <Navbar.Section>
                    <Button variant="subtle" color="gray" size="xs" compact onClick={() => setTranscodingContent('common')}>
                      {i18n.get['CommonTranscodeSettings']}
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
