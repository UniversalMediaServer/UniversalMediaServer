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
import { ActionIcon, Card, Drawer, Grid, Group, Image, Menu, Modal, Progress, ScrollArea, Slider, Stack, Table, Text } from '@mantine/core';
import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import _ from 'lodash';
import { useContext, useEffect, useState } from 'react';
import { Cast, Dots, ListDetails, PlayerPause, PlayerPlay, PlayerSkipBack, PlayerSkipForward, PlayerStop, PlayerTrackNext, PlayerTrackPrev, ScreenShare, Settings, Volume, VolumeOff } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import ServerEventContext from '../../contexts/server-event-context';
import SessionContext from '../../contexts/session-context';
import { havePermission, Permissions } from '../../services/accounts-service';
import { renderersApiUrl } from '../../utils';
import MediaChooser, { Media } from './MediaChooser';

const Renderers = () => {
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
  const sse = useContext(ServerEventContext);
  const canModify = havePermission(session, Permissions.settings_modify);
  const canControlRenderers = havePermission(session, Permissions.devices_control);
  const [renderers, setRenderers] = useState([] as Renderer[]);
  const [askInfos, setAskInfos] = useState(-1);
  const [infos, setInfos] = useState(null as RendererInfos | null);
  const [controlId, setControlId] = useState(-1);
  const [controlMedia, setControlMedia] = useState<Media | null>(null);

  useEffect(() => {
    axios.get(renderersApiUrl)
      .then(function(response: any) {
        setRenderers(response.data.renderers);
      })
      .catch(function() {
        showNotification({
          id: 'renderers-data-loading',
          color: 'red',
          title: i18n.get['Error'],
          message: i18n.get['DataNotReceived'],
          autoClose: 3000,
        });
      });
  }, [i18n]);

  useEffect(() => {
    if (!sse.hasRendererAction) {
      return;
    }
    const renderersTemp = _.cloneDeep(renderers);
    while (sse.hasRendererAction) {
      const rendererAction = sse.getRendererAction() as RendererAction;
      if (rendererAction === null) {
        break;
      }
      switch (rendererAction.action) {
        case 'renderer_add': {
          renderersTemp.push(rendererAction);
          break;
        }
        case 'renderer_delete': {
          const delIndex = renderersTemp.findIndex(renderer => renderer.id === rendererAction.id);
          if (delIndex > -1) {
            renderersTemp.splice(delIndex, 1);
          }
          break;
        }
        case 'renderer_update': {
          const index = renderersTemp.findIndex(renderer => renderer.id === rendererAction.id);
          if (index > -1) {
            renderersTemp[index] = rendererAction;
          }
          break;
        }
      }
    }
    setRenderers(renderersTemp);
  }, [renderers, sse]);

  useEffect(() => {
    if (askInfos < 0) {
      setInfos(null);
      return;
    }
    axios.post(renderersApiUrl + 'infos', { 'id': askInfos })
      .then(function(response: any) {
        setInfos(response.data);
      })
      .catch(function() {
        setInfos(null);
      });
  }, [askInfos]);

  const renderersCards = renderers.map((renderer: Renderer) => (
    <Grid.Col span={12} xs={6} key={renderer.id}>
      <Card shadow='sm' p='lg' radius='md' withBorder>
        <Card.Section withBorder inheritPadding py='xs'>
          <Group position='apart'>
            <Text weight={500} color={!renderer.isActive ? 'dimmed' : renderer.playing ? 'green' : ''}>{renderer.name}</Text>
            <Menu withinPortal position='bottom-end' shadow='sm'>
              <Menu.Target>
                <ActionIcon>
                  <Dots size={16} />
                </ActionIcon>
              </Menu.Target>
              <Menu.Dropdown>
                <Menu.Item icon={<ListDetails size={14} />} onClick={() => setAskInfos(renderer.id)}>{i18n.get['Info']}</Menu.Item>
                {canModify && (
                  <Menu.Item icon={<Settings size={14} />} color='red' disabled={true /* not implemented yet */}>{i18n.get['Settings']}</Menu.Item>
                )}
                {canControlRenderers && (
                  <Menu.Item icon={<ScreenShare size={14} />} disabled={!renderer.isActive || renderer.controls < 1} onClick={() => setControlId(renderer.id)}>{i18n.get['Controls']}</Menu.Item>
                )}
              </Menu.Dropdown>
            </Menu>
          </Group>
        </Card.Section>
        <Card.Section>
          <Image
            src={renderersApiUrl + 'icon/' + renderer.id + '/' + renderer.icon}
            height={160}
            fit='contain'
            sx={!renderer.isActive ? { filter: 'grayscale(95%)' } : undefined}
            alt={renderer.name}
          />
        </Card.Section>
        {renderer.address &&
          <Text align='center' size='sm' color='dimmed'>
            {renderer.address}
          </Text>
        }
        {renderer.playing &&
          <Progress value={renderer.progressPercent} />
        }
        {renderer.playing &&
          <Text align='center' size='sm' color='dimmed'>
            {renderer.playing}
          </Text>
        }
        {renderer.time &&
          <Text align='center' size='sm' color='dimmed'>
            {renderer.time}
          </Text>
        }
      </Card>
    </Grid.Col>
  ));

  const rendererDetail = (
    <Modal
      centered
      scrollAreaComponent={ScrollArea.Autosize}
      opened={infos != null}
      onClose={() => setAskInfos(-1)}
      title={infos?.title}
    >
      <Table><tbody>
        {infos?.details.map((detail: RendererDetail) => (
          <tr key={detail.key}>
            <td>{i18n.getI18nString(detail.key)}</td>
            <td>{detail.value}</td>
          </tr>
        ))}
      </tbody></Table>
    </Modal>
  );

  const sendRendererControl = (id: number, action: string, value?: any) => {
    axios.post(renderersApiUrl + 'control', { 'id': id, 'action': action, 'value': value })
  }

  const getRenderer = (id: number) => {
    return renderers.find((renderer) => renderer.id === id);
  }

  const rendererControlled = getRenderer(controlId);

  const rendererControls = (rendererControlled !== undefined && (
    <Drawer
      size='full'
      opened={controlId > -1}
      onClose={() => setControlId(-1)}
      title={rendererControlled.name}
    >
      <Stack>
        {!rendererControlled.isActive &&
          <Text align='center' color='red'>{i18n.get['RendererNoLongerControllable']}</Text>
        }
        {rendererControlled.isActive && rendererControlled.playing && (<>
          <Text align='center' color='blue'>{rendererControlled.playing}</Text>
          <Text align='center'>{rendererControlled.time}</Text>
        </>)}
        {((rendererControlled.controls & 1) === 1) && rendererControlled.isActive &&
          <Group spacing='xs' grow mt='md'>
            <ActionIcon style={{ flexGrow: 'unset' }} variant='filled' onClick={() => sendRendererControl(rendererControlled.id, 'mute')}>
              {rendererControlled.state.mute ?
                <VolumeOff />
                :
                <Volume />
              }
            </ActionIcon>
            <Slider labelAlwaysOn mt='lg' style={{ maxWidth: 'unset', marginInlineEnd: '10px' }}
              defaultValue={rendererControlled.state.volume}
              onChangeEnd={(value: number) => sendRendererControl(rendererControlled.id, 'volume', value)}
            />
          </Group>
        }
        {((rendererControlled.controls & 2) === 2) && rendererControlled.isActive &&
          <Group position='center'>
            <ActionIcon variant='filled' onClick={() => sendRendererControl(rendererControlled.id, 'back')}><PlayerSkipBack /></ActionIcon>
            <ActionIcon variant='filled' onClick={() => sendRendererControl(rendererControlled.id, 'prev')}><PlayerTrackPrev /></ActionIcon>
            {rendererControlled.state.playback === 1 ?
              <ActionIcon variant='filled' onClick={() => sendRendererControl(rendererControlled.id, 'pause')}><PlayerPause /></ActionIcon>
              :
              <ActionIcon variant='filled' onClick={() => sendRendererControl(rendererControlled.id, 'play')}><PlayerPlay /></ActionIcon>
            }
            <ActionIcon variant='filled' onClick={() => sendRendererControl(rendererControlled.id, 'stop')}><PlayerStop /></ActionIcon>
            <ActionIcon variant='filled' onClick={() => sendRendererControl(rendererControlled.id, 'next')}><PlayerTrackNext /></ActionIcon>
            <ActionIcon variant='filled' onClick={() => sendRendererControl(rendererControlled.id, 'forward')}><PlayerSkipForward /></ActionIcon>
          </Group>
        }
        {rendererControlled.isActive && (<Group position='center'>
          <MediaChooser
            disabled={!canModify}
            size='xs'
            id={rendererControlled.id}
            media={controlMedia}
            callback={(value: Media | null) => setControlMedia(value)}
          ></MediaChooser>
          {controlMedia && <ActionIcon onClick={() => sendRendererControl(rendererControlled.id, 'mediaid', controlMedia.value)}><Cast /></ActionIcon>}
        </Group>)}
      </Stack>
    </Drawer>
  ));

  return (
    <>
      {rendererDetail}
      {rendererControls}
      <Grid>
        {renderersCards}
      </Grid>
    </>
  );
};

interface RendererAction extends Renderer {
  action: string,
}

interface RendererState {
  mute: boolean,
  volume: number,
  playback: number,
  name: string,
  uri: string,
  metadata: string,
  position: string,
  duration: string,
  buffer: number,
}

interface Renderer {
  id: number,
  name: string,
  address: string,
  icon: string,
  playing: string,
  time: string,
  progressPercent: number,
  isActive: boolean,
  controls: number,
  state: RendererState,
}

interface RendererInfos {
  title: string,
  isUpnp: boolean,
  details: RendererDetail[],
}

interface RendererDetail {
  key: string,
  value: string
}

export default Renderers;
