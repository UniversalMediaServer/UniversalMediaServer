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
import { Box, Tabs } from '@mantine/core';
import { useContext } from 'react';

import I18nContext from '../../contexts/i18n-context';
import Renderers from './Renderers';

const Home = () => {
  const i18n = useContext(I18nContext);

  return (
    <Box sx={{ maxWidth: 1024 }} mx='auto'>
      <Tabs keepMounted={false} defaultValue='renderers'>
        <Tabs.List>
          <Tabs.Tab value='renderers'>{i18n.get['DetectedMediaRenderers']}</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value='renderers' pt='xs'>
          <Renderers />
        </Tabs.Panel>
      </Tabs>
    </Box>
  );
};

export default Home;
