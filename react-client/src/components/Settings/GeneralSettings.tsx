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
import { Accordion, Anchor, Checkbox, Divider, Group, NumberInput, Select, Stack, Text, TextInput, Tooltip } from '@mantine/core';
import { useLocalStorage } from '@mantine/hooks';
import { useContext } from 'react';

import I18nContext from '../../contexts/i18n-context';
import SessionContext from '../../contexts/session-context';
import { havePermission, Permissions } from '../../services/accounts-service';
import { allowHtml, defaultTooltipSettings } from '../../utils';
import { mantineSelectData } from './Settings';

export default function GeneralSettings(
  form: any,
  defaultConfiguration: any,
  selectionSettings: any,
) {
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
  const canModify = havePermission(session, Permissions.settings_modify);
  const [advancedSettings, setAdvancedSettings] = useLocalStorage<boolean>({
    key: 'mantine-advanced-settings',
    defaultValue: false,
  });

  const getI18nSelectData = (values: mantineSelectData[]) => {
    return values.map((value: mantineSelectData) => {
      return { value: value.value, label: i18n.getI18nString(value.label) };
    });
  }

  const getLanguagesSelectData = () => {
    return i18n.languages.map((language) => {
      return {
        value: language.id,
        label: language.name
          + (language.name !== language.defaultname ? ' (' + language.defaultname + ')' : '')
          + (!language.id.startsWith('en-') ? ' (' + language.coverage + '%)' : '')
      };
    });
  }

  return (
    <Accordion>
      <Accordion.Item value='Application'>
        <Accordion.Control>{i18n.get('Application')}</Accordion.Control>
        <Accordion.Panel>
          <Checkbox
            label={i18n.get('ShowAdvancedSettings')}
            checked={advancedSettings}
            onChange={(event: React.ChangeEvent<HTMLInputElement>) => setAdvancedSettings(event.currentTarget.checked)}
          />
          <Select
            disabled={!canModify}
            label={i18n.get('Language')}
            data={getLanguagesSelectData()}
            {...form.getInputProps('language')}
          />
          <Stack align='flex-start' mt='sm'>
            <Checkbox
              disabled={!canModify}
              label={i18n.get('EnableSplashScreen')}
              {...form.getInputProps('show_splash_screen', { type: 'checkbox' })}
            />
            <Checkbox
              disabled={!canModify}
              label={i18n.get('CheckAutomaticallyForUpdates')}
              {...form.getInputProps('auto_update', { type: 'checkbox' })}
            />
          </Stack>
        </Accordion.Panel>
      </Accordion.Item>
      <Accordion.Item value='GUI'>
        <Accordion.Control>{i18n.get('GraphicalUserInterface')}</Accordion.Control>
        <Accordion.Panel>
          <Checkbox
            disabled={!canModify}
            label={i18n.get('LaunchGuiBrowserStartup')}
            {...form.getInputProps('web_gui_on_start', { type: 'checkbox' })}
          />
          {advancedSettings && (
            <NumberInput
              disabled={!canModify}
              placeholder={defaultConfiguration.web_gui_port}
              label={i18n.get('ForcePortGuiServer')}
              hideControls
              {...form.getInputProps('web_gui_port')}
            />
          )}
        </Accordion.Panel>
      </Accordion.Item>
      {advancedSettings && (
        <Accordion.Item value='Services'>
          <Accordion.Control>{i18n.get('Services')}</Accordion.Control>
          <Accordion.Panel>
            <Group>
              <TextInput
                disabled={!canModify}
                label={i18n.get('ServerName')}
                placeholder={defaultConfiguration.server_name}
                name='server_name'
                style={{ flex: 1 }}
                {...form.getInputProps('server_name')}
              />
              <Tooltip label={allowHtml(i18n.get('WhenEnabledUmsProfileName'))} {...defaultTooltipSettings}>
                <Checkbox
                  disabled={!canModify}
                  mt='xl'
                  label={i18n.get('AppendProfileName')}
                  {...form.getInputProps('append_profile_name', { type: 'checkbox' })}
                />
              </Tooltip>
            </Group>
            <Divider mt='md' label={<Text fz='md' c={'var(--mantine-color-text)'}>{i18n.get('MediaServer')}</Text>} />
            <NumberInput
              disabled={!canModify}
              placeholder={defaultConfiguration.port}
              label={i18n.get('ForcePortServer')}
              hideControls
              {...form.getInputProps('port')}
            />
            <Stack mt='sm'>
              {/* removed until root user choice is implemented
              <Checkbox
                disabled={!canModify}
                label={i18n.get('ShowUserChoice')}
                {...form.getInputProps('show_user_choice', { type: 'checkbox' })}
              />
              */}
              <Checkbox
                disabled={!canModify}
                label={i18n.get('UPnPDlnaService')}
                {...form.getInputProps('upnp_enable', { type: 'checkbox' })}
              />
              {advancedSettings &&
                <Checkbox
                  disabled={!canModify}
                  label={i18n.get('JUPnPDIDLLite')}
                  {...form.getInputProps('upnp_jupnp_didl', { type: 'checkbox' })}
                />
              }
              {advancedSettings &&
                <Checkbox
                  disabled={!canModify}
                  label={i18n.get('DebugUpnpService')}
                  {...form.getInputProps('upnp_debug', { type: 'checkbox' })}
                />
              }
              <Checkbox
                disabled={!canModify}
                label={i18n.get('MDNSChromecastService')}
                {...form.getInputProps('chromecast_extension', { type: 'checkbox' })}
              />
            </Stack>
            <Divider mt='md' label={<Text fz='md' c={'var(--mantine-color-text)'}>{i18n.get('WebPlayer')}</Text>} />
            <Checkbox
              disabled={!canModify}
              label={i18n.get('EnableWebPlayer')}
              {...form.getInputProps('web_player_enable', { type: 'checkbox' })}
            />
            <NumberInput
              disabled={!canModify || !form.values['web_player_enable']}
              placeholder={defaultConfiguration.web_player_port}
              label={i18n.get('ForcePortPlayerServer')}
              hideControls
              {...form.getInputProps('web_player_port')}
            />
            <Stack align='flex-start' mt='sm'>
              <Checkbox
                disabled={!canModify || !form.values['web_player_enable']}
                label={i18n.get('UseAuthenticationService')}
                {...form.getInputProps('web_player_auth', { type: 'checkbox' })}
              />
              <Checkbox
                disabled={!canModify}
                label={i18n.get('AllowMediaDownload')}
                {...form.getInputProps('web_player_download', { type: 'checkbox' })}
              />
              <Checkbox
                disabled={!canModify || !form.values['web_player_enable']}
                label={i18n.get('CanControlOtherDevices')}
                {...form.getInputProps('web_player_controls', { type: 'checkbox' })}
              />
            </Stack>
          </Accordion.Panel>
        </Accordion.Item>
      )}
      {advancedSettings && (
        <Accordion.Item value='NetworkSettingsAdvanced'>
          <Accordion.Control>{i18n.get('NetworkSettingsAdvanced')}</Accordion.Control>
          <Accordion.Panel>
            <Select
              disabled={!canModify}
              label={i18n.get('ForceNetworkingInterface')}
              data={getI18nSelectData(selectionSettings.networkInterfaces)}
              {...form.getInputProps('network_interface')}
            />
            <TextInput
              disabled={!canModify}
              mt='xs'
              label={i18n.get('ForceIpServer')}
              {...form.getInputProps('hostname')}
            />
            <Group>
              <NumberInput
                label={i18n.get('MaximumBandwidthMbs')}
                disabled={!canModify || form.values['automatic_maximum_bitrate']}
                style={{ flex: 1 }}
                placeholder={i18n.get('Mbs')}
                hideControls
                {...form.getInputProps('maximum_bitrate')}
              />
              <Tooltip label={allowHtml(i18n.get('ItSetsOptimalBandwidth'))} {...defaultTooltipSettings}>
                <Checkbox
                  disabled={!canModify}
                  mt='xl'
                  label={i18n.get('UseAutomaticMaximumBandwidth')}
                  {...form.getInputProps('automatic_maximum_bitrate', { type: 'checkbox' })}
                />
              </Tooltip>
            </Group>
          </Accordion.Panel>
        </Accordion.Item>
      )}
      <Accordion.Item value='ExternalOutgoingTraffic'>
        <Accordion.Control>{i18n.get('ExternalOutgoingTraffic')}</Accordion.Control>
        <Accordion.Panel>
          <Stack>
            {advancedSettings && (
              <Tooltip label={allowHtml(i18n.get('ThisControlsWhetherUmsTry'))} {...defaultTooltipSettings}>
                <Checkbox
                  disabled={!canModify}
                  label={i18n.get('EnableExternalNetwork')}
                  {...form.getInputProps('external_network', { type: 'checkbox' })}
                />
              </Tooltip>
            )}
            <Tooltip label={allowHtml(i18n.get('UsesInformationApiAllowBrowsing'))} {...defaultTooltipSettings}>
              <Checkbox
                disabled={!canModify}
                label={i18n.get('UseInfoFromOurApi')}
                {...form.getInputProps('use_imdb_info', { type: 'checkbox' })}
              />
            </Tooltip>
            <Checkbox
              disabled={!canModify}
              label={i18n.get('UseInfoFromTMDB')}
              {...form.getInputProps('use_tmdb_info', { type: 'checkbox' })}
            />
            <TextInput
              disabled={!canModify || !form.values['use_tmdb_info']}
              mt='xs'
              label={i18n.get('TMDBApiKey')}
              description=<Anchor
                href='https://www.themoviedb.org/settings/api'
                target='_blank'
                c='dimmed'
                size='xs'
                style={{ lineHeight: 1 }}
              >
                {i18n.get('ToRegisterTmdbApiKey')}
              </Anchor>
              {...form.getInputProps('tmdb_api_key')}
            />
          </Stack>
        </Accordion.Panel>
      </Accordion.Item>
    </Accordion>
  );
}
