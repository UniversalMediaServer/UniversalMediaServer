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
import { ReactNode, useState } from 'react'

import ManageNavbar from '../components/UmsApp/ManageNavbar'
import MainContext from '../contexts/main-context'
import { I18nInterface } from '../services/i18n-service'
import { SessionInterface } from '../services/session-service'

const MainProvider = ({ children }: { children?: ReactNode }) => {
  const [navbarValue, setNavbarValue] = useState<React.ReactNode>(undefined)
  const [navbarOpened, setNavbarOpened] = useState<boolean>(false)
  const [statusLine, setStatusLine] = useState(undefined)
  const setNavbarItem = (i18n: I18nInterface, session: SessionInterface, from: string) => {
    if (i18n && session && from) {
      setNavbarValue(<ManageNavbar i18n={i18n} session={session} from={from} />)
    }
    else {
      setNavbarValue('')
    }
  }

  return (
    <MainContext.Provider value={{
      navbarValue: navbarValue,
      setNavbarValue: setNavbarValue,
      navbarOpened: navbarOpened,
      setNavbarOpened: setNavbarOpened,
      statusLine: statusLine,
      setStatusLine: setStatusLine,
      setNavbarItem: setNavbarItem,
    }}
    >
      {children}
    </MainContext.Provider>
  )
}

export default MainProvider
