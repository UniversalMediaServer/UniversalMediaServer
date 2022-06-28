import React, { useEffect, useState } from 'react';
import { Button, Paper, Text } from '@mantine/core';
import { showNotification, hideNotification } from '@mantine/notifications';

import { getJwtPayload } from '../../services/auth.service';
import axios from 'axios';

export const OnlineStatus = () => {
  const connectionStatusStr = [
    'Connecting',
    'Open',
    'Closed',
  ];
  const [connectionStatus, setConnectionStatus] = useState<number>(0);

//THIS IS ADDED FOR TEST ONLY
  const [message, setMessage] = useState<string>('');
  const handleAskMsg = () => {
    axios.post('/v1/api/sse/broadcast', {message:"This message was sent by the server"});
  };
  const handleAskMsgWithPerms = () => {
    axios.post('/v1/api/sse/broadcast', {message:"This message is received only by admins",permission:"*"});
  };

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
        setTimeout(() => { sse = createSse(); }, 5000);
      }
    };
    const onMessage = (event: MessageEvent) => {
      // process the data here
      //THIS IS ADDED FOR TEST ONLY
      setMessage(event.data);
	  //here can be JSON.parse(event.data) etc
    }
    const createSse = () => {
      const newsse = new EventSource('/v1/api/sse/' + (getJwtPayload() !== null ? encodeURI('?' + getJwtPayload()) : ''));
      newsse.onmessage = e => onMessage(e);
      newsse.onerror = e => { onError(e); }
      newsse.onopen = e => { onOpen(e); }
      return newsse;
    };
    let sse = createSse();
    return () => sse.close();
  }, []);
  return (
    <Paper shadow="xs" p="md">
      <Text size="xs">Connection status: {connectionStatusStr[connectionStatus]}</Text>
      <Button onClick={handleAskMsg}>Ask server to send a message</Button>
      <Button onClick={handleAskMsgWithPerms}>Ask server to send a message for admins</Button>
      <Text size="xs">Message: {message}</Text>
    </Paper>
  );
};

export default OnlineStatus;