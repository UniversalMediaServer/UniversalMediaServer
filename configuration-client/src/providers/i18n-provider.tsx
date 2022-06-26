import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import { ReactNode, useEffect, useState } from 'react';
import { i18nContext, LanguageValue } from '../contexts/i18n-context';

interface Props {
  children?: ReactNode
}

export const I18nProvider = ({ children, ...props }: Props) =>{
  const [i18n, setI18n] = useState({})
  const [languages, setLanguages] = useState<LanguageValue[]>([]);
  const [language, setLanguage] = useState<string>('');
  const updateLanguage = (language : string) => {
    setLanguage(language);
    axios.post('/configuration-api/i18n', {language:language})
      .then(function (response: any) {
        setLanguages(response.data.languages);
		setI18n(response.data.i18n);
      })
      .catch(function (error: Error) {
        console.log(error);
        showNotification({
          id: 'data-loading',
          color: 'red',
          title: 'Error',
          message: 'Languages were not received from the server.',
          autoClose: 3000,
        });
      });
  }
  useEffect(() => {
    updateLanguage(language);
  }, []);

  const { Provider } = i18nContext;
  return(
    <Provider value={{
         get: i18n,
         languages: languages,
         updateLanguage: updateLanguage
       }}>
      {children}
    </Provider>
  )
}