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
import { hideNotification, showNotification } from '@mantine/notifications'
import { EventSourceMessage, EventStreamContentType, fetchEventSource } from '@microsoft/fetch-event-source'
import { ReactNode, useEffect, useState } from 'react'

import ServerEventContext from '../contexts/server-event-context'
import { I18nInterface } from '../services/i18n-service'
import { SseNotificationData, UmsMemory } from '../services/server-event-service'
import { SessionInterface } from '../services/session-service'
import { sseApiUrl } from '../utils'
import { RendererAction } from '../services/home-service'

const ServerEventProvider = ({ children, i18n, session }: { children?: ReactNode, i18n: I18nInterface, session: SessionInterface }) => {
  const [prevLocation, setPrevLocation] = useState('')
  const [handled, setHandled] = useState<boolean>(true)
  const [abortController, setAbortController] = useState(new AbortController())
  const [connectionStatus, setConnectionStatus] = useState<number>(0)
  const [memory, setMemory] = useState<UmsMemory>({ max: 0, used: 0, dbcache: 0, buffer: 0 })
  const [updateAccounts, setUpdateAccounts] = useState<boolean>(false)
  const [reloadable, setReloadable] = useState<boolean>(false)
  const [userConfiguration, setUserConfiguration] = useState<Record<string, unknown> | null>(null)
  const [mediaScan, setMediaScan] = useState<boolean>(false)
  const [hasRendererAction, setRendererAction] = useState(false)
  const [rendererActions] = useState<RendererAction[]>([])
  const [hasNewLogLine, setNewLogLine] = useState(false)
  const [newLogLines] = useState([] as string[])

  useEffect(() => {
    if (handled || session.account === undefined) {
      return
    }
    setHandled(true)
    if (session.sseAs == '') {
      return
    }
    let notified = false

    const addNotification = (datas: SseNotificationData) => {
      showNotification({
        id: datas.id ? datas.id : 'sse-notification',
        color: datas.color,
        title: datas.title,
        message: datas.message ? i18n.getString(datas.message) : '',
        autoClose: datas.autoClose !== undefined ? datas.autoClose : true,
      })
    }

    const onOpen = (event: Response) => {
      if (event.ok && event.headers.get('content-type') === EventStreamContentType) {
        hideNotification('connection-lost')
        notified = false
        setConnectionStatus(1)
      }
      else if (event.status == 401) {
        // Unauthorized
      }
      else if (event.status == 403) {
        // stop Forbidden
        console.log('SSE Forbidden')
      }
      else {
        throw new Error('Expected content-type to be \'text/event-stream\'')
      }
    }

    const onMessage = (event: EventSourceMessage) => {
      if (event.event === 'message') {
        const datas = JSON.parse(event.data)
        switch (datas.action) {
          case 'update_memory':
            setMemory(datas)
            break
          case 'notify':
            addNotification(datas)
            break
          case 'update_accounts':
            setUpdateAccounts(true)
            break
          case 'refresh_session':
            session.refresh()
            break
          case 'set_reloadable':
            setReloadable(datas.value)
            break
          case 'set_configuration_changed':
            if (datas.value) {
              if (datas.value.server_name !== undefined) {
                session.setServerName(datas.value.server_name)
              }
              if (datas.value.authentication_enabled !== undefined
                || datas.value.authenticate_localhost_as_admin !== undefined
                || datas.value.web_gui_show_users !== undefined
                || datas.value.web_gui_allow_empty_pin !== undefined
              ) {
                session.refresh()
              }
            }
            setUserConfiguration(datas.value)
            break
          case 'set_media_scan_status':
            setMediaScan(datas.running)
            break
          case 'renderer_add':
          case 'renderer_delete':
          case 'renderer_update':
            rendererActions.push(datas)
            if (rendererActions.length > 20) {
              rendererActions.slice(0, rendererActions.length - 20)
            }
            setRendererAction(true)
            break
          case 'log_line':
            newLogLines.push(datas.value)
            if (newLogLines.length > 20) {
              newLogLines.slice(0, newLogLines.length - 20)
            }
            setNewLogLine(true)
            break
          case 'set_status_line':
            session.setStatusLine(datas.value)
            break
        }
      }
    }

    const onError = () => {
      if (!notified) {
        notified = true
        i18n.showServerUnreachable()
      }
      setConnectionStatus(2)
    }

    const onClose = () => {
      setConnectionStatus(0)
    }

    const headers = () => {
      return session.token ? { Authorization: 'Bearer ' + session.token } : undefined
    }

    const startSse = () => {
      setConnectionStatus(0)
      fetchEventSource(sseApiUrl, {
        headers: headers(),
        signal: abortController.signal,
        async onopen(event: Response) { onOpen(event) },
        onmessage(event: EventSourceMessage) {
          onMessage(event)
        },
        onerror(_event: Response) { onError() },
        onclose() { onClose() },
        openWhenHidden: true,
      })
    }

    startSse()
  }, [handled, session, rendererActions, newLogLines])

  useEffect(() => {
    if (prevLocation != session.sseAs) {
      setPrevLocation(session.sseAs)
      if (handled) {
        abortController.abort()
        setAbortController(new AbortController())
        setHandled(false)
      }
    }
  }, [session.sseAs])

  const getRendererAction = () => {
    if (rendererActions.length > 0) {
      const result = rendererActions.shift()
      setRendererAction(rendererActions.length > 0)
      return result
    }
  }

  const getNewLogLine = () => {
    if (newLogLines.length > 0) {
      const result = newLogLines.shift()
      setNewLogLine(newLogLines.length > 0)
      return result
    }
  }

  return (
    <ServerEventContext.Provider value={{
      connectionStatus: connectionStatus,
      memory: memory,
      updateAccounts: updateAccounts,
      setUpdateAccounts: setUpdateAccounts,
      reloadable: reloadable,
      userConfiguration: userConfiguration,
      setUserConfiguration: setUserConfiguration,
      mediaScan: mediaScan,
      hasRendererAction: hasRendererAction,
      getRendererAction: getRendererAction,
      hasNewLogLine: hasNewLogLine,
      getNewLogLine: getNewLogLine,
    }}
    >
      {children}
    </ServerEventContext.Provider>
  )
}

export default ServerEventProvider
