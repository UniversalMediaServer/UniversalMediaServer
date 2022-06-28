import React, { useEffect, useState } from 'react';
import { Text } from '@mantine/core';
import { showNotification, hideNotification } from '@mantine/notifications';

import { getJwtPayload } from '../../services/auth.service';

export const OnlineStatus = () => {
  const connectionStatusStr = [
    'Connecting',
    'Open',
    'Closed',
  ];
  const [connectionStatus, setConnectionStatus] = useState<number>(0);

  useEffect(() => {
    let connectionStatus = 0;

  const updateConnectionStatus = (event: Event) => {
    if (connectionStatus !== sse.readyState) {
      if (connectionStatus === 1) {
        showNotification({
          id: 'connection-lost',
          color: 'orange',
          title: 'Warning',
          message: 'Connectivity to Universal Media Server unavailable',
          autoClose: false
        });
      } else if (sse.readyState === 1) {
          hideNotification('connection-lost');
	  }
      connectionStatus = sse.readyState;
      setConnectionStatus(connectionStatus);
	}
  }

  const onOpen = (event: Event) => {
    updateConnectionStatus(event);
  };
  const onError = (event: Event) => {
    updateConnectionStatus(event);
	if (connectionStatus === 2) {
      setTimeout(() => { sse = createSse(); }, 1000);
    }
  };
  function onMessage(data: MessageEvent) {
    // process the data here
    updateConnectionStatus(data);
  }
  const createSse = () => {
    const newsse = new EventSource('/v1/api/sse/' + (getJwtPayload() !== null ? encodeURI('?' + getJwtPayload()) : ''));
    newsse.onmessage = e => onMessage(JSON.parse(e.data));
    newsse.onerror = e => { onError(e); }
    newsse.onopen = e => { onOpen(e); }
	return newsse;
  };
    let sse = createSse();
	return () => sse.close();
  }, []);
  return <Text size="xs">Connection status: {connectionStatusStr[connectionStatus]}</Text>;
};

export default OnlineStatus;