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
import { BaseBrowse, PlayerInterface, PlayMedia, VideoMedia } from '../../services/player-service'
import { playerApiUrl } from '../../utils'
import MediaEditButton from './MediaEditButton'
import MediaInfoModal from './MediaInfoModal'
import VideoMetadataEditModal from './VideoMetadataEditModal'

export default function MediaPanelMenu({ i18n, player, data, refreshPage, setLoading }: { i18n: I18nInterface, player: PlayerInterface, data: BaseBrowse, refreshPage: () => void, setLoading: (loading: boolean) => void }) {
  const [showVideoMetadataEdit, setShowVideoMetadataEdit] = useState(false)
  const playMedia = data.goal === 'show' ? (data.medias[0]) as PlayMedia : undefined
  const videoMedia = playMedia && playMedia.mediaType === 'video' ? playMedia as VideoMedia : undefined
  const isVideoMetadataEditable = (data.goal === 'browse' && data.metadata?.isEditable) || (videoMedia && videoMedia.metadata?.isEditable)
  const fullyplayed = (playMedia && playMedia.fullyplayed != null) ? playMedia.fullyplayed : data.fullyplayed
  const hasMediaInfo = (playMedia && playMedia.hasMediaInfo)
  const hasFullyplayedValue = fullyplayed != null
  const hasEditMenu = isVideoMetadataEditable || hasFullyplayedValue

  return playMedia
    ? (
        <Button.Group>
          <Button variant="default" size="compact-md" leftSection={<IconPlayerPlay size={14} />} onClick={() => player.askPlayId(data.medias[0].id)}>{i18n.get('Play')}</Button>
          {hasMediaInfo
            && <MediaInfoModal i18n={i18n} uuid={player.uuid} id={player.reqId} />}
          {isVideoMetadataEditable
            && <VideoMetadataEditModal i18n={i18n} uuid={player.uuid} id={player.reqId} start={showVideoMetadataEdit} started={() => setShowVideoMetadataEdit(false)} callback={() => refreshPage()} />}
          {hasEditMenu
            && <MediaEditButton i18n={i18n} player={player} fullyplayed={fullyplayed} videoMetadataEditable={isVideoMetadataEditable} refreshPage={refreshPage} setLoading={setLoading} setShowVideoMetadataEdit={setShowVideoMetadataEdit} />}
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
              <Button variant="default" size="compact-md" onClick={() => window.open(playerApiUrl + 'download/' + player.uuid + '/' + data.medias[0].id, '_blank')}><IconDownload size={14} /></Button>
            </Tooltip>
          )}
        </Button.Group>

      )
    : hasEditMenu
      ? (
          <Button.Group>
            {isVideoMetadataEditable
              && <VideoMetadataEditModal i18n={i18n} uuid={player.uuid} id={player.reqId} start={showVideoMetadataEdit} started={() => setShowVideoMetadataEdit(false)} callback={() => refreshPage()} />}
            <MediaEditButton i18n={i18n} player={player} fullyplayed={fullyplayed} videoMetadataEditable={isVideoMetadataEditable} refreshPage={refreshPage} setLoading={setLoading} setShowVideoMetadataEdit={setShowVideoMetadataEdit} />
          </Button.Group>
        )
      : undefined
}
