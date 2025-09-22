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

export interface I18nInterface {
  get: (value: string) => string
  getString: (value: string) => string
  getFormat: (value: string[]) => string
  getValueLabelData: (values: ValueLabelData[] | undefined) => ValueLabelData[] | undefined
  getLocalizedName: (value: string | undefined) => string
  language: string
  dir: Direction
  languages: LanguageValue[]
  setLanguage: (language: string) => void
  getReportLink: () => React.ReactNode
  showServerUnreachable: () => void
  languageLoaded: boolean
  serverConnected: boolean
  serverReadyState: number
  setServerReadyState: (readyState: number) => void
}

export interface LanguageValue {
  id: string
  name: string
  defaultname: string
  country: string
  coverage: number
}

export interface ValueLabelData {
  value: string
  label: string
}
