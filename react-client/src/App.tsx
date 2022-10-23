import { ActionIcon, AppShell, Box, Center, ColorSchemeProvider, ColorScheme, createEmotionCache, Group, Header, Loader, MantineProvider, Navbar, MediaQuery, Burger, Stack, ScrollArea, Footer } from '@mantine/core';
import { useLocalStorage } from '@mantine/hooks';
import { NotificationsProvider } from '@mantine/notifications';
import { lazy, useEffect } from 'react'; 
import {
  BrowserRouter as Router,
  Route,
  Routes,
  Navigate,
} from 'react-router-dom';
import rtlPlugin from 'stylis-plugin-rtl';
import { MoonStars, Sun } from 'tabler-icons-react';

import './services/http-interceptor';
import About from './components/About/About'
import Accounts from './components/Accounts/Accounts'
import Actions from './components/Actions/Actions';
import Home from './components/Home/Home';
import LanguagesMenu from './components/LanguagesMenu/LanguagesMenu';
import Login from './components/Login/Login'
import Logs from './components/Logs/Logs'
import PlayerLogin from './components/PlayerLogin/PlayerLogin';
import Settings from './components/Settings/Settings';
import UserMenu from './components/UserMenu/UserMenu';
import NavbarContext from './contexts/navbar-context';
import SessionContext from './contexts/session-context';
import { I18nProvider } from './providers/i18n-provider';
import { AccountsProvider } from './providers/accounts-provider';
import { PlayerEventProvider } from './providers/player-server-event-provider';
import { ServerEventProvider } from './providers/server-event-provider';
import { SessionProvider } from './providers/session-provider';
import { refreshAuthTokenNearExpiry } from './services/auth-service';
import { NavbarProvider } from './providers/navbar-provider';

function App() {
  const Player = lazy(() => import('./components/Player/Player'));

  const [rtl] = useLocalStorage<boolean>({
    key: 'mantine-rtl',
    defaultValue: false,
  });
  const [colorScheme, setColorScheme] = useLocalStorage<ColorScheme>({
    key: 'mantine-color-scheme',
    defaultValue: 'dark',
  });
  const toggleColorScheme = (value?: ColorScheme) => {
    setColorScheme(value || (colorScheme === 'dark' ? 'light' : 'dark'));
  }

  const rtlCache = createEmotionCache({
    key: 'mantine-rtl',
    stylisPlugins: [rtlPlugin],
  });

  useEffect(() => {
    refreshAuthTokenNearExpiry();
  });

  return (
    <ColorSchemeProvider colorScheme={colorScheme} toggleColorScheme={toggleColorScheme}>
      <MantineProvider
        withGlobalStyles
        withNormalizeCSS
        emotionCache={
          rtl
            ? // rtl cache
              rtlCache
            : // ltr cache
              undefined
        }
        theme={{ colorScheme, dir: rtl ? 'rtl' : 'ltr' }}
      >
        <NotificationsProvider>
          <I18nProvider>
            <NavbarProvider><NavbarContext.Consumer>
            {navbar => (
              <SessionProvider><SessionContext.Consumer>
                {session => (
                  <div dir={rtl ? 'rtl' : 'ltr'}>
                    <AppShell
                      padding='md'
                      navbarOffsetBreakpoint='sm'
                      navbar={navbar.value &&
                        <Navbar
                          hiddenBreakpoint='sm'
                          hidden={!navbar.opened}
                          width={{ sm: 200, lg: 300 }}
                          p="xs"
                          sx={(theme) => ({backgroundColor: theme.colorScheme === 'dark' ? 'rgba(31, 32, 35, 0.6)' : 'rgba(255, 255, 255, 0.6)',})}
                        >
                          <Navbar.Section grow component={ScrollArea}><Stack spacing={0}>{navbar.value}</Stack></Navbar.Section>
                        </Navbar>}
                      header={
                        <Header
                          height={50}
                          p='xs'
                          sx={(theme) => ({backgroundColor: theme.colorScheme === 'dark' ? 'rgba(31, 32, 35, 0.6)' : 'rgba(255, 255, 255, 0.6)',})}
                        >{
                          <Group position='apart'>
                            <Group position='left'>
                              {navbar.value && <MediaQuery largerThan='sm' styles={{ display: 'none' }}>
                                <Burger
                                  opened={navbar.opened}
                                  onClick={() => navbar.setOpened((o : boolean) => !o)}
                                  size='sm'
                                  mr='xl'
                                />
                              </MediaQuery>}
                            </Group>
                            <Group position='right'>
                              <ActionIcon variant='default' onClick={() => toggleColorScheme()} size={30}>
                                {colorScheme === 'dark' ? <Sun size={16} /> : <MoonStars size={16} />}
                              </ActionIcon>
                              <LanguagesMenu />
                              {session.account && <UserMenu />}
                            </Group>
                          </Group>
                        }</Header>
                      }
                      footer={
                        <Footer height={0}>
                        </Footer>
                      }
                      styles={(theme) => ({
                        main: { backgroundColor: theme.colorScheme === 'dark' ? theme.colors.dark[8] : theme.colors.gray[0] },
                      })}
                    >
                      {(session.initialized && session.player) ? (
                        (!session.authenticate || session.account) ? (
                          <Router>
                            <Routes>
                              <Route path='about' element={<About />}></Route>
                              <Route path='player' element={<PlayerEventProvider><Player /></PlayerEventProvider>}></Route>
                              <Route index element={<PlayerEventProvider><Player /></PlayerEventProvider>} />
                            </Routes>
                          </Router>
                        ) : (
                          <PlayerLogin />
                        )
                      ) : session.account ? (
                        <Router>
                          <Routes>
                            <Route path='about' element={<ServerEventProvider><About /></ServerEventProvider>}></Route>
                            <Route path='accounts' element={<ServerEventProvider><AccountsProvider><Accounts /></AccountsProvider></ServerEventProvider>}></Route>
                            <Route path='actions' element={<Actions />}></Route>
                            <Route path='logs' element={<ServerEventProvider><Logs /></ServerEventProvider>}></Route>
                            <Route path='player' element={<PlayerEventProvider><Player /></PlayerEventProvider>}></Route>
                            <Route path='settings' element={<ServerEventProvider><Settings /></ServerEventProvider>}></Route>
                            <Route index element={<ServerEventProvider><Home /></ServerEventProvider>} />
                            <Route
                              path="/*"
                              element={<Navigate replace to="/" />}
                            />
                          </Routes>
                        </Router>
                      ) : session.initialized ? (
                        <Login />
                      ) : (
                        <Center>
                          <Box sx={{ maxWidth: 1024 }} mx='auto'>
                            <Loader size='xl' variant='dots' sx={{marginTop: '150px'}}/>
                          </Box>
                        </Center>
                      )}
                    </AppShell>
                  </div>
                )}
              </SessionContext.Consumer></SessionProvider>
            )}
            </NavbarContext.Consumer></NavbarProvider>
          </I18nProvider>
        </NotificationsProvider>
      </MantineProvider>
    </ColorSchemeProvider>
  );
}

export default App;
