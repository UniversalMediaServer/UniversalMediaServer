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
import { Button } from '@mantine/core'
import { useState } from 'react'

import { I18nInterface } from '../../services/i18n-service'
import { SessionInterface, UmsUserLogin } from '../../services/session-service'

export default function TokenLogin({ i18n, session, user }: { i18n: I18nInterface, session: SessionInterface, user: UmsUserLogin }) {
  const [isLoading, setLoading] = useState(false)
  return (
    <Button.Group>
      <Button
        disabled={isLoading}
        loading={isLoading}
        variant="default"
        onClick={() => {
          setLoading(true)
          if (user.token) {
            session.removeLocalUser(user.id)
          }
          setLoading(false)
        }}
      >
        {i18n.get('LogOut')}
      </Button>
      <Button
        disabled={isLoading}
        loading={isLoading}
        onClick={() => {
          setLoading(true)
          if (user.token) {
            session.setToken(user.token)
            session.resetLogout()
          }
          setLoading(false)
        }}
      >
        {i18n.get('LogIn')}
      </Button>
    </Button.Group>
  )
}
