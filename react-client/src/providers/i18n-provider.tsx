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
import { useDirection } from '@mantine/core';
import { useLocalStorage } from '@mantine/hooks';
import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import { ReactNode, useEffect, useState } from 'react';
import { ExclamationMark } from 'tabler-icons-react';

import I18nContext, { LanguageValue } from '../contexts/i18n-context';
import { i18nApiUrl } from '../utils';

interface Props {
  children?: ReactNode,
}

export const I18nProvider = ({ children }: Props) => {
  const { dir, setDirection } = useDirection();
  const [i18n, setI18n] = useState<{ [key: string]: string }>({
    'Error': 'Error',
    'LanguagesNotReceived': 'Languages were not received from the server.',
    'Warning': 'Warning',
    'UniversalMediaServerUnreachable': 'Universal Media Server unreachable'
  });
  const [version, setVersion] = useState<string>();
  const [languages, setLanguages] = useState<LanguageValue[]>([]);
  const [language, setLanguage] = useLocalStorage<string>({
    key: 'language',
    defaultValue: navigator.languages
      ? navigator.languages[0]
      : (navigator.language || 'en-US'),
  });

  const get = (value: string) => {
    return i18n[value] ? i18n[value] : value;
  }

  const getI18nString = (value: string) => {
    if (value && value.startsWith('i18n@')) {
      return get(value.substring(5));
    } else {
      return value;
    }
  }

  const getI18nFormat = (value: string[]) => {
    if (value == null || value.length < 1) { return ''; }
    let result = getI18nString(value[0]);
    for (let i = 1; i < value.length; i++) {
      const str = '%' + i.toString() + '$s';
      if (result.includes(str)) {
        result = result.replace(str, getI18nString(value[i]));
      } else if (result.includes('%s')) {
        result = result.replace('%s', getI18nString(value[i]));
      }
    }
    return result;
  }

  const getI18nLanguage = (language: string, version: string) => {
    axios.get(i18nApiUrl, { params: { language, version } })
      .then(function(response: any) {
        setLanguages(response.data.languages);
        setI18n(response.data.i18n);
        setDirection(response.data.isRtl ? 'rtl' : 'ltr');
      })
      .catch(function(error) {
        if (!error.response && error.request) {
          showNotification({
            color: 'red',
            title: i18n['Warning'],
            message: i18n['UniversalMediaServerUnreachable'],
            icon: <ExclamationMark size='1rem' />
          });
        } else {
          showNotification({
            id: 'data-loading',
            color: 'red',
            title: i18n['Error'],
            message: i18n['LanguagesNotReceived']
          });
        }
      });
  }

  const getI18nVersion = (language: string) => {
    axios.post(i18nApiUrl)
      .then(function(response: any) {
        setVersion(response.data.version);
        getI18nLanguage(language, response.data.version);
      })
      .catch(function(error) {
        if (!error.response && error.request) {
          showNotification({
            color: 'red',
            title: i18n['Warning'],
            message: i18n['UniversalMediaServerUnreachable'],
            icon: <ExclamationMark size='1rem' />
          });
        } else {
          showNotification({
            id: 'data-loading',
            color: 'red',
            title: i18n['Error'],
            message: i18n['LanguagesNotReceived']
          });
        }
      });
  }

  useEffect(() => {
    if (version) {
      getI18nLanguage(language, version);
    } else {
      getI18nVersion(language);
    }
  }, [language]);

  const { Provider } = I18nContext;
  return (
    <Provider value={{
      get: get,
      getI18nString: getI18nString,
      getI18nFormat: getI18nFormat,
      language: language || 'en-US',
      dir: dir,
      languages: languages,
      setLanguage: setLanguage
    }}>
      {children}
    </Provider>
  )
}
