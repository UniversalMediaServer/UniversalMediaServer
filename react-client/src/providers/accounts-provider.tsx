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
import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import { ReactNode, useContext, useEffect, useState } from 'react';

import { accountsContext, UmsAccounts } from '../contexts/accounts-context';
import I18nContext from '../contexts/i18n-context';
import ServerEventContext from '../contexts/server-event-context';
import { accountApiUrl } from '../utils';

interface Props {
  children?: ReactNode
}

export const AccountsProvider = ({ children, ...props }: Props) => {
  const [accounts, setAccounts] = useState({ users: [], groups: [], enabled: true, localhost: false } as UmsAccounts)
  const sse = useContext(ServerEventContext);
  const i18n = useContext(I18nContext);

  useEffect(() => {
    if (!sse.updateAccounts) {
      return;
    }
    sse.setUpdateAccounts(false);
    axios.get(accountApiUrl + 'accounts')
      .then(function(response: any) {
        setAccounts(response.data);
      })
      .catch(function() {
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
  return (
    <Provider value={accounts}>
      {children}
    </Provider>
  )
}