import { useLocalStorage } from '@mantine/hooks';
import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import { ReactNode, useEffect, useState } from 'react';
import { i18nContext, LanguageValue } from '../contexts/i18n-context';

interface Props {
  children?: ReactNode
}

export const I18nProvider = ({ children, ...props }: Props) =>{
  const [i18n, setI18n] = useState<{[key: string]: string}>({});
  const [languages, setLanguages] = useState<LanguageValue[]>([]);
  const [rtl, setRtl] = useLocalStorage<boolean>({
    key: 'mantine-rtl',
    defaultValue: false,
  });
  const [language, setLanguage] = useLocalStorage<string>({
    key: 'language',
    defaultValue: navigator.languages
    ? navigator.languages[0]
    : (navigator.language || 'en-US'),
  });

  const getI18nString = (value: string) => {
    if (value.startsWith('i18n@')) {
      return i18n[value.substring(5)];
    } else {
      return value;
    }
  }

  useEffect(() => {
    axios.post('/configuration-api/i18n', {language:language})
      .then(function (response: any) {
        setLanguages(response.data.languages);
        setI18n(response.data.i18n);
        setRtl(response.data.isRtl);
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
  }, [language, setRtl]);

  const { Provider } = i18nContext;
  return(
    <Provider value={{
      get: i18n,
	  getI18nString: getI18nString,
      language: language,
      rtl: rtl,
      languages: languages,
      setLanguage: setLanguage
    }}>
      {children}
    </Provider>
  )
}