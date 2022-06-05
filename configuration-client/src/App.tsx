import { MantineProvider } from '@mantine/core';
import { NotificationsProvider } from '@mantine/notifications';
import {
  BrowserRouter as Router,
  Route,
  Routes
} from 'react-router-dom';
import './services/http-interceptor';

import Login from './components/Login/Login'
import ChangePassword from './components/ChangePassword/ChangePassword'
import Settings from './components/Settings/Settings';

function setToken(userToken: string) {
  localStorage.setItem('user', userToken);
}

function getToken() {
  return localStorage.getItem('user');
}

function App() {

  const token = getToken();

  if(!token) {
    return (
      <MantineProvider theme={{ colorScheme: 'dark' }} withGlobalStyles withNormalizeCSS>
        <Login setToken={setToken} />
      </MantineProvider>
      )
  }

  return (
    <MantineProvider theme={{ colorScheme: 'dark' }} withGlobalStyles withNormalizeCSS>
      <NotificationsProvider>
        <Router>
          <Routes>
            <Route path='/changepassword' element={<ChangePassword />}></Route>
            <Route path='/*' element={ <Settings />}></Route>
          </Routes>
        </Router>
      </NotificationsProvider>
    </MantineProvider>
  );
}

export default App;
