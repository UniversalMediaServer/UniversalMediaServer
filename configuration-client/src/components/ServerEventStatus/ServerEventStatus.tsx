import { ThemeIcon, Tooltip } from '@mantine/core';
import React, { useContext } from 'react';
import { PlugConnected, PlugConnectedX } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import ServerEventContext from '../../contexts/server-event-context';

export const ServerEventStatus = () => {
  const sse = useContext(ServerEventContext);
  const i18n = useContext(I18nContext);
  const connectionStatusStr = [
    'gray',
    'green',
    'red',
  ];
  const connectionStatusTooltip = [
    i18n.get['AutoUpdate.5'],
    i18n.get['SleepManager.PreventSleepRunningName'],
    'Server unreachable',
  ];

  return (
    <Tooltip label={connectionStatusTooltip[sse.connectionStatus]} width={350} color='blue' wrapLines withArrow>
      <ThemeIcon variant="light" color={connectionStatusStr[sse.connectionStatus]}>
        { sse.connectionStatus ===1 ? <PlugConnected /> : <PlugConnectedX /> }
      </ThemeIcon>
	</Tooltip>
  );
};

export default ServerEventStatus;