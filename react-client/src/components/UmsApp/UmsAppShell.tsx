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

import { Route, Routes, Navigate } from 'react-router-dom'

import About from '../About/About'
import Accounts from '../Accounts/Accounts'
import Actions from '../Actions/Actions'
import ColorSchemeButton from './ColorSchemeButton'
import Customize from '../Customize/Customize'
import Home from '../Home/Home'
import LanguagesMenu from '../UmsApp/LanguagesMenu'
import Login from '../Login/Login'
import Logs from '../Logs/Logs'
import ModeButtons from './ModeButtons'
import Player from '../Player/Player'
import PlayerLogin from '../PlayerLogin/PlayerLogin'
import Settings from '../Settings/Settings'
import SharedContent from '../SharedContent/SharedContent'
import UserMenu from './UserMenu'
import { havePermission, Permissions } from '../../services/accounts-service'
import { I18nInterface } from '../../services/i18n-service'
import { MainInterface } from '../../services/main-service'
import { PlayerEventInterface } from '../../services/player-server-event-service'
import { SessionInterface } from '../../services/session-service'
import { ServerEventInterface } from '../../services/server-event-service'

export default function UmsAppShell({ i18n, main, session, sse, playersse }: { i18n: I18nInterface, main: MainInterface, session: SessionInterface, sse: ServerEventInterface, playersse: PlayerEventInterface }) {
  const { dir } = useDirection()
  return (
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
              <UserMenu i18n={i18n} session={session}></UserMenu>
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
                    <Route path="customize" element={<Customize i18n={i18n} main={main} session={session} />}></Route>
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
  )
}
