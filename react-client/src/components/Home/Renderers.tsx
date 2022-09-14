import { ActionIcon, Card, Grid, Group, Image, Menu, Modal, Progress, Table, Text } from '@mantine/core';
import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import _ from 'lodash';
import { useContext, useEffect, useState } from 'react';
import { Dots, ListDetails, ScreenShare, Settings } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import ServerEventContext from '../../contexts/server-event-context';
import SessionContext from '../../contexts/session-context';
import { havePermission, Permissions } from '../../services/accounts-service';
import { renderersApiUrl } from '../../utils';

const Renderers = () => {
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
  const sse = useContext(ServerEventContext);
  const canModify = havePermission(session, Permissions.settings_modify);
  const canControlRenderers = canModify; //should be a separate perm
  const [renderers, setRenderers] = useState([] as Renderer[]);
  const [askInfos, setAskInfos] = useState(-1);
  const [infos, setInfos] = useState(null as RendererInfos | null);


  useEffect(() => {
    axios.get(renderersApiUrl)
      .then(function (response: any) {
		setRenderers(response.data.renderers);
      })
      .catch(function () {
        showNotification({
          id: 'renderers-data-loading',
          color: 'red',
          title: i18n.get["Error"],
          message: i18n.get["DatasNotReceived"],
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

  useEffect(() => {
    if (askInfos < 0) {
      setInfos(null);
      return;
    }
    axios.post(renderersApiUrl + 'infos', {'id' : askInfos})
      .then(function (response: any) {
        setInfos(response.data);
      })
      .catch(function () {
        setInfos(null);
      });
  }, [askInfos]);

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
                <Menu.Item icon={<ListDetails size={14} />} onClick={() => setAskInfos(renderer.id)}>{i18n.get["Info"]}</Menu.Item>
                { canModify && (
                  <Menu.Item icon={<Settings size={14} />} color="red" disabled={true /* not implemented yet */}>{i18n.get["Settings"]}</Menu.Item>
                )}
                { canControlRenderers && (
                  <Menu.Item icon={<ScreenShare size={14} />} disabled={true /* not implemented yet */ || !renderer.isControllable}>{i18n.get["Controls"]}</Menu.Item>
                )}
              </Menu.Dropdown>
            </Menu>
          </Group>
        </Card.Section>
        <Card.Section>
          <Image
            src={renderersApiUrl + 'icon/' + renderer.id + '/' + renderer.icon}
            height={160}
            fit="contain"
            alt={renderer.name}
          />
        </Card.Section>
        { renderer.address && 
          <Text align="center" size="sm" color="dimmed" >
            {renderer.address}
          </Text>
        }
        { renderer.playing && 
          <Progress value={renderer.progressPercent} />
        }
        { renderer.playing && 
          <Text align="center" size="sm" color="dimmed" >
            {renderer.playing}
          </Text>
        }
        { renderer.time && 
          <Text align="center" size="sm" color="dimmed" >
            {renderer.time}
          </Text>
        }
      </Card>
    </Grid.Col>
  ));

  const rendererDetail = (
    <Modal
      centered
      overflow='inside'
      opened={infos != null}
      onClose={() => setAskInfos(-1)}
      title={infos?.title}
    >
      <Table><tbody>
        {infos?.details.map((detail:RendererDetail) => (
          <tr>
            <td>{i18n.getI18nString(detail.key)}</td>
            <td>{detail.value}</td>
          </tr>
        ))}
      </tbody></Table>
    </Modal>
  );

  return (
    <>
      {rendererDetail}
      <Grid>
        {renderersCards}
      </Grid>
    </>
  );
};

interface RendererAction extends Renderer {
  action:string,
}

interface Renderer {
  id : number,
  name : string,
  address : string,
  icon : string,
  playing : string,
  time : string,
  progressPercent : number,
  isActive : boolean,
  isControllable : boolean,
}

interface RendererInfos {
  title:string,
  isUpnp:boolean,
  details:RendererDetail[],
}

interface RendererDetail {
  key:string,
  value:string
}

export default Renderers;
