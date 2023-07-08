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
import { ActionIcon, Box, Breadcrumbs, Button, Group, MantineSize, Modal, Paper, ScrollArea, Stack, TextInput, Tooltip } from '@mantine/core';
import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import { useContext, useState, ReactNode } from 'react';
import { CircleMinus, Devices2, Folder, Folders } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import { openGitHubNewIssue, settingsApiUrl } from '../../utils';

export default function DirectoryChooser(props: {
  tooltipText: string,
  path: string,
  callback: any,
  label?: string,
  disabled?: boolean,
  formKey?: string,
  size?: MantineSize,
}) {
  const [isLoading, setLoading] = useState(true);
  const [opened, setOpened] = useState(false);
  const i18n = useContext(I18nContext);

  const [directories, setDirectories] = useState([] as { value: string, label: string }[]);
  const [parents, setParents] = useState([] as { value: string, label: string }[]);
  const [selectedDirectory, setSelectedDirectory] = useState('');
  const [separator, setSeparator] = useState('/');

  const selectAndCloseModal = (clear?: boolean) => {
    if (selectedDirectory || clear) {
      if (props.formKey) {
        props.callback(props.formKey, clear ? '' : selectedDirectory);
      } else {
        props.callback(clear ? '' : selectedDirectory);
      }
      return setOpened(false);
    }
    showNotification({
      color: 'red',
      title: i18n.get['Error'],
      message: i18n.get['NoDirectorySelected'],
      autoClose: 3000,
    });
  };

  const getSubdirectories = (path: string) => {
    axios.post(settingsApiUrl + 'directories', { path: (path) ? path : '' })
      .then(function(response: any) {
        const directoriesResponse = response.data;
        setSeparator(directoriesResponse.separator);
        setDirectories(directoriesResponse.children);
        setParents(directoriesResponse.parents.reverse());
      })
      .catch(function() {
        showNotification({
          id: 'data-loading',
          color: 'red',
          title: i18n.get['Error'],
          message: i18n.get['SubdirectoriesNotReceived'],
          onClick: () => { openGitHubNewIssue(); },
          autoClose: 3000,
        });
      })
      .then(function() {
        setLoading(false);
      });
  };

  const input = (): ReactNode => {
    return <TextInput
      size={props.size}
      label={props.label}
      disabled={props.disabled}
      sx={{ flex: 1 }}
      value={props.path}
      readOnly
    />
  }

  return (
    <Group>
      <>
        <Modal
          opened={opened}
          onClose={() => setOpened(false)}
          title={
            <Group>
              <Folders />
              {i18n.get['SelectedDirectory']}
            </Group>
          }
          scrollAreaComponent={ScrollArea.Autosize}
          size='lg'
        >
          <Box mx='auto'>
            <Paper shadow='md' p='xs' withBorder>
              <Group>
                <Breadcrumbs separator={separator}>
                  <Button
                    loading={isLoading}
                    onClick={() => getSubdirectories('roots')}
                    variant='default'
                    compact
                  >
                    <Devices2 />
                  </Button>
                  {parents.map(parent => (
                    <Button
                      loading={isLoading}
                      onClick={() => getSubdirectories(parent.value)}
                      key={'breadcrumb' + parent.label}
                      variant='default'
                      compact
                    >
                      {parent.label}
                    </Button>
                  ))}
                </Breadcrumbs>
              </Group>
            </Paper>
            <Stack spacing='xs' align='flex-start' justify='flex-start' mt='sm'>
              {directories.map(directory => (
                <Group key={'group' + directory.label}>
                  <Button
                    leftIcon={<Folder size={18} />}
                    variant={(selectedDirectory === directory.value) ? 'light' : 'subtle'}
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
                      variant='filled'
                      loading={isLoading}
                      onClick={() => selectAndCloseModal()}
                      key={'select' + directory.label}
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

        {props.tooltipText ? (<Tooltip label={props.tooltipText} width={350} color={'blue'} multiline withArrow={true}>
          {input()}
        </Tooltip>) : input()
        }
        {!props.disabled && (
          <>
            <Button
              mt={props.label ? '24px' : undefined}
              size={props.size}
              onClick={() => { getSubdirectories(props.path); setOpened(true); }}
              leftIcon={<Folders size={18} />}
            >
              ...
            </Button>
            <ActionIcon
              mt={props.label ? '24px' : undefined}
              size={props.size}
              onClick={() => selectAndCloseModal(true)}
              variant='default'
            >
              <CircleMinus size={18} />
            </ActionIcon>
          </>
        )}
      </>
    </Group>
  );
}

DirectoryChooser.defaultProps = {
  tooltipText: null,
}
