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
import { useLocalStorage } from '@mantine/hooks'
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
  const [serverName, setServerName] = useState<string>('Universal Media Server')
  const [documentTitle, setDocumentTitleInternal] = useState<string>('')
  const [documentI18nTitle, setDocumentI18nTitle] = useState<string>('')
  const [navbarOpened, setNavbarOpened] = useState<boolean>(false)
  const [hasNavbar, setHasNavbar] = useState<boolean>(false)
  const [navbarValue, setNavbarValueInternal] = useState<React.ReactNode>(undefined)
  const [navbarManage, setNavbarManageInternal] = useState<string>('')
  const [statusLine, setStatusLine] = useState(undefined)
  const [playerNavbar, setPlayerNavbar] = useLocalStorage<boolean>({
    key: 'player-navbar',
    defaultValue: true,
  })
  const [playerDirectPlay, setPlayerDirectPlay] = useLocalStorage<boolean>({
    key: 'player-direct-play',
    defaultValue: false,
  })

  const refresh = () => {
    axios.get(authApiUrl + 'session')
      .then(function (response: any) {
        setSession({ ...response.data })
        setServerName(response.data.serverName ? response.data.serverName : 'Universal Media Server')
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

  const setNavbarValue = (navbarValue: React.ReactNode) => {
    if (navbarManage) {
      setNavbarManageInternal('')
    }
    setNavbarValueInternal(navbarValue)
  }

  const setNavbarManage = (navbarManage: string) => {
    if (navbarValue) {
      setNavbarValueInternal(undefined)
    }
    setNavbarManageInternal(navbarManage)
  }

  const setDocumentTitle = (documentTitle: string) => {
    setDocumentI18nTitle('')
    setDocumentTitleInternal(documentTitle)
  }

  useEffect(() => {
    setHasNavbar((navbarManage || navbarValue) ? true : false)
  }, [navbarManage, navbarValue])

  useEffect(() => {
    if (documentI18nTitle) {
      setDocumentTitleInternal(i18n.get(documentI18nTitle))
    }
  }, [i18n.get, documentI18nTitle])

  useEffect(() => {
    if (documentTitle) {
      document.title = serverName + ' - ' + documentTitle
    }
    else {
      document.title = serverName
    }
  }, [serverName, documentTitle])

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

  return (
    <SessionContext.Provider value={{
      initialized: initialized,
      authenticate: session.authenticate,
      noAdminFound: session.noAdminFound,
      account: session.account,
      player: session.player,
      users: session.users,
      refresh: refresh,
      logout: sessionLogout,
      sseAs: sse,
      useSseAs: useSseAs,
      stopSse: stopSse,
      usePlayerSse: playerSse,
      stopPlayerSse: stopPlayerSse,
      startPlayerSse: startPlayerSse,
      serverName: serverName,
      setServerName: setServerName,
      setDocumentTitle: setDocumentTitle,
      setDocumentI18nTitle: setDocumentI18nTitle,
      playerNavbar: playerNavbar,
      setPlayerNavbar: setPlayerNavbar,
      playerDirectPlay: playerDirectPlay,
      setPlayerDirectPlay: setPlayerDirectPlay,
      hasNavbar: hasNavbar,
      navbarOpened: navbarOpened,
      setNavbarOpened: setNavbarOpened,
      navbarValue: navbarValue,
      setNavbarValue: setNavbarValue,
      navbarManage: navbarManage,
      setNavbarManage: setNavbarManage,
      statusLine: statusLine,
      setStatusLine: setStatusLine,
    }}
    >
      {children}
    </SessionContext.Provider>
  )
}

export default SessionProvider
