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

import { SessionInterface, UmsUserLogin } from '../../services/session-service'

export default function PinLogin({ session, user }: { session: SessionInterface, user: UmsUserLogin }) {
  const [isLoading, setLoading] = useState(false)
  const handlePinLogin = (id: number, pin: string) => {
    setLoading(true)
    session.loginPin(id, pin).then(() =>
      setLoading(false),
    )
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
