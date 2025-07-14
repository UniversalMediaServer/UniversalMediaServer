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
import { AppShell, ScrollArea, Stack } from '@mantine/core'
import { I18nInterface } from '../../services/i18n-service'
import { SessionInterface } from '../../services/session-service'
import ManageNavbar from './ManageNavbar'

export default function SessionNavbar({ i18n, session }: { i18n: I18nInterface, session: SessionInterface }) {
  const AppNavbar = ({ children }: { children: React.ReactNode }) => (
    <AppShell.Navbar
      p="xs"
      bg={{ xs: 'default', sm: 'transparentBg' }}
    >
      <AppShell.Section grow my="md" component={ScrollArea}>
        <Stack gap={0}>
          {children}
        </Stack>
      </AppShell.Section>
    </AppShell.Navbar>
  )

  return session.navbarManage
    ? (
        <AppNavbar><ManageNavbar i18n={i18n} session={session} from={session.navbarManage} /></AppNavbar>
      )
    : session.navbarValue
      ? <AppNavbar>{session.navbarValue}</AppNavbar>
      : (
          undefined
        )
}
