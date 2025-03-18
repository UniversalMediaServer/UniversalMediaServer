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
import { Accordion, Anchor, Checkbox, Divider, Group, NumberInput, Select, Stack, Text, TextInput, Tooltip } from '@mantine/core'
import { UseFormReturnType } from '@mantine/form'
import { useLocalStorage } from '@mantine/hooks'

import { I18nInterface } from '../../services/i18n-service'
import { SelectionSettingsData } from '../../services/settings-service'
import { allowHtml, defaultTooltipSettings } from '../../utils'

export default function GeneralSettings({
  i18n,
  form,
  defaultConfiguration,
  selectionSettings,
}: {
  i18n: I18nInterface
  form: UseFormReturnType<Record<string, unknown>, (values: Record<string, unknown>) => Record<string, unknown>>
  defaultConfiguration: Record<string, unknown>
  selectionSettings: SelectionSettingsData | undefined
}) {
  const [advancedSettings, setAdvancedSettings] = useLocalStorage<boolean>({
    key: 'mantine-advanced-settings',
    defaultValue: false,
  })

  const getLanguagesSelectData = () => {
    return i18n.languages.map((language) => {
      return {
        value: language.id,
        label: language.name
          + (language.name !== language.defaultname ? ' (' + language.defaultname + ')' : '')
          + (!language.id.startsWith('en-') ? ' (' + language.coverage + '%)' : ''),
      }
    })
  }

  return (
    <Accordion>
      <Accordion.Item value="Application">
        <Accordion.Control>{i18n.get('Application')}</Accordion.Control>
        <Accordion.Panel>
          <Checkbox
            label={i18n.get('ShowAdvancedSettings')}
            checked={advancedSettings}
            onChange={(event: React.ChangeEvent<HTMLInputElement>) => setAdvancedSettings(event.currentTarget.checked)}
          />
          <Select
            label={i18n.get('Language')}
            data={getLanguagesSelectData()}
            {...form.getInputProps('language')}
          />
          <Stack align="flex-start" mt="sm">
            <Checkbox
              label={i18n.get('EnableSplashScreen')}
              {...form.getInputProps('show_splash_screen', { type: 'checkbox' })}
            />
            <Checkbox
              label={i18n.get('CheckAutomaticallyForUpdates')}
              {...form.getInputProps('auto_update', { type: 'checkbox' })}
            />
          </Stack>
        </Accordion.Panel>
      </Accordion.Item>
      <Accordion.Item value="GUI">
        <Accordion.Control>{i18n.get('GraphicalUserInterface')}</Accordion.Control>
        <Accordion.Panel>
          <Checkbox
            label={i18n.get('LaunchGuiBrowserStartup')}
            {...form.getInputProps('web_gui_on_start', { type: 'checkbox' })}
          />
          {advancedSettings && (
            <NumberInput
              placeholder={defaultConfiguration.web_gui_port?.toString()}
              label={i18n.get('ForcePortGuiServer')}
              hideControls
              {...form.getInputProps('web_gui_port')}
            />
          )}
          <Stack align="flex-start" mt="sm">
            <Checkbox
              label={i18n.get('UseAuthenticationService')}
              {...form.getInputProps('authentication_enabled', { type: 'checkbox' })}
            />
            <Tooltip label={allowHtml(i18n.get('EnablingAuthenticateLocalhost'))} {...defaultTooltipSettings}>
              <Checkbox
                disabled={!form.values['authentication_enabled']}
                label={i18n.get('AuthenticateLocalhostAdmin')}
                {...form.getInputProps('authenticate_localhost_as_admin', { type: 'checkbox' })}
              />
            </Tooltip>
            <Checkbox
              disabled={!form.values['authentication_enabled']}
              label={i18n.get('ShowUserChoice')}
              {...form.getInputProps('web_gui_show_users', { type: 'checkbox' })}
            />
            <Checkbox
              disabled={!form.values['authentication_enabled'] || !form.values['web_gui_show_users']}
              label={i18n.get('AllowEmptyPinLogin')}
              {...form.getInputProps('web_gui_allow_empty_pin', { type: 'checkbox' })}
            />
          </Stack>
        </Accordion.Panel>
      </Accordion.Item>
      {advancedSettings && (
        <Accordion.Item value="Services">
          <Accordion.Control>{i18n.get('Services')}</Accordion.Control>
          <Accordion.Panel>
            <Group>
              <TextInput
                label={i18n.get('ServerName')}
                placeholder={defaultConfiguration.server_name?.toString()}
                name="server_name"
                style={{ flex: 1 }}
                {...form.getInputProps('server_name')}
              />
              <Tooltip label={allowHtml(i18n.get('WhenEnabledUmsProfileName'))} {...defaultTooltipSettings}>
                <Checkbox
                  mt="xl"
                  label={i18n.get('AppendProfileName')}
                  {...form.getInputProps('append_profile_name', { type: 'checkbox' })}
                />
              </Tooltip>
            </Group>
            <Divider mt="md" label={<Text fz="md" c="var(--mantine-color-text)">{i18n.get('MediaServer')}</Text>} />
            <NumberInput
              placeholder={defaultConfiguration.port?.toString()}
              label={i18n.get('ForcePortServer')}
              hideControls
              {...form.getInputProps('port')}
            />
            <Stack mt="sm">
              <Checkbox
                label={i18n.get('UPnPDlnaService')}
                {...form.getInputProps('upnp_enable', { type: 'checkbox' })}
              />
              {advancedSettings
                && (
                  <Select
                    size="xs"
                    label={i18n.get('LogLevelColon')}
                    data={i18n.getValueLabelData(selectionSettings?.upnpLoglevels)}
                    {...form.getInputProps('upnp_log_level')}
                  />
                )}
              {advancedSettings
                && (
                  <Checkbox
                    label={i18n.get('JUPnPDIDLLite')}
                    {...form.getInputProps('upnp_jupnp_didl', { type: 'checkbox' })}
                  />
                )}
              <Checkbox
                label={i18n.get('MDNSChromecastService')}
                {...form.getInputProps('chromecast_extension', { type: 'checkbox' })}
              />
            </Stack>
            <Divider mt="md" label={<Text fz="md" c="var(--mantine-color-text)">{i18n.get('WebPlayer')}</Text>} />
            <Checkbox
              label={i18n.get('EnableWebPlayer')}
              {...form.getInputProps('web_player_enable', { type: 'checkbox' })}
            />
            <NumberInput
              disabled={!form.values['web_player_enable']}
              placeholder={defaultConfiguration.web_player_port?.toString()}
              label={i18n.get('ForcePortPlayerServer')}
              hideControls
              {...form.getInputProps('web_player_port')}
            />
            <Stack align="flex-start" mt="sm">
              <Checkbox
                disabled={!form.values['web_player_enable'] || !form.values['authentication_enabled']}
                label={i18n.get('UseAuthenticationService')}
                checked={!form.values['authentication_enabled'] ? false : undefined}
                description={!form.values['authentication_enabled'] ? i18n.get('AuthenticationServiceDisabled') : undefined}
                {...form.getInputProps('web_player_auth', { type: form.values['authentication_enabled'] ? 'checkbox' : 'input' })}
              />
              <Checkbox
                disabled={!form.values['web_player_auth'] || !form.values['authentication_enabled']}
                label={i18n.get('ShowUserChoice')}
                {...form.getInputProps('web_player_show_users', { type: 'checkbox' })}
              />
              <Checkbox
                disabled={!form.values['web_player_show_users'] || !form.values['web_player_auth'] || !form.values['authentication_enabled']}
                label={i18n.get('AllowEmptyPinLogin')}
                {...form.getInputProps('web_player_allow_empty_pin', { type: 'checkbox' })}
              />
              <Checkbox
                disabled={!form.values['web_player_enable'] || form.values['web_player_auth'] === true || !form.values['authentication_enabled']}
                label={i18n.get('AllowMediaDownload')}
                {...form.getInputProps('web_player_download', { type: 'checkbox' })}
              />
              <Checkbox
                disabled={!form.values['web_player_enable'] || form.values['web_player_auth'] === true || !form.values['authentication_enabled']}
                label={i18n.get('CanControlOtherDevices')}
                {...form.getInputProps('web_player_controls', { type: 'checkbox' })}
              />
            </Stack>
          </Accordion.Panel>
        </Accordion.Item>
      )}
      {advancedSettings && (
        <Accordion.Item value="NetworkSettingsAdvanced">
          <Accordion.Control>{i18n.get('NetworkSettingsAdvanced')}</Accordion.Control>
          <Accordion.Panel>
            <Select
              label={i18n.get('ForceNetworkingInterface')}
              data={i18n.getValueLabelData(selectionSettings?.networkInterfaces)}
              {...form.getInputProps('network_interface')}
            />
            <TextInput
              mt="xs"
              label={i18n.get('ForceIpServer')}
              {...form.getInputProps('hostname')}
            />
            <Group>
              <NumberInput
                label={i18n.get('MaximumBandwidthMbs')}
                disabled={form.values['automatic_maximum_bitrate'] == true}
                style={{ flex: 1 }}
                placeholder={i18n.get('Mbs')}
                hideControls
                {...form.getInputProps('maximum_bitrate')}
              />
              <Tooltip label={allowHtml(i18n.get('ItSetsOptimalBandwidth'))} {...defaultTooltipSettings}>
                <Checkbox
                  mt="xl"
                  label={i18n.get('UseAutomaticMaximumBandwidth')}
                  {...form.getInputProps('automatic_maximum_bitrate', { type: 'checkbox' })}
                />
              </Tooltip>
            </Group>
          </Accordion.Panel>
        </Accordion.Item>
      )}
      <Accordion.Item value="ExternalOutgoingTraffic">
        <Accordion.Control>{i18n.get('ExternalOutgoingTraffic')}</Accordion.Control>
        <Accordion.Panel>
          <Stack>
            {advancedSettings && (
              <Tooltip label={allowHtml(i18n.get('ThisControlsWhetherUmsTry'))} {...defaultTooltipSettings}>
                <Checkbox
                  label={i18n.get('EnableExternalNetwork')}
                  {...form.getInputProps('external_network', { type: 'checkbox' })}
                />
              </Tooltip>
            )}
            <Tooltip label={allowHtml(i18n.get('UsesInformationApiAllowBrowsing'))} {...defaultTooltipSettings}>
              <Checkbox
                label={i18n.get('UseInfoFromOurApi')}
                {...form.getInputProps('use_api_info', { type: 'checkbox' })}
              />
            </Tooltip>
            <Checkbox
              label={i18n.get('UseInfoFromTMDB')}
              {...form.getInputProps('use_tmdb_info', { type: 'checkbox' })}
            />
            <TextInput
              disabled={!form.values['use_tmdb_info']}
              mt="xs"
              label={i18n.get('TMDBApiKey')}
              description={(
                <Anchor
                  href="https://www.themoviedb.org/settings/api"
                  target="_blank"
                  c="dimmed"
                  size="xs"
                  style={{ lineHeight: 1 }}
                >
                  {i18n.get('ToRegisterTmdbApiKey')}
                </Anchor>
              )}
              {...form.getInputProps('tmdb_api_key')}
            />
          </Stack>
        </Accordion.Panel>
      </Accordion.Item>
    </Accordion>
  )
}
