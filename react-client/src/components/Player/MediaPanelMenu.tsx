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
import { Button, Tooltip } from '@mantine/core'
import { IconCast, IconDownload, IconPlayerPlay, IconPlaylistAdd } from '@tabler/icons-react'
import { useState } from 'react'

import { I18nInterface } from '../../services/i18n-service'
import { PlayerEventInterface } from '../../services/player-server-event-service'
import { BaseBrowse, PlayMedia, VideoMedia } from '../../services/player-service'
import { playerApiUrl } from '../../utils'
import MediaEditButton from './MediaEditButton'
import VideoMetadataEditModal from './VideoMetadataEditModal'

export default function MediaPanelMenu({ i18n, sse, data, refreshPage, setLoading }: { i18n: I18nInterface, sse: PlayerEventInterface, data: BaseBrowse, refreshPage: () => void, setLoading: (loading: boolean) => void }) {
  const [showVideoMetadataEdit, setShowVideoMetadataEdit] = useState(false)
  const playMedia = data.goal === 'show' ? (data.medias[0]) as PlayMedia : undefined
  const videoMedia = playMedia && playMedia.mediaType === 'video' ? playMedia as VideoMedia : undefined
  const isVideoMetadataEditable = (data.goal === 'browse' && data.metadata?.isEditable) || (videoMedia && videoMedia.metadata?.isEditable)
  const fullyplayed = (playMedia && playMedia.fullyplayed != null) ? playMedia.fullyplayed : data.fullyplayed
  const hasFullyplayedValue = fullyplayed != null
  const hasEditMenu = isVideoMetadataEditable || hasFullyplayedValue

  return playMedia
    ? (
        <Button.Group>
          <Button variant="default" size="compact-md" leftSection={<IconPlayerPlay size={14} />} onClick={() => sse.askPlayId(data.medias[0].id)}>{i18n.get('Play')}</Button>
          {isVideoMetadataEditable
            && <VideoMetadataEditModal i18n={i18n} uuid={sse.uuid} id={sse.reqId} start={showVideoMetadataEdit} started={() => setShowVideoMetadataEdit(false)} callback={() => refreshPage()} />}
          {hasEditMenu
            && <MediaEditButton i18n={i18n} sse={sse} fullyplayed={fullyplayed} videoMetadataEditable={isVideoMetadataEditable} refreshPage={refreshPage} setLoading={setLoading} setShowVideoMetadataEdit={setShowVideoMetadataEdit} />}
          {data.useWebControl && (
            <Tooltip withinPortal label={i18n.get('PlayOnAnotherRenderer')}>
              <Button variant="default" disabled size="compact-md" onClick={() => { }}><IconCast size={14} /></Button>
            </Tooltip>
          )}
          <Tooltip withinPortal label={i18n.get('AddToPlaylist')}>
            <Button variant="default" disabled size="compact-md" onClick={() => { }}><IconPlaylistAdd size={14} /></Button>
          </Tooltip>
          {playMedia.isDownload && (
            <Tooltip withinPortal label={i18n.get('Download')}>
              <Button variant="default" size="compact-md" onClick={() => window.open(playerApiUrl + 'download/' + sse.uuid + '/' + data.medias[0].id, '_blank')}><IconDownload size={14} /></Button>
            </Tooltip>
          )}
        </Button.Group>

      )
    : hasEditMenu
      ? (
          <Button.Group>
            {isVideoMetadataEditable
              && <VideoMetadataEditModal i18n={i18n} uuid={sse.uuid} id={sse.reqId} start={showVideoMetadataEdit} started={() => setShowVideoMetadataEdit(false)} callback={() => refreshPage()} />}
            <MediaEditButton i18n={i18n} sse={sse} fullyplayed={fullyplayed} videoMetadataEditable={isVideoMetadataEditable} refreshPage={refreshPage} setLoading={setLoading} setShowVideoMetadataEdit={setShowVideoMetadataEdit} />
          </Button.Group>
        )
      : undefined
}
