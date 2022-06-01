import { TextInput, Checkbox, Button, Group, Box } from '@mantine/core';
import { useForm } from '@mantine/form';
import { showNotification } from '@mantine/notifications';
import _ from 'lodash';
import { useEffect, useState } from "react";
const axios = require('axios').default;

export default function Settings() {
  const initialValues = {
    server_name: 'Universal Media Server',
    append_profile_name: false,
  };

  const openGitHubNewIssue = () => {
    window.location.href = 'https://github.com/UniversalMediaServer/UniversalMediaServer/issues/new';
  };

  const [configuration, setConfiguration] = useState(initialValues);

  const form = useForm({ initialValues });

  // Code here will run just like componentDidMount
  useEffect(() => {
    axios.get('/configuration-api/')
      .then(function (response: { data: typeof initialValues }) {
        // merge defaults with what we receive, which are ONLY non-default values
        const userConfig = _.merge(initialValues, response.data);
        setConfiguration(userConfig);
        form.setValues(configuration);
      })
      .catch(function (error: Error) {
        // handle error
        console.log(error);
      })
      .then(function () {
        // always executed
        form.validate();
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSubmit = (values: typeof form.values) => {
    axios.post('/configuration-api/', values)
      .then(function (response: { data: typeof initialValues }) {
        // merge defaults with what we receive, which are ONLY non-default values
        const userConfig = _.merge(initialValues, response.data);
        setConfiguration(userConfig);
        form.setValues(configuration);
        showNotification({
          title: 'Saved',
          message: 'Your configuration changes were saved successfully',
          onClick: () => { openGitHubNewIssue(); },
        })
      })
      .catch(function (error: Error) {
        showNotification({
          title: 'Error',
          message: 'Your configuration changes were not saved. Please click here to report the bug to us.',
          onClick: () => { openGitHubNewIssue(); },
        })
      })
      .then(function () {
        // always executed
        form.validate();
      });
  };

  return (
    <Box sx={{ maxWidth: 300 }} mx="auto">
      <form onSubmit={form.onSubmit(handleSubmit)}>
        <TextInput
          required
          label="Server name"
          name="server_name"
          {...form.getInputProps('server_name')}
        />

        <Checkbox
          mt="md"
          label="Append profile name"
          {...form.getInputProps('append_profile_name', { type: 'checkbox' })}
        />

        <Group position="right" mt="md">
          <Button type="submit">Submit</Button>
        </Group>
      </form>
    </Box>
  );
}
