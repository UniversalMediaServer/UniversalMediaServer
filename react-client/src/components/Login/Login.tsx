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
import { Box, Text } from '@mantine/core'
import { useEffect } from 'react'

import { I18nInterface } from '../../services/i18n-service'
import { SessionInterface } from '../../services/session-service'
import NoAdminLogin from './NoAdminLogin'
import PassLogin from './PassLogin'
import UsersLogin from './UsersLogin'

export default function Login({ i18n, session }: { i18n: I18nInterface, session: SessionInterface }) {
  useEffect(() => {
    session.setDocumentI18nTitle('Login')
    session.stopSse()
    session.stopPlayerSse()
    session.setNavbarValue(undefined)
  }, [])

  return (
    <Box mx="auto" style={{ maxWidth: 300 }}>
      <Text size="xl">{session.serverName}</Text>
      <Text size="lg" pb="md">{i18n.get('LogIn')}</Text>
      {(session.noAdminFound && !session.player)
        ? <NoAdminLogin i18n={i18n} session={session} />
        : session.users
          ? <UsersLogin i18n={i18n} session={session} />
          : <PassLogin i18n={i18n} session={session} />}
    </Box>
  )
}
