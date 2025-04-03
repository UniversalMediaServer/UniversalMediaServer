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
import { Anchor, useDirection } from '@mantine/core'
import { useLocalStorage } from '@mantine/hooks'
import { showNotification } from '@mantine/notifications'
import axios, { AxiosError, AxiosResponse } from 'axios'
import { ReactNode, useEffect, useState } from 'react'
import { IconServerOff } from '@tabler/icons-react'

import I18nContext from '../contexts/i18n-context'
import { LanguageValue, ValueLabelData } from '../services/i18n-service'
import { gitHubNewIssueUrl, i18nApiUrl } from '../utils'
import { showError } from '../utils/notifications'

const I18nProvider = ({ children }: { children?: ReactNode }) => {
  const { dir, setDirection } = useDirection()
  const [i18n, setI18n] = useState<{ [key: string]: string }>({
    Error: 'Error',
    LanguagesNotReceived: 'Languages were not received from the server.',
    Warning: 'Warning',
    UniversalMediaServerUnreachable: 'Universal Media Server unreachable',
  })
  const [version, setVersion] = useState<string>()
  const [languages, setLanguages] = useState<LanguageValue[]>([])
  const [language, setLanguage] = useLocalStorage<string>({
    key: 'language',
    defaultValue: navigator.languages
      ? navigator.languages[0]
      : (navigator.language || 'en-US'),
  })

  const get = (value: string) => {
    return i18n[value] ? i18n[value] : value
  }

  const getString = (value: string) => {
    if (value && value.startsWith('i18n@')) {
      return get(value.substring(5))
    }
    else {
      return value
    }
  }

  const getFormat = (value: string[]) => {
    if (value == null || value.length < 1) {
      return ''
    }
    let result = getString(value[0])
    for (let i = 1; i < value.length; i++) {
      const str = '%' + i.toString() + '$s'
      if (result.includes(str)) {
        result = result.replace(str, getString(value[i]))
      }
      else if (result.includes('%s')) {
        result = result.replace('%s', getString(value[i]))
      }
    }
    return result
  }

  const getValueLabelData = (values: ValueLabelData[] | undefined) => {
    return values?.map((value: ValueLabelData) => {
      return { value: value.value, label: getString(value.label) } as ValueLabelData
    })
  }

  const getLocalizedName = (name: string | undefined) => {
    const nameData = name ? name.split('|') : ['']
    if (nameData.length > 1) {
      return getFormat(nameData)
    }
    else {
      return getString(nameData[0])
    }
  }

  const getI18nLanguage = (language: string, version: string) => {
    axios.get(i18nApiUrl, { params: { language, version } })
      .then(function (response: AxiosResponse) {
        setLanguages(response.data.languages)
        setI18n(response.data.i18n)
        setDirection(response.data.isRtl ? 'rtl' : 'ltr')
      })
      .catch(function (error: AxiosError) {
        if (!error.response && error.request) {
          showServerUnreachable()
        }
        else {
          showError({
            title: i18n['Error'],
            message: i18n['LanguagesNotReceived'],
          })
        }
      })
  }

  const getI18nVersion = (language: string) => {
    axios.post(i18nApiUrl)
      .then(function (response: AxiosResponse) {
        setVersion(response.data.version)
        getI18nLanguage(language, response.data.version)
      })
      .catch(function (error: AxiosError) {
        if (!error.response && error.request) {
          showServerUnreachable()
        }
        else {
          showError({
            title: i18n['Error'],
            message: i18n['LanguagesNotReceived'],
          })
        }
      })
  }

  const getReportLink = () => {
    return (
      <Anchor fz="xs" href={gitHubNewIssueUrl} target="_blank">
        {get('ClickHereReportBug')}
      </Anchor>
    )
  }

  const showServerUnreachable = () => {
    showNotification({
      id: 'connection-lost',
      color: 'orange',
      title: get('Warning'),
      message: get('UniversalMediaServerUnreachable'),
      icon: <IconServerOff size="1rem" />,
      autoClose: false,
    })
  }

  useEffect(() => {
    if (version) {
      getI18nLanguage(language, version)
    }
    else {
      getI18nVersion(language)
    }
  }, [language])

  return (
    <I18nContext.Provider value={{
      get: get,
      getString: getString,
      getFormat: getFormat,
      getValueLabelData: getValueLabelData,
      getLocalizedName: getLocalizedName,
      language: language || 'en-US',
      dir: dir,
      languages: languages,
      setLanguage: setLanguage,
      getReportLink: getReportLink,
      showServerUnreachable: showServerUnreachable,
    }}
    >
      {children}
    </I18nContext.Provider>
  )
}

export default I18nProvider
