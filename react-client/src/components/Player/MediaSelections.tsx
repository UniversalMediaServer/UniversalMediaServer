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
import { BaseBrowse, PlayerInterface } from '../../services/player-service'
import { SessionInterface } from '../../services/session-service'
import MediaGrid from './MediaGrid'

export default function MediaSelections({ i18n, session, player, data }: { i18n: I18nInterface, session: SessionInterface, player: PlayerInterface, data: BaseBrowse }) {
  return data.mediasSelections
    ? (
        <>
          <MediaGrid i18n={i18n} session={session} player={player} mediaArray={data.mediasSelections.recentlyAdded} title="RecentlyAddedVideos" fixed />
          <MediaGrid i18n={i18n} session={session} player={player} mediaArray={data.mediasSelections.inProgress} title="InProgressVideos" fixed />
          <MediaGrid i18n={i18n} session={session} player={player} mediaArray={data.mediasSelections.recentlyPlayed} title="RecentlyPlayedVideos" fixed />
          <MediaGrid i18n={i18n} session={session} player={player} mediaArray={data.mediasSelections.mostPlayed} title="MostPlayedVideos" fixed />
        </>
      )
    : undefined
}
