import { hideNotification, showNotification } from '@mantine/notifications';
import { EventSourceMessage, EventStreamContentType, fetchEventSource } from '@microsoft/fetch-event-source';
import { ReactNode, useContext, useEffect, useState } from 'react';

import I18nContext from '../contexts/i18n-context';
import { serverEventContext } from '../contexts/server-event-context';
import SessionContext from '../contexts/session-context';
import { getJwt } from '../services/auth.service';

interface Props {
  children?: ReactNode
}

export const ServerEventProvider = ({ children, ...props }: Props) =>{
  const [started, setStarted] = useState<boolean>(false);
  const [connectionStatus, setConnectionStatus] = useState<number>(0);
  const [memory, setMemory] = useState<{max:number,used:number,buffer:number}>({max:0,used:0,buffer:0});
  const [message, setMessage] = useState<string>('');
  const session = useContext(SessionContext);
  const i18n = useContext(I18nContext);

  useEffect(() => {
    if (started || session.account === undefined) {
      return;
    }
    setStarted(true);
    let notified = false;
    const startSse = () => {
      setConnectionStatus(0);
      fetchEventSource('/v1/api/sse/', {
        headers: {
          'Authorization': 'Bearer ' + getJwt()
        },
        async onopen(event: Response) { onOpen(event); },
        onmessage(event: EventSourceMessage) {
          onMessage(event);
        },
        onerror(event: Response) { onError(event); },
        onclose() { onClose(); },
      });
    };

    const onOpen = (event: Response) => {
      if (event.ok && event.headers.get('content-type') === EventStreamContentType) {
        hideNotification('connection-lost');
        notified = false;
        setConnectionStatus(1);
      }
    };

    const onMessage = (event: EventSourceMessage) => {
      if (event.event === "message") {
        const datas = JSON.parse(event.data);
        switch (datas.action) {
          case 'update_memory':
            setMemory(datas);
            break;
          case 'show_message':
            setMessage(datas.message);
            break;
          case 'notify':
            addNotification(datas);
            break;
        }
      }
    }
    const onError = (event: Response) => {
      if (!notified) {
        notified = true;
        showErrorNotification();
      }
      setConnectionStatus(2);
    };

    const onClose = () => {
      setConnectionStatus(0);
    };

    const showErrorNotification = () => {
      showNotification({
        id: 'connection-lost',
        color: 'orange',
        title: 'Warning',
        message: 'Connectivity to Universal Media Server unavailable',
        autoClose: false
      });
    }

    const addNotification = (datas: any) => {
      showNotification({
        id: datas.id ? datas.id : 'sse-notification',
        color: datas.color,
        title: datas.title,
        message: datas.message ? i18n.getI18nString(datas.message) : '',
        autoClose: datas.autoClose ? datas.autoClose : true
      });
	  
    };

    startSse();
  }, [started, session, i18n]);

  const { Provider } = serverEventContext;
  return(
    <Provider value={{
      connectionStatus: connectionStatus,
      memory: memory,
      message: message,
    }}>
      {children}
    </Provider>
  )
}