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
import { Box, Button, Group, Tabs, Text } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useLocalStorage } from '@mantine/hooks';
import axios from 'axios';
import _ from 'lodash';
import { useEffect, useState } from 'react';
import { IconCheck, IconExclamationMark } from '@tabler/icons-react';

import { havePermission, Permissions } from '../../services/accounts-service';
import { I18nInterface } from '../../services/i18n-service';
import { MainInterface } from '../../services/main-service';
import { ServerEventInterface } from '../../services/server-event-service';
import { SessionInterface } from '../../services/session-service';
import { mantineSelectData } from '../../services/settings-service';
import { openGitHubNewIssue, settingsApiUrl } from '../../utils';
import GeneralSettings from './GeneralSettings';
import NavigationSettings from './NavigationSettings';
import RenderersSettings from './RenderersSettings';
import TranscodingSettings from './TranscodingSettings';
import { showError, showLoading, updateError, updateInfo, updateSuccess } from '../../utils/notifications';

export default function Settings({ i18n, main, sse, session }: { i18n: I18nInterface, main:MainInterface, sse: ServerEventInterface, session: SessionInterface }) {
  const [advancedSettings] = useLocalStorage<boolean>({
    key: 'mantine-advanced-settings',
    defaultValue: false,
  });
  const [isLoading, setLoading] = useState(true);
  const [defaultConfiguration, setDefaultConfiguration] = useState({} as any);
  const [configuration, setConfiguration] = useState({} as any);

  // key/value pairs for dropdowns
  const [selectionSettings, setSelectionSettings] = useState({
    allRendererNames: [] as mantineSelectData[],
    audioCoverSuppliers: [] as mantineSelectData[],
    enabledRendererNames: [] as mantineSelectData[],
    ffmpegLoglevels: [],
    upnpLoglevels: [] as mantineSelectData[],
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

  const form = useForm({ initialValues: {} as Record<string, unknown> });
  const formSetValues = form.setValues;

  const canModify = havePermission(session, Permissions.settings_modify);
  const canView = canModify || havePermission(session, Permissions.settings_view);

  //set the document Title to Server Settings
  useEffect(() => {
    document.title="Universal Media Server - Server Settings";
    session.useSseAs('Settings')
    session.stopPlayerSse()
    main.setNavbarValue(undefined)
  }, []);

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
    if (canView) {
      axios.get(settingsApiUrl)
        .then(function(response: any) {
          const settingsResponse = response.data;
          setSelectionSettings(settingsResponse);
          setDefaultConfiguration(settingsResponse.userSettingsDefaults);

          // merge defaults with what we receive, which might only be non-default values
          const userConfig = _.merge({}, settingsResponse.userSettingsDefaults, settingsResponse.userSettings);

          setConfiguration(userConfig);
          formSetValues(userConfig);
        })
        .catch(function() {
          showError({
            id: 'data-loading',
            title: i18n.get('Error'),
            message: i18n.get('ConfigurationNotReceived') + ' ' + i18n.get('ClickHereReportBug'),
            onClick: () => { openGitHubNewIssue(); },
          });
        })
        .then(function() {
          setLoading(false);
        });
    }
  }, [canView, formSetValues]);

  const handleSubmit = async (values: typeof form.values) => {
    setLoading(true);
    showLoading({
      id: 'settings-save',
      title: i18n.get('Save'),
      message: i18n.get('SavingConfiguration'),
    });
    try {
      const changedValues: Record<string, any> = {};

      // construct an object of only changed values to send
      for (const key in values) {
        if (!_.isEqual(configuration[key], values[key])) {
          changedValues[key] = values[key] !== undefined && values[key] !== null ? values[key] : null;
        }
      }

      if (_.isEmpty(changedValues)) {
        updateInfo({
          id: 'settings-save',
          title: i18n.get('Saved'),
          message: i18n.get('ConfigurationHasNoChanges'),
        })
      } else {
        await axios.post(settingsApiUrl, changedValues)
          .then(function() {
            setConfiguration(values);
            setLoading(false);
            updateSuccess({
              id: 'settings-save',
              title: i18n.get('Saved'),
              message: i18n.get('ConfigurationSaved'),
              icon: <IconCheck size='1rem' />,
            })
          })
          .catch(function(error) {
            if (!error.response && error.request) {
              updateError({
                id: 'settings-save',
                title: i18n.get('Error'),
                message: i18n.get('ConfigurationNotReceived'),
                icon: <IconExclamationMark size='1rem' />,
              })
            } else {
              throw new Error(error);
            }
          });
      }
    } catch (err) {
      updateError({
        id: 'settings-save',
        title: i18n.get('Error'),
        message: i18n.get('ConfigurationNotSaved') + ' ' + i18n.get('ClickHereReportBug'),
        onClick: () => { openGitHubNewIssue(); },
      })
    }

    setLoading(false);
  };

  return canView ? (
    <Box style={{ maxWidth: 1024 }} mx='auto'>
      <form onSubmit={form.onSubmit(handleSubmit)}>
        <Tabs defaultValue='GeneralSettings'>
          <Tabs.List>
            <Tabs.Tab value='GeneralSettings'>{i18n.get('GeneralSettings')}</Tabs.Tab>
            {advancedSettings &&
              <Tabs.Tab value='RenderersSettings'>{i18n.get('RenderersSettings')}</Tabs.Tab>
            }
            {advancedSettings &&
              <Tabs.Tab value='NavigationSettings'>{i18n.get('NavigationSettings')}</Tabs.Tab>
            }
            <Tabs.Tab value='TranscodingSettings'>{i18n.get('TranscodingSettings')}</Tabs.Tab>
          </Tabs.List>
          <Tabs.Panel value='GeneralSettings'>
            {GeneralSettings(i18n, session, form, defaultConfiguration, selectionSettings)}
          </Tabs.Panel>
          {advancedSettings &&
            <Tabs.Panel value='RenderersSettings'>
              {RenderersSettings(i18n, session, form, selectionSettings)}
            </Tabs.Panel>
          }
          {advancedSettings &&
            <Tabs.Panel value='NavigationSettings'>
              {NavigationSettings(i18n, session, form, defaultConfiguration, selectionSettings)}
            </Tabs.Panel>
          }
          <Tabs.Panel value='TranscodingSettings'>
            {TranscodingSettings(i18n, session, form, defaultConfiguration, selectionSettings)}
          </Tabs.Panel>
        </Tabs>
        {canModify && (
          <Group justify='flex-end' mt='md'>
            <Button type='submit' loading={isLoading}>
              {i18n.get('Save')}
            </Button>
          </Group>
        )}
      </form>
    </Box>
  ) : (
    <Box style={{ maxWidth: 1024 }} mx='auto'>
      <Text c='red'>{i18n.get('YouDontHaveAccessArea')}</Text>
    </Box>
  );
}
