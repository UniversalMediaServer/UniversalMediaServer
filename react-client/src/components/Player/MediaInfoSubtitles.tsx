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
import { SubtitlesInfo } from '../../services/player-service'
import MediaInfoBooleanValue from './MediaInfoBooleanValue'
import MediaInfoNumberValue from './MediaInfoNumberValue'
import MediaInfoStringValue from './MediaInfoStringValue'

export default function MediaInfoSubtitles({ i18n, subtitles }: { i18n: I18nInterface, subtitles: SubtitlesInfo }) {
  const [opened, { toggle }] = useDisclosure(false)

  const Title = ({ i18n, subtitles }: { i18n: I18nInterface, subtitles: SubtitlesInfo }) => {
    return subtitles.embedded
      ? i18n.get('Subtitles') + ' #' + subtitles.id
      : i18n.get('Subtitles') + ' #' + subtitles.externalFile
  }

  return subtitles && (
    <Box>
      <Group>
        <Button variant="default" size="compact-xs" onClick={toggle}><Title i18n={i18n} subtitles={subtitles} /></Button>
      </Group>

      <Collapse in={opened}>
        <MediaInfoBooleanValue i18n={i18n} value={subtitles.embedded} title={i18n.get('Embedded')} />
        <MediaInfoBooleanValue i18n={i18n} value={subtitles.default} title={i18n.get('Default')} />
        <MediaInfoBooleanValue i18n={i18n} value={subtitles.forced} title={i18n.get('Forced')} />
        <MediaInfoStringValue value={subtitles.title} title={i18n.get('Title')} />
        <MediaInfoStringValue value={subtitles.lang} title={i18n.get('Language')} />
        <MediaInfoNumberValue value={subtitles.stream} title="File Stream" />
        <MediaInfoStringValue value={subtitles.type} title="Type" />
        <MediaInfoStringValue value={subtitles.externalFile} title="External File" />
        <MediaInfoStringValue value={subtitles.subsCharacterSet} title="Character Set" />
        <MediaInfoStringValue value={subtitles.convertedFile} title="Converted File" />
      </Collapse>
    </Box>
  )
}
