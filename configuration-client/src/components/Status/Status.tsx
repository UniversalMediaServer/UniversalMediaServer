import { ActionIcon, Box, Card, Grid, Group, Image, Menu, Tabs, Text } from '@mantine/core';
import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import _ from 'lodash';
import { useContext, useEffect, useState } from 'react';
import { Dots, ListDetails, ScreenShare, Settings } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import ServerEventContext from '../../contexts/server-event-context';
import SessionContext from '../../contexts/session-context';
import { havePermission } from '../../services/accounts-service';

const Status = () => {
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
  const sse = useContext(ServerEventContext);
  const canModify = havePermission(session, "settings_modify");
  const canControl = true; //should be a perm
  const [status, setStatus] = useState({renderers : []} as StatusResponse);
  const [renderers, setRenderers] = useState([] as Renderer[]);

  useEffect(() => {
    axios.get('/v1/api/status/')
      .then(function (response: any) {
		setRenderers(response.data.renderers);
        setStatus(response.data);
      })
      .catch(function () {
        showNotification({
          id: 'console-data-loading',
          color: 'red',
          title: i18n.get["Error"],
          message: i18n.get["StatusNotReceived"],
          autoClose: 3000,
        });
      });
  }, [i18n]);

  useEffect(() => {
    if (!sse.hasRendererAction) {
      return;
    }
    const renderersTemp = _.cloneDeep(renderers);
    while (sse.hasRendererAction) {
      const rendererAction = sse.getRendererAction() as RendererAction;
      if (rendererAction === null) {
        break;
      }
      console.log(2,rendererAction);
      switch(rendererAction.action) {
        case 'renderer_add':
          renderersTemp.push(rendererAction);
        break;
        case 'renderer_delete':
          const delIndex = renderersTemp.findIndex(renderer => renderer.id === rendererAction.id);
          if (delIndex > -1) {
            renderersTemp.splice(delIndex, 1);
          }
        break;
        case 'renderer_update':
          const index = renderersTemp.findIndex(renderer => renderer.id === rendererAction.id);
          if (index > -1) {
            renderersTemp[index] = rendererAction;
          }
          break;
      }
    }
    setRenderers(renderersTemp);
  }, [renderers, sse]);

  const renderersCards = renderers.map((renderer:Renderer) => (
    <Grid.Col span={12} xs={6}>
      <Card shadow="sm" p="lg" radius="md" withBorder>
        <Card.Section withBorder inheritPadding py="xs">
          <Group position="apart">
            <Text weight={500} color={!renderer.isActive ? 'dimmed' : renderer.playing ? 'green' : ''}>{renderer.name}</Text>
            <Menu withinPortal position="bottom-end" shadow="sm">
              <Menu.Target>
                <ActionIcon>
                  <Dots size={16} />
                </ActionIcon>
              </Menu.Target>
              <Menu.Dropdown>
                <Menu.Item icon={<ListDetails size={14} />}>Details</Menu.Item>
                { canModify && (
                  <Menu.Item icon={<Settings size={14} />} color="red" disabled={true}>Settings</Menu.Item>
                )}
                { canControl && (
                  <Menu.Item icon={<ScreenShare size={14} />} disabled={true}>Controls</Menu.Item>
                )}
              </Menu.Dropdown>
            </Menu>
          </Group>
        </Card.Section>
        <Card.Section>
          <Image
            src={'/v1/api/status/icon/' + renderer.id + '/' + renderer.icon}
            height={160}
            fit="contain"
            alt={renderer.name}
          />
        </Card.Section>
        <Text align="center" size="sm" color="dimmed" >
          {renderer.name}
        </Text>
      </Card>
    </Grid.Col>
  ));

  return (
      <Box sx={{ maxWidth: 700 }} mx="auto">
        <Tabs defaultValue="renderers">
          <Tabs.List>
            <Tabs.Tab value='renderers'>{i18n.get["DetectedMediaRenderers"]}</Tabs.Tab>
          </Tabs.List>
          <Tabs.Panel value="renderers" pt="xs">
            <Grid>
              {renderersCards}
            </Grid>
          </Tabs.Panel>
        </Tabs>
      </Box>
  );
};

interface StatusResponse {
  renderers : Renderer[];
}

interface RendererAction extends Renderer {
  action:string,
}

interface Renderer {
  id : number,
  name : string,
  icon : string,
  playing : string,
  time : string,
  progressPercent : number,
  isActive : boolean,
}

export default Status;
