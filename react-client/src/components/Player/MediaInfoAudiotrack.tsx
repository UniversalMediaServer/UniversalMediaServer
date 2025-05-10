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
import { Box, Button, Collapse, Group } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'

import { I18nInterface } from '../../services/i18n-service'
import { AudioTrackInfo } from '../../services/player-service'
import MediaInfoBooleanValue from './MediaInfoBooleanValue'
import MediaInfoNumberValue from './MediaInfoNumberValue'
import MediaInfoStringValue from './MediaInfoStringValue'

export default function MediaInfoAudiotrack({ i18n, audioTrack }: { i18n: I18nInterface, audioTrack: AudioTrackInfo }) {
  const [opened, { toggle }] = useDisclosure(false)

  const Title = ({ i18n, audioTrack }: { i18n: I18nInterface, audioTrack: AudioTrackInfo }) => {
    return i18n.get('AudioStream') + ' #' + audioTrack.id
  }

  return audioTrack && (
    <Box>
      <Group>
        <Button variant="default" size="compact-xs" onClick={toggle}><Title i18n={i18n} audioTrack={audioTrack} /></Button>
      </Group>

      <Collapse in={opened}>
        <MediaInfoBooleanValue i18n={i18n} value={audioTrack.default} title={i18n.get('Default')} />
        <MediaInfoBooleanValue i18n={i18n} value={audioTrack.forced} title={i18n.get('Forced')} />
        <MediaInfoStringValue value={audioTrack.title} title={i18n.get('Title')} />
        <MediaInfoStringValue value={audioTrack.lang} title={i18n.get('Language')} />
        <MediaInfoNumberValue value={audioTrack.stream} title="File Stream" />
        <MediaInfoStringValue value={audioTrack.codec} title="Codec" />
        <MediaInfoNumberValue value={audioTrack.bitrate} title="BitRate" />
        <MediaInfoNumberValue value={audioTrack.bitdepth} title="BitDepth" />
        <MediaInfoNumberValue value={audioTrack.channel} title="Number Of Channels" />
        <MediaInfoNumberValue value={audioTrack.samplerate} title="SampleRate" />
        <MediaInfoStringValue value={audioTrack.muxing} title="Muxing" />
      </Collapse>
    </Box>
  )
}
