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
import { ActionIcon, AppShell, Avatar, Box, Burger, Button, Center, Group, Loader, MantineTheme, ScrollArea, Stack, Text, useComputedColorScheme, useDirection, useMantineColorScheme } from '@mantine/core'

import { useEffect } from 'react'
import {
  BrowserRouter as Router,
  Route,
  Routes,
  Navigate,
} from 'react-router-dom'
import { IconMoonStars, IconSun, IconUser } from '@tabler/icons-react'

import About from './components/About/About'
import Accounts from './components/Accounts/Accounts'
import Actions from './components/Actions/Actions'
import Home from './components/Home/Home'
import LanguagesMenu from './components/LanguagesMenu/LanguagesMenu'
import Login from './components/Login/Login'
import Logs from './components/Logs/Logs'
import Player from './components/Player/Player'
import PlayerLogin from './components/PlayerLogin/PlayerLogin'
import Settings from './components/Settings/Settings'
import SharedContent from './components/SharedContent/SharedContent'
import UserMenu from './components/UserMenu/UserMenu'
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
  const { toggleColorScheme } = useMantineColorScheme()
  setAxiosAuthorization()
  useEffect(() => {
    refreshAuthTokenNearExpiry()
  })
  const computedColorScheme = useComputedColorScheme('dark', { getInitialValueInEffect: true })
  return (
    <Router>
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
                                        header={{ height: 50 }}
                                        styles={theme => ({
                                          main: { backgroundColor: computedColorScheme === 'dark' ? theme.colors.darkTransparent[8] : theme.colors.lightTransparent[0] },
                                        })}
                                      >
                                        {main.navbarValue && (
                                          <AppShell.Navbar
                                            p="xs"
                                            style={(theme: MantineTheme) => ({ backgroundColor: computedColorScheme === 'dark' ? theme.colors.darkTransparent[8] : theme.colors.lightTransparent[0] })}
                                          >
                                            <AppShell.Section grow my="md" component={ScrollArea}>
                                              <Stack gap={0}>{main.navbarValue}</Stack>
                                            </AppShell.Section>
                                          </AppShell.Navbar>
                                        )}
                                        <AppShell.Header
                                          p="xs"
                                          style={theme => ({ backgroundColor: computedColorScheme === 'dark' ? theme.colors.darkTransparent[8] : theme.colors.lightTransparent[0] })}
                                        >
                                          <Group justify="space-between">
                                            <Group justify="left">
                                              {main.navbarValue
                                                && (
                                                  <Burger
                                                    hiddenFrom="sm"
                                                    opened={main.navbarOpened}
                                                    onClick={() => main.setNavbarOpened((o: boolean) => !o)}
                                                    size="sm"
                                                    mr="xl"
                                                  />
                                                )}
                                              {session.account && session.account.user
                                                && (
                                                  <Button
                                                    variant="transparent"
                                                    style={() => ({ cursor: 'default', color: computedColorScheme === 'dark' ? 'white' : 'black' })}
                                                    leftSection={(
                                                      <Avatar radius="sm" size="sm" src={session.account.user.avatar !== '' ? session.account.user.avatar : null}>
                                                        {session.account.user.avatar === '' && <IconUser size={16} />}
                                                      </Avatar>
                                                    )}
                                                  >
                                                    {session.account.user.displayName}
                                                  </Button>
                                                )}
                                            </Group>
                                            <Group justify="right">
                                              <ActionIcon variant="default" onClick={() => toggleColorScheme()} size={30}>
                                                {computedColorScheme === 'dark' ? <IconSun size={16} /> : <IconMoonStars size={16} />}
                                              </ActionIcon>
                                              <LanguagesMenu i18n={i18n} />
                                              {session.account && <UserMenu i18n={i18n} session={session} />}
                                            </Group>
                                          </Group>
                                        </AppShell.Header>
                                        <AppShell.Main>
                                          {(session.initialized && session.player)
                                            ? (
                                                (!session.authenticate || session.account)
                                                  ? (
                                                      <Routes>
                                                        <Route path="about" element={<About i18n={i18n} main={main} session={session} sse={sse} />}></Route>
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
                                                    <Route path="about" element={<About i18n={i18n} main={main} session={session} sse={sse} />}></Route>
                                                    <Route path="accounts" element={<Accounts i18n={i18n} main={main} session={session} sse={sse} />}></Route>
                                                    <Route path="actions" element={<Actions i18n={i18n} main={main} session={session} />}></Route>
                                                    <Route path="logs" element={<Logs i18n={i18n} main={main} session={session} sse={sse} />}></Route>
                                                    <Route path="player" element={<Player i18n={i18n} main={main} session={session} sse={playersse} />}></Route>
                                                    <Route path="player/:req/:id" element={<Player i18n={i18n} main={main} session={session} sse={playersse} />}></Route>
                                                    <Route path="settings" element={<Settings i18n={i18n} main={main} session={session} sse={sse} />}></Route>
                                                    <Route path="shared" element={<SharedContent i18n={i18n} main={main} session={session} sse={sse} />}></Route>
                                                    {havePermission(session, Permissions.settings_view)
                                                      ? <Route index element={<Home i18n={i18n} main={main} session={session} sse={sse} />} />
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
    </Router>
  )
}

export default UmsApp
