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
import { Accordion, ActionIcon, Box, Button, Checkbox, ColorPicker, ColorSwatch, Grid, Group, Modal, NavLink, NumberInput, Select, Stack, Tabs, Text, Textarea, TextInput, Title, Tooltip } from '@mantine/core';
import { useLocalStorage } from '@mantine/hooks';
import { Prism } from '@mantine/prism';
import { useContext, useState } from 'react';
import { arrayMove, List } from 'react-movable';
import { ArrowNarrowDown, ArrowNarrowUp, ArrowsVertical, Ban, ExclamationMark, PlayerPlay } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import SessionContext from '../../contexts/session-context';
import { havePermission, Permissions } from '../../services/accounts-service';
import { allowHtml, defaultTooltipSettings } from '../../utils';
import DirectoryChooser from '../DirectoryChooser/DirectoryChooser';
import { mantineSelectData } from './Settings';

export default function TranscodingSettings(
	form: any,
	defaultConfiguration: any,
	selectionSettings: any,
) {
	const i18n = useContext(I18nContext);
	const session = useContext(SessionContext);
	const canModify = havePermission(session, Permissions.settings_modify);
	const [transcodingContent, setTranscodingContent] = useState('common');
	const [subColor, setSubColor] = useState('rgba(255, 255, 255, 255)');
	const [subColorModalOpened, setSubColorModalOpened] = useState(false);
	const [mencoderAdvancedOpened, setMencoderAdvancedOpened] = useState(false);
	const [advancedSettings] = useLocalStorage<boolean>({
		key: 'mantine-advanced-settings',
		defaultValue: false,
	});

	const getI18nSelectData = (values: mantineSelectData[]) => {
		return values.map((value: mantineSelectData) => {
			return { value: value.value, label: i18n.getI18nString(value.label) };
		});
	}

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
			const updated = items.filter(function(value) { return value !== id; });
			form.setFieldValue('engines', updated);
		}
	}

	const getTranscodingEngineStatus = (engine: { id: string, name: string, isAvailable: boolean, purpose: number, statusText: string[] }) => {
		const items = (form.getInputProps('engines').value instanceof Array<string>) ?
			form.getInputProps('engines').value as Array<string> :
			[form.getInputProps('engines').value];
		if (!engine.isAvailable) {
			return (
				<Tooltip label={allowHtml(i18n.get['ThereIsProblemTranscodingEngineX']?.replace('%s', engine.name))} {...defaultTooltipSettings}>
					<ExclamationMark color={'orange'} strokeWidth={3} size={14} />
				</Tooltip>
			)
		} else if (items.includes(engine.id)) {
			return (
				<Tooltip label={allowHtml(i18n.get['TranscodingEngineXEnabled']?.replace('%s', engine.name))} {...defaultTooltipSettings}>
					<ActionIcon size={20} style={{ cursor: 'copy' }} onClick={(e: any) => { canModify && setTranscodingEngineStatus(engine.id, false); e.stopPropagation(); }}>
						<PlayerPlay strokeWidth={2} color={'green'} size={14} />
					</ActionIcon>
				</Tooltip>
			)
		}
		return (
			<Tooltip label={allowHtml(i18n.get['TranscodingEngineXDisabled']?.replace('%s', engine.name))} {...defaultTooltipSettings}>
				<ActionIcon size={20} style={{ cursor: 'copy' }} onClick={(e: any) => { canModify && setTranscodingEngineStatus(engine.id, true); e.stopPropagation(); }}>
					<Ban color={'red'} size={14} />
				</ActionIcon>
			</Tooltip>
		)
	}

	const getTranscodingEnginesList = (purpose: number) => {
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
									{engines.indexOf(value) === 0 ? (<ArrowNarrowDown />) : engines.indexOf(value) === engines.length - 1 ? (<ArrowNarrowUp />) : (<ArrowsVertical />)}
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
					<Button variant="subtle" color='gray' size="xs" compact key={value}
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
		return selectionSettings.transcodingEnginesPurposes.map((value: string, index: number) => {
			return (
				<Accordion.Item value={'Transcoding' + index.toString()} key={index}>
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

	const getTranscodingCommon = () => {
		return (<>
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
								data={[{ value: '6', label: i18n.get['6Channels51'] }, { value: '2', label: i18n.get['2ChannelsStereo'] }]}
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
									style={{ cursor: 'pointer', marginTop: '25px' }}
									onClick={() => { if (canModify) { setSubColor(hexAToRgba(form.getInputProps('subtitles_color').value)); setSubColorModalOpened(true); } }}
								/>
							</Group>
							<Tooltip label={allowHtml(i18n.get['DeterminesDownloadedLiveSubtitlesDeleted'])} {...defaultTooltipSettings}>
								<Checkbox
									disabled={!canModify}
									size="xs"
									label={i18n.get['DeleteDownloadedLiveSubtitlesAfter']}
									checked={!form.values['live_subtitles_keep']}
									onChange={(event:React.ChangeEvent<HTMLInputElement>) => {
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

	const getSimpleTranscodingCommon = () => {
		return (<>
			<Title mt="sm" order={5}>{i18n.get['CommonTranscodeSettings']}</Title>
			<Stack spacing="xs">
				<Checkbox
					size="xs"
					disabled={!canModify}
					label={i18n.get['DisableSubtitles']}
					{...form.getInputProps('disable_subtitles', { type: 'checkbox' })}
				/>
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
						<Text size="xs"><ExclamationMark color={'orange'} strokeWidth={3} size={14} /> {i18n.get['ThisEngineNotLoaded']}</Text>
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
								style={{ marginTop: '14px' }}
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

	const getAviSynthFFMPEG = () => {
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
						label={i18n.get['EnableMultithreading']}
						{...form.getInputProps('ffmpeg_avisynth_multithreading', { type: 'checkbox' })}
					/>
					<Checkbox
						disabled={!canModify}
						size="xs"
						label={i18n.get['EnableTrueMotion']}
						{...form.getInputProps('ffmpeg_avisynth_interframe', { type: 'checkbox' })}
					/>
					<Checkbox
						disabled={!canModify}
						size="xs"
						label={i18n.get['EnableGpuUseTrueMotion']}
						{...form.getInputProps('ffmpeg_avisynth_interframegpu', { type: 'checkbox' })}
					/>
					<Checkbox
						disabled={!canModify}
						size="xs"
						label={i18n.get['EnableAvisynthVariableFramerate']}
						{...form.getInputProps('ffmpeg_avisynth_convertfps', { type: 'checkbox' })}
					/>
					<Checkbox
						mt="xs"
						disabled={!canModify}
						size="xs"
						label={i18n.get['UseFFMS2InsteadOfDirectShowSource']}
						{...form.getInputProps('ffmpeg_avisynth_use_ffms2', { type: 'checkbox' })}
					/>
					<Tabs defaultValue="2Dto3DConversionSettings">
						<Tabs.List>
							<Tabs.Tab value='2Dto3DConversionSettings'>{i18n.get['2Dto3DConversionSettings']}</Tabs.Tab>
						</Tabs.List>
						<Tabs.Panel value='2Dto3DConversionSettings'>
							<Stack spacing="xs">
								<Checkbox
									mt="xs"
									disabled={!canModify}
									size="xs"
									label={i18n.get['Enable2Dto3DVideoConversion']}
									{...form.getInputProps('ffmpeg_avisynth_2d_to_3d_conversion', { type: 'checkbox' })}
								/>
								<Select
									disabled={!canModify}
									label={i18n.get['ConversionAlgorithm']}
									data={[
										{ value: '1', label: i18n.get['PulfrichBase'] },
										{ value: '2', label: i18n.get['PulfrichandLighting'] }
									]}
									size="xs"
									{...form.getInputProps('ffmpeg_avisynth_conversion_algorithm_index_2d_to_3d')}
								/>
								<Tooltip label={allowHtml(i18n.get['SelectOrEnterFrameStretchFactorInPercent'])} {...defaultTooltipSettings}>
									<NumberInput
										label={i18n.get['FrameStretchFactor']}
										size="xs"
										max={20}
										min={0}
										disabled={!canModify}
										{...form.getInputProps('ffmpeg_avisynth_frame_stretch_factor_2d_to_3d')}
									/>
								</Tooltip>
								<Tooltip label={allowHtml(i18n.get['SelectOrEnterLightingDepthOffsetFactor'])} {...defaultTooltipSettings}>
									<NumberInput
										label={i18n.get['LightingDepthOffsetFactor']}
										size="xs"
										max={20}
										min={1}
										disabled={!canModify}
										{...form.getInputProps('ffmpeg_avisynth_light_offset_factor_2d_to_3d')}
									/>
								</Tooltip>
								<Select
									disabled={!canModify}
									label={i18n.get['3DOutputFormat']}
									data={[
										{ value: '1', label: i18n.get['SBSFullSideBySide'] },
										{ value: '2', label: i18n.get['TBOUFullTopBottom'] },
										{ value: '3', label: i18n.get['HSBSHalfSideBySide'] },
										{ value: '4', label: i18n.get['HTBHOUHalfTopBottom'] },
										{ value: '5', label: i18n.get['HSBSUpscaledHalfSideBySide'] },
										{ value: '6', label: i18n.get['HTBHOUUpscaledHalfTopBottom'] }
									]}
									size="xs"
									{...form.getInputProps('ffmpeg_avisynth_output_format_index_3d')}
								/>
								<Checkbox
									mt="xs"
									disabled={!canModify}
									size="xs"
									label={i18n.get['ResizeVideoIfWidthLargerThan']}
									{...form.getInputProps('ffmpeg_avisynth_horizontal_resize', { type: 'checkbox' })}
								/>
								<Tooltip label={allowHtml(i18n.get['SelectOrEnterTheMaximumWidthOfTheInputVideo'])} {...defaultTooltipSettings}>
									<Select
										disabled={!canModify}
										label={''}
										data={[
											{ value: '7680', label: '7680' },
											{ value: '3840', label: '3840' },
											{ value: '1920', label: '1920' },
											{ value: '1280', label: '1280' },
											{ value: '852', label: '852' },
											{ value: '768', label: '768' },
											{ value: '720', label: '720' },
											{ value: '704', label: '704' },
											{ value: '640', label: '640' },
											{ value: '544', label: '544' },
											{ value: '480', label: '480' },
											{ value: '352', label: '352' },
											{ value: '120', label: '120' }
										]}
										size="xs"
										{...form.getInputProps('ffmpeg_avisynth_horizontal_resize_resolution')}
									/>
								</Tooltip>
							</Stack>
						</Tabs.Panel>
					</Tabs>
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
						{...form.getInputProps('ffmpeg_sox', { type: 'checkbox' })}
					/>
					<NumberInput
						disabled={!canModify}
						label={i18n.get['GpuDecodingThreadCount']}
						size="xs"
						max={16}
						min={0}
						{...form.getInputProps('ffmpeg_gpu_decoding_acceleration_thread_number')}
					/>
					<Tooltip label={allowHtml(i18n.get['NvidiaAndAmdEncoders'])} {...defaultTooltipSettings}>
						<Select
							disabled={!canModify}
							size="xs"
							label={i18n.get['AVCH264GPUEncodingAccelerationMethod']}
							data={selectionSettings.gpuEncodingH264AccelerationMethods}
							{...form.getInputProps('ffmpeg_gpu_encoding_H264_acceleration_method')}
						/>
					</Tooltip>
					<Tooltip label={allowHtml(i18n.get['NvidiaAndAmdEncoders'])} {...defaultTooltipSettings}>
						<Select
							disabled={!canModify}
							size="xs"
							label={i18n.get['HEVCH265GPUEncodingAccelerationMethod']}
							data={selectionSettings.gpuEncodingH265AccelerationMethods}
							{...form.getInputProps('ffmpeg_gpu_encoding_H265_acceleration_method')}
						/>
					</Tooltip>
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
		switch (transcodingContent) {
			case 'common':
				return getTranscodingCommon();
			case 'DCRaw':
				return noSettingsForNow();
			case 'FFmpegAudio':
				return getFFMPEGAudio();
			case 'FFmpegVideo':
				return getFFMPEGVideo();
			case 'AviSynthFFmpeg':
				return getAviSynthFFMPEG();
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

	return advancedSettings ? (
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
	) : getSimpleTranscodingCommon();
}
