import { TextInput, Checkbox, Button, Group, Box, Select, Tabs, Space } from '@mantine/core';
import { Refresh } from 'tabler-icons-react';
import { useForm } from '@mantine/form';
import { showNotification } from '@mantine/notifications';
import { sendAction } from '../../services/actions-service';
import _ from 'lodash';
import { useEffect, useRef, useState } from 'react';
import axios from 'axios';

export default function Settings() {
  const [activeTab, setActiveTab] = useState(0);
  const [isLoading, setLoading] = useState(true);
  const languageSettingsRef = useRef([]);
  let translations: {[key: string]: string} = {};
  const translationsRef = useRef(translations);

  const defaultSettings = {
    append_profile_name: false,
    minimized: false,
    language: 'en-US',
    server_name: 'Universal Media Server',
    show_splash_screen: true,
  };

  const openGitHubNewIssue = () => {
    window.location.href = 'https://github.com/UniversalMediaServer/UniversalMediaServer/issues/new';
  };

  const restartServer = async () => {
    await sendAction('Server.Restart');
  };

  const [configuration, setConfiguration] = useState(defaultSettings);

  const form = useForm({ initialValues: defaultSettings });

  // Code here will run just like componentDidMount
  useEffect(() => {
    Promise.all([
      axios.get('/configuration-api/settings'),
      axios.get('/configuration-api/i18n'),
    ])
      .then(function (response: any[]) {
        const settingsResponse = response[0].data;
        languageSettingsRef.current = settingsResponse.languages;

        // merge defaults with what we receive, which might only be non-default values
        const userConfig = _.merge(defaultSettings, settingsResponse.userSettings);
        translationsRef.current = response[1].data;

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
    setLoading(true);
    axios.post('/configuration-api/settings', values)
      .then(function () {
        showNotification({
          title: 'Saved',
          message: 'Your configuration changes were saved successfully',
          onClick: () => { openGitHubNewIssue(); },
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
        <Button
          leftIcon={<Refresh />}
          onClick={restartServer}
        >
          {translationsRef.current['LooksFrame.12']}
        </Button>
        <Space h="lg" />
        <Tabs active={activeTab} onTabChange={setActiveTab}>
          <Tabs.Tab label={translationsRef.current['LooksFrame.TabGeneralSettings']}>
            <Select
              label={translationsRef.current['LanguageSelection.Language']}
              data={languageSettingsRef.current}
              {...form.getInputProps('language')}
            />

            <Group mt="xs">
              <TextInput
                label={translationsRef.current['NetworkTab.71']}
                name="server_name"
                sx={{ flex: 1 }}
                {...form.getInputProps('server_name')}
              />

              <Checkbox
                mt="xl"
                label={translationsRef.current['NetworkTab.72']}
                {...form.getInputProps('append_profile_name', { type: 'checkbox' })}
              />
            </Group>

            <Group mt="xs">
              <Checkbox
                mt="xl"
                label={translationsRef.current['NetworkTab.3']}
                {...form.getInputProps('minimized', { type: 'checkbox' })}
              />
              <Checkbox
                mt="xl"
                label={translationsRef.current['NetworkTab.74']}
                {...form.getInputProps('show_splash_screen', { type: 'checkbox' })}
              />
            </Group>
          </Tabs.Tab>
          <Tabs.Tab label={translationsRef.current['LooksFrame.TabNavigationSettings']}>

          </Tabs.Tab>
          <Tabs.Tab label={translationsRef.current['LooksFrame.TabSharedContent']}>
            
          </Tabs.Tab>
        </Tabs>

        <Group position="right" mt="md">
          <Button type="submit" loading={isLoading}>
            {translationsRef.current['LooksFrame.9']}
          </Button>
        </Group>
      </form>
    </Box>
  );
}
