import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import { ReactNode, useEffect, useState } from 'react';
import { sessionContext, UmsSession } from '../contexts/session-context';

interface Props {
  children?: ReactNode
}

export const SessionProvider = ({ children, ...props }: Props) =>{
  const [session, setSession] = useState({noAdminFound:false, initialized: false} as UmsSession)

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
          title: 'Error',
          message: 'Session was not received from the server.',
          autoClose: 3000,
        });
      });
  }
    refresh();
  }, []);

  const { Provider } = sessionContext;
  return(
    <Provider value={session}>
      {children}
    </Provider>
  )
}