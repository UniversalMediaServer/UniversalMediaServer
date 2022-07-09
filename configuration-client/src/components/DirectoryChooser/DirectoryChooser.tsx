import { Button, Box, Stack, Modal, Group, TextInput, Breadcrumbs, Paper, Tooltip } from '@mantine/core';
import { showNotification } from '@mantine/notifications';
import React, { useContext, useState, ReactNode } from 'react';
import axios from 'axios';
import { Devices2, Folder, Folders } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import { openGitHubNewIssue } from '../../utils';

export default function DirectoryChooser(props: {
  tooltipText: string,
  path: string,
  callback: any,
  label: string,
  formKey: string,
}) {
  const [isLoading, setLoading] = useState(true);
  const [opened, setOpened] = useState(false);
  const i18n = useContext(I18nContext);

  const [directories, setDirectories] = useState([] as { value: string, label: string }[]);
  const [parents, setParents] = useState([] as { value: string, label: string }[]);
  const [selectedDirectory, setSelectedDirectory] = useState('');
  const [separator, setSeparator] = useState('/');

  const selectAndCloseModal = () => {
    if (selectedDirectory) {
      props.callback(props.formKey, selectedDirectory);
      return setOpened(false);
    }
    showNotification({
      color: 'red',
      title: i18n.get['Error'],
      message: i18n.get['WebGui.DirectoryChooserSelectError'],
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
          title: i18n.get['Error'],
          message: i18n.get['WebGui.DirectoryChooserGetError'],
          onClick: () => { openGitHubNewIssue(); },
          autoClose: 3000,
        });
      })
      .then(function () {
        setLoading(false);
      });
  };

  const input = (): ReactNode => {
   return <TextInput
      label={props.label}
      sx={{ flex: 1 }}
      value={props.path}
      readOnly
    /> }

  return (
    <Group mt="xs">
      <>
        <Modal
          opened={opened}
          onClose={() => setOpened(false)}
          title={
            <Group>
              <Folders />
              {i18n.get['WebGui.DirectoryChooserSelectedDirectory']}
            </Group>
          }
          overflow="inside"
          size="lg"
        >
          <Box mx="auto">
            <Paper shadow="md" p="xs" withBorder>
              <Group>
                <Breadcrumbs separator={separator}>
                  <Button
                    loading={isLoading}
                    onClick={() => getSubdirectories('roots')}
                    variant="default"
                    compact
                  >
                    <Devices2 />
                  </Button>
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

        {props.tooltipText ? (<Tooltip label={props.tooltipText} width={350} color={'blue'} wrapLines={true} withArrow={true}>
          {input()}
        </Tooltip>) : input()
        }
        <Button
          mt="xl"
          onClick={() => { getSubdirectories(props.path); setOpened(true); }}
          leftIcon={<Folders size={18} />}
        >
          ...
        </Button>
      </>
    </Group>
  );
}

DirectoryChooser.defaultProps = {
  tooltipText: null,
}
