import { Button, Checkbox, Divider, Group, Modal, Pagination, ScrollArea, Select, Stack, Tooltip } from '@mantine/core';
import { showNotification } from '@mantine/notifications';
import { Prism } from '@mantine/prism';
import axios from 'axios';
import { useContext, useEffect, useState } from 'react';
import { Activity, FileDescription, FileZip, Filter } from 'tabler-icons-react';

import './prism-ums';
import I18nContext from '../../contexts/i18n-context';
import ServerEventContext from '../../contexts/server-event-context';
import SessionContext from '../../contexts/session-context';
import { havePermission } from '../../services/accounts-service';
import { allowHtml, defaultTooltipSettings } from '../../utils';
import _ from 'lodash';
import { sendAction } from '../../services/actions-service';

const Logs = () => {
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
  const sse = useContext(ServerEventContext);
  const canModify = havePermission(session, "settings_modify");
  const [rootLogLevel, setRootLogLevel] = useState(0);
  const [guiLogLevel, setGuiLogLevel] = useState(0);
  const [logLevel, setLogLevel] = useState(0);
  const [traceMode, setTraceMode] = useState(0);
  const [logs, setLogs] = useState([] as string[]);
  const [filteredLogs, setFilteredLogs] = useState([] as string[]);
  const [activeLogs, setActiveLogs] = useState([] as string[]);
  const [activePage, setActivePage] = useState(1);
  const [totalPage, setTotalPage] = useState(1);
  const [filterOpened, setFilterOpened] = useState(false);
  const [settingsOpened, setSettingsOpened] = useState(false);
  const [restartOpened, setRestartOpened] = useState(false);
  const [packerOpened, setPackerOpened] = useState(false);
  const [packerItems, setPackerItems] = useState([] as PackerItem[]);
  const [packerFiles, setPackerFiles] = useState([] as string[]);
  const apiUrl = '/v1/api/logs/';

  useEffect(() => {
    if (!canModify) {
      setLogs([]);
      return;
    }
    axios.get(apiUrl)
      .then(function (response: any) {
		setLogs(response.data.logs);
        setRootLogLevel(getLogLevel(response.data.rootLogLevel));
        setGuiLogLevel(getLogLevel(response.data.guiLogLevel));
        setLogLevel(getLogLevel(response.data.guiLogLevel));
		setTraceMode(response.data.traceMode);
      })
      .catch(function () {
        showNotification({
          id: 'logs-data-loading',
          color: 'red',
          title: i18n.get["Error"],
          message: i18n.get["DatasNotReceived"],
          autoClose: 3000,
        });
      });
  }, [i18n, canModify]);

  const getLogLevel = (level:string) => {
    if (level) {
      switch (level) {
        case 'ALL':
          return 6;
        case 'TRACE':
          return 5;
        case 'DEBUG':
          return 4;
        case 'INFO':
          return 3;
        case 'WARN':
          return 2;
        case 'ERROR':
          return 1;
      }
    }
	return 0;
  }

  useEffect(() => {
    const filterLogLevel = (logLine:string) => {
      if (logLevel === 0) { return false }
      if (logLevel === 6) { return true }
      const level = logLine.substring(9, 14);
      switch (level) {
        case 'TRACE':
          return logLevel > 4;
        case 'DEBUG':
          return logLevel > 3;
        case 'INFO ':
          return logLevel > 2;
        case 'WARN ':
          return logLevel > 1;
        case 'ERROR':
          return logLevel > 0;
      }
      return false;
    }
    const logsTemp = logs.filter(filterLogLevel);
    setFilteredLogs(logsTemp);
  }, [logs, logLevel]);

  useEffect(() => {
    const total = Math.max(Math.ceil(filteredLogs.length / 500), 1);
    setTotalPage(total);
    if (activePage > total) {
      setActivePage(total);
      return;
    }
    const max = 500 * activePage;
    const min = max - 500;
    const logsTemp = filteredLogs.slice(min, max);
    if (activeLogs.at(0) !== logsTemp.at(0) && activeLogs.at(-1) !== logsTemp.at(-1)) {
      setActiveLogs(logsTemp);
    }
  }, [activeLogs, activePage, filteredLogs]);

  useEffect(() => {
    if (!sse.hasNewLogLine) {
      return;
    }
    const logsTemp = _.clone(logs);
    while (sse.hasNewLogLine) {
      const logLine = sse.getNewLogLine();
      if (logLine === null) {
        break;
      }
      if (logsTemp.length > 10000) {
        logsTemp.slice(0, logsTemp.length - 10000);
      }
      logsTemp.push(logLine);
    }
	setLogs(logsTemp);
  }, [logs, sse]);

  useEffect(() => {
    if (!packerOpened) {
      return;
    }
    axios.get(apiUrl + 'packer')
      .then(function (response: any) {
        const items = response.data as PackerItem[];
        let selItems = [];
        for (const item of items) {
          if (item.exists) {
            selItems.push(item.path);
          }
        }
        setPackerItems(items);
		setPackerFiles(selItems);
      });
  }, [packerOpened]);

  const getPackerZip = () => {
    axios.post(apiUrl + 'packer', {items:packerFiles}, {responseType:'blob'})
      .then(function (response: any) {
        let fileName = response.headers["content-disposition"].split("filename=")[1];
		const type = response.headers['content-type']
		const blob = new Blob([response.data], { type: type })
		const link = document.createElement('a')
		link.href = window.URL.createObjectURL(blob)
		link.download = fileName
		link.click()
        document.removeChild(link);
      }
    );
  }

  const getPackerItems = () => {
    return packerItems.map((packerItem : PackerItem) => (
      <Checkbox value={packerItem.path} disabled={!packerItem.exists} label={packerItem.name} />
    ));	
  }

  return (
    <>
      <Modal
        centered
        opened={filterOpened}
        onClose={() => setFilterOpened(false)}
        title={i18n.get['Filter']}
      >
        <Select
          label={i18n.get['LogLevel']}
          value={logLevel.toString()}
          onChange={(value) => value === null ? 0 : setLogLevel(parseInt(value))}
          data={[
            { value: '1', label: i18n.get['Error'], disabled: guiLogLevel < 1 },
            { value: '2', label: i18n.get['Warning'], disabled: guiLogLevel < 2 },
            { value: '3', label: i18n.get['Info'], disabled: guiLogLevel < 3 },
            { value: '4', label: i18n.get['Debug'], disabled: guiLogLevel < 4 },
            { value: '5', label: i18n.get['Trace'], disabled: guiLogLevel < 5 },
            { value: '6', label: i18n.get['All'], disabled: guiLogLevel < 6 },
            { value: '0', label: i18n.get['Off'] },
          ]}
        />
      </Modal>
      <Modal
        centered
        overflow='inside'
        opened={settingsOpened}
        onClose={() => setSettingsOpened(false)}
        title={i18n.get['Settings']}
      >
        <Stack>
        <Tooltip label={allowHtml(i18n.get['FilterLogMessagesLogWindow'])} {...defaultTooltipSettings}>
          <Select
          label={i18n.get['Filter']}
          value={guiLogLevel.toString()}
          onChange={(value) => value === null ? 0 : setGuiLogLevel(parseInt(value))}
          data={[
            { value: '1', label: i18n.get['Error'], disabled: rootLogLevel < 1 },
            { value: '2', label: i18n.get['Warning'], disabled: rootLogLevel < 2 },
            { value: '3', label: i18n.get['Info'], disabled: rootLogLevel < 3 },
            { value: '4', label: i18n.get['Debug'], disabled: rootLogLevel < 4 },
            { value: '5', label: i18n.get['Trace'], disabled: rootLogLevel < 5 },
            { value: '6', label: i18n.get['All'], disabled: rootLogLevel < 6 },
            { value: '0', label: i18n.get['Off'] },
          ]}
        />
        </Tooltip>
        { traceMode === 0 &&
          <Tooltip label={allowHtml(i18n.get['RestartUniversalMediaServerTrace'])} {...defaultTooltipSettings}>
            <Button leftIcon={<FileDescription />} onClick={() => {setRestartOpened(true)}}>{i18n.get['CreateTraceLogs']}</Button>
          </Tooltip>
        }
        <Tooltip label={allowHtml(i18n.get['PackLogConfigurationFileCompressed'])} {...defaultTooltipSettings}>
          <Button leftIcon={<FileZip />} onClick={() => {setPackerOpened(true); if (traceMode === 0) {setRestartOpened(true)}}}>{i18n.get['PackDebugFiles']}</Button>
        </Tooltip>
        </Stack>
      </Modal>
      <Modal
        overflow='inside'
        centered
        opened={packerOpened}
        onClose={() => { setPackerOpened(false);}}
        title={i18n.get['PackDebugFiles']}
      >
        <Button disabled={packerFiles.length === 0} onClick={() => {getPackerZip();setPackerOpened(false);setSettingsOpened(false) }}>{i18n.get['ZipSelectedFiles']}</Button>
        <Checkbox.Group value={packerFiles} onChange={setPackerFiles} orientation='vertical'>
          { getPackerItems() }
        </Checkbox.Group>
      </Modal>
      <Modal
        centered
        opened={restartOpened}
        onClose={() => { setRestartOpened(false); setRestartOpened(false)}}
        title={i18n.get['StartupLogLevelNotTrace']}
      >
        <span dangerouslySetInnerHTML={{__html: i18n.get['ForReportingMostIssuesBest']}}></span>
        <Group>
          <Button onClick={() => {sendAction('Process.Reboot.Trace'); setRestartOpened(false); setSettingsOpened(false)}}>{i18n.get['Yes']}</Button>
          <Button onClick={() => {setRestartOpened(false)}}>{i18n.get['No']}</Button>
        </Group>
      </Modal>
      <Group>
        <Button leftIcon={<Filter />} onClick={() => {setFilterOpened(true)}}>{i18n.get['Filter']}</Button>
        <Button leftIcon={<Activity />} onClick={() => {setSettingsOpened(true)}}>{i18n.get['Options']}</Button>
      </Group>
      <Divider my="sm" />
      <Pagination page={activePage} onChange={setActivePage} total={totalPage} />
      <Divider my="sm" />
      <ScrollArea offsetScrollbars style={{height: 'calc(100vh - 275px)'}}>
        <Prism noCopy language={'ums' as any}>
          {activeLogs.join('')}
        </Prism>
	  </ScrollArea>
    </>
  );
};

interface PackerItem {
  name:string,
  path:string,
  exists:boolean
}

export default Logs;
