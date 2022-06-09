import React, { useEffect, useState  } from 'react';
import axios from 'axios';
import useWebSocket, { ReadyState } from 'react-use-websocket';
import { Text } from '@mantine/core';
import { showNotification, hideNotification } from '@mantine/notifications';

export const OnlineStatus = () => {
  const [socketUrl, setSocketUrl] = useState('ws://localhost:');
  const [socketPort, setSocketPort] = useState('8887');
  useEffect(() => {
    axios.get('/configuration-api/settings')
      .then(function (response: any) {
        const { userSettings } = response.data;
        if (userSettings.websockets_https) {
          setSocketUrl('wss://localhost:')
        }
        if (userSettings.websockets_port) {
          setSocketPort(String(userSettings.websockets_port));
        }
      })
      .catch(function (error: Error) {
        console.log(error);
      });
  });
  const onClose = () => {
    showNotification({
      id: 'connection-lost',
      color: 'orange',
      title: 'Warning',
      message: 'Connectivity to Universal Media Server unavailable',
      autoClose: false
    });
  };
  const onOpen = () => {
    hideNotification('connection-lost');
  };

  const { readyState } = useWebSocket(socketUrl + socketPort, {
    onClose,
    onOpen,
    retryOnError: true,
    shouldReconnect: () => true,
    reconnectAttempts: 100,
    reconnectInterval: 3000,
  });
  const connectionStatus = {
    [ReadyState.CONNECTING]: 'Connecting',
    [ReadyState.OPEN]: 'Open',
    [ReadyState.CLOSING]: 'Closing',
    [ReadyState.CLOSED]: 'Closed',
    [ReadyState.UNINSTANTIATED]: 'Uninstantiated',
  }[readyState];

  return <Text size="xs">Connection status: {connectionStatus}</Text>;
};

export default OnlineStatus;
