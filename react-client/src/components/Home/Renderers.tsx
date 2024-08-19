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
import { ActionIcon, Button, Card, Drawer, Grid, Group, Image, Menu, Modal, Progress, ScrollArea, Select, Slider, Stack, Table, Text } from '@mantine/core';
import axios from 'axios';
import _ from 'lodash';
import { useEffect, useState } from 'react';
import { Cast, DevicesPc, DevicesPcOff, Dots, Link, ListDetails, PlayerPause, PlayerPlay, PlayerSkipBack, PlayerSkipForward, PlayerStop, PlayerTrackNext, PlayerTrackPrev, ScreenShare, Settings, Volume, VolumeOff } from 'tabler-icons-react';

import { I18nInterface } from '../../contexts/i18n-context';
import { renderersApiUrl } from '../../utils';
import { Renderer, User } from './Home';
import MediaChooser, { Media } from './MediaChooser';

const Renderers = (
  { allowed, blockedByDefault, canControlRenderers, canModify, i18n, renderers, users, setAllowed, setUserId }:
    {
      allowed: boolean,
      blockedByDefault: boolean,
      canControlRenderers: boolean,
      canModify: boolean,
      i18n: I18nInterface,
      renderers: Renderer[],
      users: User[],
      setAllowed: (rule: string, isAllowed: boolean) => void
      setUserId: (rule: string, userId: any) => void
    }
) => {

  const [askInfos, setAskInfos] = useState(-1);
  const [infos, setInfos] = useState(null as RendererInfos | null);
  const [controlId, setControlId] = useState(-1);
  const [controlMedia, setControlMedia] = useState<Media | null>(null);
  const [userChanger, setUserChanger] = useState<Renderer | null>();
  const [userChangerValue, setUserChangerValue] = useState<string | null>(null);

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

  const sendRendererControl = (id: number, action: string, value?: any) => {
    axios.post(renderersApiUrl + 'control', { 'id': id, 'action': action, 'value': value })
  }

  const getRenderer = (id: number) => {
    return renderers.find((renderer) => renderer.id === id);
  }

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
          <Table.Tr key={detail.key}>
            <Table.Td>{i18n.getI18nString(detail.key)}</Table.Td>
            <Table.Td>{detail.value}</Table.Td>
          </Table.Tr>
        ))}
      </tbody></Table>
    </Modal>
  );

  const getAccountNameList = () => {
    return [{ value: '-1', label: i18n.get('NoAccountAssigned') },
    { value: '0', label: i18n.get('DefaultAccount') }
    ].concat(users.map(user => ({ value: user.value.toString(), label: user.label })));
  }

  const rendererUserChanger = (
    <Modal
      centered
      opened={userChanger != null}
      title={userChanger?.name == 'UnknownRenderer' ? i18n.get('UnknownRenderer') : userChanger?.name}
      onClose={() => setUserChanger(null)}
      withinPortal={false}
      lockScroll={false}
    >
      <Select
        mb={'xl'}
        label={i18n.get('LinkRendererTo')}
        defaultValue={userChanger?.userId.toString()}
        onChange={(value) => { setUserChangerValue(value) }}
        withScrollArea={false}
        styles={{ dropdown: { maxHeight: 70, overflowY: 'auto' } }}
        data={getAccountNameList()}
      />
      <Group justify='flex-end' mt='md'>
        <Button
          disabled={!userChangerValue || userChangerValue == userChanger?.userId.toString()}
          onClick={() => {
            if (userChanger) {
              setUserId(userChanger.uuid, userChangerValue);
            }
            setUserChanger(null)
          }}
        >
          {i18n.get('Apply')}
        </Button>
      </Group>
    </Modal>
  );

  const getAccountName = (userId: number) => {
    switch (userId) {
      case -1: return i18n.get('NoAccountAssigned');
      case 0: return i18n.get('DefaultAccount');
      default: {
        const userFound = users.find((user) => user.value === userId);
        return userFound ? userFound.label : i18n.get('NonExistentUser')
      }
    }
  }

  const getNameColor = (renderer: Renderer) => {
    if (!renderer.isAllowed) {
      return 'red';
    } else if (!renderer.isActive) {
      return 'dimmed';
    } else if (renderer.state.playback > 0) {
      return 'green';
    }
    return '';
  }

  const renderersCards = renderers.map((renderer: Renderer) => allowed == renderer.isAllowed && (
    <Grid.Col span={{ base: 12, xs: 6 }} key={renderer.id}>
      <Card shadow='sm' p='lg' radius='md' withBorder>
        <Card.Section withBorder inheritPadding py='xs'>
          <Group justify='space-between'>
            <Text fw={500} c={getNameColor(renderer)}>{renderer.name == 'UnknownRenderer' ? i18n.get('UnknownRenderer') : renderer.name}</Text>
            <Menu withinPortal position='bottom-end' shadow='sm'>
              <Menu.Target>
                <ActionIcon>
                  <Dots size={16} />
                </ActionIcon>
              </Menu.Target>
              <Menu.Dropdown>
                <Menu.Item leftSection={<ListDetails size={14} />} onClick={() => setAskInfos(renderer.id)}>{i18n.get('Info')}</Menu.Item>
                {canModify && (<>
                  <Menu.Item leftSection={<Settings size={14} />} color='red' disabled={true /* not implemented yet */}>{i18n.get('Settings')}</Menu.Item>
                  {!renderer.isAuthenticated && renderer.uuid && (
                    <Menu.Item
                      leftSection={<Link size={14} />}
                      onClick={() => setUserChanger(renderer)}
                    >
                      {getAccountName(renderer.userId)}
                    </Menu.Item>
                  )}
                  {!renderer.isAuthenticated && !renderer.isAllowed && renderer.uuid && (
                    <Menu.Item leftSection={<DevicesPc size={14} />} onClick={() => setAllowed(renderer.uuid, true)} color='green'>{i18n.get('Allow')}</Menu.Item>
                  )}
                  {!renderer.isAuthenticated && renderer.isAllowed && renderer.uuid && (
                    <Menu.Item leftSection={<DevicesPcOff size={14} />} onClick={() => setAllowed(renderer.uuid, false)} color='red'>{i18n.get('Block')}</Menu.Item>
                  )}
                </>)}
                {canControlRenderers && (
                  <Menu.Item leftSection={<ScreenShare size={14} />} disabled={!renderer.isActive || renderer.controls < 1} onClick={() => setControlId(renderer.id)}>{i18n.get('Controls')}</Menu.Item>
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
            style={!renderer.isActive ? { filter: 'grayscale(95%)' } : undefined}
            alt={renderer.name}
          />
        </Card.Section>
        {renderer.address &&
          <Text ta='center' size='sm' c='dimmed'>
            {renderer.address}
          </Text>
        }
        {renderer.playing &&
          <Progress value={renderer.progressPercent} />
        }
        {renderer.playing &&
          <Text ta='center' size='sm' c='dimmed'>
            {renderer.playing}
          </Text>
        }
        {renderer.time &&
          <Text ta='center' size='sm' c='dimmed'>
            {renderer.time}
          </Text>
        }
      </Card>
    </Grid.Col>
  ));

  const rendererControlled = getRenderer(controlId);

  const rendererControls = (rendererControlled !== undefined && (
    <Drawer
      size='full'
      opened={controlId > -1}
      onClose={() => setControlId(-1)}
      title={rendererControlled.name == 'UnknownRenderer' ? i18n.get('UnknownRenderer') : rendererControlled.name}
    >
      <Stack>
        {!rendererControlled.isActive &&
          <Text ta='center' c='red'>{i18n.get('RendererNoLongerControllable')}</Text>
        }
        {rendererControlled.isActive && rendererControlled.playing && (<>
          <Text ta='center' c='blue'>{rendererControlled.playing}</Text>
          <Text ta='center'>{rendererControlled.time}</Text>
        </>)}
        {((rendererControlled.controls & 1) === 1) && rendererControlled.isActive &&
          <Group gap='xs' grow mt='md'>
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
          <Group justify='center'>
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
        {rendererControlled.isActive && (<Group justify='center'>
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

  const renderersHeader = (!allowed && (
    <Card shadow='sm' p='lg' radius='md' mb='lg' withBorder>
      <Card.Section withBorder inheritPadding py='xs'>
        <Group justify='space-between'>
          <Text fw={500} c={blockedByDefault ? 'red' : 'green'}>{blockedByDefault ? i18n.get('RenderersBlockedByDefault') : i18n.get('RenderersAllowedByDefault')}</Text>
          {canModify && (
            <Menu withinPortal position='bottom-end' shadow='sm'>
              <Menu.Target>
                <ActionIcon>
                  <Dots size={16} />
                </ActionIcon>
              </Menu.Target>
              <Menu.Dropdown>
                <>
                  {blockedByDefault ? (
                    <Menu.Item leftSection={<DevicesPc size={14} />} onClick={() => setAllowed('DEFAULT', true)} color='green'>{i18n.get('AllowByDefault')}</Menu.Item>
                  ) : (
                    <Menu.Item leftSection={<DevicesPcOff size={14} />} onClick={() => setAllowed('DEFAULT', false)} color='red'>{i18n.get('BlockByDefault')}</Menu.Item>
                  )}
                </>
              </Menu.Dropdown>
            </Menu>
          )}
        </Group>
      </Card.Section>
    </Card>
  ));

  return (
    <>
      {rendererDetail}
      {rendererUserChanger}
      {rendererControls}
      {renderersHeader}
      <Grid>
        {renderersCards}
      </Grid>
    </>
  );
};

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
