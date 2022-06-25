import { Button, Box, Stack, Modal, Group, TextInput, Breadcrumbs, Paper } from '@mantine/core';
import { showNotification } from '@mantine/notifications';
import React, { useState } from 'react';
import axios from 'axios';
import { Devices2, Folder, Folders } from 'tabler-icons-react';

export default function DirectoryChooser(props: {
  path: string,
  callback: any,
  label: string,
  formKey: string,
}) {
  const [isLoading, setLoading] = useState(true);
  const [opened, setOpened] = useState(false);

  const [directories, setDirectories] = useState([] as { value: string, label: string }[]);
  const [parents, setParents] = useState([] as { value: string, label: string }[]);
  const [selectedDirectory, setSelectedDirectory] = useState('');
  const [separator, setSeparator] = useState('/');

  const openGitHubNewIssue = () => {
    window.location.href = 'https://github.com/UniversalMediaServer/UniversalMediaServer/issues/new';
  };

  const selectAndCloseModal = () => {
    if (selectedDirectory) {
      props.callback(props.formKey, selectedDirectory);
      return setOpened(false);
    }
    showNotification({
      color: 'red',
      title: 'Error',
      message: 'No directory was selected, please click on the far left to select.',
      autoClose: 3000,
    });
  };

  const getSubdirectories = (path: string) => {
    axios.post('/configuration-api/directories', {path:(path)?path:''})
      .then(function (response: any) {
        const directoriesResponse = response.data;
        setSeparator(directoriesResponse.separator);
        setDirectories(directoriesResponse.childrens);
        setParents(directoriesResponse.parents.reverse());
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
        setLoading(false);
      });
  };

  return (
    <Group mt="xs">
      <Modal
        opened={opened}
        onClose={() => setOpened(false)}
        title={
          <Group>
            <Folders />
            Selected directory:
          </Group>
        }
        overflow="inside"
      >
        <Box sx={{ maxWidth: 700 }} mx="auto">
          <Paper shadow="md" p="xs" withBorder>
            <Group>
              <Button
                loading={isLoading}
                onClick={() => getSubdirectories('roots')}
                variant="default"
                compact
              >
                <Devices2 />
              </Button>
              <Breadcrumbs separator={separator}>
                {parents.map(parent => (
                  <Button
                    loading={isLoading}
                    onClick={() => getSubdirectories(parent.value)}
                    key={"breadcrumb" + parent.label}
                    variant="default"
                    compact
                  >
                    {parent.label}
                  </Button>
                ))}
              </Breadcrumbs>
            </Group>
          </Paper>
          <Stack spacing="xs" align="flex-start" justify="flex-start" mt="xl">
            {directories.map(directory => (
              <Group key={"group" + directory.label}>
                <Button
                  leftIcon={<Folder size={18} />}
                  variant={(selectedDirectory === directory.value) ? "light" : "subtle"}
                  loading={isLoading}
                  onClick={() => setSelectedDirectory(directory.value)}
                  onDoubleClick={() => getSubdirectories(directory.value)}
                  key={directory.label}
                  compact
                >
                  {directory.label}
                </Button>
                {selectedDirectory === directory.value &&
                  <Button
                    variant="filled"
                    loading={isLoading}
                    onClick={() => selectAndCloseModal()}
                    key={"select" + directory.label}
                    compact
                  >
                    Select
                  </Button>
                }
              </Group>
            ))}
          </Stack>
        </Box>
      </Modal>

      <TextInput
        label={props.label}
        sx={{ flex: 1 }}
        value={props.path}
        readOnly
      />
      <Button
        mt="xl"
        onClick={() => { getSubdirectories(props.path); setOpened(true); }}
        leftIcon={<Folders size={18} />}
      >
        ...
      </Button>
    </Group>
  );
}
