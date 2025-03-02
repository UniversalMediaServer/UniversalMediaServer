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
import axios from 'axios'
import { ReactNode, useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import videojs from 'video.js'

import PlayerEventContext from '../contexts/player-server-event-context'
import { getJwt } from '../services/auth-service'
import { I18nInterface } from '../services/i18n-service'
import { SessionInterface } from '../services/session-service'
import { playerApiUrl } from '../utils'
import { showError, showWarning } from '../utils/notifications'

const PlayerEventProvider = ({ children, i18n, session }: { children?: ReactNode, i18n: I18nInterface, session: SessionInterface }) => {
  const navigate = useNavigate()
  const [usePlayerSse, setUsePlayerSse] = useState(false)
  const [handled, setHandled] = useState<boolean>(true)
  const [abortController, setAbortController] = useState(new AbortController())
  const [connectionStatus, setConnectionStatus] = useState<number>(0)
  const [reqId, setReqId] = useState('0')
  const [reqType, setReqType] = useState('browse')
  const [uuid, setUuid] = useState('')
  const [askingUuid, setAskingUuid] = useState<boolean>(false)

  const askReqId = (id: string, type: string) => {
    setReqType('')
    setReqId(id)
    setReqType(type)
  }

  const askPlayId = (id: string) => {
    askReqId(id, 'play')
  }

  const askBrowseId = (id: string) => {
    askReqId(id, 'browse')
  }

  const askShowId = (id: string) => {
    askReqId(id, 'show')
  }

  const setPlayerVolume = (volume: number) => {
    try {
      const player = videojs.getPlayer('player')
      player?.volume(volume / 100)
    }
    catch (error) {
      console.log('volume', error)
    }
  }

  const mutePlayer = () => {
    console.log('mute')
    try {
      const player = videojs.getPlayer('player')
      player?.muted(true)
    }
    catch (error) {
      console.log('mute', error)
    }
  }

  const pausePlayer = () => {
    try {
      const player = videojs.getPlayer('player')
      player?.pause()
    }
    catch (error) {
      console.log('pausePlayer', error)
    }
  }

  const stopPlayer = () => {
    try {
      const player = videojs.getPlayer('player')
      player?.pause()
    }
    catch (error) {
      console.log('stop', error)
    }
  }

  useEffect(() => {
    if (uuid || askingUuid || !session.initialized || !usePlayerSse) return
    setAskingUuid(true)
    if (sessionStorage.getItem('player')) {
      setUuid(sessionStorage.getItem('player') as string)
      setAskingUuid(false)
    }
    else {
      axios.get(playerApiUrl)
        .then(function (response: any) {
          if (response.data.uuid) {
            sessionStorage.setItem('player', response.data.uuid)
            setUuid(response.data.uuid)
          }
        })
        .catch(function () {
          showError({
            id: 'session-lost',
            title: i18n.get('Error'),
            message: 'Your player session was not received from the server.',
          })
        })
        .then(function () {
          setAskingUuid(false)
        })
    }
  }, [session, usePlayerSse])

  useEffect(() => {
    if (handled || !uuid) {
      return
    }
    setHandled(true)
    if (!usePlayerSse) {
      return
    }

    let notified = false

    async function playPlayer() {
      // most browser does not autoplay or script play.
      try {
        const player = videojs.getPlayer('player')
        await player?.play()
      }
      catch {
        showWarning({
          title: i18n.get('RemoteControl'),
          message: i18n.get('RemotePlayOnlyAllowed'),
        })
      }
    }

    const addNotification = (datas: any) => {
      showNotification({
        id: datas.id ? datas.id : 'sse-notification',
        color: datas.color,
        title: datas.title,
        message: datas.message ? i18n.getI18nString(datas.message) : '',
        autoClose: datas.autoClose ? datas.autoClose : true,
      })
    }

    const showErrorNotification = () => {
      showNotification({
        id: 'connection-lost',
        color: 'orange',
        title: i18n.get('Warning'),
        message: i18n.get('UniversalMediaServerUnreachable'),
        autoClose: false,
      })
    }

    const onOpen = (event: Response) => {
      if (event.ok && event.headers.get('content-type') === EventStreamContentType) {
        hideNotification('connection-lost')
        notified = false
        setConnectionStatus(1)
      }
      else if (event.status == 401) {
        // reload Unauthorized
        navigate(0)
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
        if (datas.action === 'player') {
          switch (datas.request) {
            case 'setPlayId':
              setReqType('')
              setReqId(datas.arg0)
              setReqType('play')
              break
            case 'notify':
              switch (datas.arg0) {
                case 'okay':
                  addNotification({ color: 'green', title: datas.arg1 })
                  break
                case 'info':
                  addNotification({ color: 'blue', title: datas.arg1 })
                  break
                case 'warn':
                  addNotification({ color: 'orange', title: datas.arg1 })
                  break
                case 'err':
                  addNotification({ color: 'red', title: datas.arg1 })
                  break
              }
              break
            case 'play':
              playPlayer()
              break
            case 'setvolume':
              setPlayerVolume(datas.arg0)
              break
            case 'pause':
              pausePlayer()
              break
            case 'mute':
              mutePlayer()
              break
            case 'stop':
              stopPlayer()
              break
          }
        }
      }
    }

    const onError = () => {
      if (!notified) {
        notified = true
        showErrorNotification()
      }
      setConnectionStatus(2)
    }

    const onClose = () => {
      setConnectionStatus(0)
    }

    const startSse = () => {
      setConnectionStatus(0)
      fetchEventSource(playerApiUrl + 'sse/' + uuid, {
        headers: {
          Authorization: 'Bearer ' + getJwt(),
          Player: uuid,
        },
        signal: abortController.signal,
        async onopen(event: Response) { onOpen(event) },
        onmessage(event: EventSourceMessage) {
          onMessage(event)
        },
        onerror() { onError() },
        onclose() { onClose() },
        openWhenHidden: true,
      })
    }

    startSse()
  }, [handled, uuid])

  useEffect(() => {
    if (usePlayerSse != session.usePlayerSse) {
      setUsePlayerSse(session.usePlayerSse)
      if (handled) {
        abortController.abort()
        setAbortController(new AbortController())
        setHandled(false)
      }
    }
  }, [session.usePlayerSse])

  const { Provider } = PlayerEventContext

  return (
    <Provider value={{
      uuid: uuid,
      connectionStatus: connectionStatus,
      reqId: reqId,
      reqType: reqType,
      askReqId: askReqId,
      askBrowseId: askBrowseId,
      askPlayId: askPlayId,
      askShowId: askShowId,
    }}
    >
      {children}
    </Provider>
  )
}

export default PlayerEventProvider
