import { Accordion, Checkbox, Group, MultiSelect, Select, Stack, TextInput, Tooltip } from '@mantine/core';
import { UseFormReturnType } from '@mantine/form';
import { useLocalStorage } from '@mantine/hooks';
import { useContext } from 'react';

import I18nContext from '../../contexts/i18n-context';
import SessionContext from '../../contexts/session-context';
import { havePermission, Permissions } from '../../services/accounts-service';
import { allowHtml, defaultTooltipSettings } from '../../utils';
import { mantineSelectData } from './Settings';

export default function GeneralSettings(form:UseFormReturnType<any>,defaultConfiguration:any,selectionSettings:any) {
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
  const canModify = havePermission(session, Permissions.settings_modify);
  const [advancedSettings, setAdvancedSettings] = useLocalStorage<boolean>({
    key: 'mantine-advanced-settings',
    defaultValue: false,
  });

  const getI18nSelectData = (values: mantineSelectData[]) => {
    return values.map((value: mantineSelectData) => {
      return {value: value.value, label: i18n.getI18nString(value.label)};
    });
  }

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

  return (
        <Accordion>
          <Accordion.Item value='Application'>
            <Accordion.Control>{i18n.get['Application']}</Accordion.Control>
            <Accordion.Panel>
              <Checkbox
                label={i18n.get['ShowAdvancedSettings']}
                checked={advancedSettings}
                onChange={(event) => setAdvancedSettings(event.currentTarget.checked)}
              />
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
          { advancedSettings && (
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
          ) }
          { advancedSettings && (
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
          ) }
          <Accordion.Item value='ExternalOutgoingTraffic'>
            <Accordion.Control>{i18n.get['ExternalOutgoingTraffic']}</Accordion.Control>
            <Accordion.Panel>
              <Stack>
                { advancedSettings && (
                <Tooltip label={allowHtml(i18n.get['ThisControlsWhetherUmsTry'])} {...defaultTooltipSettings}>
                  <Checkbox
                    disabled={!canModify}
                    label={i18n.get['EnableExternalNetwork']}
                    {...form.getInputProps('external_network', { type: 'checkbox' })}
                  />
                </Tooltip>
                ) }
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
          { advancedSettings && (
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
          ) }
        </Accordion>
      );
}
