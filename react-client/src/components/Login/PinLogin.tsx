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
import { PinInput } from '@mantine/core'
import { useState } from 'react'

import { I18nInterface } from '../../services/i18n-service'
import { SessionInterface, UmsUserLogin } from '../../services/session-service'
import { showError } from '../../utils/notifications'

export default function PinLogin({ i18n, session, user }: { i18n: I18nInterface, session: SessionInterface, user: UmsUserLogin }) {
  const [isLoading, setLoading] = useState(false)
  const handlePinLogin = (id: number, pin: string) => {
    setLoading(true)
    session.loginPin(id, pin).then(
      () => {
        session.resetLogout()
      },
      () => {
        showError({
          id: 'pwd-error',
          title: i18n.get('Error'),
          message: i18n.get('ErrorLoggingIn'),
        })
      },
    )
    setLoading(false)
  }

  return (
    <PinInput
      disabled={isLoading}
      oneTimeCode
      mask
      type="number"
      onComplete={(value: string) => {
        handlePinLogin(user.id, value)
      }}
    />
  )
}
