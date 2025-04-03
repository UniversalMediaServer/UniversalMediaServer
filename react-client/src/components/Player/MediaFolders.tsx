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
import { I18nInterface } from '../../services/i18n-service'
import { PlayerEventInterface } from '../../services/player-server-event-service'
import { BaseBrowse } from '../../services/player-service'
import { SessionInterface } from '../../services/session-service'
import MediaGrid from './MediaGrid'

export default function MediaFolders({ i18n, session, sse, data }: { i18n: I18nInterface, session: SessionInterface, sse: PlayerEventInterface, data: BaseBrowse }) {
  return !session.playerNavbar
    ? data.mediaLibraryFolders && data.mediaLibraryFolders.length > 0
      ? (
          <>
            <MediaGrid i18n={i18n} session={session} sse={sse} mediaArray={data.mediaLibraryFolders} title="MediaLibrary" />
            <MediaGrid i18n={i18n} session={session} sse={sse} mediaArray={data.folders} title="YourFolders" />
          </>
        )
      : (
          <MediaGrid i18n={i18n} session={session} sse={sse} mediaArray={data.folders} />
        )
    : undefined
}
