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
import { AppShell, Box, Burger, Center, Group, Loader, ScrollArea, Stack, Text, useDirection } from '@mantine/core'

import { useEffect } from 'react'
import { Route, Routes, Navigate } from 'react-router-dom'

import About from './components/About/About'
import Accounts from './components/Accounts/Accounts'
import Actions from './components/Actions/Actions'
import ColorSchemeButton from './components/UmsApp/ColorSchemeButton'
import Home from './components/Home/Home'
import LanguagesMenu from './components/UmsApp/LanguagesMenu'
import Login from './components/Login/Login'
import Logs from './components/Logs/Logs'
import ModeButtons from './components/UmsApp/ModeButtons'
import Player from './components/Player/Player'
import PlayerLogin from './components/PlayerLogin/PlayerLogin'
import Settings from './components/Settings/Settings'
import SharedContent from './components/SharedContent/SharedContent'
import UserMenu from './components/UmsApp/UserMenu'
import I18nContext from './contexts/i18n-context'
import MainContext from './contexts/main-context'
import PlayerEventContext from './contexts/player-server-event-context'
import ServerEventContext from './contexts/server-event-context'
import SessionContext from './contexts/session-context'
import I18nProvider from './providers/i18n-provider'
import MainProvider from './providers/main-provider'
import PlayerEventProvider from './providers/player-server-event-provider'
import ServerEventProvider from './providers/server-event-provider'
import SessionProvider from './providers/session-provider'
import { havePermission, Permissions } from './services/accounts-service'
import { refreshAuthTokenNearExpiry, setAxiosAuthorization } from './services/auth-service'

function UmsApp() {
  const { dir } = useDirection()
  setAxiosAuthorization()

  useEffect(() => {
    refreshAuthTokenNearExpiry()
  })
  return (
    <I18nProvider>
      <I18nContext.Consumer>
        {i18n => (
          <MainProvider>
            <MainContext.Consumer>
              {main => (
                <SessionProvider i18n={i18n}>
                  <SessionContext.Consumer>
                    {session => (
                      <ServerEventProvider i18n={i18n} main={main} session={session}>
                        <ServerEventContext.Consumer>
                          {sse => (
                            <PlayerEventProvider i18n={i18n} session={session}>
                              <PlayerEventContext.Consumer>
                                {playersse => (
                                  <div dir={dir} className="bodyBackgroundImageScreen">
                                    <AppShell
                                      padding="md"
                                      navbar={main.navbarValue
                                        ? {
                                            width: { sm: 200, lg: 300 },
                                            breakpoint: 'sm',
                                            collapsed: { mobile: !main.navbarOpened, desktop: false },
                                          }
                                        : undefined}
                                      header={{ height: 60 }}
                                      bg="transparentBg"
                                    >
                                      {main.navbarValue && (
                                        <AppShell.Navbar
                                          p="xs"
                                          bg="transparentBg"
                                        >
                                          <AppShell.Section grow my="md" component={ScrollArea}>
                                            <Stack gap={0}>{main.navbarValue}</Stack>
                                          </AppShell.Section>
                                        </AppShell.Navbar>
                                      )}
                                      <AppShell.Header
                                        p="xs"
                                        bg="transparentBg"
                                      >
                                        <Group grow preventGrowOverflow={false} gap="xs">
                                          <Group justify="flex-start" gap="xs">
                                            {main.navbarValue
                                              && (
                                                <Burger
                                                  hiddenFrom="sm"
                                                  opened={main.navbarOpened}
                                                  onClick={() => main.setNavbarOpened(!main.navbarOpened)}
                                                  size="sm"
                                                />
                                              )}
                                            <ModeButtons i18n={i18n} session={session} />
                                            <ColorSchemeButton />
                                            <LanguagesMenu i18n={i18n} />
                                          </Group>
                                          <Group justify="flex-end">
                                            {session.account && session.account.user
                                              && <UserMenu i18n={i18n} session={session}></UserMenu>}
                                          </Group>
                                        </Group>
                                      </AppShell.Header>
                                      <AppShell.Main>
                                        {(session.initialized && session.player)
                                          ? (
                                              (!session.authenticate || session.account)
                                                ? (
                                                    <Routes>
                                                      <Route path="about" element={<About i18n={i18n} main={main} sse={sse} session={session} />}></Route>
                                                      <Route path="player" element={<Player i18n={i18n} main={main} session={session} sse={playersse} />}></Route>
                                                      <Route path="player/:req/:id" element={<Player i18n={i18n} main={main} session={session} sse={playersse} />}></Route>
                                                      <Route index element={<Player i18n={i18n} main={main} session={session} sse={playersse} />} />
                                                    </Routes>
                                                  )
                                                : (
                                                    <PlayerLogin i18n={i18n} main={main} session={session} />
                                                  )
                                            )
                                          : session.account
                                            ? (
                                                <Routes>
                                                  <Route path="about" element={<About i18n={i18n} main={main} sse={sse} session={session} />}></Route>
                                                  <Route path="accounts" element={<Accounts i18n={i18n} main={main} sse={sse} session={session} />}></Route>
                                                  <Route path="actions" element={<Actions i18n={i18n} main={main} session={session} />}></Route>
                                                  <Route path="logs" element={<Logs i18n={i18n} main={main} session={session} sse={sse} />}></Route>
                                                  <Route path="player" element={<Player i18n={i18n} main={main} session={session} sse={playersse} />}></Route>
                                                  <Route path="player/:req/:id" element={<Player i18n={i18n} main={main} session={session} sse={playersse} />}></Route>
                                                  <Route path="settings" element={<Settings i18n={i18n} main={main} sse={sse} session={session} />}></Route>
                                                  <Route path="shared" element={<SharedContent i18n={i18n} main={main} sse={sse} session={session} />}></Route>
                                                  {havePermission(session, Permissions.settings_view)
                                                    ? <Route index element={<Home i18n={i18n} main={main} sse={sse} session={session} />} />
                                                    : <Route index element={<Player i18n={i18n} main={main} session={session} sse={playersse} />} />}
                                                  <Route
                                                    path="/*"
                                                    element={<Navigate replace to="/" />}
                                                  />
                                                </Routes>
                                              )
                                            : session.initialized
                                              ? (
                                                  <Login i18n={i18n} main={main} session={session} />
                                                )
                                              : (
                                                  <Center>
                                                    <Box style={{ maxWidth: 1024 }} mx="auto">
                                                      <Loader size="xl" variant="dots" style={{ marginTop: '150px' }} />
                                                    </Box>
                                                  </Center>
                                                )}
                                      </AppShell.Main>
                                      {main.statusLine && (
                                        <AppShell.Footer>
                                          <Text>{main.statusLine}</Text>
                                        </AppShell.Footer>
                                      )}
                                    </AppShell>
                                  </div>
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
            </MainContext.Consumer>
          </MainProvider>
        )}
      </I18nContext.Consumer>
    </I18nProvider>
  )
}

export default UmsApp
