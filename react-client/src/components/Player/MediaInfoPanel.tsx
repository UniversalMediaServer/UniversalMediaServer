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
import { Stack } from '@mantine/core'

import { I18nInterface } from '../../services/i18n-service'
import { MediaInfo } from '../../services/player-service'
import MediaInfoAudiotrack from './MediaInfoAudiotrack'
import MediaInfoStringValue from './MediaInfoStringValue'
import MediaInfoSubtitles from './MediaInfoSubtitles'
import MediaInfoVideotrack from './MediaInfoVideotrack'
import MediaInfoImage from './MediaInfoImage'

export default function MediaInfoPanel({ i18n, mediaInfo }: { i18n: I18nInterface, mediaInfo?: MediaInfo }) {
  return mediaInfo && (
    <Stack>
      <MediaInfoStringValue value={mediaInfo.container} title={i18n.get('Format')} />
      {mediaInfo.videotracks && mediaInfo.videotracks.map((videoTrack, index) =>
        <MediaInfoVideotrack key={index} i18n={i18n} videoTrack={videoTrack} />,
      )}
      {mediaInfo.audiotracks && mediaInfo.audiotracks.map((audioTrack, index) =>
        <MediaInfoAudiotrack key={index} i18n={i18n} audioTrack={audioTrack} />,
      )}
      {mediaInfo.subtitlestracks && mediaInfo.subtitlestracks.map((subtitles, index) =>
        <MediaInfoSubtitles key={index} i18n={i18n} subtitles={subtitles} />,
      )}
      <MediaInfoImage i18n={i18n} image={mediaInfo.image} title={i18n.get('Image')} />
      <MediaInfoImage i18n={i18n} image={mediaInfo.thumbnail} title={i18n.get('Thumbnail')} />
    </Stack>
  )
}
