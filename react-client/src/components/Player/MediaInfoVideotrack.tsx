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
import { VideoTrackInfo } from '../../services/player-service'
import MediaInfoNumberValue from './MediaInfoNumberValue'
import MediaInfoStringValue from './MediaInfoStringValue'
import MediaInfoBooleanValue from './MediaInfoBooleanValue'

export default function MediaInfoVideotrack({ i18n, videoTrack }: { i18n: I18nInterface, videoTrack: VideoTrackInfo }) {
  const [opened, { toggle }] = useDisclosure(false)

  const Title = ({ i18n, videoTrack }: { i18n: I18nInterface, videoTrack: VideoTrackInfo }) => {
    return i18n.get('VideoStream') + ' #' + videoTrack.id
  }

  return videoTrack && (
    <Box>
      <Group>
        <Button variant="default" size="compact-xs" onClick={toggle}><Title i18n={i18n} videoTrack={videoTrack} /></Button>
      </Group>

      <Collapse in={opened}>
        <MediaInfoStringValue value={videoTrack.title} title={i18n.get('Title')} />
        <MediaInfoStringValue value={videoTrack.lang} title={i18n.get('Language')} />
        <MediaInfoBooleanValue i18n={i18n} value={videoTrack.default} title={i18n.get('Default')} />
        <MediaInfoBooleanValue i18n={i18n} value={videoTrack.forced} title={i18n.get('Forced')} />
        <MediaInfoStringValue value={videoTrack.resolution} title="Resolution" />
        <MediaInfoStringValue value={videoTrack.duration} title="Duration" />
        <MediaInfoStringValue value={videoTrack.codec} title={i18n.get('Codec')} />
        <MediaInfoStringValue value={videoTrack.formatProfile} title="Format Profile" />
        <MediaInfoStringValue value={videoTrack.formatLevel} title="Format Level" />
        <MediaInfoStringValue value={videoTrack.formatTier} title="Format Tier" />
        <MediaInfoNumberValue value={videoTrack.stream} title="File Stream" />
        <MediaInfoNumberValue value={videoTrack.bitDepth} title="Bit Depth" />
        <MediaInfoStringValue value={videoTrack.displayaspectratio} title="Display Aspect ratio" />
        <MediaInfoNumberValue value={videoTrack.pixelaspectratio} title="Pixel Aspect ratio" />
        <MediaInfoStringValue value={videoTrack.scantype} title="Scan type" />
        <MediaInfoStringValue value={videoTrack.scanOrder} title="Scan order" />
        <MediaInfoNumberValue value={videoTrack.framerate} title="Framerate" />
        <MediaInfoStringValue value={videoTrack.framerateMode} title="Framerate mode" />
        <MediaInfoStringValue value={videoTrack.framerateModeRaw} title="Framerate mode raw" />
        <MediaInfoStringValue value={videoTrack.muxingMode} title="Muxing mode" />
        <MediaInfoStringValue value={videoTrack.matrixCoefficients} title="Matrix Coefficients" />
        <MediaInfoNumberValue value={videoTrack.referenceFrameCount} title="Reference Frame Count" />
        <MediaInfoStringValue value={videoTrack.hdrFormat} title="HDR Format" />
        <MediaInfoStringValue value={videoTrack.hdrFormatRenderer} title="HDR Format (renderer)" />
        <MediaInfoStringValue value={videoTrack.hdrFormatCompatibility} title="HDR Compatibility Format" />
        <MediaInfoStringValue value={videoTrack.hdrFormatCompatibilityRenderer} title="HDR Compatibility Format (renderer)" />
      </Collapse>
    </Box>
  )
}
