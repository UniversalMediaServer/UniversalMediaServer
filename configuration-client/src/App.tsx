import { MantineProvider } from '@mantine/core';
import { NotificationsProvider } from '@mantine/notifications';
import './services/http-interceptor';
import React, {useState} from 'react';
import { Route, Routes, Link, BrowserRouter } from "react-router-dom";
import Login from './components/Login/Login'

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
        <Settings />
      </NotificationsProvider>
    </MantineProvider>
  );
}

export default App;
