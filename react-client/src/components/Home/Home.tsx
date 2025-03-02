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
import { Box, LoadingOverlay, Tabs, Text } from '@mantine/core';
import axios from 'axios';
import _ from 'lodash';
import { useEffect, useState } from 'react';
import { IconCheck, IconExclamationMark } from '@tabler/icons-react';

import Renderers from './Renderers';
import NetworkDevices from './NetworkDevices';
import { renderersApiUrl } from '../../utils';
import { havePermission, Permissions } from '../../services/accounts-service';
import { NetworkDevicesFilter, Renderer, User } from '../../services/home-service';
import { I18nInterface } from '../../services/i18n-service';
import { MainInterface } from '../../services/main-service';
import { ServerEventInterface } from '../../services/server-event-service';
import { SessionInterface } from '../../services/session-service';
import { showError, showLoading, updateError, updateSuccess } from '../../utils/notifications';

const Home = ({ i18n, main, session, sse }: { i18n:I18nInterface, main:MainInterface, session:SessionInterface, sse:ServerEventInterface }) => {
  const [renderers, setRenderers] = useState([] as Renderer[]);
  const [renderersBlockedByDefault, setRenderersBlockedByDefault] = useState(false);
  const [networkDeviceFilters, setNetworkDeviceFilters] = useState([] as NetworkDevicesFilter[]);
  const [networkDevicesBlockedByDefault, setNetworkDevicesBlockedByDefault] = useState(false);
  const [isLocalhost, setIsLocalhost] = useState(false);
  const [users, setUsers] = useState([] as User[]);
  const [currentTime, setCurrentTime] = useState(0);
  const canModify = havePermission(session, Permissions.settings_modify);
  const canView = canModify || havePermission(session, Permissions.settings_view);
  const canControlRenderers = havePermission(session, Permissions.devices_control);
  const [loading, setLoading] = useState(false);

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
    if (canView) {
      refreshData();
    }
  }, [canView]);

  useEffect(() => {
    session.useSseAs('Home')
    session.stopPlayerSse()
    main.setNavbarValue(undefined)
  }, []);

  const refreshData = async () => {
    setLoading(true);
    axios.get(renderersApiUrl)
      .then(function(response: any) {
        setRenderers(response.data.renderers);
        setRenderersBlockedByDefault(response.data.renderersBlockedByDefault);
        setNetworkDevicesBlockedByDefault(response.data.networkDevicesBlockedByDefault);
        setUsers(response.data.users);
        setCurrentTime(response.data.currentTime);
      })
      .catch(function() {
        showError({
          id: 'renderers-data-loading',
          title: i18n.get('Error'),
          message: i18n.get('DataNotReceived'),
        });
      })
      .then(function() {
        setLoading(false);
      });
  }

  const refreshDeviceData = async () => {
    setLoading(true);
    axios.get(renderersApiUrl + 'devices')
      .then(function(response: any) {
        setIsLocalhost(response.data.isLocalhost);
        setNetworkDeviceFilters(response.data.networkDevices);
        setNetworkDevicesBlockedByDefault(response.data.networkDevicesBlockedByDefault);
        setCurrentTime(response.data.currentTime);
      })
      .catch(function() {
        showError({
          id: 'renderers-data-loading',
          title: i18n.get('Error'),
          message: i18n.get('DataNotReceived'),
        });
      })
      .then(function() {
        setLoading(false);
      });
  }

  const setUserId = async (rule: string, userId: any) => {
    setSettings('renderers', { rule, userId }, false);
  }

  const setRenderersAllowed = async (rule: string, isAllowed: boolean) => {
    setSettings('renderers', { rule, isAllowed }, false);
  }

  const setDevicesAllowed = async (rule: string, isAllowed: boolean) => {
    setSettings('devices', { rule, isAllowed }, true);
  };

  const reset = async () => {
    setSettings('reset', null, true);
  };

  const setSettings = async (endpoint: string, data: any, fromDevice: boolean) => {
    showLoading({
      id: 'settings-save',
      title: i18n.get('Save'),
      message: i18n.get('SavingConfiguration'),
    });
    await axios.post(renderersApiUrl + endpoint, data)
      .then(function() {
        updateSuccess({
          id: 'settings-save',
          title: i18n.get('Saved'),
          message: i18n.get('ConfigurationSaved'),
          icon: <IconCheck size='1rem' />
        });
        if (fromDevice) {
          refreshDeviceData()
        } else {
          refreshData();
        }
      })
      .catch(function(error) {
        if (!error.response && error.request) {
          updateError({
            id: 'settings-save',
            title: i18n.get('Error'),
            message: i18n.get('ConfigurationNotReceived'),
            icon: <IconExclamationMark size='1rem' />
          })
        } else {
          updateError({
            id: 'settings-save',
            title: i18n.get('Error'),
            message: i18n.get('ConfigurationNotSaved')
          })
        }
      });
  };

  return canView ? (
    <Box style={{ maxWidth: 1024 }} mx='auto'>
      <LoadingOverlay visible={loading} />
      <Tabs keepMounted={false} defaultValue='renderers'>
        <Tabs.List>
          <Tabs.Tab value='renderers'>{i18n.get('DetectedMediaRenderers')}</Tabs.Tab>
          <Tabs.Tab value='blocked_renderers'>{i18n.get('BlockedMediaRenderers')}</Tabs.Tab>
          <Tabs.Tab value='network_devices'>{i18n.get('NetworkDevices')}</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value='renderers' pt='xs'>
          <Renderers
            allowed={true}
            blockedByDefault={renderersBlockedByDefault}
            canControlRenderers={canControlRenderers}
            canModify={canModify}
            i18n={i18n}
            renderers={renderers}
            users={users}
            setAllowed={setRenderersAllowed}
            setUserId={setUserId}
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
            users={users}
            setAllowed={setRenderersAllowed}
            setUserId={setUserId}
          />
        </Tabs.Panel>
        <Tabs.Panel value='network_devices' pt='xs'>
          <NetworkDevices
            blockedByDefault={networkDevicesBlockedByDefault}
            isLocalhost={isLocalhost}
            canModify={canModify}
            currentTime={currentTime}
            i18n={i18n}
            networkDeviceFilters={networkDeviceFilters}
            refreshDeviceData={refreshDeviceData}
            setAllowed={setDevicesAllowed}
            reset={reset}
          />
        </Tabs.Panel>
      </Tabs>
    </Box>
  ) : (
    <Box style={{ maxWidth: 1024 }} mx='auto'>
      <Text c='red'>{i18n.get('YouDontHaveAccessArea')}</Text>
    </Box>
  );
};

interface RendererAction extends Renderer {
  action: string,
}

export default Home;
