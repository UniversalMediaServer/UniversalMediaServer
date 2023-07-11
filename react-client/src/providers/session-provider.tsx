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

import I18nContext from '../contexts/i18n-context';
import { sessionContext, UmsSession } from '../contexts/session-context';
import { authApiUrl } from '../utils';

interface Props {
  children?: ReactNode
}

export const SessionProvider = ({ children, ...props }: Props) => {
  const [session, setSession] = useState({ noAdminFound: false, authenticate: true, initialized: false, player: false } as UmsSession)
  const i18n = useContext(I18nContext);

  useEffect(() => {
    const refresh = () => {
      axios.get(authApiUrl + 'session')
        .then(function(response: any) {
          setSession({ ...response.data, initialized: true, refresh: refresh });
        })
        .catch(function() {
          showNotification({
            id: 'data-loading',
            color: 'red',
            title: i18n.get['Error'],
            message: i18n.get['SessionNotReceived'],
            autoClose: 3000,
          });
        });
    }
    if (!session.initialized) {
      refresh();
    }
  }, [session.initialized, i18n]);

  const { Provider } = sessionContext;
  return (
    <Provider value={session}>
      {children}
    </Provider>
  )
}
