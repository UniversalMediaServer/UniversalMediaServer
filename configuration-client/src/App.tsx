import { AppShell, Box, Center, Header, MantineProvider, Group, ActionIcon, ColorSchemeProvider, ColorScheme, Loader } from '@mantine/core';
import { NotificationsProvider } from '@mantine/notifications';
import {
  BrowserRouter as Router,
  Route,
  Routes,
  Navigate,
} from 'react-router-dom';
import { useEffect } from 'react'; 
import rtlPlugin from 'stylis-plugin-rtl';
import './services/http-interceptor';

import Login from './components/Login/Login'
import FirstLogin from './components/FirstLogin/FirstLogin'
import Accounts from './components/Accounts/Accounts'
import { refreshAuthTokenNearExpiry } from './services/auth.service';
import Settings from './components/Settings/Settings';
import UserMenu from './components/UserMenu/UserMenu';
import { MoonStars, Sun, TextDirectionLtr, TextDirectionRtl } from 'tabler-icons-react';
import { useLocalStorage } from '@mantine/hooks';
import { I18nProvider } from './providers/i18n-provider';
import { SessionProvider } from './providers/session-provider';
import SessionContext from './contexts/session-context';

function App() {
  useEffect(() => {
    refreshAuthTokenNearExpiry();
  });

  const [rtl, setRtl] = useLocalStorage({
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

  return (
    <ColorSchemeProvider colorScheme={colorScheme} toggleColorScheme={toggleColorScheme}>
      <MantineProvider
        withGlobalStyles
        withNormalizeCSS
        emotionOptions={
          rtl
            ? // rtl cache
              { key: 'mantine-rtl', stylisPlugins: [rtlPlugin] }
            : // ltr cache
              { key: 'mantine' }
        }
        theme={{ colorScheme, dir: rtl ? 'rtl' : 'ltr' }}
      >
        <NotificationsProvider>
          <I18nProvider>
            <SessionProvider>
              <SessionContext.Consumer>
                {session => (
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
                          <ActionIcon variant="default" onClick={() => setRtl((c) => !c)} size={30}>
                            {rtl ? <TextDirectionLtr size={16} /> : <TextDirectionRtl size={16} />}
                          </ActionIcon>
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
                            <Route path='accounts' element={<Accounts />}></Route>
                            <Route path='settings' element={<Settings />}></Route>
                            <Route index element={<Settings />} />
                            <Route
                              path="/*"
                              element={<Navigate replace to="/" />}
                            />
                          </Routes>
                        </Router>
                      ) : session.firstLogin ? (
                        <FirstLogin />
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
