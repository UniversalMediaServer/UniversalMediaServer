import { hideNotification, showNotification } from '@mantine/notifications';
import { EventSourceMessage, EventStreamContentType, fetchEventSource } from '@microsoft/fetch-event-source';
import { ReactNode, useContext, useEffect, useState } from 'react';
import videojs from 'video.js';

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
  const session = useContext(SessionContext);
  const i18n = useContext(I18nContext);
  const [browseId, setBrowseId] = useState('0');
  const [playId, setPlayId] = useState('');

  const askPlayId = (id:string) => {
	  setBrowseId('');
	  setPlayId(id);
  }

  const askBrowseId = (id:string) => {
	  setPlayId('');
	  setBrowseId(id);
  }

  const setPlayerVolume = (volume:number) => {
    console.log('volume', volume);
    try {
      const player = videojs.getPlayer('player');
      player?.volume(volume/100);
    } catch(error) {
      console.log('volume', error);
    }
  }

  const mutePlayer = () => {
    console.log('mute');
    try {
      const player = videojs.getPlayer('player');
      player?.muted(true);
    } catch(error) {
      console.log('mute', error);
    }
  }

  const pausePlayer = () => {
    try {
      const player = videojs.getPlayer('player');
      player?.pause();
    } catch(error) {
      console.log('pausePlayer', error);
    }
  }

  const stopPlayer = () => {
    try {
      const player = videojs.getPlayer('player');
      player?.pause();
    } catch(error) {
      console.log('stop', error);
    }
  }

  useEffect(() => {
    if (started || !sessionStorage.getItem('player')) {
      return;
    }
    setStarted(true);
    let notified = false;

    async function playPlayer() {
      //most browser does not autoplay or script play.
      try {
        const player = videojs.getPlayer('player');
        await player?.play();
      } catch {
        showNotification({
          id: 'player-play',
          color: 'orange',
          title: i18n.get['RemoteControl'],
          message: i18n.get['RemotePlayOnlyAllowed'],
          autoClose: true
        });
      }
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
        if (datas.action === 'player') {
          switch (datas.request) {
            case 'setPlayId':
              askPlayId(datas.arg0);
              break;
            case 'notify':
              switch (datas.arg0) {
                case 'okay':
                  addNotification({color:'green', title:datas.arg1});
                  break;
                case 'info':
                  addNotification({color:'blue', title:datas.arg1});
                  break;
                case 'warn':
                  addNotification({color:'orange', title:datas.arg1});
                  break;
                case 'err':
                  addNotification({color:'red', title:datas.arg1});
                  break;
              }
              break;
            case 'control':
              switch (datas.arg0) {
                case 'play':
                  playPlayer();
                  break;
                case 'setvolume':
                  setPlayerVolume(datas.arg1);
                  break;
                case 'pause':
                  pausePlayer();
                  break;
                case 'mute':
                  mutePlayer();
                  break;
                case 'stop':
                  stopPlayer();
                  break;
              }
              break;
          }
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

  const { Provider } = PlayerEventContext;

  return(
    <Provider value={{
      connectionStatus: connectionStatus,
      browseId: browseId,
      askBrowseId: askBrowseId,
      playId:playId,
      askPlayId: askPlayId,
    }}>
      {children}
    </Provider>
  )
}
