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
      setConnectionStatus(1);
      hideNotification('connection-lost');
	  if (event.ok && event.headers.get('content-type') === EventStreamContentType) {
            console.log('EventSource::onopen');
			}
    };

    const onMessage = (event: EventSourceMessage) => {
      // process the data here
      //THIS IS ADDED FOR TEST ONLY
	  console.log('EventSource::onMessage');
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
      console.log('EventSource::onError');
      setConnectionStatus(0);
    };
    const onClose = () => {
      console.log('EventSource::onClose');
      setConnectionStatus(2);
      showErrorNotification();
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