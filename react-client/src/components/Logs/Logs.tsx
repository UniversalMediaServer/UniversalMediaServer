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
import { Box, Button, Checkbox, Divider, Group, Modal, MultiSelect, Pagination, ScrollArea, SegmentedControl, Select, Stack, Switch, Text, TextInput, Tooltip } from '@mantine/core';
import { Dropzone, FileWithPath } from '@mantine/dropzone';
import { useForm } from '@mantine/form';
import { showNotification } from '@mantine/notifications';
import { Prism } from '@mantine/prism';
import axios from 'axios';
import _ from 'lodash';
import { useContext, useEffect, useState } from 'react';
import { Activity, FileDescription, FileZip, Filter, ListSearch } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import ServerEventContext from '../../contexts/server-event-context';
import SessionContext from '../../contexts/session-context';
import { havePermission, Permissions } from '../../services/accounts-service';
import { sendAction } from '../../services/actions-service';
import { allowHtml, defaultTooltipSettings, logsApiUrl } from '../../utils';
import './prism-ums';

const Logs = () => {
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
  const sse = useContext(ServerEventContext);
  const canModify = havePermission(session, Permissions.settings_modify);
  const [rootLogLevel, setRootLogLevel] = useState(0);
  const [guiLogLevel, setGuiLogLevel] = useState(0);
  const [logLevel, setLogLevel] = useState(0);
  const [logLevelFilter, setLogLevelFilter] = useState<string[]>([]);
  const [logSearchFilter, setLogSearchFilter] = useState<SearchValue>({search:'',isCapSensitive:true,isRexp:false});
  const [logSearchFilterIndexes, setLogSearchFilterIndexes] = useState<number[]>([]);
  const [logSearchFilterIndex, setLogSearchFilterIndex] = useState<number>(0);
  const [logThreads, setLogThreads] = useState<string[]>([]);
  const [logThreadFilter, setLogThreadFilter] = useState<string[]>([]);
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
  const [fileMode, setFileMode] = useState('');
  const searchForm = useForm({ initialValues: {search:'',isCapSensitive:true,isRexp:false} as SearchValue});

  const logLevels = [
    { value: '1', label: i18n.get['Error'] },
    { value: '2', label: i18n.get['Warning'] },
    { value: '3', label: i18n.get['Info'] },
    { value: '4', label: i18n.get['Debug'] },
    { value: '5', label: i18n.get['Trace'] },
  ];

  const allLogLevels = logLevels.concat([
    { value: '6', label: i18n.get['All'] },
    { value: '0', label: i18n.get['Off'] },
  ]);

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
    if (!canModify || fileMode) {
      setLogs([]);
      return;
    }
    axios.get(logsApiUrl)
      .then(function (response: any) {
        setLogs(response.data.logs);
        setLogThreadFilter([]);
        setLogThreads([]);
        setRootLogLevel(getLogLevel(response.data.rootLogLevel));
        setGuiLogLevel(getLogLevel(response.data.guiLogLevel));
        setLogLevel(getLogLevel(response.data.guiLogLevel));
        setTraceMode(response.data.traceMode);
      })
      .catch(function () {
        showNotification({
          id: 'logs-data-loading',
          color: 'red',
          title: i18n.get['Error'],
          message: i18n.get['DataNotReceived'],
          autoClose: 3000,
        });
      });
  }, [i18n, canModify, fileMode]);

  useEffect(() => {
    const filterLogLevelFilter = (level:string) => {
      if (logLevelFilter.length === 0 || logLevelFilter.length === 5) { return true }
      return logLevelFilter.includes(level);
    }
    const filterLogLevel = (logLine:string) => {
      if (logLevel === 0) { return false }
      if (logLevel === 6 && logLevelFilter.length === 0) { return true }
      const level = fileMode ? logLine.substring(0, 5) : logLine.substring(9, 14);
      switch (level) {
        case 'TRACE':
          return logLevel > 4 && filterLogLevelFilter('5');
        case 'DEBUG':
          return logLevel > 3 && filterLogLevelFilter('4');
        case 'INFO ':
          return logLevel > 2 && filterLogLevelFilter('3');
        case 'WARN ':
          return logLevel > 1 && filterLogLevelFilter('2');
        case 'ERROR':
          return logLevel > 0 && filterLogLevelFilter('1');
      }
      return (logLevel === 6);
    }
    const filterLogThread = (logLine:string) => {
      if (logThreadFilter.length === 0) { return true }
      const thread = logLine.substring(31, logLine.indexOf(']')).replace(/[\d|-]+$/, '');
      return logThreadFilter.includes(thread);
    }
    const logsTemp = logs.filter(filterLogLevel).filter(filterLogThread);
    setFilteredLogs(logsTemp);
  }, [logs, logLevel, logLevelFilter, logThreadFilter, fileMode]);

  const handleSearchSubmit = (values: SearchValue) => {
    if (logSearchFilter.search !== values.search || logSearchFilter.search !== values.search || logSearchFilter.search !== values.search) {
      setLogSearchFilter(values);
      setLogSearchFilterIndex(0);
    } else {
      setLogSearchFilterIndex(logSearchFilterIndex+1);
    }
  }

  useEffect(() => {
    const searchFilterLine = (logLine:string) => {
      if (!logSearchFilter.search) { return false }
      return logLine.includes(logSearchFilter.search);
    }
    const logsTemp = filteredLogs.map((e, i) => searchFilterLine(e) ? i : -1).filter(i => i !== -1);
    setLogSearchFilterIndexes(logsTemp);
  }, [filteredLogs, logSearchFilter]);

  useEffect(() => {
    if (logSearchFilterIndex < 0) { return }
    const reqIndex = logSearchFilterIndexes[logSearchFilterIndex];
    const reqPage = Math.max(Math.ceil(reqIndex / 500), 1);
    setActivePage(reqPage);
  }, [filteredLogs, logSearchFilterIndex, logSearchFilterIndexes]);

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
    if (!sse.hasNewLogLine || fileMode) {
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
  }, [logs, sse, fileMode]);

  useEffect(() => {
    if (!packerOpened) {
      return;
    }
    axios.get(logsApiUrl + 'packer')
      .then(function (response: any) {
        const items = response.data as PackerItem[];
        const selItems = [];
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
    axios.post(logsApiUrl + 'packer', {items:packerFiles}, {responseType:'blob'})
      .then(function (response: any) {
        const fileName = response.headers["content-disposition"].split("filename=")[1];
		const type = response.headers['content-type'];
		const blob = new Blob([response.data], { type: type });
		const link = document.createElement('a');
		link.href = window.URL.createObjectURL(blob);
		link.download = fileName;
		link.click();
		link.remove();
      }
    );
  }

  const getPackerItems = () => {
    return packerItems.map((packerItem : PackerItem) => (
      <Checkbox key={packerItem.path} value={packerItem.path} disabled={!packerItem.exists} label={packerItem.name} />
    ));	
  }

  const logFileReader = new FileReader();
  logFileReader.onloadend = (e) => {
    const contents = e?.target?.result;
    setLogFileLogs(contents as string);
  };

  const readLogFile = (files: FileWithPath[]) => {
    logFileReader.readAsText(files[0]);
  }

  const setLogFileLogs = (contents: string) => {
    const tmplines = contents.split(/\r?\n/);
    const lines = [] as string[];
    const threads = [] as string[];
    for (let i = 0; i < tmplines.length; i++) {
      if (tmplines[i].length > 30 && tmplines[i].substring(30, 31) === '[') {
        const thread = tmplines[i].substring(31, tmplines[i].indexOf(']')).replace(/[\d|-]+$/, '');
		if (!threads.includes(thread)) { threads.push(thread) }
        lines.push(tmplines[i]);
      } else {
        (lines.length === 0) ? lines.push(tmplines[i]) : lines[lines.length - 1] = lines[lines.length - 1].concat('\r', tmplines[i]);
      }
    }
	setLogThreads(threads);
    setLogs(lines);
  }

  return canModify ? (
    <Box sx={{ maxWidth: 1024 }} mx="auto">
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
          data={allLogLevels}
        />
        <Divider my="sm" />
        <form onSubmit={searchForm.onSubmit(handleSearchSubmit)}>
          <TextInput
            label={i18n.get['EnterSearchString']}
            {...searchForm.getInputProps('search')}
          />
          <Tooltip label={i18n.get['FilterLogMessagesLogWindow']} {...defaultTooltipSettings}>
            <Switch
              label={i18n.get['CaseSensitive']}
              {...searchForm.getInputProps('isCapSensitive', { type: 'checkbox' })}
            />
          </Tooltip>
          <Tooltip label={i18n.get['SearchUsingRegularExpressions']} {...defaultTooltipSettings}>
            <Switch
              label={i18n.get['RegularExpression']}
              {...searchForm.getInputProps('isRexp', { type: 'checkbox' })}
            />
          </Tooltip>
          <Button
            type="submit"
            leftIcon={<ListSearch />}
          >
            {i18n.get['Search']}
          </Button>
        </form>
        <MultiSelect
          label={i18n.get['FilterBySeverity']}
          value={logLevelFilter}
          onChange={setLogLevelFilter}
          data={logLevels}
        />
        {fileMode && (
          <MultiSelect
            label={i18n.get['FilterByThread']}
            value={logThreadFilter}
            onChange={setLogThreadFilter}
            data={logThreads}
            searchable
          />
        )}
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
      {!fileMode && (<>
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
      </>)}
      <Group>
        <SegmentedControl
          value={fileMode}
          onChange={setFileMode}
          data={[{ label: i18n.get['Server'], value: '' }, { label: i18n.get['File'], value: 'file' }]}
        />
        <Button leftIcon={<Filter />} onClick={() => {setFilterOpened(true)}}>{i18n.get['Filter']}</Button>
        {fileMode ?
          <Dropzone
            padding={5}
            onDrop={(files: FileWithPath[]) => readLogFile(files)}
            maxFiles={1}
          >
            <Text align="center">Drop/Select log here</Text>
          </Dropzone>
        :
          <Button leftIcon={<Activity />} onClick={() => {setSettingsOpened(true)}}>{i18n.get['Options']}</Button>
        }
      </Group>
      <Divider my="sm" />
      <Pagination page={activePage} onChange={setActivePage} total={totalPage} />
      <Divider my="sm" />
      <ScrollArea offsetScrollbars style={{height: 'calc(100vh - 275px)'}}>
        <Prism
          noCopy
          language={'ums' as any}
        >
          {activeLogs.join(fileMode ? '\n' : '')}
        </Prism>
      </ScrollArea>
    </Box>
  ) : (
    <Box sx={{ maxWidth: 1024 }} mx="auto">
      <Text color="red">{i18n.get['YouDontHaveAccessArea']}</Text>
    </Box>
  )
};

interface SearchValue {
  search:string,
  isCapSensitive:boolean,
  isRexp:boolean,
}

interface PackerItem {
  name:string,
  path:string,
  exists:boolean
}

export default Logs;
