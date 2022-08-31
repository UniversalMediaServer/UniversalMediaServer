import { Box, Tabs } from '@mantine/core';
import { useContext } from 'react';

import Renderers from './Renderers';
import I18nContext from '../../contexts/i18n-context';

const Home = () => {
  const i18n = useContext(I18nContext);

  return (
      <Box sx={{ maxWidth: 700 }} mx="auto">
        <Tabs keepMounted={false} defaultValue="renderers">
          <Tabs.List>
            <Tabs.Tab value='renderers'>{i18n.get["DetectedMediaRenderers"]}</Tabs.Tab>
            <Tabs.Tab value='logs'>{i18n.get["Logs"]}</Tabs.Tab>
          </Tabs.List>
          <Tabs.Panel value="renderers" pt="xs">
            <Renderers/>
          </Tabs.Panel>
          <Tabs.Panel value="logs" pt="xs">
          </Tabs.Panel>
        </Tabs>
      </Box>
  );
};

export default Home;
