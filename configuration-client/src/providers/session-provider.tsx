import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import { ReactNode, useContext, useEffect, useState } from 'react';

import I18nContext from '../contexts/i18n-context';
import { sessionContext, UmsSession } from '../contexts/session-context';

interface Props {
  children?: ReactNode
}

export const SessionProvider = ({ children, ...props }: Props) =>{
  const [session, setSession] = useState({noAdminFound:false, initialized: false} as UmsSession)
  const i18n = useContext(I18nContext);

  useEffect(() => {
    const refresh = () => {
      axios.get('/v1/api/auth/session')
        .then(function (response: any) {
          setSession({...response.data, initialized: true, refresh: refresh});
        })
        .catch(function (error: Error) {
          console.log(error);
          showNotification({
            id: 'data-loading',
            color: 'red',
            title: i18n.get['Error'],
            message: i18n.get['SessionNotReceived'],
            autoClose: 3000,
          });
        });
    }
    refresh();
  }, [i18n]);

  const { Provider } = sessionContext;
  return(
    <Provider value={session}>
      {children}
    </Provider>
  )
}
