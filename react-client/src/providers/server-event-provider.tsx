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
import { hideNotification, showNotification } from '@mantine/notifications';
import { EventSourceMessage, EventStreamContentType, fetchEventSource } from '@microsoft/fetch-event-source';
import { ReactNode, useContext, useEffect, useState } from 'react';

import I18nContext from '../contexts/i18n-context';
import MainContext from '../contexts/main-context';
import ServerEventContext, { UmsMemory } from '../contexts/server-event-context';
import SessionContext from '../contexts/session-context';
import { getJwt } from '../services/auth-service';
import { sseApiUrl } from '../utils';

interface Props {
  children?: ReactNode
}

export const ServerEventProvider = ({ children }: Props) => {
  const [started, setStarted] = useState<boolean>(false);
  const [connectionStatus, setConnectionStatus] = useState<number>(0);
  const [memory, setMemory] = useState<UmsMemory>({ max: 0, used: 0, dbcache: 0, buffer: 0 });
  const [updateAccounts, setUpdateAccounts] = useState<boolean>(true);
  const [reloadable, setReloadable] = useState<boolean>(false);
  const [userConfiguration, setUserConfiguration] = useState(null);
  const [mediaScan, setMediaScan] = useState<boolean>(false);
  const [hasRendererAction, setRendererAction] = useState(false);
  const [rendererActions] = useState([] as any[]);
  const [hasNewLogLine, setNewLogLine] = useState(false);
  const [newLogLines] = useState([] as string[]);
  const session = useContext(SessionContext);
  const i18n = useContext(I18nContext);
  const main = useContext(MainContext);

  useEffect(() => {
    if (started || session.account === undefined) {
      return;
    }
    setStarted(true);
    let notified = false;

    const addNotification = (datas: any) => {
      showNotification({
        id: datas.id ? datas.id : 'sse-notification',
        color: datas.color,
        title: datas.title,
        message: datas.message ? i18n.getI18nString(datas.message) : '',
        autoClose: datas.autoClose ? datas.autoClose : true
      });
    };

    const showErrorNotification = () => {
      showNotification({
        id: 'connection-lost',
        color: 'orange',
        title: i18n.get('Warning'),
        message: i18n.get('UniversalMediaServerUnreachable'),
        autoClose: false
      });
    }

    const onOpen = (event: Response) => {
      if (event.ok && event.headers.get('content-type') === EventStreamContentType) {
        hideNotification('connection-lost');
        notified = false;
        setConnectionStatus(1);
      } else if (event.status == 401) {
        //reload Unauthorized
        window.location.reload();
      } else if (event.status == 403) {
        //stop Forbidden
        console.log('SSE Forbidden');
      } else {
        throw new Error('Expected content-type to be \'text/event-stream\'');
      }
    };

    const onMessage = (event: EventSourceMessage) => {
      if (event.event === 'message') {
        const datas = JSON.parse(event.data);
        switch (datas.action) {
          case 'update_memory':
            setMemory(datas);
            break;
          case 'notify':
            addNotification(datas);
            break;
          case 'update_accounts':
            setUpdateAccounts(true);
            break;
          case 'refresh_session':
            session.refresh();
            break;
          case 'set_reloadable':
            setReloadable(datas.value);
            break;
          case 'set_configuration_changed':
            setUserConfiguration(datas.value);
            break;
          case 'set_media_scan_status':
            setMediaScan(datas.running);
            break;
          case 'renderer_add':
          case 'renderer_delete':
          case 'renderer_update':
            rendererActions.push(datas);
            if (rendererActions.length > 20) {
              rendererActions.slice(0, rendererActions.length - 20);
            }
            setRendererAction(true);
            break;
          case 'log_line':
            newLogLines.push(datas.value);
            if (newLogLines.length > 20) {
              newLogLines.slice(0, newLogLines.length - 20);
            }
            setNewLogLine(true);
            break;
          case 'set_status_line':
            main.setStatusLine(datas.value);
            break;
        }
      }
    }

    const onError = () => {
      if (!notified) {
        notified = true;
        showErrorNotification();
      }
      setConnectionStatus(2);
    };

    const onClose = () => {
      setConnectionStatus(0);
    };

    const startSse = () => {
      setConnectionStatus(0);
      fetchEventSource(sseApiUrl, {
        headers: {
          'Authorization': 'Bearer ' + getJwt()
        },
        async onopen(event: Response) { onOpen(event); },
        onmessage(event: EventSourceMessage) {
          onMessage(event);
        },
        onerror(_event: Response) { onError(); },
        onclose() { onClose(); },
        openWhenHidden: true,
      });
    };

    startSse();
  }, [started, session, rendererActions, newLogLines]);

  const getRendererAction = () => {
    let result = null;
    if (rendererActions.length > 0) {
      result = rendererActions.shift();
      setRendererAction(rendererActions.length > 0);
    }
    return result;
  };

  const getNewLogLine = () => {
    let result = null;
    if (newLogLines.length > 0) {
      result = newLogLines.shift();
      setNewLogLine(rendererActions.length > 0);
    }
    return result;
  };

  const { Provider } = ServerEventContext;
  return (
    <Provider value={{
      connectionStatus: connectionStatus,
      memory: memory,
      updateAccounts: updateAccounts,
      setUpdateAccounts: setUpdateAccounts,
      reloadable: reloadable,
      userConfiguration: userConfiguration,
      setUserConfiguration: setUserConfiguration,
      mediaScan: mediaScan,
      hasRendererAction: hasRendererAction,
      getRendererAction: getRendererAction,
      hasNewLogLine: hasNewLogLine,
      getNewLogLine: getNewLogLine,
    }}>
      {children}
    </Provider>
  )
}
