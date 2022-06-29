import { Button, Paper, Text } from '@mantine/core';
import { showNotification, hideNotification } from '@mantine/notifications';
import { EventSourceMessage, EventStreamContentType, fetchEventSource } from '@microsoft/fetch-event-source';
import axios from 'axios';
import React, { useEffect, useState } from 'react';
import { getJwt } from '../../services/auth.service';

export const OnlineStatus = () => {
  const connectionStatusStr = [
    'Connecting',
    'Open',
    'Closed',
  ];
  const [connectionStatus, setConnectionStatus] = useState<number>(0);

//THIS IS ADDED FOR TEST ONLY
  const [message, setMessage] = useState<string>('');
  const [memory, setMemory] = useState<{max:number,used:number,buffer:number}>({max:0,used:0,buffer:0});
  const handleAskMsg = () => {
    axios.post('/v1/api/sse/broadcast', {message:"This message was sent by the server"});
  };
  const handleAskMsgWithPerms = () => {
    axios.post('/v1/api/sse/broadcast', {message:"This message is received only by admins",permission:"*"});
  };

  useEffect(() => {
    let connectionStatus = 0;
    const startSse = () => {
      setConnectionStatus(connectionStatus);
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

    const showErrorNotification = () => {
      showNotification({
        id: 'connection-lost',
        color: 'orange',
        title: 'Warning',
        message: 'Connectivity to Universal Media Server unavailable',
        autoClose: false
      });
    }

    const onOpen = (event: Response) => {
      hideNotification('connection-lost');
      if (event.ok && event.headers.get('content-type') === EventStreamContentType) {
        connectionStatus = 1;
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
      if (connectionStatus !== 2) {
        showErrorNotification();
      }
      connectionStatus = 2;
      setConnectionStatus(2);
    };
    const onClose = () => {
      connectionStatus = 0;
      setConnectionStatus(0);
    };
    startSse();
  }, []);
  return (
    <Paper shadow="xs" p="md">
      <Text size="xs">Connection status: {connectionStatusStr[connectionStatus]}</Text>
      <Text size="xs">Memory status: {memory.used}/{memory.max}({memory.buffer} for buffer)</Text>
      <Button size="xs" onClick={handleAskMsg}>Ask server to send a message</Button>
      <Text size="xs"> </Text>
      <Button size="xs" onClick={handleAskMsgWithPerms}>Ask server to send a message for admins</Button>
      <Text size="xs">Message: {message}</Text>
    </Paper>
  );
};

export default OnlineStatus;