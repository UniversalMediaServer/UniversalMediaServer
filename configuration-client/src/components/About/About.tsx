import { Box, Table, Tabs, Text } from '@mantine/core';
import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import { useContext, useEffect, useState } from 'react';

import I18nContext from '../../contexts/i18n-context';
import SessionContext from '../../contexts/session-context';
import { havePermission } from '../../services/accounts-service';
import MemoryBar from '../MemoryBar/MemoryBar';

const About = () => {
  const [activeTab, setActiveTab] = useState(0);
  const [aboutDatas, setAboutDatas] = useState({links:[]} as any);
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
  const canView = havePermission(session, "settings_view") || havePermission(session, "settings_modify");
  const linksRows = aboutDatas.links.map((link:{key:string,value:string}) => (
    <tr>
      <td><Text align="center" style={{cursor:'pointer'}} onClick={() => { window.open(link.value, '_blank'); }}>{link.key}</Text></td>
    </tr>
  ));

  useEffect(() => {
    axios.get('/v1/api/about/')
      .then(function (response: any) {
        setAboutDatas(response.data);
      })
      .catch(function (error: Error) {
        showNotification({
          id: 'about-data-loading',
          color: 'red',
          title: 'Error',
          message: 'Your datas was not received from the server.',
          autoClose: 3000,
        });
      });
  }, []);

  return (
      <Box sx={{ maxWidth: 700 }} mx="auto">
        <Tabs active={activeTab} onTabChange={setActiveTab}>
          <Tabs.Tab label="Application">
            <Table striped>
              <thead>
                <tr>
                  <th colSpan={2}><Text color="blue" size="lg" align="center">{aboutDatas.app}</Text></th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>{i18n.getI18nString("Version")}</td>
                  <td>{aboutDatas.version}</td>
                </tr>
                <tr>
                  <td>{i18n.getI18nString("Commit")}</td>
                  <td><Text style={{cursor:'pointer'}} onClick={() => { window.open(aboutDatas.commitUrl, '_blank'); }}>{aboutDatas.commit}</Text></td>
                </tr>
                <tr>
                  <td>{i18n.getI18nString("Website")}</td>
                  <td><Text style={{cursor:'pointer'}} onClick={() => { window.open(aboutDatas.website, '_blank'); }}>{aboutDatas.website}</Text></td>
                </tr>
                <tr>
                  <td>{i18n.getI18nString("Licence")}</td>
                  <td><Text style={{cursor:'pointer'}} onClick={() => { window.open(aboutDatas.licenceUrl, '_blank'); }}>{aboutDatas.licence}</Text></td>
                </tr>
              </tbody>
            { canView && <>
                <thead>
                  <tr>
                    <th colSpan={2}><Text color="blue" size="lg" align="center">{i18n.getI18nString("System")}</Text></th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>{i18n.getI18nString("Operating system")}</td>
                    <td>{aboutDatas.operatingSystem}</td>
                  </tr>
                  <tr>
                    <td>{i18n.getI18nString("System memory size")}</td>
                    <td>{aboutDatas.systemMemorySize}</td>
                  </tr>
                  <tr>
                    <td>{i18n.getI18nString("JVM memory max")}</td>
                    <td>{aboutDatas.jvmMemoryMax}</td>
                  </tr>
                  <tr>
                    <td>{i18n.getI18nString("JVM memory usage")}</td>
                    <td><MemoryBar decorate={false} /></td>
                  </tr>
                </tbody>
            </>}
            </Table>
          </Tabs.Tab>
          <Tabs.Tab label="Related links">
            <Table striped>
              {linksRows}
            </Table>
          </Tabs.Tab>
        </Tabs>
      </Box>
  );
};

export default About;
