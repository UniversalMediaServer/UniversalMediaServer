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
import { ValueLabelData } from './i18n-service'

export interface TranscodingEngineData {
  id: string
  name: string
  isAvailable: boolean
  purpose: number
  statusText: string[]
}

export interface SelectionSettingsData {
  allRendererNames: ValueLabelData[]
  audioCoverSuppliers: ValueLabelData[]
  enabledRendererNames: ValueLabelData[]
  ffmpegLoglevels: ValueLabelData[]
  fullyPlayedActions: ValueLabelData[]
  gpuEncodingH264AccelerationMethods: ValueLabelData[]
  gpuEncodingH265AccelerationMethods: ValueLabelData[]
  networkInterfaces: ValueLabelData[]
  sortMethods: ValueLabelData[]
  subtitlesCodepages: ValueLabelData[]
  subtitlesDepth: ValueLabelData[]
  subtitlesInfoLevels: ValueLabelData[]
  transcodingEngines: Record<string, TranscodingEngineData>
  transcodingEnginesPurposes: string[]
  upnpLoglevels: ValueLabelData[]
}
