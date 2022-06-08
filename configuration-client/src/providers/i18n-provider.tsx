import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import { ReactNode, useEffect, useState } from 'react';
import { i18nContext } from '../contexts/i18n-context';

interface Props {
  children?: ReactNode
}

export const I18nProvider = ({ children, ...props }: Props) =>{
  const [i18n, setI18n] = useState({})

  useEffect(() => {
    axios.get('/configuration-api/i18n')
      .then(function (response: any) {
        setI18n(response.data);
      })
      .catch(function (error: Error) {
        console.log(error);
        showNotification({
          id: 'data-loading',
          color: 'red',
          title: 'Error',
          message: 'Translations were not received from the server.',
          autoClose: 3000,
        });
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  
  const { Provider } = i18nContext;
  return(
    <Provider value={i18n}>
      {children}
    </Provider>
  )
}