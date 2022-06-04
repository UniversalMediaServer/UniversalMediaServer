import { MantineProvider } from '@mantine/core';
import { NotificationsProvider } from '@mantine/notifications';
import React from 'react';
import { Route, Routes, Link, BrowserRouter } from "react-router-dom";
import Login from './components/Login/Login'

import Settings from './components/Settings/Settings';

function App() {
  return (
    <MantineProvider theme={{ colorScheme: 'dark' }} withGlobalStyles withNormalizeCSS>
      <BrowserRouter>
        <NotificationsProvider>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/" element={<Settings />} />
          </Routes>
        </NotificationsProvider>
      </BrowserRouter>
    </MantineProvider>
  );
}

export default App;
