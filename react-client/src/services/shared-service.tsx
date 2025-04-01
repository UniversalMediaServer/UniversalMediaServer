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
import axios from 'axios'
import { UmsGroup } from './session-service'
import { sharedApiUrl } from '../utils'

export interface SharedContentInterface {
  type: string
  active: boolean
  groups: number[]
}

export interface Folder extends SharedContentInterface {
  file: string
  monitored: boolean
  metadata: boolean
}

export interface NamedSharedContent extends SharedContentInterface {
  parent: string
  name: string
}

export interface VirtualFolder extends NamedSharedContent {
  childs: SharedContentInterface[]
  addToMediaLibrary: boolean
}

export interface Feed extends NamedSharedContent {
  uri: string
}

export interface Stream extends Feed {
  thumbnail: string
}

export interface ITunes extends SharedContentInterface {
  path: string
}

export interface SharedContentConfiguration {
  shared_content: SharedContentInterface[]
  show_itunes_library: boolean
  show_iphoto_library: boolean
  show_aperture_library: boolean
  groups: UmsGroup[]
}

export interface SharedContentData extends Record<string, unknown> {
  index: number
  type: string
  groups: string[]
  name: string
  parent: string
  source: string
  childs: SharedContentInterface[]
}

export const getFeedName = async (uri: string): Promise<string> => {
  const response: { data: { name: string } } = await axios.post(sharedApiUrl + 'web-content-name', { source: uri })
  return response.data.name
}
