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
import useWebSocket from 'react-use-websocket'
import { wsApiUrl } from '../utils'
import { useEffect } from 'react'
import { I18nInterface } from '../services/i18n-service'

const WebSocketProvider = ({ i18n }: { i18n: I18nInterface }) => {
  const { readyState: serverWebSocketReadyState } = useWebSocket(
    wsApiUrl,
    {
      heartbeat: true,
      share: true,
      shouldReconnect: () => true,
      reconnectAttempts: Number.MAX_SAFE_INTEGER,
    },
  )

  useEffect(() => {
    console.log('WebSocket connection state changed ' + serverWebSocketReadyState)
    i18n.setServerReadyState(serverWebSocketReadyState)
  }, [i18n.setServerReadyState, serverWebSocketReadyState])

  return undefined
}

export default WebSocketProvider
