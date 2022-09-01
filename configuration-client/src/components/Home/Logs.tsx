import { Divider, Pagination, Select, Tooltip } from '@mantine/core';
import { showNotification } from '@mantine/notifications';
import './prism-ums';
import { Prism } from '@mantine/prism';
import axios from 'axios';
import { useContext, useEffect, useState } from 'react';

import I18nContext from '../../contexts/i18n-context';
import SessionContext from '../../contexts/session-context';
import { havePermission } from '../../services/accounts-service';

const Logs = () => {
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
  const canModify = havePermission(session, "settings_modify");
  const [logLevel, setLogLevel] = useState<string | null>(null);
  const [logs, setLogs] = useState([] as string[]);
  const [activeLogs, setActiveLogs] = useState([] as string[]);
  const [activePage, setActivePage] = useState(1);
  const [totalPage, setTotalPage] = useState(1);
  const apiUrl = '/v1/api/logs/';

  useEffect(() => {
    axios.get(apiUrl)
      .then(function (response: any) {
		setLogs(response.data.logs);
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
  }, [i18n]);

  useEffect(() => {
    const total = Math.max(Math.ceil(logs.length / 500), 1);
    setTotalPage(total);
    if (activePage > total) {
      setActivePage(total);
      return;
    }
    const max = 500 * activePage;
    const min = max - 500;
    const logsTemp = logs.slice(min, max);
    setActiveLogs(logsTemp);
  }, [activePage, logs]);

  return (
    <>
      <Tooltip label={i18n.get['SetRootLoggingLevelDecides']}>
        <Select
          label={i18n.get['LogLevel']}
          disabled={!canModify}
          value={logLevel}
          onChange={setLogLevel}
          data={[
            { value: 'ERROR', label: i18n.get['Error'] },
            { value: 'WARN', label: i18n.get['Warning'] },
            { value: 'INFO', label: i18n.get['Info'] },
            { value: 'DEBUG', label: i18n.get['Debug'] },
            { value: 'TRACE', label: i18n.get['Trace'] },
            { value: 'ALL', label: i18n.get['All'] },
            { value: 'OFF', label: i18n.get['Off'] },
          ]}
        />
      </Tooltip>
      <Tooltip label={i18n.get['FilterLogMessagesLogWindow']}>
        <Select
          label={i18n.get['Filter']}
          disabled={!canModify}
          value={logLevel}
          onChange={setLogLevel}
          data={[
            { value: 'ERROR', label: i18n.get['Error'] },
            { value: 'WARN', label: i18n.get['Warning'] },
            { value: 'INFO', label: i18n.get['Info'] },
            { value: 'DEBUG', label: i18n.get['Debug'] },
            { value: 'TRACE', label: i18n.get['Trace'] },
            { value: 'ALL', label: i18n.get['All'] },
            { value: 'OFF', label: i18n.get['Off'] },
          ]}
        />
      </Tooltip>
      <Pagination page={activePage} onChange={setActivePage} total={totalPage} />
      <Divider my="sm" />
      <Prism noCopy language={'ums' as any}>
        {activeLogs.join('')}
      </Prism>
      <Divider my="sm" />
      <Pagination page={activePage} onChange={setActivePage} total={totalPage} />
    </>
  );
};

export default Logs;
