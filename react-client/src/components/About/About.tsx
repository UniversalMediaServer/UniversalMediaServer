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
import { ActionIcon, Box, Group, Table, Tabs, Text } from '@mantine/core';
import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import { useContext, useEffect, useState } from 'react';
import ReactCountryFlag from 'react-country-flag';
import { Edit, EditOff } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import SessionContext from '../../contexts/session-context';
import { ServerEventProvider } from '../../providers/server-event-provider';
import { havePermission, Permissions } from '../../services/accounts-service';
import { aboutApiUrl } from '../../utils';
import MemoryBar from '../MemoryBar/MemoryBar';

const About = () => {
  const [aboutDatas, setAboutDatas] = useState({links:[]} as any);
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
  const canView = havePermission(session, Permissions.settings_view | Permissions.settings_modify);
  const languagesRows = i18n.languages.map((language) => (
    <tr key={language.id}>
      <td><Group style={{cursor: 'default'}}><ReactCountryFlag countryCode={language.country} style={{fontSize: '1.5em'}}/><Text>{language.name}</Text></Group></td>
      {language.id==='en-US' ? (
        <td><Group style={{cursor: 'default'}} position='right'><Text>{i18n.get['Source']}</Text><ActionIcon disabled><EditOff /></ActionIcon></Group></td>
      ) : (
        <td><Group style={{cursor: 'default'}} position='right'>
          <Text>{language.coverage === 100 ? i18n.get['Completed'] : i18n.get['InProgress'] + ' (' + language.coverage + '%)'}</Text>
          <ActionIcon variant="default" onClick={() => { window.open('https://crowdin.com/project/universalmediaserver/' + language.id, '_blank'); }}>
            <Edit />
          </ActionIcon>
        </Group></td>
      )}
    </tr>
  ));
  const linksRows = aboutDatas.links.map((link:{ key: string, value: string }) => (
    <tr key={link.key}>
      <td><Text align="center" style={{cursor:'pointer'}} onClick={() => { window.open(link.value, '_blank'); }}>{link.key}</Text></td>
    </tr>
  ));

  useEffect(() => {
    axios.get(aboutApiUrl)
      .then(function (response: any) {
        setAboutDatas(response.data);
      })
      .catch(function () {
        showNotification({
          id: 'about-data-loading',
          color: 'red',
          title: i18n.get['Error'],
          message: i18n.get['DataNotReceived'],
          autoClose: 3000,
        });
      });
  }, [i18n]);

  return (
      <Box sx={{ maxWidth: 1024 }} mx="auto">
        <Tabs defaultValue="application">
          <Tabs.List>
            <Tabs.Tab value='application'>{i18n.get['Application']}</Tabs.Tab>
            <Tabs.Tab value='translations'>{i18n.get['Translations']}</Tabs.Tab>
            <Tabs.Tab value='relatedLinks'>{i18n.get['RelatedLinks_title']}</Tabs.Tab>
          </Tabs.List>
          <Tabs.Panel value="application" pt="xs">
            <Table striped>
              <thead>
                <tr>
                  <th colSpan={2}><Text color="blue" size="lg" align="center">{aboutDatas.app}</Text></th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>{i18n.get['Version']}</td>
                  <td>{aboutDatas.version}</td>
                </tr>
                <tr>
                  <td>{i18n.get['GitCommitHash']}</td>
                  <td><Text style={{cursor:'pointer'}} onClick={() => { window.open(aboutDatas.commitUrl, '_blank'); }}>{aboutDatas.commit}</Text></td>
                </tr>
                <tr>
                  <td>{i18n.get['Website']}</td>
                  <td><Text style={{cursor:'pointer'}} onClick={() => { window.open(aboutDatas.website, '_blank'); }}>{aboutDatas.website}</Text></td>
                </tr>
                <tr>
                  <td>{i18n.get['Licence']}</td>
                  <td><Text style={{cursor:'pointer'}} onClick={() => { window.open(aboutDatas.licenceUrl, '_blank'); }}>{aboutDatas.licence}</Text></td>
                </tr>
              </tbody>
            { (canView && !session.player) && <>
                <thead>
                  <tr>
                    <th colSpan={2}><Text color="blue" size="lg" align="center">{i18n.get['System']}</Text></th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>{i18n.get['OperatingSystem']}</td>
                    <td>{aboutDatas.operatingSystem}</td>
                  </tr>
                  <tr>
                    <td>{i18n.get['SystemMemorySize']}</td>
                    <td>{aboutDatas.systemMemorySize}</td>
                  </tr>
                  <tr>
                    <td>{i18n.get['JVMMemoryMax']}</td>
                    <td>{aboutDatas.jvmMemoryMax}</td>
                  </tr>
                  <tr>
                    <td>{i18n.get['JVMMemoryUsage']}</td>
                    <td><ServerEventProvider><MemoryBar decorate={false} /></ServerEventProvider></td>
                  </tr>
                </tbody>
            </>}
            </Table>
          </Tabs.Panel>
          <Tabs.Panel value='translations'>
            <Table highlightOnHover>
              <tbody>
               {languagesRows}
              </tbody>
            </Table>
          </Tabs.Panel>
          <Tabs.Panel value='relatedLinks'>
            <Table>
              {linksRows}
            </Table>
          </Tabs.Panel>
        </Tabs>
      </Box>
  );
};

export default About;
