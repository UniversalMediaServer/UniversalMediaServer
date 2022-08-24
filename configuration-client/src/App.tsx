import { ActionIcon, AppShell, Box, Center, ColorSchemeProvider, ColorScheme, createEmotionCache, Group, Header, Loader, MantineProvider } from '@mantine/core';
import { useLocalStorage } from '@mantine/hooks';
import { NotificationsProvider } from '@mantine/notifications';
import { useEffect } from 'react'; 
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
import Login from './components/Login/Login'
import Settings from './components/Settings/Settings';
import LanguagesMenu from './components/LanguagesMenu/LanguagesMenu';
import UserMenu from './components/UserMenu/UserMenu';
import SessionContext from './contexts/session-context';
import { I18nProvider } from './providers/i18n-provider';
import { AccountsProvider } from './providers/accounts-provider';
import { ServerEventProvider } from './providers/server-event-provider';
import { SessionProvider } from './providers/session-provider';
import { refreshAuthTokenNearExpiry } from './services/auth-service';

function App() {
  useEffect(() => {
    refreshAuthTokenNearExpiry();
  });

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
            <SessionProvider>
              <SessionContext.Consumer>
                {session => (
                  <ServerEventProvider>
                    <div dir={rtl ? 'rtl' : 'ltr'}>
                      <AppShell
                        padding="md"
                        // navbar={<Navbar width={{
                        //   // When viewport is larger than theme.breakpoints.sm, Navbar width will be 300
                        //   sm: 200,

                        //   // When other breakpoints do not match base width is used, defaults to 100%
                        //   base: 100,
                        // }} height={500} p="xs">{/* Navbar content */}</Navbar>}
                        header={<Header height={50} p="xs">{
                          <Group position="right">
                            <ActionIcon variant="default" onClick={() => toggleColorScheme()} size={30}>
                              {colorScheme === 'dark' ? <Sun size={16} /> : <MoonStars size={16} />}
                            </ActionIcon>
                            <LanguagesMenu />
                            {session.account && <UserMenu />}
                          </Group>
                        }</Header>}
                        styles={(theme) => ({
                          main: { backgroundColor: theme.colorScheme === 'dark' ? theme.colors.dark[8] : theme.colors.gray[0] },
                        })}
                      >
                        {session.account ? (
                          <Router>
                            <Routes>
                              <Route path='about' element={<About />}></Route>
                              <Route path='accounts' element={<AccountsProvider><Accounts /></AccountsProvider>}></Route>
                              <Route path='settings' element={<Settings />}></Route>
                              <Route index element={<Settings />} />
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
                            <Box sx={{ maxWidth: 700 }} mx="auto">
                              <Loader size="xl" variant="dots" sx={{marginTop: '150px'}}/>
                            </Box>
                          </Center>
                        )}
                      </AppShell>
                    </div>
                  </ServerEventProvider>
                )}
              </SessionContext.Consumer>
            </SessionProvider>
          </I18nProvider>
        </NotificationsProvider>
      </MantineProvider>
    </ColorSchemeProvider>
  );
}

export default App;
