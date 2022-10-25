import { Box, Button, Group, Tabs, Text } from '@mantine/core';
import { useForm } from '@mantine/form';
import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import _ from 'lodash';
import { useContext, useEffect, useState } from 'react';
import I18nContext from '../../contexts/i18n-context';
import ServerEventContext from '../../contexts/server-event-context';
import SessionContext from '../../contexts/session-context';
import { havePermission, Permissions } from '../../services/accounts-service';
import {openGitHubNewIssue, settingsApiUrl} from '../../utils';
import SharedContentSettings from './SharedContentSettings';

export default function SharedContent() {
  const [isLoading, setLoading] = useState(true);
  const [configuration, setConfiguration] = useState({} as any);

  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
  const sse = useContext(ServerEventContext);
  const form = useForm({ initialValues: {} as Record<string, unknown> });
  const formSetValues = form.setValues;

  const canModify = havePermission(session, Permissions.settings_modify);
  const canView = canModify || havePermission(session, Permissions.settings_view);

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
    canView && axios.get(settingsApiUrl)
      .then(function (response: any) {
        const settingsResponse = response.data;

        // merge defaults with what we receive, which might only be non-default values
        const userConfig = _.merge({}, settingsResponse.userSettingsDefaults, settingsResponse.userSettings);

        setConfiguration(userConfig);
        formSetValues(userConfig);
      })
      .catch(function () {
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
  }, [canView, formSetValues]);

  const handleSubmit = async(values: typeof form.values) => {
    setLoading(true);
    try {
      const changedValues: Record<string, any> = {};
  
      // construct an object of only changed values to send
      for (const key in values) {
        if (!_.isEqual(configuration[key], values[key])) {
          changedValues[key] = values[key]?values[key]:null;
        }
      }

      if (_.isEmpty(changedValues)) {
        showNotification({
          title: i18n.get['Saved'],
          message: i18n.get['ConfigurationHasNoChanges'],
        })
      } else {
        await axios.post(settingsApiUrl, changedValues);
        setConfiguration(values);
        setLoading(false);
        showNotification({
          title: i18n.get['Saved'],
          message: i18n.get['ConfigurationSaved'],
        })
      }
    } catch (err) {
      showNotification({
        color: 'red',
        title: i18n.get['Error'],
        message: i18n.get['ConfigurationNotSaved'] + ' ' + i18n.get['ClickHereReportBug'],
        onClick: () => { openGitHubNewIssue(); },
      })
    }

    setLoading(false);
  };

  return canView ? (
    <Box sx={{ maxWidth: 1024 }} mx="auto">
      <form onSubmit={form.onSubmit(handleSubmit)}>
        <Tabs defaultValue="SharedContent">
          <Tabs.List>
            <Tabs.Tab value="SharedContent">{i18n.get['SharedContent']}</Tabs.Tab>
          </Tabs.List>
          <Tabs.Panel value="SharedContent">
            { SharedContentSettings(form,configuration) }
          </Tabs.Panel>
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
    <Box sx={{ maxWidth: 1024 }} mx="auto">
      <Text color="red">{i18n.get['YouNotHaveAccessArea']}</Text>
    </Box>
  );
}

export interface mantineSelectData {
  value: string;
  label: string;
}
