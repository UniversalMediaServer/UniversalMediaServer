import { useLocalStorage } from '@mantine/hooks';
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
  const [language, setLanguage] = useLocalStorage<string>({
    key: 'language',
    defaultValue: navigator.languages
    ? navigator.languages[0]
    : (navigator.language || 'en-US'),
  });
  const [rtl, setRtl] = useLocalStorage<boolean>({
    key: 'mantine-rtl',
    defaultValue: false,
  });

  const updateLanguage = (askedLanguage : string) => {
    setLanguage(askedLanguage);
    axios.post('/configuration-api/i18n', {language:askedLanguage})
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
  }

  useEffect(() => {
    updateLanguage(language);
	// eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const { Provider } = i18nContext;
  return(
    <Provider value={{
      get: i18n,
      language: language,
      rtl: rtl,
      languages: languages,
      updateLanguage: updateLanguage
    }}>
      {children}
    </Provider>
  )
}