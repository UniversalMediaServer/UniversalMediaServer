import React, { useEffect, useState  } from 'react';
import axios from 'axios';
import { Text } from '@mantine/core';
import { showNotification, hideNotification } from '@mantine/notifications';

export const OnlineStatus = () => {
  function getToken() {
    return localStorage.getItem('user');
  }
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
  function onMessage(data: any) {
    // process the data here
    
  }
  const sse = new EventSource('/v1/api/sse/' + (getToken() !== null ? encodeURI('?' + getToken()) : ''));
  sse.onmessage = e => onMessage(JSON.parse(e.data));
  sse.onerror = () => { onClose(); }
  sse.onopen = () => { onOpen(); }

  const connectionStatus = {
    [0]: 'Connecting',
    [1]: 'Open',
    [2]: 'Closed',
  }[sse.readyState];
  return <Text size="xs">Connection status: {connectionStatus}</Text>;
};

export default OnlineStatus;