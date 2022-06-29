import { showNotification } from '@mantine/notifications';
import { EventSourceMessage, EventStreamContentType, fetchEventSource } from '@microsoft/fetch-event-source';
import { ReactNode, useEffect, useState } from 'react';
import { serverEventContext } from '../contexts/server-event-context';
import { getJwt } from '../services/auth.service';

interface Props {
  children?: ReactNode
}

export const ServerEventProvider = ({ children, ...props }: Props) =>{
  const [connectionStatus, setConnectionStatus] = useState<number>(0);
  const [memory, setMemory] = useState<{max:number,used:number,buffer:number}>({max:0,used:0,buffer:0});
  const [message, setMessage] = useState<string>('');

  useEffect(() => {
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
        notified = false;
        setConnectionStatus(1);
      }
    };

    const onMessage = (event: EventSourceMessage) => {
      // process the data here
      //THIS IS ADDED FOR TEST ONLY
      if (event.event === "message") {
        const datas = JSON.parse(event.data);
        switch (datas.action) {
          case 'update_memory':
            setMemory(datas);
            break;
          case 'show_message':
            setMessage(datas.message);
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

    startSse();
  }, []);

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