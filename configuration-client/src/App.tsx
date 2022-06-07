import { AppShell, Header, MantineProvider, Group, ActionIcon, ColorSchemeProvider, ColorScheme } from '@mantine/core';
import { NotificationsProvider } from '@mantine/notifications';
import {
  BrowserRouter as Router,
  Route,
  Routes
} from 'react-router-dom';
import { useEffect } from 'react'; 
import rtlPlugin from 'stylis-plugin-rtl';
import './services/http-interceptor';

import Login from './components/Login/Login'
import { refreshAuthTokenNearExpiry } from './services/auth.service';
import ChangePassword from './components/ChangePassword/ChangePassword'
import Settings from './components/Settings/Settings';
import UserMenu from './components/UserMenu/UserMenu';
import { MoonStars, Sun, TextDirectionLtr, TextDirectionRtl } from 'tabler-icons-react';
import { useLocalStorage } from '@mantine/hooks';

function getToken() {
  return localStorage.getItem('user');
}

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

  const token = getToken();

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
                  <UserMenu />
                </Group>
              }</Header>}
              styles={(theme) => ({
                main: { backgroundColor: theme.colorScheme === 'dark' ? theme.colors.dark[8] : theme.colors.gray[0] },
              })}
            >
              {token ? (
                <Router>
                  <Routes>
                    <Route path='/changepassword' element={<ChangePassword />}></Route>
                    <Route path='/*' element={ <Settings />}></Route>
                  </Routes>
                </Router>
              ) : (
                <Login />
              )}
            </AppShell>
          </div>
        </NotificationsProvider>
      </MantineProvider>
    </ColorSchemeProvider>
  );
}

export default App;
