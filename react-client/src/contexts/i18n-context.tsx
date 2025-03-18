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
import { Direction } from '@mantine/core'
import { Context, createContext } from 'react'

import { I18nInterface, LanguageValue, ValueLabelData } from '../services/i18n-service'

const I18nContext: Context<I18nInterface> = createContext({
  get: (value: string) => { return value },
  getString: (value: string) => { return value },
  getFormat: (values: string[]) => { return values.length ? values[0] : '' },
  getValueLabelData: (values: ValueLabelData[] | undefined) => { return values },
  getLocalizedName: (value: string | undefined) => { return value ? value : '' },
  language: 'en-US',
  dir: 'ltr' as Direction,
  languages: [] as LanguageValue[],
  setLanguage: (_language: string) => { },
  getReportLink: () => { return undefined as React.ReactNode },
})

export default I18nContext
