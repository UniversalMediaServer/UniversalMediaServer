import { Box, Tabs } from '@mantine/core';
import { useContext } from 'react';

import I18nContext from '../../contexts/i18n-context';
import SessionContext from '../../contexts/session-context';
import { havePermission, Permissions } from '../../services/accounts-service';
import Logs from './Logs';
import Renderers from './Renderers';

const Home = () => {
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
  const canModify = havePermission(session, Permissions.settings_modify);

  return (
      <Box sx={{ maxWidth: 700 }} mx="auto">
        <Tabs keepMounted={false} defaultValue="renderers">
          <Tabs.List>
            <Tabs.Tab value='renderers'>{i18n.get["DetectedMediaRenderers"]}</Tabs.Tab>
            {canModify && (
              <Tabs.Tab value='logs'>{i18n.get["Logs"]}</Tabs.Tab>
            )}
          </Tabs.List>
          <Tabs.Panel value="renderers" pt="xs">
            <Renderers/>
          </Tabs.Panel>
          {canModify && (
            <Tabs.Panel value="logs" pt="xs">
              <Logs/>
            </Tabs.Panel>
          )}
        </Tabs>
      </Box>
  );
};

export default Home;
