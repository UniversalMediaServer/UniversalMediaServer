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
import { ColorSchemeScript, DirectionProvider, MantineProvider } from '@mantine/core';
import { Notifications } from '@mantine/notifications';
import '@mantine/core/styles.css';
import '@mantine/dropzone/styles.css';
import '@mantine/notifications/styles.css';

import UmsApp from './UmsApp';

function App() {

  return (
    <DirectionProvider>
      <ColorSchemeScript defaultColorScheme='auto' localStorageKey='mantine-color-scheme' />
      <MantineProvider
        theme={{
          colors: {
            'darkTransparent': ['rgba(31, 32, 35, 0.6)', 'rgba(31, 32, 35, 0.6)', 'rgba(31, 32, 35, 0.6)', 'rgba(31, 32, 35, 0.6)', 'rgba(31, 32, 35, 0.6)', 'rgba(31, 32, 35, 0.6)', 'rgba(31, 32, 35, 0.6)', 'rgba(31, 32, 35, 0.6)', 'rgba(31, 32, 35, 0.6)', 'rgba(31, 32, 35, 0.6)'],
            'lightTransparent': ['rgba(255, 255, 255, 0.6)', 'rgba(255, 255, 255, 0.6)', 'rgba(255, 255, 255, 0.6)', 'rgba(255, 255, 255, 0.6)', 'rgba(255, 255, 255, 0.6)', 'rgba(255, 255, 255, 0.6)', 'rgba(255, 255, 255, 0.6)', 'rgba(255, 255, 255, 0.6)', 'rgba(255, 255, 255, 0.6)', 'rgba(255, 255, 255, 0.6)'],
          },
        }}
        defaultColorScheme='auto'
      >
        <Notifications autoClose={3000} />
        <UmsApp />
      </MantineProvider>
    </DirectionProvider>
  );
}

export default App;
