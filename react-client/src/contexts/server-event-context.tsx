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
import { Context, createContext } from 'react'

import { RendererAction } from '../services/home-service'
import { ServerEventInterface } from '../services/server-event-service'

const ServerEventContext: Context<ServerEventInterface> = createContext({
  connectionStatus: 0,
  memory: { max: 0, used: 0, dbcache: 0, buffer: 0 },
  updateAccounts: false as boolean,
  setUpdateAccounts: (_updateAccounts: boolean) => { },
  reloadable: false as boolean,
  userConfiguration: null as Record<string, unknown> | null,
  setUserConfiguration: (_config: Record<string, unknown> | null) => { },
  mediaScan: false as boolean,
  hasRendererAction: false as boolean,
  getRendererAction: () => undefined as RendererAction | undefined,
  hasNewLogLine: false as boolean,
  getNewLogLine: () => undefined as string | undefined,
})

export default ServerEventContext
