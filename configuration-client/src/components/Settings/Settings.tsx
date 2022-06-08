import { TextInput, Checkbox, Button, Group, Box, Select, Tabs } from '@mantine/core';
import { useForm } from '@mantine/form';
import { showNotification } from '@mantine/notifications';
import _ from 'lodash';
import { useContext, useEffect, useRef, useState } from 'react';
import axios from 'axios';

import I18nContext from '../../contexts/i18n-context';

export default function Settings() {
  const [activeTab, setActiveTab] = useState(0);
  const [isLoading, setLoading] = useState(true);
  const languageSettingsRef = useRef([]);
  const i18n = useContext(I18nContext);

  const defaultSettings: Record<string, any> = {
    append_profile_name: false,
    auto_update: true,
    minimized: false,
    language: 'en-US',
    server_name: 'Universal Media Server',
    show_splash_screen: true,
  };

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

            <Group mt="xs">
              <Checkbox
                mt="xl"
                label={i18n['NetworkTab.3']}
                {...form.getInputProps('minimized', { type: 'checkbox' })}
              />
              <Checkbox
                mt="xl"
                label={i18n['NetworkTab.74']}
                {...form.getInputProps('show_splash_screen', { type: 'checkbox' })}
              />
            </Group>

            <Checkbox
              mt="xl"
              label={i18n['NetworkTab.9']}
              {...form.getInputProps('auto_update', { type: 'checkbox' })}
            />
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
  );
}
