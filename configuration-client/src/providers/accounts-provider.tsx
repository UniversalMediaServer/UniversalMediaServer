import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import { ReactNode, useContext, useEffect, useState } from 'react';

import { accountsContext, UmsAccounts } from '../contexts/accounts-context';
import I18nContext from '../contexts/i18n-context';
import ServerEventContext from '../contexts/server-event-context';

interface Props {
  children?: ReactNode
}

export const AccountsProvider = ({ children, ...props }: Props) => {
  const [accounts, setAccounts] = useState({users:[],groups:[]} as UmsAccounts)
  const sse = useContext(ServerEventContext);
  const i18n = useContext(I18nContext);

  useEffect(() => {
    if (!sse.updateAccounts) {
      return;
    }
	sse.setUpdateAccounts(false);
    axios.get('/v1/api/account/accounts')
      .then(function (response: any) {
        setAccounts(response.data);
      })
      .catch(function (error: Error) {
        console.log(error);
        showNotification({
          id: 'accounts-data-loading',
          color: 'red',
          title: i18n.get['Error'],
          message: i18n.get['AccountsNotReceived'],
          autoClose: 3000,
        });
      });
  }, [i18n, sse]);

  const { Provider } = accountsContext;
  return(
    <Provider value={accounts}>
      {children}
    </Provider>
  )
}