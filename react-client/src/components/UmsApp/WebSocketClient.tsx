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
import { showNotification } from '@mantine/notifications'
import { useEffect } from 'react'
import useWebSocket from 'react-use-websocket'

import { RendererAction } from '../../services/home-service'
import { I18nInterface } from '../../services/i18n-service'
import { PlayerInterface, PlayerRemoteAction } from '../../services/player-service'
import { SessionInterface, UmsMemory, UmsNotificationData } from '../../services/session-service'
import { wsApiUrl } from '../../utils'

const WebSocketClient = ({ i18n, session, player }: { i18n: I18nInterface, session: SessionInterface, player: PlayerInterface }) => {
  const { lastJsonMessage, readyState: serverWebSocketReadyState, sendJsonMessage } = useWebSocket(
    wsApiUrl,
    {
      heartbeat: true,
      share: true,
      shouldReconnect: () => true,
      reconnectAttempts: Number.MAX_SAFE_INTEGER,
    },
  )

  const addNotification = (datas: UmsNotificationData) => {
    showNotification({
      id: datas.id ? datas.id : 'sse-notification',
      color: datas.color,
      title: datas.title,
      message: datas.message ? i18n.getString(datas.message) : '',
      autoClose: datas.autoClose !== undefined ? datas.autoClose : true,
    })
  }

  useEffect(() => {
    console.log('WebSocket connection state changed ' + serverWebSocketReadyState)
    i18n.setServerReadyState(serverWebSocketReadyState)
    if (serverWebSocketReadyState == 1) {
      session.setSendJsonMessage(sendJsonMessage)
      sendJsonMessage({ action: 'token', data: session.token })
      sendJsonMessage({ action: 'subscribe', data: session.subscribe })
      sendJsonMessage({ action: 'uuid', data: session.uuid })
    }
  }, [i18n.setServerReadyState, serverWebSocketReadyState])

  useEffect(() => {
    if (lastJsonMessage == null) {
      return
    }
    const message = lastJsonMessage as Record<string, unknown>
    if (message.action === undefined) {
      return
    }
    switch (message.action) {
      case 'log_line':
        session.addLogLine(message.data as string)
        break
      case 'notify':
        addNotification(message.data as UmsNotificationData)
        break
      case 'refresh_session':
        session.refresh()
        break
      case 'renderer_add':
      case 'renderer_delete':
      case 'renderer_update':
        session.addRendererAction(lastJsonMessage as RendererAction)
        break
      case 'player':
        player.onRemoteAction(message.data as PlayerRemoteAction)
        break
      case 'set_configuration_changed':
        if (message.data) {
          const configuration = message.data as Record<string, unknown>
          session.setServerConfiguration(configuration)
        }
        break
      case 'set_media_scan_status':
        session.setMediaScan(message.data as boolean)
        break
      case 'set_reloadable':
        session.setReloadable(message.data as boolean)
        break
      case 'set_status_line':
        session.setStatusLine(message.data as string)
        break
      case 'update_accounts':
        session.setUpdateAccounts(true)
        break
      case 'update_memory':
        session.setMemory(message.data as UmsMemory)
        break
    }
  }, [lastJsonMessage])

  useEffect(() => {
    sendJsonMessage({ action: 'subscribe', data: session.subscribe })
  }, [session.subscribe])

  useEffect(() => {
    sendJsonMessage({ action: 'token', data: session.token })
  }, [session.token])

  useEffect(() => {
    sendJsonMessage({ action: 'uuid', data: session.uuid })
  }, [session.uuid])

  return undefined
}

export default WebSocketClient
