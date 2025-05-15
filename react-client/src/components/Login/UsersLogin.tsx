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
import { Group, ScrollArea, Stack } from '@mantine/core'
import { useEffect, useRef, useState } from 'react'

import { I18nInterface } from '../../services/i18n-service'
import { SessionInterface, UmsUserLogin } from '../../services/session-service'
import LocalhostLogin from './LocalhostLogin'
import LoginUserCard from './LoginUserCard'
import NoneLogin from './NoneLogin'
import PassLogin from './PassLogin'
import PinLogin from './PinLogin'
import TokenLogin from './TokenLogin'

export default function UsersLogin({ i18n, session }: { i18n: I18nInterface, session: SessionInterface }) {
  const [selectedUser, selectUser] = useState<UmsUserLogin | undefined>()
  const viewport = useRef<HTMLDivElement>(null)
  const hideCards = selectedUser?.id === 0 && session.users?.length == 1

  const getLastUserId = () => {
    if (session.users && session.users.length > 0) {
      const lastUserLogin = session.users.find((userLogin: UmsUserLogin) => userLogin.id === session.lastUserId)
      if (lastUserLogin) {
        return lastUserLogin
      }
      return session.users[0]
    }
    return undefined
  }

  const SelectedLogin = () => {
    if (selectedUser) {
      if (selectedUser.login === 'pass') {
        return <PassLogin i18n={i18n} session={session} user={selectedUser} />
      }
      if (selectedUser.login === 'localhost') {
        return <LocalhostLogin i18n={i18n} session={session} />
      }
      if (selectedUser.login === 'token') {
        return <TokenLogin i18n={i18n} session={session} user={selectedUser} />
      }
      if (selectedUser.login === 'pin') {
        return <PinLogin session={session} user={selectedUser} />
      }
      if (selectedUser.login === 'none') {
        return <NoneLogin i18n={i18n} session={session} user={selectedUser} />
      }
    }
    return undefined
  }

  useEffect(() => {
    selectUser(getLastUserId())
  }, [session.users])

  return (
    <Stack justify="center" align="center" h="100%">
      { !hideCards && (
        <ScrollArea
          scrollbars="x"
          h={130}
          viewportRef={viewport}
          overscrollBehavior="contain"
          onWheel={(e) => {
            e.preventDefault()
            viewport.current!.scrollBy({ top: 0, left: e.deltaY, behavior: 'smooth' })
          }}
        >
          <Group
            justify="center"
            grow
            preventGrowOverflow={false}
            wrap="nowrap"
          >
            {session.users?.map((user) => {
              return (
                <LoginUserCard key={user.id} i18n={i18n} user={user} selectedUser={selectedUser} selectUser={selectUser} />
              )
            })}
          </Group>
        </ScrollArea>
      )}
      <SelectedLogin />
    </Stack>
  )
}
