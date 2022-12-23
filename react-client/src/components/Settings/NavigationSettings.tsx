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
import { Accordion, Button, Checkbox, Group, NumberInput, Select, Stack, Tooltip } from '@mantine/core';
import { useContext } from 'react';

import I18nContext from '../../contexts/i18n-context';
import SessionContext from '../../contexts/session-context';
import { havePermission, Permissions } from '../../services/accounts-service';
import { sendAction } from '../../services/actions-service';
import { allowHtml, defaultTooltipSettings } from '../../utils';
import DirectoryChooser from '../DirectoryChooser/DirectoryChooser';
import { mantineSelectData } from './Settings';

export default function NavigationSettings(
  form: any,
  defaultConfiguration: any,
  selectionSettings: any,
) {
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
  const canModify = havePermission(session, Permissions.settings_modify);

  const getI18nSelectData = (values: mantineSelectData[]) => {
    return values.map((value: mantineSelectData) => {
      return {value: value.value, label: i18n.getI18nString(value.label)};
    });
  }

  const resetCache = async () => {
    await sendAction('Server.ResetCache');
  };

  return (
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
                  <Select
                    disabled={!canModify}
                    label={i18n.get['FullyPlayedAction']}
                    data={getI18nSelectData(selectionSettings.fullyPlayedActions)}
                    {...form.getInputProps('fully_played_action')}
                  />
                  {(form.values['fully_played_action'] === '3' || form.values['fully_played_action'] === '5') && (
                    <DirectoryChooser
                      disabled={!canModify}
                      label={i18n.get['DestinationFolder']}
                      path={form.getInputProps('fully_played_output_directory').value}
                      callback={form.setFieldValue}
                      formKey="fully_played_output_directory"
                    />
                  )}
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
                <NumberInput
                  label={i18n.get['ThumbnailSeekingPosition']}
                  disabled={!canModify || !form.values['generate_thumbnails']}
                  placeholder={defaultConfiguration.thumbnail_seek_position}
                  hideControls
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
                      onChange={(event:React.ChangeEvent<HTMLInputElement>) => {
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
      );
}
