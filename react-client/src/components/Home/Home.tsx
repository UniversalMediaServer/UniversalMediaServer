import { Box, Tabs } from '@mantine/core';
import { useContext } from 'react';

import I18nContext from '../../contexts/i18n-context';
import Renderers from './Renderers';

const Home = () => {
  const i18n = useContext(I18nContext);

  return (
      <Box sx={{ maxWidth: 1024 }} mx="auto">
        <Tabs keepMounted={false} defaultValue="renderers">
          <Tabs.List>
            <Tabs.Tab value='renderers'>{i18n.get["DetectedMediaRenderers"]}</Tabs.Tab>
          </Tabs.List>
          <Tabs.Panel value="renderers" pt="xs">
            <Renderers/>
          </Tabs.Panel>
        </Tabs>
      </Box>
  );
};

export default Home;
