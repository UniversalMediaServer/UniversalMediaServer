/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
import { ThemeIcon, Tooltip } from '@mantine/core';
import { useContext } from 'react';
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
    i18n.get['ConnectingToServer'],
    i18n.get['UniversalMediaServerRunning'],
    i18n.get['UniversalMediaServerUnreachable'],
  ];

  return (
    <Tooltip label={connectionStatusTooltip[sse.connectionStatus]} width={350} color='blue' multiline withArrow>
      <ThemeIcon variant='light' color={connectionStatusStr[sse.connectionStatus]}>
        {sse.connectionStatus === 1 ? <PlugConnected /> : <PlugConnectedX />}
      </ThemeIcon>
    </Tooltip>
  );
};

export default ServerEventStatus;