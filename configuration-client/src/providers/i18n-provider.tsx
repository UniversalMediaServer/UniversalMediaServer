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

  const storeLanguageInLocalStorage = (language: string) => {
    localStorage.setItem('i18n', language);
  }

  const getLanguage = () => {
    return localStorage.getItem('i18n') || navigator.languages
    ? navigator.languages[0]
    : (navigator.language || 'en-US');
  }

  const updateLanguage = (language : string) => {
    storeLanguageInLocalStorage(language);
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
    updateLanguage(getLanguage());
	// eslint-disable-next-line react-hooks/exhaustive-deps
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