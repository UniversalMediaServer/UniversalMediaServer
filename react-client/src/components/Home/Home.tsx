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
import { Box, Tabs } from '@mantine/core';
import { showNotification, updateNotification } from '@mantine/notifications';
import axios from 'axios';
import _ from 'lodash';
import { useContext, useEffect, useState } from 'react';

import I18nContext from '../../contexts/i18n-context';
import SessionContext from '../../contexts/session-context';
import ServerEventContext from '../../contexts/server-event-context';
import Renderers from './Renderers';
import NetworkDevices from './NetworkDevices';
import { renderersApiUrl } from '../../utils';
import { havePermission, Permissions } from '../../services/accounts-service';
import { Check, ExclamationMark } from 'tabler-icons-react';

const Home = () => {
  const i18n = useContext(I18nContext);
  const sse = useContext(ServerEventContext);
  const [renderers, setRenderers] = useState([] as Renderer[]);
  const [renderersBlockedByDefault, setRenderersBlockedByDefault] = useState(false);
  const [networkDeviceFilters, setNetworkDeviceFilters] = useState([] as NetworkDevicesFilter[]);
  const [networkDevicesBlockedByDefault, setNetworkDevicesBlockedByDefault] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const session = useContext(SessionContext);
  const canModify = havePermission(session, Permissions.settings_modify);
  const canControlRenderers = havePermission(session, Permissions.devices_control);

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
    refreshData();
  }, [i18n]);

  const refreshData = async () => {
    axios.get(renderersApiUrl)
      .then(function(response: any) {
        setRenderers(response.data.renderers);
        setRenderersBlockedByDefault(response.data.renderersBlockedByDefault);
        setNetworkDeviceFilters(response.data.networkDevices);
        setNetworkDevicesBlockedByDefault(response.data.networkDevicesBlockedByDefault);
        setCurrentTime(response.data.currentTime);
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
  }

  const setRenderersAllowed = async (rule: string, isAllowed: boolean) => {
    setAllowed('renderers', rule, isAllowed);
  }

  const setDevicesAllowed = async (rule: string, isAllowed: boolean) => {
    setAllowed('devices', rule, isAllowed);
  };

  const setAllowed = async (endpoint:string, rule: string, isAllowed: boolean) => {
    showNotification({
      id: 'settings-save',
      loading: true,
      title: i18n.get['Save'],
      message: i18n.get['SavingConfiguration'],
      autoClose: false,
      withCloseButton: false
    });
    await axios.post(renderersApiUrl + endpoint, { rule, isAllowed })
      .then(function() {
        updateNotification({
          id: 'settings-save',
          color: 'teal',
          title: i18n.get['Saved'],
          message: i18n.get['ConfigurationSaved'],
          icon: <Check size='1rem' />
        });
        refreshData();
      })
      .catch(function(error) {
        if (!error.response && error.request) {
          updateNotification({
            id: 'settings-save',
            color: 'red',
            title: i18n.get['Error'],
            message: i18n.get['ConfigurationNotReceived'],
            icon: <ExclamationMark size='1rem' />
          })
        } else {
          updateNotification({
            id: 'settings-save',
            color: 'red',
            title: i18n.get['Error'],
            message: i18n.get['ConfigurationNotSaved']
          })
        }
      });
  };

  return (
    <Box sx={{ maxWidth: 1024 }} mx='auto'>
      <Tabs keepMounted={false} defaultValue='renderers'>
        <Tabs.List>
          <Tabs.Tab value='renderers'>{i18n.get['DetectedMediaRenderers']}</Tabs.Tab>
          <Tabs.Tab value='blocked_renderers'>{i18n.get['BlockedMediaRenderers']}</Tabs.Tab>
          <Tabs.Tab value='network_devices'>{i18n.get['NetworkDevices']}</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value='renderers' pt='xs'>
          <Renderers
            allowed={true}
            blockedByDefault={renderersBlockedByDefault}
            canControlRenderers={canControlRenderers}
            canModify={canModify}
            i18n={i18n}
            renderers={renderers}
            setAllowed={setRenderersAllowed}
          />
        </Tabs.Panel>
        <Tabs.Panel value='blocked_renderers' pt='xs'>
          <Renderers
            allowed={false}
            blockedByDefault={renderersBlockedByDefault}
            canControlRenderers={canControlRenderers}
            canModify={canModify}
            i18n={i18n}
            renderers={renderers}
            setAllowed={setRenderersAllowed}
          />
        </Tabs.Panel>
        <Tabs.Panel value='network_devices' pt='xs'>
          <NetworkDevices
            blockedByDefault={networkDevicesBlockedByDefault}
            canModify={canModify}
            currentTime={currentTime}
            i18n={i18n}
            networkDeviceFilters={networkDeviceFilters}
            refreshData={refreshData}
            setAllowed={setDevicesAllowed}
          />
        </Tabs.Panel>
      </Tabs>
    </Box>
  );
};

interface RendererAction extends Renderer {
  action: string,
}

export interface RendererState {
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

export interface Renderer {
  id: number,
  name: string,
  address: string,
  uuid: string,
  icon: string,
  playing: string,
  time: string,
  progressPercent: number,
  isActive: boolean,
  isAllowed: boolean,
  controls: number,
  state: RendererState,
}

export interface NetworkDevice {
  hostName: string,
  ipAddress: string,
  lastSeen: number
}

export interface NetworkDevicesFilter {
  name: string,
  isAllowed: boolean,
  isDefault: boolean,
  devices: NetworkDevice[]
}
export default Home;
