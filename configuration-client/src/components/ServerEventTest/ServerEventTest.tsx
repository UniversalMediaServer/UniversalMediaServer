import { Button, Group, Paper, Text } from '@mantine/core';
import React, { useContext } from 'react';
import axios from 'axios';

import ServerEventContext from '../../contexts/server-event-context';
import MemoryBar from '../MemoryBar/MemoryBar';
import ServerEventStatus from '../ServerEventStatus/ServerEventStatus';

export const ServerEventTest = () => {
  const sse = useContext(ServerEventContext);
  const connectionStatusStr = [
    'Connecting',
    'Open',
    'Closed',
  ];

  const handleAskMsg = () => {
    axios.post('/v1/api/sse/broadcast', {message:"This message was sent from the server"});
  };
  const handleAskMsgWithPerms = () => {
    axios.post('/v1/api/sse/broadcast', {message:"This message is received only by admins",permission:"*"});
  };
  const handleAskNotification = () => {
    axios.post('/v1/api/sse/notify', {message:"This notication was sent from the server"});
  };

  return (
        <Paper shadow="xs" p="md">
          <Text size="xs">Connection status: {connectionStatusStr[sse.connectionStatus]}</Text>
		  <MemoryBar />
		  <ServerEventStatus />
          <Text size="xs">Memory status: {sse.memory.used}/{sse.memory.max}({sse.memory.buffer} for buffer)</Text>
          <Group>
            <Button size="xs" onClick={handleAskMsg}>Ask server to send a message</Button>
            <Button size="xs" onClick={handleAskMsgWithPerms}>Ask server to send a message for admins</Button>
          </Group>
          <Text size="xs">Message: {sse.message}</Text>
          <Button size="xs" onClick={handleAskNotification}>Ask server to send a notification</Button>
        </Paper>
      );
};

export default ServerEventTest;