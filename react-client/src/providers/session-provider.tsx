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
import { useInterval, useLocalStorage, useSessionStorage } from '@mantine/hooks'
import { hideNotification } from '@mantine/notifications'
import axios, { AxiosError } from 'axios'
import { jwtDecode, JwtPayload } from 'jwt-decode'
import _ from 'lodash'
import { ReactNode, useEffect, useState } from 'react'

import SessionContext from '../contexts/session-context'
import { accountHavePermission } from '../services/accounts-service'
import { I18nInterface } from '../services/i18n-service'
import { LocalUser, UmsSession, UmsUser, UmsUserLogin } from '../services/session-service'
import { authApiUrl, playerApiUrl } from '../utils'
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
  const [statusLine, setStatusLine] = useState<string>('')
  const [isDefaultUser, setIsDefaultUser] = useState<boolean>(false)
  const [isLogout, setIsLogout] = useState<boolean>(false)
  const [lastUserId, setLastUserId] = useState<number>(0)
  const [canSwitchUser, setCanSwitchUser] = useState<boolean>(false)
  const [switchUsers, setSwitchUsers] = useState<UmsUserLogin[]>([])
  const [playerNavbar, setPlayerNavbar] = useLocalStorage<boolean>({
    key: 'player-navbar',
    defaultValue: true,
  })
  const [playerDirectPlay, setPlayerDirectPlay] = useLocalStorage<boolean>({
    key: 'player-direct-play',
    defaultValue: false,
  })
  const [token, setToken, clearToken] = useLocalStorage<string>({
    key: 'user',
    defaultValue: '',
    getInitialValueInEffect: false,
  })
  const [localUsers, setLocalUsers, clearLocalUsers] = useLocalStorage<LocalUser[]>({
    key: 'local-users',
    defaultValue: [],
  })
  const [uuid, setUuid, clearUuid] = useSessionStorage<string>({
    key: 'player',
    defaultValue: '',
  })
  useInterval(
    () => refreshLocalUsers(),
    30000,
    { autoInvoke: true },
  )

  const refresh = () => {
    axios.get(authApiUrl + 'session')
      .then(function (response: any) {
        hideNotification('connection-lost')
        setSession({ ...response.data })
        setServerName(response.data.serverName ? response.data.serverName : 'Universal Media Server')
        setInitialized(true)
      })
      .catch(function (error: AxiosError) {
        if (!error.response && error.request) {
          i18n.showServerUnreachable()
        }
        else {
          showError({
            id: 'session_error',
            title: i18n.get('Error'),
            message: i18n.get('SessionNotReceived'),
          })
        }
      })
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

  const havePermission = (permission: number) => {
    return (typeof session.account !== 'undefined'
      && accountHavePermission(session.account, permission)
    )
  }

  const storeLocalUser = (user?: UmsUser) => {
    if (user && user.id && user.id != 2147483647) {
      const localUsersTemp = _.cloneDeep(localUsers).filter((localUser: LocalUser) => localUser.id != user.id)
      localUsersTemp.push({
        id: user.id,
        displayName: user.displayName,
        token: token,
        avatar: user.avatar,
      })
      setLocalUsers(localUsersTemp)
    }
  }

  const removeLocalUser = (userId: number) => {
    const localUsersTemp = _.cloneDeep(localUsers).filter((localUser: LocalUser) => localUser.id != userId)
    setLocalUsers(localUsersTemp)
  }

  const updateSwitchUsers = () => {
    const switchUsersTemp = [] as UmsUserLogin[]
    if (session.authenticate) {
      if (session.users) {
        session.users.map((user: UmsUserLogin) => {
          const switchUser = _.cloneDeep(user)
          const localUser = localUsers.find((localUser: LocalUser) => user.id == localUser.id)
          if (localUser && localUser.token) {
            switchUser.login = 'token'
            switchUser.token = localUser.token
          }
          switchUsersTemp.push(switchUser)
        })
      }
      else {
        if (session.account?.user.id === 2147483647) {
          switchUsersTemp.push(
            {
              id: 2147483647,
              username: '',
              displayName: '',
              login: 'localhost',
            },
          )
        }
        switchUsersTemp.push(
          {
            id: 0,
            username: '',
            displayName: '',
            login: 'pass',
          },
        )
        localUsers.map((localUser: LocalUser) => {
          if (localUser && localUser.token) {
            switchUsersTemp.push(
              {
                id: localUser.id,
                username: '',
                displayName: localUser.displayName,
                avatar: localUser.avatar,
                login: 'token',
                token: localUser.token,
              },
            )
          }
        })
      }
    }
    setSwitchUsers(switchUsersTemp)
  }

  const login = async (username: string, password: string) => {
    await axios.post(authApiUrl + 'login', {
      username,
      password,
    })
      .then(function (response: any) {
        if (response.data.token) {
          setToken(response.data.token)
          hideNotification('pwd-error')
          resetLogout()
        }
      })
      .catch(function (error: AxiosError) {
        if (!error.response && error.request) {
          i18n.showServerUnreachable()
        }
        else {
          showError({
            id: 'pwd-error',
            title: i18n.get('Error'),
            message: i18n.get('ErrorLoggingIn'),
          })
        }
      })
  }

  const loginPin = async (id: number, pin: string) => {
    await axios.post(authApiUrl + 'loginpin', {
      id,
      pin,
    })
      .then(function (response: any) {
        if (response.data.token) {
          setToken(response.data.token)
          hideNotification('pwd-error')
          resetLogout()
        }
      })
      .catch(function (error: AxiosError) {
        if (!error.response && error.request) {
          i18n.showServerUnreachable()
        }
        else {
          showError({
            id: 'pwd-error',
            title: i18n.get('Error'),
            message: i18n.get('ErrorLoggingIn'),
          })
        }
      })
  }

  const logout = async (keepLocal: boolean) => {
    const currentUserId = session.account?.user.id
    if (currentUserId !== undefined) {
      if (!keepLocal) {
        removeLocalUser(currentUserId)
      }
      setLastUserId(currentUserId)
    }
    if (uuid) {
      try {
        await axios.post(playerApiUrl + 'logout', { uuid: uuid })
      }
      catch { /* server Forbidden or Unauthorized */ }
      clearUuid()
    }
    clearToken()
    setIsLogout(true)
  }

  const resetLogout = () => {
    setIsLogout(false)
    refresh()
  }

  const tokenIsValid = () => {
    if (!token) {
      return false
    }
    const now = Math.floor(new Date().getTime() / 1000) + 300
    try {
      const decoded = jwtDecode<JwtPayload>(token)
      return (decoded.exp && decoded.exp > now)
    }
    catch (e) {
      return false
    }
  }

  const refreshLocalUsers = () => {
    const localUsersTemp = [] as LocalUser[]
    const renew = Math.floor(new Date().getTime() / 1000) + 300
    localUsers.map((localUser: LocalUser) => {
      const currentToken = localUser.token
      const decoded = jwtDecode<JwtPayload>(currentToken)
      if (decoded.exp) {
        if (decoded.exp < renew) {
          axios.post(authApiUrl + 'refresh', { token: currentToken })
            .then(function (response: any) {
              if (response.data.token) {
                localUser.token = response.data.token
                localUsersTemp.push(localUser)
                if (token == currentToken) {
                  setToken(currentToken)
                }
              }
            })
            .catch(function (error: AxiosError) {
              if (!error.response && error.request) {
                i18n.showServerUnreachable()
              }
              else if (error.response?.status == 403) {
                localUser.token = ''
              }
              else {
                showError({
                  id: 'session_error',
                  title: i18n.get('Error'),
                  message: i18n.get('SessionNotReceived'),
                })
              }
            })
        }
        else {
          localUsersTemp.push(localUser)
        }
      }
    })
    if (!_.isEqual(localUsers, localUsersTemp)) {
      setLocalUsers(localUsersTemp)
    }
  }

  useEffect(() => {
    updateSwitchUsers()
  }, [localUsers])

  useEffect(() => {
    if (!session.authenticate && session.account && session.account.user) {
      clearLocalUsers()
      clearToken()
    }
    storeLocalUser(session.account?.user)
    setIsDefaultUser(session.account?.user.id === 2147483647)
    setCanSwitchUser(session.authenticate && (!session.users || session.users.length > 1))
    updateSwitchUsers()
  }, [session])

  useEffect(() => {
    axios.defaults.headers.common['Authorization'] = token ? 'Bearer ' + token : undefined
    refresh()
  }, [token])

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
    if (tokenIsValid()) {
      axios.defaults.headers.common['Authorization'] = token ? 'Bearer ' + token : undefined
    }
    else {
      clearToken()
    }
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
    refresh()
  }, [initialized])

  return (
    <SessionContext.Provider value={{
      initialized: initialized,
      authenticate: session.authenticate,
      noAdminFound: session.noAdminFound,
      account: session.account,
      player: session.player,
      users: switchUsers,
      isDefaultUser: isDefaultUser,
      canSwitchUser: canSwitchUser,
      havePermission: havePermission,
      refresh: refresh,
      token: token,
      setToken: setToken,
      login: login,
      loginPin: loginPin,
      logout: logout,
      isLogout: isLogout,
      resetLogout: resetLogout,
      removeLocalUser: removeLocalUser,
      lastUserId: lastUserId,
      sseAs: sse,
      useSseAs: useSseAs,
      stopSse: stopSse,
      usePlayerSse: playerSse,
      stopPlayerSse: stopPlayerSse,
      startPlayerSse: startPlayerSse,
      uuid: uuid,
      setUuid: setUuid,
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
