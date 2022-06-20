import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import { ReactNode, useEffect, useState } from 'react';
import { sessionContext, Session } from '../contexts/session-context';

interface Props {
  children?: ReactNode
}

export const SessionProvider = ({ ...props }: Props) =>{
  const [session, setSession] = useState({firstlogin:false} as Session)

  useEffect(() => {
    axios.get('/v1/api/auth/session')
      .then(function (response: any) {
        setSession(response.data);
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  
  const { Provider } = sessionContext;
  return(
    <Provider value={session}>
    </Provider>
  )
}