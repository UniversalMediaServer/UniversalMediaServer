import React, { useState  } from 'react';
import useWebSocket, { ReadyState } from 'react-use-websocket';
import { Text } from '@mantine/core';
import { showNotification, hideNotification } from '@mantine/notifications';

export const OnlineStatus = () => {
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
  const [socketUrl, setSocketUrl] = useState('ws://localhost:8887');

  const { sendMessage, lastMessage, readyState } = useWebSocket(socketUrl, {
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
