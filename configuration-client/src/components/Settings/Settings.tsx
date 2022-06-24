import { TextInput, Checkbox, Button, Group, Box, Select, Tabs, Text, Accordion, MultiSelect } from '@mantine/core';
import { useForm } from '@mantine/form';
import { showNotification } from '@mantine/notifications';
import _ from 'lodash';
import React, { useContext, useEffect, useRef, useState } from 'react';
import axios from 'axios';

import I18nContext from '../../contexts/i18n-context';
import SessionContext from '../../contexts/session-context';
import { havePermission } from '../../services/accounts-service';

export default function Settings() {
  const [activeTab, setActiveTab] = useState(0);
  const [isLoading, setLoading] = useState(true);

  // key/value pairs for dropdowns
  const languageSettingsRef = useRef([]);
  const networkInterfaceSettingsRef = useRef([]);
  const serverEnginesSettingsRef = useRef([]);
  const allRendererNamesSettingsRef = useRef([]);
  const enabledRendererNamesSettingsRef = useRef([]);

  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);

  const defaultSettings: Record<string, any> = {
    append_profile_name: false,
    auto_update: true,
    automatic_maximum_bitrate: true,
    external_network: true,
    hostname: '',
    ip_filter: '',
    language: 'en-US',
    maximum_bitrate: '90',
    minimized: false,
    network_interface: '',
    port: '',
    renderer_default: '',
    renderer_force_default: false,
    selected_renderers: ['All renderers'],
    server_engine: '0',
    server_name: 'Universal Media Server',
    show_splash_screen: true,
  };

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
        </Tabs>

        <Group position="right" mt="md">
          <Button type="submit" loading={isLoading}>
            {i18n['LooksFrame.9']}
          </Button>
        </Group>
      </form>
    </Box>
  ) : (
    <Box sx={{ maxWidth: 700 }} mx="auto">
      <Text color="red">You don't have access to this area.</Text>
    </Box>
  );
}
