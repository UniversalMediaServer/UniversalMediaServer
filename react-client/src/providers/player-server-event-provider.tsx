import { hideNotification, showNotification } from '@mantine/notifications';
import { EventSourceMessage, EventStreamContentType, fetchEventSource } from '@microsoft/fetch-event-source';
import { ReactNode, useContext, useEffect, useState } from 'react';

import I18nContext from '../contexts/i18n-context';
import PlayerEventContext from '../contexts/player-server-event-context';
import SessionContext from '../contexts/session-context';
import { playerApiUrl } from '../utils';

interface Props {
  children?: ReactNode
}

export const PlayerEventProvider = ({ children, ...props }: Props) =>{
  const [started, setStarted] = useState<boolean>(false);
  const [connectionStatus, setConnectionStatus] = useState<number>(0);
  const [hasPlayerAction, setPlayerAction] = useState(false);
  const [playerActions] = useState([] as any[]);
  const session = useContext(SessionContext);
  const i18n = useContext(I18nContext);

  useEffect(() => {
    if (started || !sessionStorage.getItem('player')) {
      return;
    }
    setStarted(true);
    let notified = false;

    const showErrorNotification = () => {
      showNotification({
        id: 'connection-lost',
        color: 'orange',
        title: i18n.get['Warning'],
        message: i18n.get['UniversalMediaServerUnreachable'],
        autoClose: false
      });
    }

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
      fetchEventSource(playerApiUrl + 'sse/' + sessionStorage.getItem('player'), {
        async onopen(event: Response) { onOpen(event); },
        onmessage(event: EventSourceMessage) {
          onMessage(event);
        },
        onerror() { onError(); },
        onclose() { onClose(); },
        openWhenHidden: true,
      });
    };

    startSse();
  }, [started, session, i18n]);

  const getPlayerAction = () => {
    let result = null;
    if (playerActions.length > 0) {
      result = playerActions.shift();
    }
    setPlayerAction(playerActions.length > 0);
    return result;
  };

  const { Provider } = PlayerEventContext;

  return(
    <Provider value={{
      connectionStatus: connectionStatus,
	  hasPlayerAction: hasPlayerAction,
	  getPlayerAction: getPlayerAction,
    }}>
      {children}
    </Provider>
  )
}
