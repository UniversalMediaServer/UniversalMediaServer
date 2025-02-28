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
import axios from 'axios'
import { ReactNode, useEffect, useState } from 'react'

import SessionContext from '../contexts/session-context'
import { I18nInterface } from '../services/i18n-service'
import { SessionInterface } from '../services/session-service'
import { authApiUrl } from '../utils'

const SessionProvider = ({ children, i18n }: { children?: ReactNode, i18n: I18nInterface }) => {
  const [session, setSession] = useState({ noAdminFound: false, authenticate: true, initialized: false, player: false } as SessionInterface)

  useEffect(() => {
    const refresh = () => {
      axios.get(authApiUrl + 'session')
        .then(function (response: any) {
          setSession({ ...response.data, initialized: true, refresh: refresh })
        })
        .catch(function () {
          showNotification({
            id: 'data-loading',
            color: 'red',
            title: i18n.get('Error'),
            message: i18n.get('SessionNotReceived'),
            autoClose: 3000,
          })
        })
    }
    if (!session.initialized) {
      refresh()
    }
    axios.interceptors.response.use(function (response) {
      return response
    }, function (error) {
      if (error?.response?.status === 401 && error?.config?.url !== authApiUrl + 'login') {
        showNotification({
          id: 'authentication-error',
          color: 'red',
          title: 'Authentication error',
          message: 'You have been logged out from Universal Media Server. Please click here to log in again.',
          autoClose: false,
        })
        refresh()
      }
      return Promise.reject(error)
    })
  }, [session.initialized])

  const { Provider } = SessionContext
  return (
    <Provider value={session}>
      {children}
    </Provider>
  )
}

export default SessionProvider
