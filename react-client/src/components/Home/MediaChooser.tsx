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
import { Box, Breadcrumbs, Button, Group, MantineSize, Modal, Paper, ScrollArea, Stack, TextInput, Tooltip } from '@mantine/core';
import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import { useContext, useState, ReactNode } from 'react';
import { Folder, Home, PictureInPicture, PictureInPictureOn } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import { openGitHubNewIssue, renderersApiUrl } from '../../utils';

export default function MediaChooser(props: {
  tooltipText: string,
  id: number,
  media: Media | null,
  callback: any,
  label?: string,
  disabled?: boolean,
  formKey?: string,
  size?: MantineSize,
}) {
  const [isLoading, setLoading] = useState(true);
  const [opened, setOpened] = useState(false);
  const i18n = useContext(I18nContext);

  const [medias, setMedias] = useState<Media[]>([]);
  const [parents, setParents] = useState([] as { value: string, label: string }[]);
  const [selectedMedia, setSelectedMedia] = useState<Media | null>(null);

  const selectAndCloseModal = () => {
    if (selectedMedia) {
      if (props.formKey) {
        props.callback(props.formKey, selectedMedia);
      } else {
        props.callback(selectedMedia);
      }
      return setOpened(false);
    }
    showNotification({
      color: 'red',
      title: i18n.get['Error'],
      message: i18n.get['NoMediaSelected'],
      autoClose: 3000,
    });
  };

  const getMedias = (media: string) => {
    axios.post(renderersApiUrl + 'browse', { id: props.id, media: media ? media : '0' })
      .then(function(response: any) {
        const mediasResponse = response.data;
        setMedias(mediasResponse.childrens);
        setParents(mediasResponse.parents.reverse());
      })
      .catch(function() {
        showNotification({
          id: 'data-loading',
          color: 'red',
          title: i18n.get['Error'],
          message: i18n.get['DataNotReceived'],
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
      value={props.media ? props.media.label : ''}
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
              <PictureInPictureOn />
              {i18n.get['SelectedMedia']}
            </Group>
          }
          scrollAreaComponent={ScrollArea.Autosize}
          size='lg'
        >
          <Box mx='auto'>
            <Paper shadow='md' p='xs' withBorder>
              <Group>
                <Breadcrumbs>
                  <Button
                    loading={isLoading}
                    onClick={() => getMedias('0')}
                    variant='default'
                    compact
                  >
                    <Home />
                  </Button>
                  {parents.map(parent => (
                    <Button
                      loading={isLoading}
                      onClick={() => getMedias(parent.value)}
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
              {medias.map(media => (
                <Group key={'group' + media.label}>
                  <Button
                    leftIcon={media.browsable ? <Folder size={18} /> : <PictureInPicture size={18} />}
                    variant={(selectedMedia?.value === media.value) ? 'light' : 'subtle'}
                    loading={isLoading}
                    onClick={() => media.browsable ? getMedias(media.value) : setSelectedMedia(media)}
                    key={media.label}
                    compact
                  >
                    {media.label}
                  </Button>
                  {selectedMedia?.value === media.value &&
                    <Button
                      variant='filled'
                      loading={isLoading}
                      onClick={() => selectAndCloseModal()}
                      key={'select' + media.label}
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
          <Button
            mt={props.label ? '24px' : undefined}
            size={props.size}
            onClick={() => { getMedias(props.media ? props.media.value : '0'); setOpened(true); }}
            leftIcon={<PictureInPictureOn size={18} />}
          >
            ...
          </Button>
        )}
      </>
    </Group>
  );
}

export interface Media {
  value: string,
  label: string,
  browsable: boolean
}

MediaChooser.defaultProps = {
  tooltipText: null,
}
