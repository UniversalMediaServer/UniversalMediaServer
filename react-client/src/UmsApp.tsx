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
import { useEffect } from 'react'

import UmsAppShell from './components/UmsApp/UmsAppShell'
import I18nContext from './contexts/i18n-context'
import PlayerEventContext from './contexts/player-server-event-context'
import ServerEventContext from './contexts/server-event-context'
import SessionContext from './contexts/session-context'
import I18nProvider from './providers/i18n-provider'
import PlayerEventProvider from './providers/player-server-event-provider'
import ServerEventProvider from './providers/server-event-provider'
import SessionProvider from './providers/session-provider'
import { refreshAuthTokenNearExpiry, setAxiosAuthorization } from './services/auth-service'

function UmsApp() {
  setAxiosAuthorization()

  useEffect(() => {
    refreshAuthTokenNearExpiry()
  })
  return (
    <I18nProvider>
      <I18nContext.Consumer>
        {i18n => (
          <SessionProvider i18n={i18n}>
            <SessionContext.Consumer>
              {session => (
                <ServerEventProvider i18n={i18n} session={session}>
                  <ServerEventContext.Consumer>
                    {sse => (
                      <PlayerEventProvider i18n={i18n} session={session}>
                        <PlayerEventContext.Consumer>
                          {playersse => (
                            <UmsAppShell i18n={i18n} session={session} sse={sse} playersse={playersse} />
                          )}
                        </PlayerEventContext.Consumer>
                      </PlayerEventProvider>
                    )}
                  </ServerEventContext.Consumer>
                </ServerEventProvider>
              )}
            </SessionContext.Consumer>
          </SessionProvider>
        )}
      </I18nContext.Consumer>
    </I18nProvider>
  )
}

export default UmsApp
