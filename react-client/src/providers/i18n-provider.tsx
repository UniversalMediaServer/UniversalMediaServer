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
import { useLocalStorage } from '@mantine/hooks';
import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import { ReactNode, useEffect, useState } from 'react';
import { i18nContext, LanguageValue } from '../contexts/i18n-context';
import { i18nApiUrl } from '../utils';

interface Props {
  children?: ReactNode,
  rtl: boolean,
  setRtl:(val: boolean) => void,
}

export const I18nProvider = ({ rtl, setRtl, children, ...props }: Props) =>{
  const [i18n, setI18n] = useState<{[key: string]: string}>({});
  const [languages, setLanguages] = useState<LanguageValue[]>([]);
  const [language, setLanguage] = useLocalStorage<string>({
    key: 'language',
    defaultValue: navigator.languages
    ? navigator.languages[0]
    : (navigator.language || 'en-US'),
  });

  const getI18nString = (value: string) => {
    if (value && value.startsWith('i18n@')) {
      return i18n[value.substring(5)];
    } else {
      return value;
    }
  }

  const getI18nFormat = (value: string[]) => {
    if (value == null || value.length < 1) { return "";}
    let result = getI18nString(value[0]);
    for (let i = 1; i < value.length; i++) {
      const str = '%' + i.toString() + '$s';
      if (value[i].includes(str)) {
        result = result.replace(str, getI18nString(value[i]));
      } else if (value[i].includes('%s')) {
        result = result.replace('%s', getI18nString(value[i]));
      }
	}
	return result;
  }

  useEffect(() => {
    axios.post(i18nApiUrl, {language:language})
      .then(function (response: any) {
        setLanguages(response.data.languages);
        setI18n(response.data.i18n);
        setRtl(response.data.isRtl);
      })
      .catch(function () {
        showNotification({
          id: 'data-loading',
          color: 'red',
          title: 'Error',
          message: 'Languages were not received from the server.',
          autoClose: 3000,
        });
    });
  }, [language]);

  const { Provider } = i18nContext;
  return(
    <Provider value={{
      get: i18n,
      getI18nString: getI18nString,
      getI18nFormat: getI18nFormat,
      language: language,
      rtl: rtl,
      languages: languages,
      setLanguage: setLanguage
    }}>
      {children}
    </Provider>
  )
}