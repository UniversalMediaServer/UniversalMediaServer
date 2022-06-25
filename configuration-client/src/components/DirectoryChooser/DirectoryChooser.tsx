import { Button, Box, Stack, Modal, Group, TextInput, Breadcrumbs, Paper } from '@mantine/core';
import { showNotification } from '@mantine/notifications';
import React, { useState } from 'react';
import axios from 'axios';
import { Folder, Folders } from 'tabler-icons-react';

export default function DirectoryChooser(props: {
  path: string,
  callback: any,
  label: string,
  formKey: string,
}) {
  const [isLoading, setLoading] = useState(true);
  const [opened, setOpened] = useState(false);

  const [directories, setDirectories] = useState([] as { value: string, label: string }[]);
  const [children, setChildren] = useState([] as { value: string, label: string }[]);
  const [selectedDirectory, setSelectedDirectory] = useState('');

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
    let apiUri = '/configuration-api/directories';
    if (path) {
      apiUri += '?path=' + path;
    }
    axios.get(apiUri)
      .then(function (response: any) {
        const directoriesResponse = response.data;
        setDirectories(directoriesResponse.parents);
        setChildren(directoriesResponse.children.reverse());
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
            <Breadcrumbs>
              {children.map(child => (
                <Button
                  loading={isLoading}
                  onClick={() => getSubdirectories(child.value)}
                  key={"breadcrumb" + child.label}
                  variant="default"
                  compact
                >
                  {child.label}
                </Button>
              ))}
            </Breadcrumbs>
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
