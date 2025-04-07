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
import { AppShell, Box, Burger, Center, Group, Loader, useDirection } from '@mantine/core'
import { useLocalStorage } from '@mantine/hooks'
import { IconServer, IconServerOff } from '@tabler/icons-react'
import { Route, Routes, Navigate } from 'react-router-dom'

import { I18nInterface } from '../../services/i18n-service'
import { PlayerEventInterface } from '../../services/player-server-event-service'
import { SessionInterface, UmsPermission } from '../../services/session-service'
import About from '../About/About'
import Accounts from '../Accounts/Accounts'
import Actions from '../Actions/Actions'
import BrowserSettings from '../BrowserSettings/BrowserSettings'
import ColorSchemeButton from './ColorSchemeButton'
import Home from '../Home/Home'
import LanguagesMenu from '../UmsApp/LanguagesMenu'
import Login from '../Login/Login'
import Logs from '../Logs/Logs'
import ModeButtons from './ModeButtons'
import Player from '../Player/Player'
import ServerSettings from '../ServerSettings/ServerSettings'
import SessionNavbar from './SessionNavbar'
import SharedContent from '../SharedContent/SharedContent'
import StatusLine from './StatusLine'
import UserMenu from './UserMenu'
import WebSocketClient from './WebSocketClient'

export default function UmsAppShell({ i18n, session, playersse }: { i18n: I18nInterface, session: SessionInterface, playersse: PlayerEventInterface }) {
  const { dir } = useDirection()
  const [navbarWidthSmall] = useLocalStorage<number>({
    key: 'mantine-navbar-width-sm',
    defaultValue: 250,
  })
  const [navbarWidthMedium] = useLocalStorage<number>({
    key: 'mantine-navbar-width-md',
    defaultValue: 300,
  })
  const [navbarWidthLarge] = useLocalStorage<number>({
    key: 'mantine-navbar-width-lg',
    defaultValue: 400,
  })
  return (
    <div dir={dir} className="bodyBackgroundImageScreen">
      <WebSocketClient i18n={i18n} session={session} />
      <AppShell
        padding="md"
        navbar={session.hasNavbar
          ? {
              width: { sm: navbarWidthSmall, md: navbarWidthMedium, lg: navbarWidthLarge },
              breakpoint: 'sm',
              collapsed: { mobile: !session.navbarOpened, desktop: false },
            }
          : undefined}
        header={{ height: 60 }}
        footer={session.statusLine ? { height: 30 } : undefined}
        bg="transparentBg"
      >
        <SessionNavbar i18n={i18n} session={session} />
        <AppShell.Header
          p="xs"
          bg="transparentBg"
        >
          <Group grow preventGrowOverflow={false} gap="xs">
            <Group justify="flex-start" gap="xs">
              {(session.hasNavbar)
                && (
                  <Burger
                    hiddenFrom="sm"
                    opened={session.navbarOpened}
                    onClick={() => session.setNavbarOpened(!session.navbarOpened)}
                    size="sm"
                  />
                )}
              <ModeButtons i18n={i18n} session={session} />
              <ColorSchemeButton />
              <LanguagesMenu i18n={i18n} />
            </Group>
            <Group justify="flex-end">
              <UserMenu i18n={i18n} session={session}></UserMenu>
            </Group>
          </Group>
        </AppShell.Header>
        <AppShell.Main>
          {(session.initialized && session.player)
            ? (
                (!session.authenticate || (session.account && !session.isLogout))
                  ? (
                      <Routes>
                        <Route path="about" element={<About i18n={i18n} session={session} />}></Route>
                        <Route path="customize" element={<BrowserSettings i18n={i18n} session={session} />}></Route>
                        <Route path="player" element={<Player i18n={i18n} session={session} sse={playersse} />}></Route>
                        <Route path="player/:req/:id" element={<Player i18n={i18n} session={session} sse={playersse} />}></Route>
                        <Route index element={<Player i18n={i18n} session={session} sse={playersse} />} />
                      </Routes>
                    )
                  : (
                      <Login i18n={i18n} session={session} />
                    )
              )
            : session.account && !session.isLogout
              ? (
                  <Routes>
                    <Route path="about" element={<About i18n={i18n} session={session} />}></Route>
                    <Route path="accounts" element={<Accounts i18n={i18n} session={session} />}></Route>
                    <Route path="actions" element={<Actions i18n={i18n} session={session} />}></Route>
                    <Route path="customize" element={<BrowserSettings i18n={i18n} session={session} />}></Route>
                    <Route path="logs" element={<Logs i18n={i18n} session={session} />}></Route>
                    <Route path="player" element={<Player i18n={i18n} session={session} sse={playersse} />}></Route>
                    <Route path="player/:req/:id" element={<Player i18n={i18n} session={session} sse={playersse} />}></Route>
                    <Route path="settings" element={<ServerSettings i18n={i18n} session={session} />}></Route>
                    <Route path="shared" element={<SharedContent i18n={i18n} session={session} />}></Route>
                    {session.havePermission(UmsPermission.settings_view)
                      ? <Route index element={<Home i18n={i18n} session={session} />} />
                      : <Route index element={<Player i18n={i18n} session={session} sse={playersse} />} />}
                    <Route
                      path="/*"
                      element={<Navigate replace to="/" />}
                    />
                  </Routes>
                )
              : session.initialized
                ? (
                    <Login i18n={i18n} session={session} />
                  )
                : (
                    <Center>
                      <Box style={{ maxWidth: 1024 }} mx="auto">
                        { i18n.serverReadyState === 0
                          ? (
                              <Group style={{ marginTop: '150px' }}>
                                <Loader type="dots" />
                                <IconServer size="4rem" />
                                <Loader type="dots" />
                              </Group>
                            )
                          : i18n.serverReadyState === 3
                            ? (
                                <Group style={{ marginTop: '150px' }}>
                                  <IconServerOff size="4rem" />
                                </Group>
                              )
                            : <Loader size="xl" type="dots" style={{ marginTop: '150px' }} />}
                      </Box>
                    </Center>
                  )}
        </AppShell.Main>
        <AppShell.Footer>
          <StatusLine session={session} />
        </AppShell.Footer>
      </AppShell>
    </div>
  )
}
