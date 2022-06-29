import { Button, Paper, Text } from '@mantine/core';
import axios from 'axios';

import ServerEventContext from '../../contexts/server-event-context';
import { ServerEventProvider } from '../../providers/server-event-provider';

export const OnlineStatus = () => {
  const connectionStatusStr = [
    'Connecting',
    'Open',
    'Closed',
  ];

  const handleAskMsg = () => {
    axios.post('/v1/api/sse/broadcast', {message:"This message was sent by the server"});
  };
  const handleAskMsgWithPerms = () => {
    axios.post('/v1/api/sse/broadcast', {message:"This message is received only by admins",permission:"*"});
  };

  return (
    <ServerEventProvider>
      <ServerEventContext.Consumer>
	  { sse => (
        <Paper shadow="xs" p="md">
          <Text size="xs">Connection status: {connectionStatusStr[sse.connectionStatus]}</Text>
          <Text size="xs">Memory status: {sse.memory.used}/{sse.memory.max}({sse.memory.buffer} for buffer)</Text>
          <Button size="xs" onClick={handleAskMsg}>Ask server to send a message</Button>
          <Text size="xs"> </Text>
          <Button size="xs" onClick={handleAskMsgWithPerms}>Ask server to send a message for admins</Button>
          <Text size="xs">Message: {sse.message}</Text>
        </Paper>
      )}
      </ServerEventContext.Consumer>
    </ServerEventProvider>
  );
};

export default OnlineStatus;