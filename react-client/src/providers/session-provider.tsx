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
import axios from 'axios'
import { ReactNode, useEffect, useState } from 'react'

import SessionContext from '../contexts/session-context'
import { logout, refreshAuthTokenNearExpiry } from '../services/auth-service'
import { I18nInterface } from '../services/i18n-service'
import { UmsSession } from '../services/session-service'
import { authApiUrl } from '../utils'
import { showError } from '../utils/notifications'

const SessionProvider = ({ children, i18n }: { children?: ReactNode, i18n: I18nInterface }) => {
  const [initialized, setInitialized] = useState(false)
  const [session, setSession] = useState<UmsSession>({ noAdminFound: false, authenticate: true, player: false })
  const [sse, setSse] = useState('')
  const [playerSse, setPlayerSse] = useState(false)

  const refresh = () => {
    axios.get(authApiUrl + 'session')
      .then(function (response: any) {
        setSession({ ...response.data })
        setInitialized(true)
      })
      .catch(function () {
        showError({
          id: 'session_error',
          title: i18n.get('Error'),
          message: i18n.get('SessionNotReceived'),
        })
      })
  }

  const sessionLogout = async () => {
    await logout()
    refresh()
  }

  const useSseAs = (name: string) => {
    setSse(name)
  }

  const stopSse = () => {
    setSse('')
  }

  const startPlayerSse = () => {
    setPlayerSse(true)
  }

  const stopPlayerSse = () => {
    setPlayerSse(false)
  }

  useEffect(() => {
    if (initialized) {
      return
    }
    refresh()
    refreshAuthTokenNearExpiry()
    axios.interceptors.response.use(function (response) {
      return response
    }, function (error) {
      if (error?.response?.status === 401 && error?.config?.url !== authApiUrl + 'login') {
        showError({
          id: 'authentication_error',
          title: 'Authentication error',
          message: 'You have been logged out from Universal Media Server.',
        })
        refresh()
      }
      return Promise.reject(error)
    })
  }, [initialized])

  const { Provider } = SessionContext
  return (
    <Provider value={{
      initialized: initialized,
      authenticate: session.authenticate,
      noAdminFound: session.noAdminFound,
      account: session.account,
      player: session.player,
      refresh: refresh,
      logout: sessionLogout,
      sseAs: sse,
      useSseAs: useSseAs,
      stopSse: stopSse,
      usePlayerSse: playerSse,
      stopPlayerSse: stopPlayerSse,
      startPlayerSse: startPlayerSse,
    }}
    >
      {children}
    </Provider>
  )
}

export default SessionProvider
