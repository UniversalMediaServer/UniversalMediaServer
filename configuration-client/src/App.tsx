import { MantineProvider } from '@mantine/core';
import { NotificationsProvider } from '@mantine/notifications';
import React from 'react';

import Settings from './components/Settings/Settings';

function App() {
  return (
    <MantineProvider theme={{ colorScheme: 'dark' }} withGlobalStyles withNormalizeCSS>
      <NotificationsProvider>
        <Settings></Settings>
      </NotificationsProvider>
    </MantineProvider>
  );
}

export default App;
