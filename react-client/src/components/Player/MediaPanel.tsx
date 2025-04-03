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
import { BaseBrowse, VideoMedia } from '../../services/player-service'
import MediaMetadataPanel from './MediaMetadataPanel'
import MediaShowPanel from './MediaShowPanel'
import MediaPanelMenu from './MediaPanelMenu'

export default function MediaPanel({ i18n, sse, data, refreshPage, setLoading }: { i18n: I18nInterface, sse: PlayerEventInterface, data: BaseBrowse, refreshPage: () => void, setLoading: (loading: boolean) => void }) {
  const media = data.goal === 'show' ? data.medias[0] : data.breadcrumbs[data.breadcrumbs.length - 1]
  const metadata = data.goal === 'show' ? (media as BaseBrowse | VideoMedia).metadata : data.metadata

  return metadata
    ? (
        <MediaMetadataPanel i18n={i18n} sse={sse} media={media} metadata={metadata}>
          <MediaPanelMenu i18n={i18n} sse={sse} data={data} refreshPage={refreshPage} setLoading={setLoading} />
        </MediaMetadataPanel>
      )
    : data.goal === 'show'
      ? (
          <MediaShowPanel i18n={i18n} sse={sse} media={media}>
            <MediaPanelMenu i18n={i18n} sse={sse} data={data} refreshPage={refreshPage} setLoading={setLoading} />
          </MediaShowPanel>
        )

      : undefined
}
